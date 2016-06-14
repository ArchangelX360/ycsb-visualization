package com.yahoo.ycsb.frontend;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.yahoo.ycsb.measurements.SeriesUnit;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Handles the periodically fill of our storage DB for the frontend.
 */
public class MongoHandler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String DB_NAME = "dbMeasurements";
    private static final int DB_PORT = 27017;
    private static final MongoHandler INSTANCE = new MongoHandler();

    public static MongoHandler getInstance() {
        return MongoHandler.INSTANCE;
    }

    private MongoClient mongoClient;
    private String collectionName = "unamed";

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    private MongoHandler() {
        // opens the DB client for the thread of our executor
        executor.execute(() -> {
            mongoClient = new MongoClient("localhost", MongoHandler.DB_PORT);
        });
    }

    public void handleNewValueInThread(String key, SeriesUnit measurement) {
        // runs handleNewValue() in the thread of the executor
        executor.execute(() -> MongoHandler.handleNewValue(key, measurement));
    }

    // this is run in a different thread
    private static void handleNewValue(String key, SeriesUnit measurement) {
        double latency = Double.isNaN(measurement.average) ? 0 : measurement.average;
        // FIXME(archangelx360) :  WARNING IT'S A NaN NOT A ZERO

        MongoHandler mH = MongoHandler.getInstance();
        MongoDatabase db = mH.mongoClient.getDatabase(MongoHandler.DB_NAME);

        Document doc = new Document("operationType", key)
                .append("latency", latency)
                .append("time", measurement.time)
                .append("createdAt", System.currentTimeMillis());


        db.getCollection(mH.collectionName).insertOne(doc);
    }

    public void closeConnection() {
        // closes the DB for the thread of the executor
        System.err.println("Shutting down DB hook...");
        executor.shutdown();
        try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
