/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.models.imagingStudy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagingStudyMainDicomTags {

    @JsonProperty("InstitutionName")
    public String institutionName;

    @JsonProperty("ReferringPhysicianName")
    public String referringPhysicianName;

    @JsonProperty("StudyDate")
    public String studyDate;

    @JsonProperty("StudyDescription")
    public String studyDescription;

    @JsonProperty("StudyID")
    public String studyID;

    @JsonProperty("StudyInstanceUID")
    public String studyInstanceUID;

    @JsonProperty("StudyTime")
    public String studyTime;
}
