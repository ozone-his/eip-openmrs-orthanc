/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.processors;

import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsAttachmentHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsObsHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsPatientHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.orthanc.OrthancImagingStudyHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.orthanc.OrthancPatientHandler;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Obs;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
public class ImagingStudyProcessor implements Processor {

    private static final String ATTACHMENT_CONCEPT_ID = "7cac8397-53cd-4f00-a6fe-028e8d743f8e";

    private static final String url = "http://localhost:8889/dicom-web/studies/%s/series/%s/instances/%s/rendered";

    @Autowired
    private OpenmrsPatientHandler openmrsPatientHandler;

    @Autowired
    private OrthancPatientHandler orthancPatientHandler;

    @Autowired
    private OpenmrsObsHandler openmrsObsHandler;

    @Autowired
    private OpenmrsAttachmentHandler openmrsAttachmentHandler;

    @Autowired
    private OrthancImagingStudyHandler orthancImagingStudyHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            log.debug("ImagingStudyProcessor: bundle {}", bundle.getId());
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            for (Bundle.BundleEntryComponent entry : entries) {
                ImagingStudy imagingStudy = null;
                Resource resource = entry.getResource();
                if (resource instanceof ImagingStudy) {
                    imagingStudy = (ImagingStudy) resource;
                }

                /*
                - Get subject id from imaging study
                - Search OpenMRS for the Patient with identifier as Orthanc patient id (present in subject of Imaging study)
                - Fetch all obs of patient
                - Check obs comment if study id already exists then don't save another attachment
                - Otherwise create patient in OpenMRS with identifier as orthanc patient id
                - Fetch binary image from orthanc endpoint http://localhost:8889/dicom-web/studies/1.2.840.113745.101000.1008000.38179.6792.6324567/series/1.3.12.2.1107.5.99.1.24063.4.0.446793548272429/instances/1.3.12.2.1107.5.99.1.24063.4.0.447989428888616/rendered
                - Save attachment in OpenMRS with http://localhost/openmrs/ws/rest/v1/attachment and in filecaption add study id
                 */

                String orthancPatientID =
                        imagingStudy.getSubject().getReference().split("/")[1];

                Patient openmrsPatient = openmrsPatientHandler.getPatientByIdentifier(orthancPatientID);
                if (openmrsPatient == null) {
                    Patient orthancPatient = orthancPatientHandler.getPatientByID(orthancPatientID);
                    openmrsPatient = openmrsPatientHandler.createPatient(orthancPatient);
                    createAttachment(imagingStudy, openmrsPatient.getIdPart());
                } else if (!doesObsExists(openmrsPatient.getIdPart(), imagingStudy.getIdPart())) {
                    createAttachment(imagingStudy, openmrsPatient.getIdPart());
                }
            }
        } catch (Exception e) {
            throw new EIPException(String.format("Error processing ImagingStudy %s", e.getMessage()));
        }
    }

    private void createAttachment(ImagingStudy imagingStudy, String patientID) throws IOException {
        String studyImageUrl = buildStudyImageUrl(
                imagingStudy.getIdPart(),
                imagingStudy.getSeries().get(0).getUid(),
                imagingStudy.getSeries().get(0).getInstance().get(0).getUid());
        byte[] orthancStudyBinaryData = orthancImagingStudyHandler.fetchStudyBinaryData(studyImageUrl);
        openmrsAttachmentHandler.saveAttachment(orthancStudyBinaryData, patientID, imagingStudy.getIdPart());
    }

    private String buildStudyImageUrl(String studyID, String seriesID, String instanceID) {
        return String.format(url, studyID, seriesID, instanceID);
    }

    private boolean doesObsExists(String patientID, String imagingStudyID) {
        List<Obs> obsList = openmrsObsHandler.getObsByPatientIDAndConceptID(patientID, ATTACHMENT_CONCEPT_ID);
        for (Obs obs : obsList) {
            if (obs.getComment().equals(imagingStudyID)) {
                return true;
            }
        }
        return false;
    }
}
