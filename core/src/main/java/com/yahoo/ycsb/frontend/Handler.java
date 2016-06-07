package com.yahoo.ycsb.frontend;

import com.yahoo.ycsb.measurements.SeriesUnit;
import org.codehaus.jackson.map.ObjectMapper;
import org.javalite.activejdbc.Base;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * Handles the periodically fill of our storage DB for the frontend.
 */
public class Handler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    static final String DB_USER = "ycsb";
    static final String DB_PASS = "ycsb";
    static final String DB_NAME = "dbMeasurements";

    private static final Handler INSTANCE = new Handler();

    public static Handler getInstance() {
        return Handler.INSTANCE;
    }

    private Handler() {
        // opens the DB for the thread of our executor
        executor.execute(() ->
                Base.open("com.mysql.jdbc.Driver",
                        "jdbc:mysql://localhost:3306/" + Handler.DB_NAME,
                        Handler.DB_USER,
                        Handler.DB_PASS));

    }

    public void handleNewValueInThread(String key, SeriesUnit measurement) {
        // runs handleNewValue() in the thread of the executor
        executor.execute(() -> Handler.handleNewValue(key, measurement));
    }

    // this is run in a different thread
    private static void handleNewValue(String key, SeriesUnit measurement) {
        double latency = Double.isNaN(measurement.average) ? 0 : measurement.average;
        // FIXME(archangelx360) :  WARNING IT'S A NaN NOT A ZERO
        Measure.createIt("operationType", key, "time", measurement.time, "latency", latency, "createdAt", new Date(System.currentTimeMillis()));
    }

    String handleGetOperationInThread(String operationType) {
        Future<String> future = executor.submit(() -> handleGetOperation(operationType));
        String result = "";
        try {
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String handleGetOperation(String operationType) {
        List<Measure> result = Measure.where("operationType = ?", operationType);
        return convertToJson(result);
    }

    private String convertToJson(List<Measure> list) {
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writeValueAsString(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void closeConnection() {
        // closes the DB for the thread of the executor
        System.err.println("Closing DB connection...");
        executor.execute(Base::close);
        System.err.println("Shutting down thread...");
        executor.shutdown();
        try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}