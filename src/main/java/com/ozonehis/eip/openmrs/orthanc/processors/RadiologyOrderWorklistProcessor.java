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
import com.ozonehis.eip.openmrs.orthanc.handlers.orthanc.OrthancWorklistHandler;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class RadiologyOrderWorklistProcessor implements Processor {

    @Autowired
    private OrthancWorklistHandler orthancWorklistHandler;

    @Value("${openmrs.baseUrl:http://openmrs:8080/openmrs}")
    private String openmrsBaseUrl;

    @Value("${openmrs.username:admin}")
    private String openmrsUsername;

    @Value("${openmrs.password:Admin123}")
    private String openmrsPassword;

    // Radiology concept UUIDs - whitelist for worklist creation
    private static final Set<String> RADIOLOGY_CONCEPT_UUIDS = new HashSet<>(java.util.Arrays.asList(
        "e3dea2c8-62c6-4487-bdaa-1d009642f7ad", // RX01 - Chest X-ray
        "82e7d36c-078d-40c6-9854-92b376099307", // RX02 - Abdominal X-ray
        "701257a2-885e-4249-8319-d9597d2970af", // RX03 - Bone X-ray
        "b25dcc00-800f-48ac-b31a-f1e9cc53d787", // RX04 - Intravenous urography
        "81e0643c-a871-475e-8bd5-93945da8877d", // RX05 - Salpingo-urethrogram
        "1a5e3d73-f897-47ed-840b-d4537b7cc586", // RX06 - Barium enema
        "0a5ba175-fb7e-4d66-aa6a-ba058f3468c1", // RX07 - CT scan
        "d0b5d4a0-1001-0000-0000-000000000001", // Chest X-Ray
        "d0b5d4a0-1002-0000-0000-000000000001", // Abdominal X-Ray
        "d0b5d4a0-1003-0000-0000-000000000001", // Hand X-Ray
        "d0b5d4a0-1004-0000-0000-000000000001", // Foot X-Ray
        "d0b5d4a0-1005-0000-0000-000000000001", // Knee X-Ray
        "d0b5d4a0-1006-0000-0000-000000000001", // Spine X-Ray
        "d0b5d4a0-1007-0000-0000-000000000001", // Pelvis X-Ray
        "d0b5d4a0-1008-0000-0000-000000000001"  // Skull X-Ray
    ));

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private Instant lastPollTime = Instant.now().minusSeconds(3600);

    @Autowired
    private com.ozonehis.eip.openmrs.orthanc.repository.ProcessedRadiologyOrderRepository processedRadiologyOrderRepository;

    @Override
    public void process(Exchange exchange) throws Exception {
        // Format lastPollTime as FHIR date
        String since = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(lastPollTime);

        log.debug("Polling for radiology ServiceRequests since {}", since);

        try {
            // Search for active ServiceRequests with imaging category
            String url = openmrsBaseUrl + "/ws/fhir2/R4/ServiceRequest" +
                "?_sort=-_lastUpdated&_count=100";

            JsonNode bundle = fetchFhir(url);
            if (bundle == null) return;

            log.info("Found {} total ServiceRequests in bundle", bundle.path("entry").size());
            int count = 0;
            for (JsonNode entry : bundle.path("entry")) {
                JsonNode sr = entry.path("resource");
                String srId = sr.path("id").asText("");
                String authored = sr.path("authoredOn").asText("");

                log.info("Checking SR id={} status={} code_codings_size={} coding_isArray={}", 
                    srId, sr.path("status").asText(), 
                    sr.path("code").path("coding").size(),
                    sr.path("code").path("coding").isArray());
                if (processedRadiologyOrderRepository.exists(srId)) { log.info("Already processed {}", srId); continue; }

                // Only process active orders
                String status = sr.path("status").asText("");
                if (!"active".equals(status)) { log.info("Skipping non-active SR {} status={}", srId, status); continue; }

                // Filter by concept UUID whitelist
                boolean isRadiology = false;
                for (JsonNode coding : sr.path("code").path("coding")) {
                    String conceptCode = coding.path("code").asText("");
                    log.info("SR {} concept code: {}", srId.substring(0,8), conceptCode);
                    if (RADIOLOGY_CONCEPT_UUIDS.contains(conceptCode)) {
                        isRadiology = true;
                        break;
                    }
                }
                if (!isRadiology) {
                    log.info("Skipping non-radiology SR {}", srId.substring(0,8));
                    continue;
                }
                // Get patient info
                String patientRef = sr.path("subject").path("reference").asText("");
                String patientUuid = patientRef.contains("/") ? patientRef.split("/")[1] : patientRef;
                JsonNode patient = fetchFhir(openmrsBaseUrl + "/ws/fhir2/R4/Patient/" + patientUuid);
                if (patient == null) continue;

                // Patient identifier
                String patientId = patientUuid;
                for (JsonNode id : patient.path("identifier")) {
                    String val = id.path("value").asText("");
                    if (!val.isEmpty() && !val.equals(patientUuid)) {
                        patientId = val;
                        break;
                    }
                }

                // Patient name
                String family = patient.path("name").path(0).path("family").asText("");
                String given = patient.path("name").path(0).path("given").path(0).asText("");
                String dicomName = family + "^" + given;

                // Procedure
                String procedureDesc = sr.path("code").path("text").asText("Radiology");
                String accessionNumber = srId.replace("-", "").substring(0, 12).toUpperCase();

                // Modality
                String modality = "XR";
                String desc = procedureDesc.toLowerCase();
                if (desc.contains("ct") || desc.contains("scan")) modality = "CT";
                else if (desc.contains("ultrasound") || desc.contains("echo")) modality = "US";
                else if (desc.contains("mri") || desc.contains("mr ")) modality = "MR";

                // Check for a payment-confirmation Task (created by eip-odoo-openmrs's
                // RadiologyPaymentTaskProcessor once Odoo payment is confirmed) - only
                // create the worklist entry once one exists with status=completed.
                // A missing Task means payment isn't confirmed yet; a Task with status
                // entered-in-error means the payment check itself failed and should
                // never be treated as a green light - both cases correctly skip here.
                if (!isPaymentConfirmedViaTask(srId)) {
                    log.info("No completed payment Task found for ServiceRequest {} (patient {}, procedure '{}'), skipping worklist",
                        srId, patientUuid, procedureDesc);
                    continue;
                }

                try {
                    orthancWorklistHandler.createWorklistEntry(
                        patientId, dicomName, accessionNumber, procedureDesc, modality);
                    processedRadiologyOrderRepository.save(srId, accessionNumber);
                    count++;
                } catch (Exception e) {
                    log.error("Failed to create worklist for order {}: {}", srId, e.getMessage());
                }
            }

            if (count > 0) {
                log.info("Created {} Orthanc worklist entries from radiology orders", count);
            }
            lastPollTime = Instant.now();

        } catch (Exception e) {
            log.error("Error polling radiology orders: {}", e.getMessage());
        }
    }

    private boolean isPaymentConfirmedViaTask(String serviceRequestId) {
    JsonNode bundle = fetchFhir(openmrsBaseUrl + "/ws/fhir2/R4/Task?based-on=" + serviceRequestId);
    if (bundle == null) {
        return false;
    }
    for (JsonNode entry : bundle.path("entry")) {
        JsonNode task = entry.path("resource");
        if ("accepted".equals(task.path("status").asText(""))) {
            return true;
        }
    }
        return false;
    }

    private JsonNode fetchFhir(String url) {
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString((openmrsUsername + ":" + openmrsPassword).getBytes()))
                .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                return mapper.readTree(response.body().string());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }
}
