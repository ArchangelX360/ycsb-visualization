package com.yahoo.ycsb.frontend;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
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
    private static final int PERIOD = 1000;
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
                INITIAL_DELAY, MongoHandler.PERIOD, TimeUnit.MILLISECONDS);
    }

    private void initConnection() {
        mongoClient = new MongoClient(MongoHandler.DB_HOST, MongoHandler.DB_PORT);
        db = mongoClient.getDatabase(MongoHandler.DB_NAME);
        db.getCollection("names")
                .createIndex(new Document("name", 1), new IndexOptions().unique(true));
        db.getCollection(collectionName)
                .createIndex(new Document("createdAt", 1), new IndexOptions().unique(true));
        db.getCollection("names").insertOne(new Document("name", collectionName));
    }

    private static void handleValues(List<Document> documents) {
        MongoHandler mH = MongoHandler.getInstance();
        MongoDatabase db = MongoHandler.getInstance().db;
        while (documents.size() > 1000) {
            db.getCollection(mH.collectionName).insertMany(documents.subList(0, 1000));
            documents = documents.subList(1000, documents.size());
        }
        db.getCollection(mH.collectionName).insertMany(documents);
    }

    private static void fetchPoints(Map<String, OneMeasurement> opToMesurementMap) {
        for (String operationType : opToMesurementMap.keySet()) {
            // FIXME: copy done here... That's bad but I can't figure out a other way to have consistency.
            FrontEndList<Document> documents = new FrontEndList<>(((OneMeasurementFrontend)
                    opToMesurementMap.get(operationType)).getPoints());
            int nextIndexToInsert = documents.getNextIndexToInsert();
            int end = documents.size();
            ((OneMeasurementFrontend) opToMesurementMap.get(operationType)).getPoints().setNextIndexToInsert(end);
            MongoHandler.handleValues(documents.subList(nextIndexToInsert, end));
        }
        System.err.println("Points stored.");
    }

    public void closeConnection() {
        // closes the DB for the thread of the executor
        executor.execute(() -> {
            System.err.println("Fetching last points...");
            MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap());
        });
        System.err.println("Shutting down DB hook...");
        executor.shutdown();
        try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
