/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.orthanc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.Utils;
import com.ozonehis.eip.openmrs.orthanc.config.OrthancConfig;
import com.ozonehis.eip.openmrs.orthanc.models.series.Series;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OrthancImagingStudyHandlerTest {

    private static final String SERIES_ID = "17cc7e52-4f1a3e4d-9182f727-56e9cc71-c037892f";

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private OrthancConfig orthancConfig;

    @InjectMocks
    private OrthancImagingStudyHandler orthancImagingStudyHandler;

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
    void shouldReturnSeriesByID() throws JsonProcessingException {
        // Setup
        String responseBody = new Utils().readJSON("response/orthanc-single-series-response.json");
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERIES_ID, SERIES_ID);

        // Mock
        when(producerTemplate.requestBodyAndHeaders(
                        eq("direct:orthanc-get-series-route"), isNull(), eq(headers), eq(String.class)))
                .thenReturn(responseBody);

        // Act
        Series result = orthancImagingStudyHandler.getSeriesByID(producerTemplate, SERIES_ID);

        // Verify
        assertEquals(SERIES_ID, result.getId());
        assertFalse(result.getInstances().isEmpty());
    }
}
