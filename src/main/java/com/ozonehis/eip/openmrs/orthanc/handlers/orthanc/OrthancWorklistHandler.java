/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.orthanc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ozonehis.eip.openmrs.orthanc.config.OrthancConfig;
import com.ozonehis.eip.openmrs.orthanc.config.OrthancTokenProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class OrthancWorklistHandler {

    @Autowired
    private OrthancConfig orthancConfig;

    @Autowired
    private OrthancTokenProvider orthancTokenProvider;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String createWorklistEntry(String patientId, String patientName,
                                       String accessionNumber, String procedureDesc,
                                       String modality) throws IOException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        ObjectNode tags = mapper.createObjectNode();
        tags.put("PatientID", patientId);
        tags.put("PatientName", patientName != null ? patientName : patientId);
        tags.put("AccessionNumber", accessionNumber);
        tags.put("RequestedProcedureDescription", procedureDesc != null ? procedureDesc : "Radiology");
        tags.put("RequestedProcedureID", accessionNumber);

        ArrayNode stepSequence = mapper.createArrayNode();
        ObjectNode step = mapper.createObjectNode();
        step.put("Modality", modality != null ? modality : "XR");
        step.put("ScheduledProcedureStepStartDate", today);
        step.put("ScheduledProcedureStepStartTime", now);
        step.put("ScheduledStationAETitle", "ORTHANC");
        step.put("ScheduledProcedureStepID", accessionNumber);
        step.put("ScheduledProcedureStepDescription", procedureDesc != null ? procedureDesc : "Radiology");
        stepSequence.add(step);
        tags.set("ScheduledProcedureStepSequence", stepSequence);

        ObjectNode payload = mapper.createObjectNode();
        payload.set("Tags", tags);

        String url = orthancConfig.getOrthancBaseUrl() + "/worklists/create";
        String token = orthancTokenProvider.getToken();

        Request request = new Request.Builder()
            .url(url)
            .header("token", token)
            .post(RequestBody.create(
                mapper.writeValueAsString(payload),
                MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create Orthanc worklist: " + body);
            }
            String id = mapper.readTree(body).path("ID").asText();
            log.info("Created Orthanc worklist entry {} for patient {} accession {}",
                id, patientId, accessionNumber);
            return id;
        }
    }
}
