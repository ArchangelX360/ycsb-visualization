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

import com.yahoo.ycsb.frontend.FrontEndConcurrentMap;
import com.yahoo.ycsb.frontend.MongoHandler;
import com.yahoo.ycsb.measurements.exporter.MeasurementsExporter;
import org.bson.Document;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Record a series of measurements as raw data points without down sampling,
 * optionally write to an output file when configured.
 *
 * @author stfeng
 */
public class OneMeasurementFrontend extends OneMeasurement {

    private FrontEndConcurrentMap points;

    private int currentNum = 0;
    private int offset = 0;

    OneMeasurementFrontend(String name, Properties props) {
        super(name);
        points = new FrontEndConcurrentMap();

        String operationType = super.getName();
        while (!MongoHandler.getInstance().isHandlerReady()) {
            try {
                System.err.println("Handler not ready yet");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Map<String, Integer> operationTypeToLastNum = MongoHandler.getInstance().getOperationTypeToLastInsertedNum();
        Integer lastNum = operationTypeToLastNum.putIfAbsent(operationType, -1);
        if (lastNum == null) {
            lastNum = -1;
        }
        offset = lastNum + 1;
    }

    @Override
    public synchronized void measure(int latency) {
        points.addDocument(new Document()
                .append("num", currentNum + offset)
                .append("measure", latency)
                .append("label", super.getName()));
        ++currentNum;
    }

    @Override
    public void exportMeasurements(MeasurementsExporter exporter)
            throws IOException {
    }

    @Override
    public synchronized String getSummary() {
        return System.lineSeparator() + "No summary available with this measurement type.";
    }

    public FrontEndConcurrentMap getPoints() {
        return points;
    }

}
