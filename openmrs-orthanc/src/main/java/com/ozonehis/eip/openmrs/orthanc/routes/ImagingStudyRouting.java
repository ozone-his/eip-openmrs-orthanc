/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.routes;

import com.ozonehis.eip.openmrs.orthanc.converters.ResourceConverter;
import com.ozonehis.eip.openmrs.orthanc.processors.ImagingStudyProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ImagingStudyRouting extends RouteBuilder {

    @Autowired
    private ImagingStudyProcessor imagingStudyProcessor;

    @Autowired
    private ResourceConverter resourceConverter;

    @Override
    public void configure() {
        getContext().getTypeConverterRegistry().addTypeConverters(resourceConverter);
        // spotless:off
        from("scheduler:studyUpdate?initialDelay=10000&delay=10000")// TODO: Make initialDelay and delay configurable
            .routeId("poll-orthanc")
            .log(LoggingLevel.INFO, "Polling ImagingStudy started...")
            .to("direct:orthanc-get-studies-route")
            .process(imagingStudyProcessor)
            .log(LoggingLevel.INFO, "Polling ImagingStudy completed.")
                .end();
        // spotless:on
    }
}
