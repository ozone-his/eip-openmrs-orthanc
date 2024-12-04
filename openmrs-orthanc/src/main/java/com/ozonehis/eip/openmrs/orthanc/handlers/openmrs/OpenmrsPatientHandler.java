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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
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
                .where(Patient.IDENTIFIER.exactly().code(identifier))
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

    public Patient createPatient(String name, String gender, String birthDate, String identifier) {
        Patient patient = new Patient();
        patient.setName(Collections.singletonList(new HumanName().addGiven(name).setFamily(name)));
        if (gender != null) {
            if (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("m")) {
                patient.setGender(Enumerations.AdministrativeGender.MALE);
            } else if (gender.equalsIgnoreCase("female") || gender.equalsIgnoreCase("f")) {
                patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            }
        } else {
            patient.setGender(Enumerations.AdministrativeGender.MALE); // TODO: Fix this
        }
        if (birthDate != null) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                patient.setBirthDate(dateFormat.parse(birthDate));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (identifier != null) {
            patient.setIdentifier(Collections.singletonList(new Identifier()
                    .setType(new CodeableConcept(new Coding().setCode("05a29f94-c0ed-11e2-94be-8c13b969e334"))
                            .setText("OpenMRS ID"))
                    .setValue(identifier)));
        } else {
            patient.setIdentifier(Collections.singletonList(new Identifier()
                    .setType(new CodeableConcept(new Coding().setCode("05a29f94-c0ed-11e2-94be-8c13b969e334"))
                            .setText("OpenMRS ID"))
                    .setValue("100000Y"))); // TODO: Fix this
        }
        MethodOutcome methodOutcome =
                openmrsFhirClient.create().resource(patient).encodedJson().execute();

        log.debug("OpenmrsPatientHandler: Patient created {}", methodOutcome.getCreated());
        return (Patient) methodOutcome.getResource();
    }
}
