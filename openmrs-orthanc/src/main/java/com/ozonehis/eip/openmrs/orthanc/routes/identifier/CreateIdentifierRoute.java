/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes.identifier;

import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class CreateIdentifierRoute extends RouteBuilder {

    @Autowired
    private OpenmrsConfig openmrsConfig;

    private static final String CREATE_IDENTIFIER_ENDPOINT =
            "/rest/v1/idgen/identifiersource/8549f706-7e85-4c1d-9424-217d50a2988b/identifier"; // TODO: Remove hard
    // coding

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-create-identifier-route")
                .log(LoggingLevel.INFO, "Creating OpenmrsIdentifier in OpenMRS...")
                .routeId("openmrs-create-identifier-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.POST))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsConfig.authHeader()))
                .toD(openmrsConfig.getOpenmrsBaseUrl() + CREATE_IDENTIFIER_ENDPOINT)
                .end();
        // spotless:on
    }
}
