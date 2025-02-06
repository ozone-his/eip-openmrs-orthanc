/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.processors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.Utils;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsAttachmentHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsObsHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsPatientHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.orthanc.OrthancImagingStudyHandler;
import com.ozonehis.eip.openmrs.orthanc.models.obs.Attachment;
import com.ozonehis.eip.openmrs.orthanc.models.series.Series;
import java.io.IOException;
import java.util.Collections;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class ImagingStudyProcessorTest extends BaseProcessorTest {

    private static final String PATIENT_ID = "866f25bf-d930-4886-9332-75443047e38e";

    private static final String PATIENT_IDENTIFIER = "10000Y";

    private static final String STUDY_ID = "2.16.840.1.113669.632.20.1211.10000315526";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OpenmrsPatientHandler openmrsPatientHandler;

    @Mock
    private OpenmrsAttachmentHandler openmrsAttachmentHandler;

    @Mock
    private OrthancImagingStudyHandler orthancImagingStudyHandler;

    @Mock
    private OpenmrsObsHandler openmrsObsHandler;

    @InjectMocks
    private ImagingStudyProcessor imagingStudyProcessor;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldProcessImagingStudyWhenPatientIdentifierMatches() throws IOException {
        // Setup
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setIdentifier(Collections.singletonList(new Identifier().setValue(PATIENT_IDENTIFIER)));

        String studiesString = new Utils().readJSON("response/orthanc-studies-response.json");
        Exchange exchange = createExchange(studiesString, "c");

        when(openmrsPatientHandler.getPatientByIdentifier(PATIENT_IDENTIFIER)).thenReturn(patient);
        when(orthancImagingStudyHandler.getSeriesByID(any(), any())).thenReturn(getSeries());
        when(orthancImagingStudyHandler.fetchStudyBinaryData(any())).thenReturn(new byte[] {});

        // Act
        imagingStudyProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        verify(openmrsAttachmentHandler, times(1)).saveAttachment(any(), any(), any());
    }

    @Test
    void shouldNotThrowErrorWhenStudyOtherPatientIdDoesNotMatchPatientIdentifier() throws IOException {
        // Setup
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setIdentifier(Collections.singletonList(new Identifier().setValue("XYZ123")));

        String studiesString = new Utils().readJSON("response/orthanc-studies-response.json");
        Exchange exchange = createExchange(studiesString, "c");

        when(openmrsPatientHandler.getPatientByIdentifier(PATIENT_IDENTIFIER)).thenReturn(null);

        // Act
        imagingStudyProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        verify(orthancImagingStudyHandler, times(0)).getSeriesByID(any(), any());
        verify(orthancImagingStudyHandler, times(0)).fetchStudyBinaryData(any());
        verify(openmrsAttachmentHandler, times(0)).saveAttachment(any(), any(), any());
    }

    @Test
    void shouldNotCreateAttachmentWhenObsWithSameStudyAlreadyExists() throws IOException {
        // Setup
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setIdentifier(Collections.singletonList(new Identifier().setValue(PATIENT_IDENTIFIER)));

        String studiesString = new Utils().readJSON("response/orthanc-studies-response.json");
        Exchange exchange = createExchange(studiesString, "c");

        Attachment attachment = new Attachment();
        attachment.setComment("http://localhost:8889/stone-webviewer/index.html?study=" + STUDY_ID);

        when(openmrsPatientHandler.getPatientByIdentifier(PATIENT_IDENTIFIER)).thenReturn(patient);
        when(orthancImagingStudyHandler.getSeriesByID(any(), any())).thenReturn(getSeries());
        when(orthancImagingStudyHandler.fetchStudyBinaryData(any())).thenReturn(new byte[] {});
        when(openmrsObsHandler.getObsByPatientIDAndConceptID(any(), any(), any()))
                .thenReturn(Collections.singletonList(attachment));

        // Act
        imagingStudyProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        verify(orthancImagingStudyHandler, times(0)).fetchStudyBinaryData(any());
        verify(openmrsAttachmentHandler, times(0)).saveAttachment(any(), any(), any());
    }

    private Series getSeries() throws JsonProcessingException {
        String body = new Utils().readJSON("response/orthanc-series-response.json");
        Series[] series = objectMapper.readValue(body, Series[].class);
        return series[0];
    }
}
