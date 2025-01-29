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
import com.ozonehis.eip.openmrs.orthanc.models.imagingStudy.Study;
import com.ozonehis.eip.openmrs.orthanc.models.obs.Attachment;
import java.io.IOException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
public class ImagingStudyProcessor implements Processor {

    private static final String orthancRenderedImageEndpoint = "%s/instances/%s/rendered";

    @Value("${orthanc.baseUrl}")
    private String orthancBaseUrl;

    @Value("${eip.attachment.concept}")
    private String attachmentConceptId;

    @Autowired
    private OpenmrsPatientHandler openmrsPatientHandler;

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

            for (Study study : studies) {
                if (study.getPatientMainDicomTags().getOtherPatientIDs() == null) {
                    continue;
                }
                Patient openmrsPatient = openmrsPatientHandler.getPatientByIdentifier(
                        study.getPatientMainDicomTags().getOtherPatientIDs());
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
        return String.format(orthancRenderedImageEndpoint, orthancBaseUrl, instanceID);
    }

    private boolean doesObsExists(ProducerTemplate producerTemplate, String patientID, String imagingStudyID)
            throws JsonProcessingException {
        List<Attachment> attachmentList =
                openmrsObsHandler.getObsByPatientIDAndConceptID(producerTemplate, patientID, attachmentConceptId);
        for (Attachment attachment : attachmentList) {
            if (attachment.getComment().contains(imagingStudyID)) {
                return true;
            }
        }
        return false;
    }
}
