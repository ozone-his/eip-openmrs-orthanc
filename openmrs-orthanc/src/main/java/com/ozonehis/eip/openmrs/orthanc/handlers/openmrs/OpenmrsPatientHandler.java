/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;

public class OpenmrsPatientHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public Patient getPatientByIdentifier(String identifier) {
        return new Patient();
    }

    public Patient createPatient(Patient patient) {
        return new Patient();
    }
}
