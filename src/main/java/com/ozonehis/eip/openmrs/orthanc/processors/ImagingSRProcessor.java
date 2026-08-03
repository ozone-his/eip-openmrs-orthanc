/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.processors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import com.ozonehis.eip.openmrs.orthanc.config.OrthancTokenProvider;
import com.ozonehis.eip.openmrs.orthanc.config.CursorStore;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsDiagnosticReportHandler;
import com.ozonehis.eip.openmrs.orthanc.repository.ProcessedStudyRepository;
import com.ozonehis.eip.openmrs.orthanc.repository.ProcessedSRRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Setter
@Component
public class ImagingSRProcessor implements Processor {

    @Autowired
    private OpenmrsDiagnosticReportHandler openmrsDiagnosticReportHandler;

    @Autowired
    private ProcessedStudyRepository processedStudyRepository;
    @Autowired
    private ProcessedSRRepository processedSRRepository;

    @Autowired
    private OpenmrsConfig openmrsConfig;

    @Autowired
    private OrthancTokenProvider orthancTokenProvider;

    @Autowired
    private CursorStore cursorStore;

    @Value("${orthanc.baseUrl:http://orthanc:8042}")
    private String orthancBaseUrl;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            String body = exchange.getMessage().getBody(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            JsonNode changes = root.get("Changes");
            long lastSeq = root.get("Last").asLong();

            if (changes == null || changes.isEmpty()) {
                exchange.getMessage().setHeader(Constants.HEADER_STUDIES_SINCE, lastSeq);
                cursorStore.setSrChangesCursor(lastSeq);
                return;
            }

            log.info("SR processor: processing {} changes, last={}", changes.size(), lastSeq);
            for (JsonNode change : changes) {
                String changeType = change.get("ChangeType").asText();
                String resourceType = change.get("ResourceType").asText();
                String instanceId = change.get("ID").asText();

                // Only process new SR instances
                if (!"Instance".equals(resourceType) || !"NewInstance".equals(changeType)) {
                    continue;
                }
                log.info("SR processor: checking instance {} for SR modality", instanceId);

                // Check if this instance is an SR
                handlePotentialSR(producerTemplate, mapper, instanceId);
            }

            exchange.getMessage().setHeader(Constants.HEADER_STUDIES_SINCE, lastSeq);
            cursorStore.setSrChangesCursor(lastSeq);
        } catch (Exception e) {
            throw new EIPException(
                    String.format("Error processing SR changes: %s", e.getMessage()));
        }
    }

    private void handlePotentialSR(ProducerTemplate producerTemplate, ObjectMapper mapper, String instanceId) {
        if (processedSRRepository.exists(instanceId)) {
            log.debug("SR instance {} already processed, skipping", instanceId);
            return;
        }
        try {
            // Fetch instance metadata from Orthanc
            Map<String, Object> headers = new HashMap<>();
            headers.put(Constants.CAMEL_HTTP_METHOD, Constants.GET);
            headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
            headers.put("token", orthancTokenProvider.getToken());

            String instanceJson = producerTemplate.requestBodyAndHeaders(
                    "direct:orthanc-get-instance-route",
                    null,
                    Map.of(
                        Constants.CAMEL_HTTP_METHOD, Constants.GET,
                        Constants.CONTENT_TYPE, Constants.APPLICATION_JSON,
                        "token", orthancTokenProvider.getToken(),
                        "orthanc.instance.id", instanceId
                    ),
                    String.class);

            JsonNode instance = mapper.readTree(instanceJson);
            if (instance == null) return;

            // Modality is at series level, not instance level - get parent series
            String parentSeries = instance.has("ParentSeries") ? instance.get("ParentSeries").asText() : null;
            if (parentSeries == null) return;

            // Fetch series to check modality
            String seriesJson = producerTemplate.requestBodyAndHeaders(
                    "direct:orthanc-get-series-by-id-route",
                    null,
                    Map.of(
                        Constants.CAMEL_HTTP_METHOD, Constants.GET,
                        Constants.CONTENT_TYPE, Constants.APPLICATION_JSON,
                        "token", orthancTokenProvider.getToken(),
                        "orthanc.series.id", parentSeries
                    ),
                    String.class);

            JsonNode series = mapper.readTree(seriesJson);
            JsonNode seriesDicomTags = series.has("MainDicomTags") ? series.get("MainDicomTags") : null;
            String modality = seriesDicomTags != null && seriesDicomTags.has("Modality")
                ? seriesDicomTags.get("Modality").asText() : "";

            if (!"SR".equals(modality)) return;

            log.info("SR instance detected: {}", instanceId);

            // Get parent study ID from series
            String parentStudy = series.has("ParentStudy") ? series.get("ParentStudy").asText() : null;
            if (parentStudy == null) return;

            // Check if we have a DiagnosticReport for this study
            String[] patientAndReport = processedStudyRepository.findByOrthancStudyId(parentStudy);
            if (patientAndReport == null) {
                log.warn("No processed study found for SR parent study {}", parentStudy);
                return;
            }

            String patientUUID = patientAndReport[0];
            String reportUUID = patientAndReport[1];

            // Parse SR content
            String srText = parseSRContent(mapper, seriesDicomTags, series);

            // Resolve which specific radiology order this SR belongs to, via
            // the study's AccessionNumber (set by RadiologyOrderWorklistProcessor
            // as the first 12 hex chars of the ServiceRequest UUID, and echoed
            // back by the modality on the resulting study). This lets us use
            // the ACTUAL ordered procedure's concept for the Observation
            // instead of a generic placeholder - important when a patient has
            // multiple simultaneous radiology orders, where "most recent" is
            // not a reliable way to tell them apart.
            String procedureConceptUuid = resolveProcedureConceptFromAccessionNumber(
                    producerTemplate, mapper, parentStudy, patientUUID);

            // Record the SR result as a new Observation
            openmrsDiagnosticReportHandler.updateDiagnosticReportWithSR(
                    producerTemplate, patientUUID, srText, procedureConceptUuid);

            processedSRRepository.save(instanceId);


        } catch (Exception e) {
            log.error("Error handling potential SR instance {}: {}", instanceId, e.getMessage());
        }
    }

    // Radiology concept UUIDs - same whitelist used when creating worklist
    // entries in RadiologyOrderWorklistProcessor.
    private static final Set<String> RADIOLOGY_CONCEPT_UUIDS = new HashSet<>(java.util.Arrays.asList(
        "e3dea2c8-62c6-4487-bdaa-1d009642f7ad", // RX01 - Chest X-ray
        "82e7d36c-078d-40c6-9854-92b376099307", // RX02 - Abdominal X-ray
        "701257a2-885e-4249-8319-d9597d2970af", // RX03 - Bone X-ray
        "b25dcc00-800f-48ac-b31a-f1e9cc53d787", // RX04 - Intravenous urography
        "81e0643c-a871-475e-8bd5-93945da8877d", // RX05 - Salpingo-urethrogram
        "1a5e3d73-f897-47ed-840b-d4537b7cc586", // RX06 - Barium enema
        "0a5ba175-fb7e-4d66-aa6a-ba058f3468c1", // RX07 - CT scan
        "d0b5d4a0-1001-0000-0000-000000000001",
        "d0b5d4a0-1002-0000-0000-000000000001",
        "d0b5d4a0-1003-0000-0000-000000000001",
        "d0b5d4a0-1004-0000-0000-000000000001",
        "d0b5d4a0-1005-0000-0000-000000000001",
        "d0b5d4a0-1006-0000-0000-000000000001",
        "d0b5d4a0-1007-0000-0000-000000000001",
        "d0b5d4a0-1008-0000-0000-000000000001"
    ));

    /**
     * Resolves the exact radiology ServiceRequest (and its concept UUID) that
     * this SR's parent study corresponds to, using AccessionNumber as the
     * unambiguous link: RadiologyOrderWorklistProcessor sets AccessionNumber
     * to the first 12 hex chars of the ordering ServiceRequest's UUID when
     * creating the Orthanc worklist entry, and the modality echoes this back
     * onto the resulting study. Returns null if no AccessionNumber is present
     * or no matching active ServiceRequest is found (falls back to the
     * generic "General patient note" concept at the call site).
     */
    private String resolveProcedureConceptFromAccessionNumber(
            ProducerTemplate producerTemplate, ObjectMapper mapper, String orthancStudyId, String patientUUID) {
        try {
            String studyJson = producerTemplate.requestBodyAndHeaders(
                    "direct:orthanc-get-study-by-id-route",
                    null,
                    Map.of(
                        Constants.CAMEL_HTTP_METHOD, Constants.GET,
                        Constants.CONTENT_TYPE, Constants.APPLICATION_JSON,
                        "token", orthancTokenProvider.getToken(),
                        "orthanc.study.id", orthancStudyId
                    ),
                    String.class);
            JsonNode study = mapper.readTree(studyJson);
            JsonNode studyTags = study.has("MainDicomTags") ? study.get("MainDicomTags") : null;
            String accessionNumber = studyTags != null && studyTags.has("AccessionNumber")
                ? studyTags.get("AccessionNumber").asText("") : "";
            if (accessionNumber.isEmpty()) {
                log.debug("No AccessionNumber on study {} - cannot resolve exact procedure", orthancStudyId);
                return null;
            }

            String url = openmrsConfig.getOpenmrsBaseUrl()
                + "/ws/fhir2/R4/ServiceRequest?patient=" + patientUUID + "&_count=100";
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", openmrsConfig.authHeader())
                .build();
            try (Response response = new OkHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                JsonNode bundle = mapper.readTree(response.body().string());
                for (JsonNode entry : bundle.path("entry")) {
                    JsonNode sr = entry.path("resource");
                    String srId = sr.path("id").asText("");
                    String expectedAccession = srId.replace("-", "").length() >= 12
                        ? srId.replace("-", "").substring(0, 12).toUpperCase() : "";
                    if (!expectedAccession.equalsIgnoreCase(accessionNumber)) continue;
                    for (JsonNode coding : sr.path("code").path("coding")) {
                        String conceptCode = coding.path("code").asText("");
                        if (RADIOLOGY_CONCEPT_UUIDS.contains(conceptCode)) {
                            log.info("Resolved SR to exact procedure concept {} via AccessionNumber {}",
                                conceptCode, accessionNumber);
                            return conceptCode;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve procedure concept via AccessionNumber for study {}: {}",
                orthancStudyId, e.getMessage());
        }
        return null;
    }

    private String parseSRContent(ObjectMapper mapper, JsonNode mainDicomTags, JsonNode instance) {
        // OHIF cannot author full DICOM Structured Reports (no ContentSequence
        // tree with TextValue items - confirmed via live testing). The only
        // report text OHIF actually sets on an SR series is a short title in
        // SeriesDescription (e.g. "Chest AP"). Treat that as the report
        // content directly, rather than parsing a content tree that
        // OHIF-authored SRs never populate.
        if (mainDicomTags.has("SeriesDescription")) {
            String seriesDescription = mainDicomTags.get("SeriesDescription").asText();
            if (seriesDescription != null && !seriesDescription.isBlank()) {
                return "Report: " + seriesDescription;
            }
        }
        if (mainDicomTags.has("StudyDescription")) {
            String studyDescription = mainDicomTags.get("StudyDescription").asText();
            if (studyDescription != null && !studyDescription.isBlank()) {
                return "Report: " + studyDescription;
            }
        }
        return "Radiology report available (SR instance: " + instance.get("ID") + ")";
    }
}
