/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes.series;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.openmrs.orthanc.config.OrthancConfig;
import com.ozonehis.eip.openmrs.orthanc.models.series.Series;
import org.apache.camel.Endpoint;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
class GetSeriesRouteTest extends CamelSpringTestSupport {

    private static final String GET_SERIES_ENDPOINT = "direct:orthanc-get-series-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        OrthancConfig orthancConfig = new OrthancConfig();
        orthancConfig.setOrthancUsername("orthanc");
        orthancConfig.setOrthancPassword("orthanc");
        orthancConfig.setOrthancBaseUrl("http://localhost:8889/");
        return new GetSeriesRoute(orthancConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("orthanc-get-series-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8889/*").replace().to("mock:get-series");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(GET_SERIES_ENDPOINT);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldGetSeries() throws Exception {
        Series series = new Series();

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:get-series");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(GET_SERIES_ENDPOINT, exchange -> {
            exchange.getMessage().setBody(series);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
