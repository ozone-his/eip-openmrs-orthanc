/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.models.identifier.OpenmrsIdentifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenmrsPatientHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public Patient getPatientByIdentifier(String identifier) {
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().identifier(identifier))
                .returnBundle(Bundle.class)
                .execute();

        log.debug("OpenmrsPatientHandler: Patient getPatientByIdentifier {}", bundle.getId());

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Patient.class::isInstance)
                .map(Patient.class::cast)
                .findFirst()
                .orElse(null);
    }

    public Patient createPatient(
            ProducerTemplate producerTemplate, String name, String gender, String birthDate, String orthancIdentifier)
            throws JsonProcessingException {
        Patient patient = new Patient();
        patient.setName(Collections.singletonList(new HumanName().addGiven(name).setFamily(name)));
        if (gender != null) {
            if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("m")) {
                patient.setGender(Enumerations.AdministrativeGender.MALE);
            } else if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("f")) {
                patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            } else {
                patient.setGender(Enumerations.AdministrativeGender.MALE); // TODO: Fix this
            }
        } else {
            patient.setGender(Enumerations.AdministrativeGender.MALE); // TODO: Fix this
        }
        if (birthDate != null && !birthDate.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                patient.setBirthDate(dateFormat.parse(birthDate));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        String openmrsIdentifier = createPatientIdentifier(producerTemplate).getIdentifier();
        List<Identifier> identifierList = new ArrayList<>();
        identifierList.add(new Identifier()
                .setUse(Identifier.IdentifierUse.USUAL)
                .setType(new CodeableConcept(new Coding().setCode("05a29f94-c0ed-11e2-94be-8c13b969e334"))
                        .setText("OpenMRS ID"))
                .setValue(openmrsIdentifier));
        identifierList.add(new Identifier()
                .setUse(Identifier.IdentifierUse.OFFICIAL)
                .setType(new CodeableConcept(new Coding().setCode("b4143563-16cd-4439-b288-f83d61670fc8"))
                        .setText("ID Card"))
                .setValue(orthancIdentifier));
        patient.setIdentifier(identifierList);

        MethodOutcome methodOutcome =
                openmrsFhirClient.create().resource(patient).encodedJson().execute();

        log.debug("OpenmrsPatientHandler: Patient created {}", methodOutcome.getCreated());
        return (Patient) methodOutcome.getResource();
    }

    public OpenmrsIdentifier createPatientIdentifier(ProducerTemplate producerTemplate) throws JsonProcessingException {
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openmrs-create-identifier-route", "{}", null, String.class);
        OpenmrsIdentifier openmrsIdentifier = new ObjectMapper().readValue(response, OpenmrsIdentifier.class);
        return openmrsIdentifier;
    }
}
