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

/**
 * Tracks which SR (Structured Report) DICOM instances have already been
 * processed into an OpenMRS Observation. Without this, ImagingSRProcessor
 * has no way to know it already handled a given instance, and the
 * srDetection poller can re-process the same SR instance across multiple
 * poll cycles (e.g. before Orthanc/tracking state settles), each time
 * creating a brand new, duplicate Observation - since DiagnosticReport.result
 * never persists in OpenMRS, there is no other way to detect "this SR's
 * content is already recorded".
 */
@Slf4j
@Repository
public class ProcessedSRRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedSRRepository(@Qualifier("mngtDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void createTableIfNotExists() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS eip_processed_sr_instance (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  orthanc_instance_id VARCHAR(255) NOT NULL UNIQUE," +
            "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        log.info("eip_processed_sr_instance table ready");
    }

    public boolean exists(String orthancInstanceId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM eip_processed_sr_instance WHERE orthanc_instance_id = ?",
            Integer.class, orthancInstanceId);
        return count != null && count > 0;
    }

    public void save(String orthancInstanceId) {
        jdbcTemplate.update(
            "INSERT IGNORE INTO eip_processed_sr_instance (orthanc_instance_id) VALUES (?)",
            orthancInstanceId);
        log.info("Marked SR instance {} as processed", orthancInstanceId);
    }
}
