/**
 * Copyright (c) 2015 Google Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.measurements;

import com.yahoo.ycsb.frontend.FrontEndList;
import com.yahoo.ycsb.measurements.exporter.MeasurementsExporter;
import org.bson.Document;

import java.io.IOException;
import java.util.Properties;

/**
 * Record a series of measurements as raw data points without down sampling,
 * optionally write to an output file when configured.
 *
 * @author stfeng
 */
public class OneMeasurementFrontend extends OneMeasurement {

    private FrontEndList<Document> points;

    OneMeasurementFrontend(String name, Properties props) {
        super(name);
        int operationToDo = 10;

        if (!Boolean.parseBoolean(props.getProperty("DO_TRANSACTIONS_PROPERTY"))) {
            boolean hasInsertProp = props.containsKey("INSERT_COUNT_PROPERTY");
            boolean hasRecordProp = props.containsKey("RECORD_COUNT_PROPERTY");
            boolean hasBoth = hasInsertProp && hasRecordProp;
            Integer insert_count_property = 0;
            Integer record_count_property = 0;

            if (hasInsertProp) {
                insert_count_property = Integer.parseInt(props.getProperty("INSERT_COUNT_PROPERTY"));
                operationToDo = insert_count_property;
            }
            if (hasRecordProp) {
                record_count_property = Integer.parseInt(props.getProperty("RECORD_COUNT_PROPERTY"));
                operationToDo = record_count_property;
            }
            if (hasBoth) {
                operationToDo = Math.min(insert_count_property, record_count_property);
            }
        } else {
            if (props.containsKey("OPERATION_COUNT_PROPERTY")) {
                operationToDo = Integer.parseInt(props.getProperty("OPERATION_COUNT_PROPERTY"));
            }
        }
        points = new FrontEndList<>(operationToDo);
    }

    @Override
    public synchronized void measure(int latency) {
        points.add(new Document()
                .append("num", points.size() + 1)
                .append("latency", latency)
                .append("operationType", super.getName()));
    }

    @Override
    public void exportMeasurements(MeasurementsExporter exporter)
            throws IOException {
    }

    @Override
    public synchronized String getSummary() {
        return "No summary available with this measurement type.";
    }

    public FrontEndList<Document> getPoints() {
        return points;
    }

}
