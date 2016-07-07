package com.yahoo.ycsb.frontend;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.yahoo.ycsb.measurements.Measurements;
import com.yahoo.ycsb.measurements.OneMeasurement;
import com.yahoo.ycsb.measurements.OneMeasurementFrontend;
import org.bson.Document;

import java.util.List;
import java.util.Map;
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
    private static final int PERIOD = 5000; // Storage process interval
    private static final String DB_NAME = "dbMeasurements";
    private static final String DB_URI = "mongodb://localhost:27017/";
    private String countersCollectionName = "counters";

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
        mongoClient = new MongoClient(new MongoClientURI(MongoHandler.DB_URI));
        db = mongoClient.getDatabase(MongoHandler.DB_NAME);
        db.getCollection(collectionName).createIndex(new Document("num", 1));
    }

    private static void createCountersCollection(String operationType) {
        MongoHandler mH = MongoHandler.getInstance();
        Document document = new Document()
                .append("collection", mH.collectionName)
                .append("operationType", operationType)
                .append("seq", 0);
        mH.db.getCollection("counters").insertOne(document);
    }

    private static Object getNextSequence(String collectionName, String operationType) {
        MongoHandler mH = MongoHandler.getInstance();
        Document searchQuery = new Document("collection", collectionName).append("operationType", operationType);
        Document increase = new Document("seq", 1);
        Document updateQuery = new Document("$inc", increase);
        Document result =  mH.db.getCollection(mH.countersCollectionName).findOneAndUpdate(searchQuery, updateQuery);
        return result.get("seq");
    }

    private static void handleValues(List<Document> documents, String operationType) {
        MongoHandler mH = MongoHandler.getInstance();

        Document counter = mH.db.getCollection(mH.countersCollectionName)
                .find(new Document("collection", mH.collectionName).append("operationType", operationType))
                .first();

        if (counter == null) {
            MongoHandler.createCountersCollection(operationType);
        }
        for (Document d : documents) {
            d.append("num", MongoHandler.getNextSequence(mH.collectionName, operationType));
            mH.db.getCollection(mH.collectionName).insertOne(d);
        }
    }

    private static void fetchPoints(Map<String, OneMeasurement> opToMesurementMap) {
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
