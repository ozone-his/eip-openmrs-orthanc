/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

@Slf4j
@Repository
public class ProcessedStudyRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedStudyRepository(@Qualifier("mngtDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void createTableIfNotExists() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS eip_processed_orthanc_study (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  orthanc_study_id VARCHAR(255) NOT NULL UNIQUE," +
            "  patient_uuid VARCHAR(38) NOT NULL," +
            "  diagnostic_report_uuid VARCHAR(38)," +
            "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        log.info("eip_processed_orthanc_study table ready");
    }

    public boolean exists(String orthancStudyId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM eip_processed_orthanc_study WHERE orthanc_study_id = ?",
            Integer.class, orthancStudyId);
        return count != null && count > 0;
    }


    public String[] findByOrthancStudyId(String orthancStudyId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT patient_uuid, diagnostic_report_uuid FROM eip_processed_orthanc_study WHERE orthanc_study_id = ?",
                (rs, rowNum) -> new String[]{rs.getString("patient_uuid"), rs.getString("diagnostic_report_uuid")},
                orthancStudyId);
        } catch (Exception e) {
            return null;
        }
    }

    public void delete(String orthancStudyId) {
        jdbcTemplate.update(
            "DELETE FROM eip_processed_orthanc_study WHERE orthanc_study_id = ?",
            orthancStudyId);
        log.info("Deleted processed study {}", orthancStudyId);
    }
    public static class TrackedStudy {
        public String orthancStudyId;
        public String patientUuid;
        public String diagnosticReportUuid;
    }

    public java.util.List<TrackedStudy> findAllTracked() {
        return jdbcTemplate.query(
            "SELECT orthanc_study_id, patient_uuid, diagnostic_report_uuid FROM eip_processed_orthanc_study",
            (rs, rowNum) -> {
                TrackedStudy t = new TrackedStudy();
                t.orthancStudyId = rs.getString("orthanc_study_id");
                t.patientUuid = rs.getString("patient_uuid");
                t.diagnosticReportUuid = rs.getString("diagnostic_report_uuid");
                return t;
            });
    }

    public void save(String orthancStudyId, String patientUUID, String diagnosticReportUUID) {
        jdbcTemplate.update(
            "INSERT IGNORE INTO eip_processed_orthanc_study (orthanc_study_id, patient_uuid, diagnostic_report_uuid) VALUES (?, ?, ?)",
            orthancStudyId, patientUUID, diagnosticReportUUID);
        log.info("Saved processed study {} for patient {}", orthancStudyId, patientUUID);
    }
}
