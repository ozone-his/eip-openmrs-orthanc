/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class OpenmrsDiagnosticReportHandler {

    @Autowired
    private OpenmrsConfig openmrsConfig;

    @Autowired
    private OpenmrsRestDeleteHelper deleteHelper;

    @Value("${openmrs.baseUrl}")
    private String openmrsBaseUrl;

    /**
     * Creates a plain Observation carrying a result/note for a patient,
     * via OpenMRS's native REST API - the same simple pattern already
     * proven by saveAttachment(). Confirmed via live testing that this
     * requires no Encounter and displays correctly in O3's Results tab,
     * replacing the old createDiagnosticReport() + Encounter + FHIR
     * DiagnosticReport chain, whose conclusion/presentedForm/result
     * fields never persisted in OpenMRS anyway.
     *
     * @return the new Observation's UUID, or null if creation failed
     */
    public String saveResult(
            ProducerTemplate producerTemplate,
            String patientUUID,
            String conceptUUID,
            String value,
            String obsDate) throws JsonProcessingException {

        String effectiveDate = obsDate != null && obsDate.length() == 8
                ? obsDate.substring(0, 4) + "-" + obsDate.substring(4, 6) + "-" + obsDate.substring(6, 8)
                : java.time.LocalDate.now().toString();

        String obsJson = String.format(
                "{\"person\":\"%s\"," +
                "\"concept\":\"%s\"," +
                "\"value\":\"%s\"," +
                "\"obsDatetime\":\"%s\"}",
                patientUUID,
                conceptUUID,
                value.replace("\"", "\\\"").replace("\n", "\\n"),
                effectiveDate);

        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.CAMEL_HTTP_METHOD, Constants.POST);
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(Constants.AUTHORIZATION, openmrsConfig.authHeader());

        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-create-obs-native-route", obsJson, headers, String.class);

        String obsUUID = null;
        try {
            Map<?, ?> result = new ObjectMapper().readValue(response, Map.class);
            obsUUID = (String) result.get("uuid");
            log.info("saveResult: created Observation {} for patient {}", obsUUID, patientUUID);
        } catch (Exception e) {
            log.warn("saveResult: could not parse Observation UUID from response: {}", e.getMessage());
        }
        return obsUUID;
    }

    /**
     * Records SR (Structured Report) content as a result Observation for a
     * patient. Simplified from the original design, which tried to find and
     * update an existing Observation linked via DiagnosticReport.result -
     * that field never persists in OpenMRS, so the lookup always failed and
     * a new Observation was always created anyway (see ProcessedSRRepository
     * for the separate mechanism that now prevents duplicate processing of
     * the same SR instance). This goes straight to creating the result,
     * using the exact ordered procedure's concept when resolved via
     * AccessionNumber, falling back to the generic note concept otherwise.
     */
    public String updateDiagnosticReportWithSR(
            ProducerTemplate producerTemplate,
            String patientUUID,
            String srText,
            String procedureConceptUuid) throws JsonProcessingException {

        String observationConceptUuid = (procedureConceptUuid != null && !procedureConceptUuid.isEmpty())
                ? procedureConceptUuid : Constants.GENERAL_PATIENT_NOTE_CONCEPT_UUID;

        String resultUUID = saveResult(
                producerTemplate, patientUUID, observationConceptUuid, srText, null);

        log.info("updateDiagnosticReportWithSR: recorded SR result {} for patient {}", resultUUID, patientUUID);
        return resultUUID;
    }

    /**
     * Deletes a result Observation created by saveResult(), using the shared
     * purge=true delete pattern (matching the pattern already proven
     * necessary for attachments in OpenmrsAttachmentHandler.deleteAttachment()) -
     * a plain DELETE only voids the record without actually removing it
     * (confirmed via live testing: GET still returns 200 with full data
     * after a non-purge delete).
     */
    public boolean deleteResult(String observationUUID) throws java.io.IOException {
        return deleteHelper.deleteByUuid("obs", observationUUID, "result Observation");
    }
}
