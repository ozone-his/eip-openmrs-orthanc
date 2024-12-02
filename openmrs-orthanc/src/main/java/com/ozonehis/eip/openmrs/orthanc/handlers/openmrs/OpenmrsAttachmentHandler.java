/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import java.io.IOException;
import java.util.Base64;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

@Component
public class OpenmrsAttachmentHandler {

    private static final String ATTACHMENT_FHIR_ENDPOINT = "http://localhost/openmrs/ws/rest/v1/attachment";

    private static final String ORTHANC_VIEWER_BASE_URL = "http://localhost:8889/stone-webviewer/index.html?study=";

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

        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println(
                        "POST request successful! Response: " + response.body().string());
            } else {
                System.err.println("POST request failed: " + response.code());
            }
        }
    }
}
