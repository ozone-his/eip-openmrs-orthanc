/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class OrthancTokenProvider {

    @Value("${OAUTH_ACCESS_TOKEN_URL:}")
    private String tokenUrl;

    @Value("${OAUTH_CLIENT_ID:}")
    private String clientId;

    @Value("${OAUTH_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${ORTHANC_OAUTH_ENABLED:false}")
    private boolean oauthEnabled;

    private String cachedToken;
    private long tokenExpiryMs;

    public String getToken() {
        if (!oauthEnabled) {
            log.warn("OAuth is disabled, cannot get token for Orthanc");
            return null;
        }
        if (cachedToken == null || System.currentTimeMillis() >= tokenExpiryMs) {
            refreshToken();
        }
        return cachedToken;
    }

    private void refreshToken() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(tokenUrl)
                    .post(new FormBody.Builder()
                            .add("grant_type", "client_credentials")
                            .add("client_id", clientId)
                            .add("client_secret", clientSecret)
                            .build())
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                Map tokenMap = new ObjectMapper().readValue(body, Map.class);
                cachedToken = (String) tokenMap.get("access_token");
                int expiresIn = (Integer) tokenMap.get("expires_in");
                // Refresh 30 seconds before expiry
                tokenExpiryMs = System.currentTimeMillis() + ((expiresIn - 30) * 1000L);
                log.info("Successfully obtained Orthanc OAuth token, expires in {}s", expiresIn);
            } else {
                log.error("Failed to get OAuth token: {}", response.code());
            }
        } catch (IOException e) {
            log.error("Error refreshing OAuth token: {}", e.getMessage());
        }
    }
}
