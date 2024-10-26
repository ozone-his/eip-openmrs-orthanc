/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.orthanc;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrthancImagingStudyHandler {

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
}
