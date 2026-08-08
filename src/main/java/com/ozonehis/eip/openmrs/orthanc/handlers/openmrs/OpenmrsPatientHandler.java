/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
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
}
