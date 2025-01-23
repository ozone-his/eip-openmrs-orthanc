/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes.series;

import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.ozonehis.eip.openmrs.orthanc.config.OrthancConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetSeriesRoute extends RouteBuilder {

    @Autowired
    private OrthancConfig orthancConfig;

    private static final String GET_SERIES_ENDPOINT = "/series/";

    @Override
    public void configure() {
        // spotless:off
        from("direct:orthanc-get-series-route")
                .log(LoggingLevel.INFO, "Fetching Series from Orthanc...")
                .routeId("orthanc-get-series-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(orthancConfig.authHeader()))
                .toD(orthancConfig.getOrthancBaseUrl() + GET_SERIES_ENDPOINT + "${header." + Constants.HEADER_SERIES_ID
                        + "}")
                .end();
        // spotless:on
    }
}
