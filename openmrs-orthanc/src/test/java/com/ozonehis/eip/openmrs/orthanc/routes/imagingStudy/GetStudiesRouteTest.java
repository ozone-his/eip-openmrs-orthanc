/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes.imagingStudy;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.openmrs.orthanc.config.OrthancConfig;
import com.ozonehis.eip.openmrs.orthanc.models.imagingStudy.Study;
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
class GetStudiesRouteTest extends CamelSpringTestSupport {
    private static final String GET_IMAGING_STUDY_ENDPOINT = "direct:orthanc-get-studies-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        OrthancConfig orthancConfig = new OrthancConfig();
        orthancConfig.setOrthancUsername("orthanc");
        orthancConfig.setOrthancPassword("orthanc");
        orthancConfig.setOrthancBaseUrl("http://localhost:8889/");
        return new GetStudiesRoute(orthancConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("orthanc-get-studies-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8889/*").replace().to("mock:get-studies");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(GET_IMAGING_STUDY_ENDPOINT);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldGetStudies() throws Exception {
        Study study = new Study();

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:get-studies");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(GET_IMAGING_STUDY_ENDPOINT, exchange -> {
            exchange.getMessage().setBody(study);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
