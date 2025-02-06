/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import java.util.*;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.hl7.fhir.r4.model.*;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public abstract class BaseProcessorTest extends CamelSpringTestSupport {

    protected static final String ENCOUNTER_REFERENCE_ID = "Encounter/1234";

    protected Exchange createExchange(Object object, String eventType) {
        Message message = new DefaultMessage(new DefaultCamelContext());
        message.setBody(object);
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_FHIR_EVENT_TYPE, eventType);
        message.setHeaders(headers);
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setMessage(message);
        return exchange;
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    protected Patient buildPatient() {
        Patient patient = new Patient();
        patient.setId("ioaea498-e146-98c6-bf1c-dccc7d39f30d");
        patient.setActive(true);
        patient.setName(Collections.singletonList(
                new HumanName().setFamily("Doe").addGiven("John").setText("John Doe")));
        patient.setIdentifier(Collections.singletonList(
                new Identifier().setUse(Identifier.IdentifierUse.OFFICIAL).setValue("10IDH12H")));
        return patient;
    }

    protected ServiceRequest buildServiceRequest() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setSubject(new Reference("Patient/iiaea498-e046-09c6-bf9c-dbbc7d39f54c"));
        serviceRequest.setCode(
                new CodeableConcept().setCoding(Collections.singletonList(new Coding().setCode("123ABC"))));
        serviceRequest.setOccurrence(new Period().setStart(new Date(1628468672000L)));
        serviceRequest.setEncounter(new Reference(ENCOUNTER_REFERENCE_ID));
        return serviceRequest;
    }

    protected Encounter buildEncounter() {
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        encounter.setPeriod(new Period().setStart(new Date(1728468672000L)));
        return encounter;
    }

    protected Observation buildObservation() {
        Observation observation = new Observation();
        observation.setId("itaea498-e022-43c6-bf9c-dbbc7d39f67i");
        return observation;
    }
}
