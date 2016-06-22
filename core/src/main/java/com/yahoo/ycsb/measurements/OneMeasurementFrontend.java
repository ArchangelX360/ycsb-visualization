/**
 * Copyright (c) 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.measurements;

import com.yahoo.ycsb.frontend.MongoHandler;
import com.yahoo.ycsb.measurements.exporter.MeasurementsExporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Record a series of measurements as raw data points without down sampling,
 * optionally write to an output file when configured.
 *
 * @author stfeng
 *
 */
class OneMeasurementFrontend extends OneMeasurement {

  OneMeasurementFrontend(String name, Properties props) {
    super(name);
  }

  @Override
  public void measure(int latency) {
    MongoHandler.getInstance().handleNewValueInThread(super.getName(), latency);
  }

  @Override
  public void exportMeasurements(MeasurementsExporter exporter)
      throws IOException {

  }

  @Override
  public synchronized String getSummary() {
    return "No summary available with this measurement type.";
  }
}
