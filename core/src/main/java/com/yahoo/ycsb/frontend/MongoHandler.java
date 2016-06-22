package com.yahoo.ycsb.frontend;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import java.util.concurrent.*;

/**
 * Handles the periodically fill of our storage DB for the frontend.
 */
public class MongoHandler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String DB_NAME = "dbMeasurements";
    private static final String DB_HOST = "localhost";
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
            mongoClient = new MongoClient(MongoHandler.DB_HOST, MongoHandler.DB_PORT);
            MongoCollection<Document> collection = mongoClient.getDatabase(MongoHandler.DB_NAME).getCollection("names");
            collection.createIndex(new Document("name", 1), new IndexOptions().unique(true));
        });
    }

    public void handleNewValueInThread(String key, long latency) {
        // runs handleNewValue() in the thread of the executor
        executor.execute(() -> MongoHandler.handleNewValue(key, latency));
    }

    // this is run in a different thread
    private static void handleNewValue(String key, long latency) {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MongoHandler mH = MongoHandler.getInstance();
        MongoDatabase db = mH.mongoClient.getDatabase(MongoHandler.DB_NAME);

        Document doc = new Document("operationType", key)
                .append("latency", latency)
                .append("createdAt", System.currentTimeMillis());

        db.getCollection(mH.collectionName).insertOne(doc);
        try {
            db.getCollection("names").insertOne(new Document("name", mH.collectionName));
        } catch (MongoWriteException ignored) {
        }
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
