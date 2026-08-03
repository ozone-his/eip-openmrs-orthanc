/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.orthanc.config;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CursorStore {
    private final AtomicLong changesCursor = new AtomicLong(0);
    private final AtomicLong srChangesCursor = new AtomicLong(0);

    public long getChangesCursor() {
        return changesCursor.get();
    }

    public void setChangesCursor(long value) {
        changesCursor.set(value);
    }

    public long getSrChangesCursor() {
        return srChangesCursor.get();
    }

    public void setSrChangesCursor(long value) {
        srChangesCursor.set(value);
    }
}
