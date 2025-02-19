/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes.obs;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import com.ozonehis.eip.openmrs.orthanc.models.obs.AttachmentObs;
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
class GetObsByConceptIDRouteTest extends CamelSpringTestSupport {

    private static final String GET_OBS_BY_CONCEPT_ID_ROUTE = "direct:orthanc-get-openmrs-obs-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        OpenmrsConfig openmrsConfig = new OpenmrsConfig();
        openmrsConfig.setOpenmrsUsername("admin");
        openmrsConfig.setOpenmrsPassword("Admin123");
        openmrsConfig.setOpenmrsBaseUrl("http://localhost:8080/openmrs");
        openmrsConfig.setOauthEnabled(false); // TODO: Add unit test when is Oauth is enabled
        openmrsConfig.setTokenCache(null);
        return new GetObsByConceptIDRoute(openmrsConfig);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("orthanc-get-openmrs-obs-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("http://localhost:8080/openmrs/*").replace().to("mock:get-obs");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(GET_OBS_BY_CONCEPT_ID_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldGetObsByConceptID() throws Exception {
        AttachmentObs attachmentObs = new AttachmentObs();

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:get-obs");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(GET_OBS_BY_CONCEPT_ID_ROUTE, exchange -> {
            exchange.getMessage().setBody(attachmentObs);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
