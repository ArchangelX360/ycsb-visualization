package com.yahoo.ycsb.measurements;

import com.yahoo.ycsb.frontend.MongoHandlerRaw;

import java.util.Properties;

public class OneMeasurementFrontendRaw extends OneMeasurementRaw {

    OneMeasurementFrontendRaw(String name, Properties props) {
        super(name, props);
    }

    @Override
    public void measure(int latency) {
        MongoHandlerRaw.getInstance().handleNewValueInThread(super.getName(), latency);
        super.measure(latency);
    }

}
