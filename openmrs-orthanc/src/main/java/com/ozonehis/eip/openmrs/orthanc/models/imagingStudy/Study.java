/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.models.imagingStudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozonehis.eip.openmrs.orthanc.models.patient.PatientMainDicomTags;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Study {

    @JsonProperty("ID")
    public String id;

    @JsonProperty("IsStable")
    public boolean isStable;

    @JsonProperty("Labels")
    public String[] labels;

    @JsonProperty("LastUpdate")
    public String lastUpdate;

    @JsonProperty("ImagingStudyMainDicomTags")
    public ImagingStudyMainDicomTags imagingStudyMainDicomTags;

    @JsonProperty("ParentPatient")
    public String parentPatient;

    @JsonProperty("PatientMainDicomTags")
    public PatientMainDicomTags patientMainDicomTags;

    @JsonProperty("Series")
    public String[] series;

    @JsonProperty("Type")
    public String type;
}