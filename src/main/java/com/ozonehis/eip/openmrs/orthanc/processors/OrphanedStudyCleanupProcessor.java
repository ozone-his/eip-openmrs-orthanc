/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.processors;

import com.ozonehis.eip.openmrs.orthanc.config.OrthancTokenProvider;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsAttachmentHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsObsHandler;
import com.ozonehis.eip.openmrs.orthanc.handlers.openmrs.OpenmrsDiagnosticReportHandler;
import com.ozonehis.eip.openmrs.orthanc.models.obs.Attachment;
import com.ozonehis.eip.openmrs.orthanc.repository.ProcessedStudyRepository;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Orthanc's /changes REST feed does not report study deletions (confirmed by
 * live testing) - an earlier processor that listened for a "Deletion" change
 * event was removed since Orthanc never actually emits one.
 *
 * This processor instead periodically re-checks every study we've previously
 * tracked in eip_processed_orthanc_study by doing a lightweight existence
 * check (GET /studies/{id}) against Orthanc. If a study no longer exists
 * (404), its corresponding result Observation and attachment are deleted and
 * the tracking record is removed, keeping the two systems in sync.
 */
@Slf4j
@Component
public class OrphanedStudyCleanupProcessor implements Processor {

    @Autowired
    private ProcessedStudyRepository processedStudyRepository;

    @Autowired
    private OpenmrsDiagnosticReportHandler openmrsDiagnosticReportHandler;

    @Autowired
    private OrthancTokenProvider orthancTokenProvider;

    @Autowired
    private OpenmrsAttachmentHandler openmrsAttachmentHandler;

    @Autowired
    private OpenmrsObsHandler openmrsObsHandler;

    @org.springframework.beans.factory.annotation.Value("${eip.attachment.concept}")
    private String attachmentConceptId;

    @Value("${orthanc.baseUrl:http://orthanc:8042}")
    private String orthancBaseUrl;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            List<ProcessedStudyRepository.TrackedStudy> tracked = processedStudyRepository.findAllTracked();
            log.debug("Orphaned study cleanup: checking {} tracked studies", tracked.size());

            int deletedCount = 0;
            for (ProcessedStudyRepository.TrackedStudy entry : tracked) {
                String orthancStudyId = entry.orthancStudyId;
                String patientUuid = entry.patientUuid;
                String diagnosticReportUuid = entry.diagnosticReportUuid;

                if (!studyExistsInOrthanc(orthancStudyId)) {
                    log.info("Orthanc study {} no longer exists - cleaning up result Observation {} and any linked attachments",
                        orthancStudyId, diagnosticReportUuid);
                    try {
                        if (diagnosticReportUuid != null && !diagnosticReportUuid.isEmpty()) {
                            openmrsDiagnosticReportHandler.deleteResult(diagnosticReportUuid);
                        }
                        deleteAttachmentsForStudy(producerTemplate, patientUuid, orthancStudyId);
                        processedStudyRepository.delete(orthancStudyId);
                        deletedCount++;
                    } catch (Exception e) {
                        log.error("Failed to clean up orphaned study {}: {}", orthancStudyId, e.getMessage());
                    }
                }
            }

            if (deletedCount > 0) {
                log.info("Orphaned study cleanup: removed {} stale DiagnosticReport(s)", deletedCount);
            }

        } catch (Exception e) {
            log.error("Error during orphaned study cleanup: {}", e.getMessage());
        }
    }

    /**
     * Finds and deletes any OpenMRS attachments whose comment references the
     * given (now-deleted) Orthanc study ID. Attachment comments contain the
     * Stone viewer URL with an orthancId= query parameter matching the study.
     */
    private void deleteAttachmentsForStudy(ProducerTemplate producerTemplate, String patientUuid, String orthancStudyId) {
        try {
            List<Attachment> patientAttachments =
                    openmrsObsHandler.getObsByPatientUUIDAndConceptUUID(producerTemplate, patientUuid, attachmentConceptId);
            for (Attachment attachment : patientAttachments) {
                String comment = attachment.getComment();
                if (comment != null && comment.contains(orthancStudyId)) {
                    try {
                        openmrsAttachmentHandler.deleteAttachment(attachment.getUuid());
                    } catch (Exception e) {
                        log.warn("Failed to delete attachment {} for study {}: {}",
                            attachment.getUuid(), orthancStudyId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to search attachments for orphaned study {}: {}", orthancStudyId, e.getMessage());
        }
    }

    /**
     * Lightweight existence check - HEAD is not supported by Orthanc's REST API
     * for this route, so we use GET but discard the body.
     */
    private boolean studyExistsInOrthanc(String orthancStudyId) {
        try {
            Request request = new Request.Builder()
                .url(orthancBaseUrl + "/studies/" + orthancStudyId)
                .header("token", orthancTokenProvider.getToken())
                .get()
                .build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("Could not check existence of study {}: {} - assuming it still exists (fail safe)",
                orthancStudyId, e.getMessage());
            return true; // fail safe: don't delete on network error
        }
    }
}
