/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.models.diagnosticreport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosticReportResource {

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("identifier")
    private List<Identifier> identifier;

    @JsonProperty("category")
    private List<CodeableConcept> category;

    @JsonProperty("code")
    private CodeableConcept code;

    @JsonProperty("subject")
    private Reference subject;

    @JsonProperty("effectiveDateTime")
    private String effectiveDateTime;

    @JsonProperty("encounter")
    private Reference encounter;

    @JsonProperty("conclusion")
    private String conclusion;

    @JsonProperty("presentedForm")
    private List<Attachment> presentedForm;

    // ── Static factory helpers ────────────────────────────────────────────────

    public static List<CodeableConcept> buildRadiologyCategory() {
        Coding coding = new Coding(
                "http://terminology.hl7.org/CodeSystem/v2-0074",
                "RAD",
                "Radiology");
        return Collections.singletonList(new CodeableConcept(
                Collections.singletonList(coding), "Radiology"));
    }

    // OpenMRS FHIR2 maps DiagnosticReport.code by concept UUID (no system).
    // The DICOM StudyInstanceUID is stored in the identifier field instead.
    public static final String PROCEDURE_CONCEPT_UUID = "27fe6714-0bc6-4435-adb0-818538abe42c";

    public static CodeableConcept buildCode(String studyInstanceUID, String modality) {
        Coding conceptCoding = new Coding(
                null,
                PROCEDURE_CONCEPT_UUID,
                "Procedure");
        return new CodeableConcept(Collections.singletonList(conceptCoding), "Procedure");
    }

    public static Reference buildReference(String resourceType, String uuid) {
        return new Reference(resourceType + "/" + uuid);
    }

    public static List<Attachment> buildAttachment(String viewerUrl, String studyInstanceUID) {
        Attachment att = new Attachment();
        att.setContentType("text/html");
        att.setUrl(viewerUrl);
        att.setTitle("DICOM Study " + studyInstanceUID);
        return Collections.singletonList(att);
    }

    public static List<Identifier> buildIdentifier(String orthancStudyId) {
        return Collections.singletonList(new Identifier("urn:orthanc:study-id", orthancStudyId));
    }

    // ── Nested types ──────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Identifier {
        @JsonProperty("system")
        private String system;

        @JsonProperty("value")
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Coding {
        @JsonProperty("system")
        private String system;

        @JsonProperty("code")
        private String code;

        @JsonProperty("display")
        private String display;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CodeableConcept {
        @JsonProperty("coding")
        private List<Coding> coding;

        @JsonProperty("text")
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Reference {
        @JsonProperty("reference")
        private String reference;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Attachment {
        @JsonProperty("contentType")
        private String contentType;

        @JsonProperty("url")
        private String url;

        @JsonProperty("title")
        private String title;
    }
}