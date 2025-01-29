/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenmrsAttachmentHandler {

    private static final String ATTACHMENT_FHIR_ENDPOINT = "%s/ws/rest/v1/attachment";

    private static final String ORTHANC_VIEWER_BASE_URL = "%s/stone-webviewer/index.html?study=%s";

    @Value("${orthanc.publicUrl}")
    private String orthancPublicUrl;

    @Autowired
    private OpenmrsConfig openmrsConfig;

    // TODO: Use Apache Camel Route instead of okhttp3 (Error: payload content too big)
    public void saveAttachment(byte[] binaryData, String patientID, String studyID) throws IOException {
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileCaption", String.format(ORTHANC_VIEWER_BASE_URL, orthancPublicUrl, studyID))
                .addFormDataPart("patient", patientID)
                .addFormDataPart(
                        "file", "radiology-image.png", RequestBody.create(binaryData, MediaType.parse("image/png")))
                .build();

        Request request = new Request.Builder()
                .url(String.format(ATTACHMENT_FHIR_ENDPOINT, openmrsConfig.getOpenmrsBaseUrl()))
                .header("Accept", "application/json")
                .header("Authorization", openmrsConfig.authHeader())
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
