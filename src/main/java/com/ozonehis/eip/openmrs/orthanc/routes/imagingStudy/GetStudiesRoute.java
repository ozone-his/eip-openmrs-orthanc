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
import com.ozonehis.eip.openmrs.orthanc.config.OrthancTokenProvider;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Component
public class GetStudiesRoute extends RouteBuilder {

    @Autowired
    private OrthancConfig orthancConfig;

    @Autowired
    private OrthancTokenProvider orthancTokenProvider;

    private static final String GET_IMAGING_STUDY_ENDPOINT =
            "/studies?limit=100&expand&since=0";

    @Override
    public void configure() {
        // spotless:off
        from("direct:orthanc-get-studies-route")
                .log(LoggingLevel.INFO, "Fetching Studies from Orthanc...")
                .routeId("orthanc-get-studies-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader("token", method(orthancTokenProvider, "getToken"))
                .toD(orthancConfig.getOrthancBaseUrl() + GET_IMAGING_STUDY_ENDPOINT + "${header." + Constants.HEADER_STUDIES_SINCE + "}")
                .end();
        // spotless:on
    }
}
