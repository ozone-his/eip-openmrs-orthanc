/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.fhir.security.TokenCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class OpenmrsConfig {

    @Value("${openmrs.username}")
    private String openmrsUsername;

    @Value("${openmrs.password}")
    private String openmrsPassword;

    @Value("${openmrs.baseUrl}")
    private String openmrsBaseUrl;

    @Value("${oauth.enabled:false}")
    private boolean isOauthEnabled;

    @Autowired
    private TokenCache tokenCache;

    public boolean isOauthEnabled() {
        return isOauthEnabled;
    }

    public String authHeader() {
        if (StringUtils.isEmpty(getOpenmrsUsername())) {
            throw new IllegalArgumentException("OpenMRS username is empty");
        }
        if (StringUtils.isEmpty(getOpenmrsPassword())) {
            throw new IllegalArgumentException("OpenMRS password is empty");
        }
        String auth = getOpenmrsUsername() + ":" + getOpenmrsPassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        if (isOauthEnabled()) {
            return "Bearer " + tokenCache.getTokenInfo().getAccessToken();
        } else {
            return "Basic " + new String(encodedAuth);
        }
    }
}
