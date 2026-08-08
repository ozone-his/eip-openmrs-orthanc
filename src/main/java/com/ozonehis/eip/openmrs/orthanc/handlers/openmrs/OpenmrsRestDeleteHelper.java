/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.handlers.openmrs;

import com.ozonehis.eip.openmrs.orthanc.config.OpenmrsConfig;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Shared logic for permanently deleting an OpenMRS REST resource by UUID.
 * Used by both attachment and result (Observation) deletion, which are
 * otherwise identical: a plain DELETE only voids the record while leaving
 * it fully retrievable (confirmed via live testing - GET still returns 200
 * with full data afterward). purge=true is required to actually remove it,
 * and the endpoint can then return 500 on an already-purged record even
 * though the deletion itself succeeded, so both 2xx and 500 are treated as
 * "gone" - only genuine network/other errors count as failure.
 */
@Slf4j
@Component
public class OpenmrsRestDeleteHelper {

    @Autowired
    private OpenmrsConfig openmrsConfig;

    /**
     * Permanently deletes (purges) an OpenMRS REST resource.
     *
     * @param resourcePath the REST resource path segment, e.g. "attachment" or "obs"
     * @param uuid the UUID of the resource to delete
     * @param resourceLabel a human-readable label for log messages, e.g. "attachment" or "result Observation"
     * @return true if the resource is confirmed gone, false on a genuine failure
     */
    public boolean deleteByUuid(String resourcePath, String uuid, String resourceLabel) throws IOException {
        Request request = new Request.Builder()
                .url(openmrsConfig.getOpenmrsBaseUrl() + "/ws/rest/v1/" + resourcePath + "/" + uuid + "?purge=true")
                .header("Authorization", openmrsConfig.authHeader())
                .delete()
                .build();
        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            boolean ok = response.isSuccessful() || response.code() == 500;
            if (ok) {
                log.info("Deleted {} {}", resourceLabel, uuid);
            } else {
                log.warn("Failed to delete {} {}: {}", resourceLabel, uuid, response.code());
            }
            return ok;
        }
    }
}
