/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.processors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ozonehis.eip.openmrs.orthanc.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsAttachmentHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsDiagnosticReportHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsObsHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsPatientHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.orthanc.OrthancImagingStudyHandler;
import com.ozonehis.eip.openmrs.orthanc.models.imagingStudy.Study;
import com.ozonehis.eip.openmrs.orthanc.models.obs.Attachment;
import com.ozonehis.eip.openmrs.orthanc.models.series.Series;
import com.ozonehis.eip.openmrs.orthanc.repository.ProcessedStudyRepository;
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
    private static final String ORTHANC_RENDERED_IMAGE_ENDPOINT = "%s/instances/%s/rendered";
    @Autowired
    private ProcessedStudyRepository processedStudyRepository;
    @Value("${orthanc.publicUrl:${orthanc.baseUrl}}")
    private String orthancPublicUrl;

    @Value("${orthanc.baseUrl}")
    private String orthancBaseUrl;
    @Value("${eip.attachment.concept}")
    private String attachmentConceptId;
    @Autowired
    private OpenmrsPatientHandler openmrsPatientHandler;
    @Autowired
    private OpenmrsDiagnosticReportHandler openmrsDiagnosticReportHandler;
    @Autowired
    private OrthancImagingStudyHandler orthancImagingStudyHandler;
    @Autowired
    private OpenmrsAttachmentHandler openmrsAttachmentHandler;
    @Autowired
    private OpenmrsObsHandler openmrsObsHandler;
    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            String body = exchange.getMessage().getBody(String.class);
            ObjectMapper mapper = new ObjectMapper();
            Study[] studies = mapper.readValue(body, Study[].class);
            for (Study study : studies) {
                // Use OtherPatientIDs first, fall back to PatientID
                String patientIdentifier = study.getPatientMainDicomTags().getOtherPatientIDs();
                if (patientIdentifier == null || patientIdentifier.isEmpty()) {
                    patientIdentifier = study.getPatientMainDicomTags().getPatientID();
                }
                if (patientIdentifier == null || patientIdentifier.isEmpty()) {
                    log.warn("No patient identifier found in DICOM study {}, skipping", study.id);
                    continue;
                }
                Patient openmrsPatient = openmrsPatientHandler.getPatientByIdentifier(patientIdentifier);
                if (openmrsPatient == null || openmrsPatient.getIdentifier().isEmpty()) {
                    continue;
                }
                String patientUUID = openmrsPatient.getIdPart();
                String studyInstanceUID = study.getImagingStudyMainDicomTags().getStudyInstanceUID();
                if (processedStudyRepository.exists(study.id)) {
                    log.debug("DiagnosticReport already processed for study {}", study.id);
                    continue;
                }
                String modality = null;
                String studyDate = study.getImagingStudyMainDicomTags().getStudyDate();
                Series series = null;
                if (study.getSeries() != null && !study.getSeries().isEmpty()) {
                    try {
                        series = orthancImagingStudyHandler.getSeriesByID(
                                producerTemplate, study.getSeries().get(0));
                        if (series != null && series.getMainDicomTags() != null) {
                            modality = series.getMainDicomTags().getModality();
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch series for study {}: {}", study.id, e.getMessage());
                    }
                }
                String viewerUrl = orthancPublicUrl
                        + "/stone-webviewer/index.html?study=" + studyInstanceUID
                        + "&orthancId=" + study.id;
                String resultUUID = openmrsDiagnosticReportHandler.saveResult(
                        producerTemplate,
                        patientUUID,
                        Constants.GENERAL_PATIENT_NOTE_CONCEPT_UUID,
                        "DICOM study available. View at: " + viewerUrl,
                        studyDate);
                processedStudyRepository.save(study.id, patientUUID, resultUUID);
                // Upload preview image as attachment
                try {
                    if (!doesObsExists(producerTemplate, patientUUID, studyInstanceUID)) {
                        if (series != null && series.getInstances() != null && !series.getInstances().isEmpty()) {
                            createAttachment(study, patientUUID, series.getInstances().get(0));
                            log.info("Saved attachment for patient {} study {}", patientUUID, study.id);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not save attachment for study {}: {}", study.id, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new EIPException(String.format("Error processing ImagingStudy: %s", e.getMessage()));
        }
    }
    private void createAttachment(Study study, String patientUUID, String instanceID) throws IOException {
        String studyImageUrl = String.format(ORTHANC_RENDERED_IMAGE_ENDPOINT, orthancBaseUrl, instanceID);
        byte[] orthancStudyBinaryData = orthancImagingStudyHandler.fetchStudyBinaryData(studyImageUrl);
        if (orthancStudyBinaryData != null) {
            openmrsAttachmentHandler.saveAttachment(
                    orthancStudyBinaryData,
                    patientUUID,
                    study.getImagingStudyMainDicomTags().getStudyInstanceUID(),
                    study.id);
        }
    }
    private boolean doesObsExists(ProducerTemplate producerTemplate, String patientUUID, String imagingStudyID)
            throws JsonProcessingException {
        List<Attachment> attachmentList =
                openmrsObsHandler.getObsByPatientUUIDAndConceptUUID(producerTemplate, patientUUID, attachmentConceptId);
        for (Attachment attachment : attachmentList) {
            if (attachment.getComment() != null && attachment.getComment().contains(imagingStudyID)) {
                return true;
            }
        }
        return false;
    }
}
