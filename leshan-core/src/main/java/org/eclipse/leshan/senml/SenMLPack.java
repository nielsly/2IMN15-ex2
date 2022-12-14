/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The class representing the SenML Pack.
 *
 * @see <a href="https://tools.ietf.org/html/rfc8428#section-4">rfc8428 - Sensor Measurement Lists (SenML)</a>
 */
public class SenMLPack {

    private List<SenMLRecord> records;

    public SenMLPack() {
    }

    public SenMLPack(List<SenMLRecord> records) {
        this.records = records;
    }

    public void addRecord(SenMLRecord record) {
        if (records == null) {
            records = new ArrayList<>();
        }

        records.add(record);
    }

    public void addRecords(List<SenMLRecord> records) {
        if (this.records == null) {
            this.records = new ArrayList<>();
        }
        this.records.addAll(records);
    }

    public void setRecords(List<SenMLRecord> records) {
        this.records = records;
    }

    public List<SenMLRecord> getRecords() {
        if (records == null)
            return Collections.emptyList();
        return records;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((records == null) ? 0 : records.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SenMLPack other = (SenMLPack) obj;
        if (records == null) {
            if (other.records != null)
                return false;
        } else if (!records.equals(other.records))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("SenMLPack [records=%s]", records);
    }
}
