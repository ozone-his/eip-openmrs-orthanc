/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.models.series;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Series {

    @JsonProperty("ID")
    public String id;

    @JsonProperty("Instances")
    public List<String> instances;

    /*
        {
        "ExpectedNumberOfInstances": null,
        "ID": "20b9d0c2-97d85e07-f4dbf4d2-f09e7e6a-0c19062e",
        "Instances": [
            "3bf568c4-7985461f-b55f9cd4-f652d328-099a29fd",
            "a9889b74-9251b780-9441e316-13e696eb-622fc418",
            "7e4a5fab-0760591c-c4289442-30634878-ba79b82e",
            "b47d53a9-52e796ac-3beafb7f-f5e63b5e-64418397",
            "d7b89ac7-95737b44-992a720c-3313be09-dd4803cd",
            "d67fd59c-9684ace4-2ff2331f-683321a2-4ee60937",
            "a6953185-44e7f391-218f10c2-de402fb3-b254e08d",
            "ad30b651-9de5d9bc-e2bdc44d-dc7852d8-9a36d693",
            "eff168ff-ce64aca2-e2009752-30867d5a-71956f65",
            "a88bd0de-35cb472a-7414915a-3457d24d-d9244641",
            "d28d356f-f2c5c8a1-781c1958-eb1e899c-f434f7aa",
            "41716972-1f497907-5004d40a-5f8a245b-ec346d8c",
            "6eed68df-12388ce4-ef7534b5-2daf3019-b3be8870",
            "b007388c-d3650cd1-503bd080-bd8585f2-14b60ab2",
            "b697b1b9-9cd86744-c22e5508-dc31860b-97309e20",
            "22b266c0-2290ad5a-b11c7467-20b73fd9-7263b379",
            "12b6e99c-fb9f90f3-cfe5ccaf-4267fe4f-8cefaeaf",
            "a7cc906e-26b023aa-9c0f36e0-251b151e-3fc75d42",
            "977f59b8-0ea99d3b-5877b556-8425cfcf-7d0e83a5",
            "41c5553b-bced2f31-09543bd6-e22ba17c-8d5ff2ad"
        ],
        "IsStable": true,
        "Labels": [],
        "LastUpdate": "20241204T105932",
        "MainDicomTags": {
            "CardiacNumberOfImages": "0",
            "ImageOrientationPatient": "0.999993\\-0.0036927\\0\\-0\\-0\\-1",
            "ImagesInAcquisition": "20",
            "Manufacturer": "GE MEDICAL SYSTEMS",
            "Modality": "MR",
            "OperatorsName": "ca",
            "ProtocolName": "324-58-2995/5",
            "SeriesDate": "20070101",
            "SeriesDescription": "Cor FSE PD",
            "SeriesInstanceUID": "1.2.840.113619.2.176.2025.1499492.7391.1171285944.393",
            "SeriesNumber": "4",
            "SeriesTime": "120000.000000",
            "StationName": "TWINOW"
        },
        "ParentStudy": "b9c08539-26f93bde-c81ab0d7-bffaf2cb-a4d0bdd0",
        "Status": "Unknown",
        "Type": "Series"
    }
         */
}
