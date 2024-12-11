/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes.imagingStudy;

import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.config.OrthancConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetImagingStudyRoute extends RouteBuilder {

    @Autowired
    private OrthancConfig orthancConfig;

    private static final String GET_IMAGING_STUDY_ENDPOINT = "/fhir/ImagingStudy/";

    @Override
    public void configure() {
        // spotless:off
        from("direct:orthanc-get-imaging-study-route")
                .log(LoggingLevel.INFO, "Fetching ImagingStudy from Orthanc...")
                .routeId("orthanc-get-imaging-study-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(orthancConfig.authHeader()))
                .toD(orthancConfig.getOrthancBaseUrl() + GET_IMAGING_STUDY_ENDPOINT + "${header." + Constants.HEADER_IMAGING_STUDY_ID
                        + "}")
                .end();
        // spotless:on
    }
}
