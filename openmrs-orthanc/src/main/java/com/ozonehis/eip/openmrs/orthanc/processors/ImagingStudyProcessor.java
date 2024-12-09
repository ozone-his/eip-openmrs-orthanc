/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsAttachmentHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsObsHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsPatientHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.orthanc.OrthancImagingStudyHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.orthanc.OrthancPatientHandler;
import com.ozonehis.eip.openmrs.orthanc.models.imagingStudy.Study;
import com.ozonehis.eip.openmrs.orthanc.models.obs.Attachment;
import com.ozonehis.eip.openmrs.orthanc.models.patient.PatientMainDicomTags;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
public class ImagingStudyProcessor implements Processor {

    private static final String ATTACHMENT_CONCEPT_ID = "7cac8397-53cd-4f00-a6fe-028e8d743f8e";

    private static final String url = "http://orthanc:8042/instances/%s/rendered";

    @Autowired
    private OpenmrsPatientHandler openmrsPatientHandler;

    @Autowired
    private OrthancPatientHandler orthancPatientHandler;

    @Autowired
    private OpenmrsAttachmentHandler openmrsAttachmentHandler;

    @Autowired
    private OrthancImagingStudyHandler orthancImagingStudyHandler;

    @Autowired
    private OpenmrsObsHandler openmrsObsHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            String body = exchange.getMessage().getBody(String.class);
            ObjectMapper mapper = new ObjectMapper();
            Study[] studies = mapper.readValue(body, Study[].class);

            log.info("ImagingStudyProcessor: {} and studies {}", studies.length, studies);
            for (Study study : studies) {
                /*
                - Get subject id from imaging study
                - Search OpenMRS for the Patient with identifier as Orthanc patient id (present in subject of Imaging study) // TODO: Find an identifier
                - Fetch all obs of patient // TODO: Try Task
                - Check obs comment if study id already exists then don't save another attachment
                - Otherwise create patient in OpenMRS with identifier as orthanc patient id  // Not to be done
                - Fetch binary image from orthanc endpoint http://localhost:8889/dicom-web/studies/1.2.840.113745.101000.1008000.38179.6792.6324567/series/1.3.12.2.1107.5.99.1.24063.4.0.446793548272429/instances/1.3.12.2.1107.5.99.1.24063.4.0.447989428888616/rendered
                - Save attachment in OpenMRS with http://localhost/openmrs/ws/rest/v1/attachment and in filecaption add study url
                 Remove host name in comment
                 */

                Patient openmrsPatient = openmrsPatientHandler.getPatientByName(
                        study.getPatientMainDicomTags().getPatientName());
                if (openmrsPatient == null) {
                    String generatedIdentifier = openmrsPatientHandler
                            .createPatientIdentifier(producerTemplate)
                            .getIdentifier();
                    PatientMainDicomTags patientMainDicomTags = study.getPatientMainDicomTags();
                    openmrsPatient = openmrsPatientHandler.createPatient(
                            patientMainDicomTags.getPatientName(),
                            patientMainDicomTags.getPatientSex(),
                            patientMainDicomTags.getPatientBirthDate(),
                            generatedIdentifier);
                }
                if (!doesObsExists(
                        producerTemplate,
                        openmrsPatient.getIdPart(),
                        study.getImagingStudyMainDicomTags().getStudyInstanceUID())) {
                    createAttachment(
                            study,
                            openmrsPatient.getIdPart(),
                            orthancImagingStudyHandler
                                    .getSeriesByID(
                                            producerTemplate, study.getSeries().get(0))
                                    .getInstances()
                                    .get(0));
                }
            }
        } catch (Exception e) {
            throw new EIPException(String.format("Error processing ImagingStudy %s", e.getMessage()));
        }
    }

    private void createAttachment(Study study, String patientID, String instanceID) throws IOException {
        String studyImageUrl = buildStudyImageUrl(instanceID);
        byte[] orthancStudyBinaryData = orthancImagingStudyHandler.fetchStudyBinaryData(studyImageUrl);
        if (orthancStudyBinaryData != null) {
            openmrsAttachmentHandler.saveAttachment(
                    orthancStudyBinaryData,
                    patientID,
                    study.getImagingStudyMainDicomTags().getStudyInstanceUID());
        }
    }

    private String buildStudyImageUrl(String instanceID) {
        return String.format(url, instanceID);
    }

    private boolean doesObsExists(ProducerTemplate producerTemplate, String patientID, String imagingStudyID)
            throws JsonProcessingException {
        List<Attachment> attachmentList =
                openmrsObsHandler.getObsByPatientIDAndConceptID(producerTemplate, patientID, ATTACHMENT_CONCEPT_ID);
        for (Attachment attachment : attachmentList) {
            if (attachment.getComment().contains(imagingStudyID)) {
                return true;
            }
        }
        return false;
    }

    public Long convertDateToTimestamp(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);

        return Timestamp.valueOf(dateTime).getTime();
    }
}
