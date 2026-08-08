/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.models.diagnosticreport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiagnosticReportBundle {

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("total")
    private int total;

    @JsonProperty("entry")
    private List<BundleEntry> entry;

    public List<DiagnosticReportResource> getEntries() {
        if (entry == null) return Collections.emptyList();
        return entry.stream()
                .map(BundleEntry::getResource)
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BundleEntry {
        @JsonProperty("resource")
        private DiagnosticReportResource resource;
    }
}