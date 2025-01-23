/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.models.patient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientMainDicomTags {

    @JsonProperty("OtherPatientIDs")
    public String otherPatientIDs;

    @JsonProperty("PatientID")
    public String patientID;

    @JsonProperty("PatientName")
    public String patientName;

    @JsonProperty("PatientBirthDate")
    public String patientBirthDate;

    @JsonProperty("PatientSex")
    public String patientSex;
}
