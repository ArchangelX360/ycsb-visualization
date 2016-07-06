package com.yahoo.ycsb.frontend;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import com.yahoo.ycsb.measurements.Measurements;
import com.yahoo.ycsb.measurements.OneMeasurement;
import com.yahoo.ycsb.measurements.OneMeasurementFrontend;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles the periodically fill of our storage DB for the frontend.
 */
public class MongoHandler {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final int INITIAL_DELAY = 200;

    // TODO : extract these parameters in workloads
    private static final int PERIOD = 5000;
    private static final String DB_NAME = "dbMeasurements";
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 27017;

    private static final MongoHandler INSTANCE = new MongoHandler();

    public static MongoHandler getInstance() {
        return MongoHandler.INSTANCE;
    }

    private MongoClient mongoClient;
    MongoDatabase db;

    private String collectionName = "unamed";

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    private MongoHandler() {
        // opens the DB client for the thread of our executor
        executor.execute(this::initConnection);
        executor.scheduleAtFixedRate(() -> {
                    System.err.println("Fetching points...");
                    MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap());
                },
                MongoHandler.INITIAL_DELAY, MongoHandler.PERIOD, TimeUnit.MILLISECONDS);
    }

    private void initConnection() {
        mongoClient = new MongoClient(MongoHandler.DB_HOST, MongoHandler.DB_PORT);
        db = mongoClient.getDatabase(MongoHandler.DB_NAME);
        db.getCollection(collectionName).createIndex(new Document("num", 1));
    }

    private static void handleValues(List<Document> documents) {
        MongoHandler mH = MongoHandler.getInstance();
        MongoDatabase db = MongoHandler.getInstance().db;
        db.getCollection(mH.collectionName).insertMany(documents);
    }

    private static void fetchPoints(Map<String, OneMeasurement> opToMesurementMap) {
        for (String operationType : opToMesurementMap.keySet()) {
            // FIXME: copy done here... That's bad but I can't figure out a other way to have consistency for now.
            FrontEndList<Document> documents = new FrontEndList<>(((OneMeasurementFrontend)
                    opToMesurementMap.get(operationType)).getPoints());
            int nextIndexToInsert = documents.getNextIndexToInsert();
            int end = documents.size();
            System.err.println(end);
            ((OneMeasurementFrontend) opToMesurementMap.get(operationType)).getPoints().setNextIndexToInsert(end);
            MongoHandler.handleValues(documents.subList(nextIndexToInsert, end));
        }
        System.err.println("Points stored.");
    }

    public void closeConnection() throws InterruptedException {
        // closes the DB for the thread of the executor
        executor.execute(() -> {
            System.err.println("Fetching last points...");
            MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap());
        });
        System.err.println("Shutting down DB hook...");
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}
