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
import com.ozonehis.eip.openmrs.orthanc.models.obs.Attachment;
import com.ozonehis.eip.openmrs.orthanc.models.obs.AttachmentObs;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenmrsObsHandler {

    @Autowired
    private OpenmrsConfig openmrsConfig;

    public List<Attachment> getObsByPatientUUIDAndConceptUUID(
            ProducerTemplate producerTemplate, String patientID, String conceptID) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_OPENMRS_PATIENT_UUID, patientID);
        headers.put(Constants.HEADER_OPENMRS_OBS_CONCEPT_UUID, conceptID);
        headers.put(Constants.CAMEL_HTTP_METHOD, Constants.GET);
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(Constants.AUTHORIZATION, openmrsConfig.authHeader());
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:orthanc-get-openmrs-obs-route", null, headers, String.class);
        AttachmentObs results = new ObjectMapper().readValue(response, AttachmentObs.class);
        return results.getResults();
    }
}
