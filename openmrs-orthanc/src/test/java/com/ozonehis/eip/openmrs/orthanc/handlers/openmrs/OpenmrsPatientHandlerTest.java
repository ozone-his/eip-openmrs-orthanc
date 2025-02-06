/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import java.util.Collections;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OpenmrsPatientHandlerTest {
    private static final String PATIENT_IDENTIFIER = "10000Y";

    @Mock
    private IGenericClient openmrsFhirClient;

    @Mock
    private ICreate iCreate;

    @Mock
    private ICreateTyped iCreateTyped;

    @Mock
    private IUntypedQuery iUntypedQuery;

    @Mock
    private IQuery iQuery;

    @InjectMocks
    private OpenmrsPatientHandler openmrsPatientHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    void shouldReturnPatientByIdentifier() {
        // Setup
        String patientID = UUID.randomUUID().toString();
        Patient patient = new Patient();
        patient.setId(patientID);
        patient.setIdentifier(Collections.singletonList(new Identifier().setValue(PATIENT_IDENTIFIER)));

        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(patient);
        bundle.setEntry(Collections.singletonList(bundleEntryComponent));

        // Mock behavior
        when(openmrsFhirClient.search()).thenReturn(iUntypedQuery);
        when(iUntypedQuery.forResource(Patient.class)).thenReturn(iQuery);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery);
        when(iQuery.returnBundle(Bundle.class)).thenReturn(iQuery);
        when(iQuery.execute()).thenReturn(bundle);

        // Act
        Patient result = openmrsPatientHandler.getPatientByIdentifier(PATIENT_IDENTIFIER);

        // Verify
        assertNotNull(result);
        assertEquals(PATIENT_IDENTIFIER, result.getIdentifier().get(0).getValue());
    }
}
