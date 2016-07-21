package com.yahoo.ycsb.frontend;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.yahoo.ycsb.measurements.Measurements;
import com.yahoo.ycsb.measurements.OneMeasurement;
import com.yahoo.ycsb.measurements.OneMeasurementFrontend;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Handles the periodically fill of our storage DB for the frontend.
 */
public class MongoHandler {

    private static final MongoHandler INSTANCE = new MongoHandler();

    private static final int INITIAL_DELAY = 200;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private int fetchFrequency = 0;

    private boolean isHandlerReady = false;

    private MongoClient mongoClient;

    private MongoDatabase db;

    private String collectionName;

    /**
     * This collection is used to have a num consistency between different YCSB loads/runs.
     * For example, if your run phase inserts data (Workload D), the MongoHandler should be able to insert
     * new documents with a consistent "num" for MongoDB.
     */
    private String countersCollectionName;

    private Map<String, Integer> operationTypeToLastInsertedNum = new ConcurrentHashMap<>();

    private MongoHandler() {
    }

    public static MongoHandler getInstance() {
        return MongoHandler.INSTANCE;
    }

    public boolean isHandlerReady() {
        return isHandlerReady;
    }

    public Map<String, Integer> getOperationTypeToLastInsertedNum() {
        return operationTypeToLastInsertedNum;
    }

    /**
     * Initialize the MongoDB Handler and the fetching process
     *
     * @param dbURI the MongoDB URI (check MongoDB documentation)
     * @param dbName the name of the database where the benchmark collections will be stored
     * @param fetchFrequency the fetch frequency of the fetching process in milliseconds
     * @param benchmarkName the name of the current benchmark collection (will be created if non-existent)
     * @param countersCollectionName the name of the counters collection in your MongoDB database (will be created if non-existent)
     */
    public void initHandler(String dbURI, String dbName, int fetchFrequency, String benchmarkName, String countersCollectionName) {
        this.fetchFrequency = fetchFrequency;
        this.collectionName = benchmarkName;
        this.countersCollectionName = countersCollectionName;
        initConnection(dbURI, dbName);
        initFetchingProcess();
    }

    /**
     * Initialize the MongoDB connection for the executor thread
     *
     * @param dbURI the MongoDB URI (check MongoDB documentation)
     * @param dbName the name of the database where the benchmark collections will be stored
     */
    private void initConnection(String dbURI, String dbName) {
        // opens the DB client for the thread of our executor
        executor.execute(() -> {
            mongoClient = new MongoClient(new MongoClientURI(dbURI));
            // FIXME: MongoDB driver does not throw exception, so we can't kill the programm if there's an error
            db = mongoClient.getDatabase(dbName);
            db.getCollection(collectionName).createIndex(new Document("num", 1));
            MongoHandler.getLastInsertedNums();
            MongoHandler.getInstance().isHandlerReady = true;
        });
    }

    /**
     * Initialize the fetching process in the executor thread
     */
    private void initFetchingProcess() {
        executor.scheduleAtFixedRate(() -> {
            // Fail safe system that add task if no more tasks in the queue
            if (executor.getQueue().size() <= 0) {
                executor.execute(() -> MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap(), false));
            }
        }, MongoHandler.INITIAL_DELAY, MongoHandler.getInstance().fetchFrequency, TimeUnit.MILLISECONDS);

