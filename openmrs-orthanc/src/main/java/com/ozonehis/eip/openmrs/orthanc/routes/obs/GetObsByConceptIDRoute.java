/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes.obs;

import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetObsByConceptIDRoute extends RouteBuilder {

    @Autowired
    private OpenmrsConfig openmrsConfig;

    private static final String GET_OBS_ENDPOINT = "%s/rest/v1/obs?concept=%s&v=full&patient=%s";

    @Override
    public void configure() {
        // spotless:off
        from("direct:orthanc-get-openmrs-obs-route")
            .log(LoggingLevel.INFO, "Fetching Obs from OpenMRS...")
            .routeId("orthanc-get-openmrs-obs-route")
            .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
            .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
            .setHeader(Constants.AUTHORIZATION, constant(openmrsConfig.authHeader()))
            .toD(String.format(GET_OBS_ENDPOINT, openmrsConfig.getOpenmrsBaseUrl(), "${header." + Constants.HEADER_OPENMRS_OBS_CONCEPT_ID
                        + "}", "${header." + Constants.HEADER_OPENMRS_PATIENT_ID + "}"))
                .end();
        // spotless:on
    }
}
