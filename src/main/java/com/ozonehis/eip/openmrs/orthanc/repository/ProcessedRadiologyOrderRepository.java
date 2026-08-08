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
 * Tracks which radiology ServiceRequests have already had an Orthanc
 * worklist entry created, so RadiologyOrderWorklistProcessor doesn't
 * recreate one on every poll cycle.
 *
 * Backed by the shared, persistent MySQL database - matching
 * ProcessedStudyRepository's established pattern - rather than a flat
 * file on the EIP bridge container's own filesystem, since that
 * filesystem is confirmed ephemeral (wiped on container recreation,
 * e.g. during an unrelated config change requiring the container to be
 * recreated), which would either lose this tracking (causing duplicate
 * worklist entries) or, if Orthanc's own worklist storage is wiped
 * independently, leave no way to detect and recover a lost entry.
 */
@Slf4j
@Repository
public class ProcessedRadiologyOrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedRadiologyOrderRepository(@Qualifier("mngtDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void createTableIfNotExists() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS eip_processed_radiology_order (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  service_request_id VARCHAR(38) NOT NULL UNIQUE," +
            "  accession_number VARCHAR(32)," +
            "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        log.info("eip_processed_radiology_order table ready");
    }

    public boolean exists(String serviceRequestId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM eip_processed_radiology_order WHERE service_request_id = ?",
            Integer.class, serviceRequestId);
        return count != null && count > 0;
    }

    public void save(String serviceRequestId, String accessionNumber) {
        jdbcTemplate.update(
            "INSERT IGNORE INTO eip_processed_radiology_order (service_request_id, accession_number) VALUES (?, ?)",
            serviceRequestId, accessionNumber);
        log.info("Saved processed radiology order {} (accession {})", serviceRequestId, accessionNumber);
    }

    public void delete(String serviceRequestId) {
        jdbcTemplate.update(
            "DELETE FROM eip_processed_radiology_order WHERE service_request_id = ?",
            serviceRequestId);
        log.info("Deleted processed radiology order {}", serviceRequestId);
    }
}