        // Two fetching tasks to avoid empty queue gap
        executor.execute(() -> MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap(), false));
        executor.execute(() -> MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap(), false));
    }

    /**
     * Gets the former counters collection nums
     */
    private static void getLastInsertedNums() {
        MongoHandler mH = MongoHandler.getInstance();
        FindIterable<Document> counters = mH.db.getCollection(mH.countersCollectionName)
                .find(new Document("collection", mH.collectionName));
        counters.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                mH.getOperationTypeToLastInsertedNum().put(document.get("label").toString(),
                        Integer.parseInt(document.get("seq").toString()));
            }
        });
    }

    /**
     * Updates the counters collection of MongoDB
     */
    private static void updateLastInsertedNums() {
        MongoHandler mH = MongoHandler.getInstance();
        List<WriteModel<Document>> updates = new ArrayList<>(mH.getOperationTypeToLastInsertedNum().keySet().size());
        updates.addAll(mH.getOperationTypeToLastInsertedNum().keySet().stream().map(operationType -> new UpdateOneModel<Document>(
                new Document("label", operationType).append("collection", mH.collectionName),
                new Document("$set", new Document("seq", mH.getOperationTypeToLastInsertedNum().get(operationType))),
                new UpdateOptions().upsert(true)
        )).collect(Collectors.toList()));

        mH.db.getCollection(mH.countersCollectionName).bulkWrite(updates, new BulkWriteOptions().ordered(false));
    }

    /**
     * Insert documents into the database with a Bulk Write (InsertMany)
     *
     * @param documents BSON documents to insert
     */
    private static void handleValues(List<Document> documents) {
        try {
            MongoHandler mH = MongoHandler.getInstance();
            mH.db.getCollection(mH.collectionName).insertMany(documents, new InsertManyOptions().ordered(false));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Internal error during storage");
        }
    }

    /**
     * Get available points of a map of measures based on its ids parameters.
     * If this is the last fetching operation, the last list of points became available because no more points will be
     * added by the client thread.
     *
     * @param map the map of measures
     * @param isLastIteration true if this is the last fetching operation
     * @return a list of BSON documents to insert in the MongoDB database
     */
    private static List<Document> getAvailablePoints(FrontEndConcurrentMap map, boolean isLastIteration) {
        Integer nextListIdToInsert = map.getLastInsertedListId() + 1;
        Integer lastListIdToInsert = (isLastIteration) ? map.getCurrentListId() : map.getCurrentListId() - 1;
        map.setLastInsertedListId(lastListIdToInsert); // Updating the last inserted value

        List<Document> pointsMap = new ArrayList<>(lastListIdToInsert - nextListIdToInsert + 1);
        for (int i = nextListIdToInsert; i <= lastListIdToInsert; ++i) {
            pointsMap.addAll(map.get(i));
            map.remove(i);
        }

        return pointsMap;
    }

    /**
     * Fetch available points into Measurements map structure
     *
     * @param opToMesurementMap the Measurements map
     * @param isLastIteration true if this is the last fetching operation
     */
    private static void fetchPoints(Map<String, OneMeasurement> opToMesurementMap, boolean isLastIteration) {
        MongoHandler mH = MongoHandler.getInstance();
        try {
            if (opToMesurementMap.size() > 0) {
                for (String operationType : opToMesurementMap.keySet()) {
                    List<Document> documentsToAdd = MongoHandler.getAvailablePoints(((OneMeasurementFrontend) opToMesurementMap.get(operationType)).getPoints(), isLastIteration);
                    if (documentsToAdd.size() > 0) {
                        MongoHandler.handleValues(documentsToAdd);
                        mH.getOperationTypeToLastInsertedNum().replace(operationType, Integer.parseInt(documentsToAdd.get(documentsToAdd.size() - 1).get("num").toString()));
                        System.err.println('[' + mH.collectionName + "] [" + operationType + ']' + ' ' + documentsToAdd.size() + " points inserted");
                    }
                }
            }

            if (!isLastIteration) {
                // Task renew
                mH.executor.schedule(() -> MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap(), false), MongoHandler.getInstance().fetchFrequency, TimeUnit.MILLISECONDS);
            }
        } catch (RejectedExecutionException e) {
            System.err.println('[' + MongoHandler.getInstance().collectionName + ']' + " Task rejected!");
        }
    }

    /**
     * Fetches last missing points and closes MongoDB connection
     *
     * @throws InterruptedException if thread is interrupted
     */
    public void closeConnection() throws InterruptedException {
        executor.execute(() -> {
            System.err.println('[' + MongoHandler.getInstance().collectionName + ']' + " Last fetch...");
            MongoHandler.fetchPoints(Measurements.getMeasurements().get_opToMesurementMap(), true); // fetching last points
            MongoHandler.updateLastInsertedNums(); // updating counters
            MongoHandler.getInstance().mongoClient.close(); // closing database
        });
        System.err.println("Shutting down MongoDB handler...");
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}
