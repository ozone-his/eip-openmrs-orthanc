/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import groovy.util.logging.Slf4j;
import java.io.IOException;
import java.util.Base64;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenmrsAttachmentHandler {

    private static final String ATTACHMENT_FHIR_ENDPOINT = "http://openmrs:8080/openmrs/ws/rest/v1/attachment";

    private static final String ORTHANC_VIEWER_BASE_URL = "http://localhost:8889/stone-webviewer/index.html?study=";

    private static final Logger log = LoggerFactory.getLogger(OpenmrsAttachmentHandler.class);

    public void saveAttachment(byte[] binaryData, String patientID, String studyID) throws IOException {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString("admin:Admin123".getBytes());
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileCaption", ORTHANC_VIEWER_BASE_URL + studyID)
                .addFormDataPart("patient", patientID)
                .addFormDataPart("file", studyID, RequestBody.create(binaryData, MediaType.parse("image/png")))
                .build();

        Request request = new Request.Builder()
                .url(ATTACHMENT_FHIR_ENDPOINT)
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .post(requestBody)
                .build();

        log.info("Request: {} patientID {} studyID {}", request.body(), patientID, studyID);

        OkHttpClient client = new OkHttpClient();
        for (int i = 0; i < 10; i++) {
            if (apiCall(client, request)) {
                break;
            }
        }
    }

    private boolean apiCall(OkHttpClient client, Request request) throws IOException {
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            log.info("POST request successful! Response: {}", response.body().string());
            return true;
        } else {
            log.error(
                    "POST request failed: {} error {}",
                    response.code(),
                    response.body().string());
            return false;
        }
    }
}
