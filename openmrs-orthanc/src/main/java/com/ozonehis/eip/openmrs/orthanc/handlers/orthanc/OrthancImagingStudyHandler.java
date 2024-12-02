/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.orthanc;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.models.imagingStudy.Study;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrthancImagingStudyHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private IGenericClient orthancFhirClient;

    public byte[] fetchStudyBinaryData(String studyUrl) throws IOException {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString("orthanc:orthanc".getBytes());
        Request request = new Request.Builder()
                .header("Authorization", authHeader)
                .url(studyUrl)
                .build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to fetch binary data: " + response.code());
                return null;
            }
            return Objects.requireNonNull(response.body()).bytes();
        }
    }

    public Study[] getStudiesByLastIndex(ProducerTemplate producerTemplate, String lastStudyIndex)
            throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_STUDIES_SINCE, lastStudyIndex);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:orthanc-get-studies-route", null, headers, String.class);

        return objectMapper.readValue(response, Study[].class);
    }

    public ImagingStudy getImagingStudyByID(String id) {
        ImagingStudy imagingStudy =
                orthancFhirClient.read().resource(ImagingStudy.class).withId(id).execute();
        return imagingStudy;
    }
}
