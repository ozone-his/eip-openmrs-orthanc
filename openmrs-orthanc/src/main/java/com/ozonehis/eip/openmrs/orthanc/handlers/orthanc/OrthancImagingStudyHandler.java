/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.orthanc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.config.OrthancConfig;
import com.ozonehis.eip.openmrs.orthanc.models.series.Series;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrthancImagingStudyHandler {

    @Autowired
    private OrthancConfig orthancConfig;

    public byte[] fetchStudyBinaryData(String studyUrl) throws IOException {
        Request request = new Request.Builder()
                .header("Authorization", orthancConfig.authHeader())
                .url(studyUrl)
                .build();

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to fetch binary data: {}", response.code());
                return null;
            }
            return Objects.requireNonNull(response.body()).bytes();
        }
    }

    public Series getSeriesByID(ProducerTemplate producerTemplate, String seriesID) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERIES_ID, seriesID);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:orthanc-get-series-route", null, headers, String.class);
        Series series = new ObjectMapper().readValue(response, Series.class);
        log.debug("Fetch series {}", series);
        return series;
    }
}
