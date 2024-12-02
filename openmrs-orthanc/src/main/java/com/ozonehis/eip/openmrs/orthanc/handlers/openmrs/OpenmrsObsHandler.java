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
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.openmrs.Obs;
import org.springframework.stereotype.Component;

@Component
public class OpenmrsObsHandler {

    public Obs[] getObsByPatientIDAndConceptID(ProducerTemplate producerTemplate, String patientID, String conceptID)
            throws JsonProcessingException {
        // Concept ID 7cac8397-53cd-4f00-a6fe-028e8d743f8e
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_OPENMRS_PATIENT_ID, patientID);
        headers.put(Constants.HEADER_OPENMRS_OBS_CONCEPT_ID, conceptID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:orthanc-get-openmrs-obs-route", null, headers, String.class);

        return new ObjectMapper().readValue(response, Obs[].class);
    }
}
