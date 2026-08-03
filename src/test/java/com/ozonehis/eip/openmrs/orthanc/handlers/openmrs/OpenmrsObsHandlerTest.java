/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.Utils;
import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import com.ozonehis.eip.openmrs.orthanc.models.obs.Attachment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OpenmrsObsHandlerTest {

    private static final String PATIENT_UUID = "7a43f897-4aad-4578-8680-e433acaa615d";

    private static final String ATTACHMENT_CONCEPT_UUID = "7cac8397-53cd-4f00-a6fe-028e8d743f8e";

    private static final String OBS_UUID = "c16da8bf-a2ea-4dd7-b1d4-bae2f6bd663b";

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private OpenmrsConfig openmrsConfig;

    @InjectMocks
    private OpenmrsObsHandler openmrsObsHandler;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldReturnObsByPatientUUIDAndConceptUUID() throws JsonProcessingException {
        String basicAuthHeader = "Basic dXNlcjpwYXNzd29yZA=="; // Mocked auth header

        // Setup
        String responseBody = new Utils().readJSON("response/openmrs-obs-response.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_OPENMRS_PATIENT_UUID, PATIENT_UUID);
        headers.put(Constants.HEADER_OPENMRS_OBS_CONCEPT_UUID, ATTACHMENT_CONCEPT_UUID);
        headers.put(Constants.CAMEL_HTTP_METHOD, Constants.GET);
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(Constants.AUTHORIZATION, basicAuthHeader);

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:orthanc-get-openmrs-obs-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);
        when(openmrsConfig.authHeader()).thenReturn(basicAuthHeader);

        // Act
        List<Attachment> result = openmrsObsHandler.getObsByPatientUUIDAndConceptUUID(
                producerTemplate, PATIENT_UUID, ATTACHMENT_CONCEPT_UUID);

        // Verify
        assertEquals(1, result.size());
        assertEquals(OBS_UUID, result.get(0).getUuid());
    }
}
