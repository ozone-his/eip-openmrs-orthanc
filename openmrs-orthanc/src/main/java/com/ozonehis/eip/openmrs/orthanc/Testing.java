/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Testing {

    // OkHttp client instance
    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {
        try {
            // Source endpoint returning binary data
            String sourceUrl =
                    "http://localhost:8889/dicom-web/studies/1.2.840.113745.101000.1008000.38179.6792.6324567/series/1.3.12.2.1107.5.1.4.36085.2.0.517714246252254/instances/1.3.12.2.1107.5.1.4.36085.2.0.517910617722172/rendered";

            // Destination endpoint for multipart POST
            String destinationUrl = "http://localhost/openmrs/ws/rest/v1/attachment";

            // Authorization token (Base64-encoded "admin:Admin123")
            String authHeader = "Basic " + Base64.getEncoder().encodeToString("admin:Admin123".getBytes());

            // Step 1: Fetch binary data from the source API
            byte[] binaryData = fetchBinaryData(sourceUrl);

            // Step 2: Send binary data as multipart form-data to destination API
            if (binaryData != null) {
                sendBinaryDataAsMultipart(destinationUrl, binaryData, authHeader);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to fetch binary data from the source endpoint
    private static byte[] fetchBinaryData(String url) throws IOException {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString("orthanc:orthanc".getBytes());
        Request request = new Request.Builder()
                .header("Authorization", authHeader)
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to fetch binary data: " + response.code());
                return null;
            }
            return Objects.requireNonNull(response.body()).bytes();
        }
    }

    // Method to send binary data as multipart form-data
    private static void sendBinaryDataAsMultipart(String url, byte[] binaryData, String authHeader) throws IOException {
        // Create the request body for multipart form-data
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileCaption", "test")
                .addFormDataPart("patient", "7c34d5ec-bd78-4815-8a8a-1983e18c72de")
                .addFormDataPart(
                        "file", "binary-file.png", RequestBody.create(binaryData, MediaType.parse("image/png")))
                .build();

        // Build the POST request with authorization and headers
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .post(requestBody)
                .build();

        // Execute the request and handle the response
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
