package com.yahoo.ycsb.frontend;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import com.yahoo.ycsb.measurements.Measurements;
import com.yahoo.ycsb.measurements.OneMeasurement;
import com.yahoo.ycsb.measurements.OneMeasurementFrontend;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Handles the periodically fill of our storage DB for the frontend.
 */
public class MongoHandler {

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private static final int INITIAL_DELAY = 0;
    private static final MongoHandler INSTANCE = new MongoHandler();

    public static MongoHandler getInstance() {
        return MongoHandler.INSTANCE;
    }

    private MongoClient mongoClient;
    private MongoDatabase db;
    private String collectionName;
    private String countersCollectionName;

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setCountersCollectionName(String countersCollectionName) {
        this.countersCollectionName = countersCollectionName;
    }

    private MongoHandler() {
    }

    public void initConnection(String dbURI, String dbName, int fetchFrequency) {
        // opens the DB client for the thread of our executor
        executor.execute(() -> {
            mongoClient = new MongoClient(new MongoClientURI(dbURI));
            // FIXME: MongoDB driver does not throw exception, so we can't kill the programm if there's an error
            db = mongoClient.getDatabase(dbName);
            db.getCollection(collectionName).createIndex(new Document("num", 1));
        });
        executor.scheduleAtFixedRate(() -> {
            // Fail safe system that add task if no more tasks in the queue
            if (executor.getQueue().size() <= 0) {
                executor.execute(() -> MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap()));
            }
        }, MongoHandler.INITIAL_DELAY, fetchFrequency, TimeUnit.MILLISECONDS);
        // Two fetching tasks to avoid empty queue gap
        executor.execute(() -> MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap()));
        executor.execute(() -> MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap()));
    }

    private static void createCountersCollection(String operationType) {
        MongoHandler mH = MongoHandler.getInstance();
        Document document = new Document()
                .append("collection", mH.collectionName)
                .append("label", operationType)
                .append("seq", 0);
        mH.db.getCollection(mH.countersCollectionName).insertOne(document);
    }

    private static Object getNextSequence(String collectionName, String operationType) {
        MongoHandler mH = MongoHandler.getInstance();
        Document searchQuery = new Document("collection", collectionName).append("label", operationType);
        Document increase = new Document("seq", 1);
        Document updateQuery = new Document("$inc", increase);
        Document result = mH.db.getCollection(mH.countersCollectionName)
                .findOneAndUpdate(searchQuery, updateQuery);
        return result.get("seq");
    }

    private static void handleValues(List<Document> documents, String operationType) {
        MongoHandler mH = MongoHandler.getInstance();

        Document counter = mH.db.getCollection(mH.countersCollectionName)
                .find(new Document("collection", mH.collectionName).append("label", operationType))
                .first();

        if (counter == null) {
            MongoHandler.createCountersCollection(operationType);
        }
        for (Document d : documents) {
            d.append("num", MongoHandler.getNextSequence(mH.collectionName, operationType));
        }
        mH.db.getCollection(mH.collectionName).insertMany(documents);
    }

    private static void fetchPoints(Map<String, OneMeasurement> opToMesurementMap) {
        try {
            System.err.println("Fetching points...");
            for (String operationType : opToMesurementMap.keySet()) {
                // FIXME: copy done here... That's bad but I can't figure out a other way to have consistency for now.
                FrontEndList<Document> documents = new FrontEndList<>(((OneMeasurementFrontend)
                        opToMesurementMap.get(operationType)).getPoints());
                int nextIndexToInsert = documents.getNextIndexToInsert();
                int end = documents.size();
                ((OneMeasurementFrontend) opToMesurementMap.get(operationType)).getPoints().setNextIndexToInsert(end);
                MongoHandler.handleValues(documents.subList(nextIndexToInsert, end), operationType);
            }
            System.err.println("Points stored.");
        } finally {
            // Task renew
            MongoHandler.getInstance().executor.execute(() ->
                    MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap()));
        }
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
