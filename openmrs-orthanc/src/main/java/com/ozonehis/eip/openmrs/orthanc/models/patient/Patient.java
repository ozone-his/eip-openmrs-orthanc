/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.models.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Patient {

    @JsonProperty("ID")
    public String id;

    @JsonProperty("IsStable")
    public boolean isStable;

    @JsonProperty("Labels")
    public String[] labels;

    @JsonProperty("LastUpdate")
    public String lastUpdate;

    @JsonProperty("ImagingStudyMainDicomTags")
    public PatientMainDicomTags patientMainDicomTags;

    @JsonProperty("Studies")
    public String[] studies;

    @JsonProperty("Type")
    public String type;
}
