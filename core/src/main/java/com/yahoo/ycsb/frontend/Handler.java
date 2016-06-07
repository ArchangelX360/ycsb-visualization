package com.yahoo.ycsb.frontend;

import com.yahoo.ycsb.measurements.SeriesUnit;
import org.javalite.activejdbc.Base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
        // TODO : ATTENTION C'EST UN NAN PAS UN 0 NORMALEMENT
        SeriesUnit.createIt("operationType", key, "time", measurement.time, "latency", latency);
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

    List<SeriesUnit> handleGetOperationInThread(String operationType) {
        //Future<List<SeriesUnit>> future = executor.submit(() -> handleGetOperation(operationType));
        List<SeriesUnit> l = new ArrayList<>();
        /*try {
            l = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }*/
        //l.add(new SeriesUnit(20, 25));
        return l;
    }

    private List<SeriesUnit> handleGetOperation(String operationType) {
        List<SeriesUnit> result = SeriesUnit.where("operationType = ?", operationType);
        return result;
    }
}