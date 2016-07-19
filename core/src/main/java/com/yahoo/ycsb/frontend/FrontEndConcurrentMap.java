package com.yahoo.ycsb.frontend;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FrontEndConcurrentMap extends ConcurrentHashMap<Integer, List<Document>> {

    private static final int CHUNK_SIZE = 10000;

    private Integer currentListId;
    private Integer lastInsertedListId;

    public FrontEndConcurrentMap() {
        this.put(0, new ArrayList<>(FrontEndConcurrentMap.CHUNK_SIZE));
        this.currentListId = 0;
        this.lastInsertedListId = -1;
    }

    public void addDocument(Document document) {
        if (this.get(currentListId).size() >= FrontEndConcurrentMap.CHUNK_SIZE) {
            ++this.currentListId;
            this.put(this.currentListId, new ArrayList<>(FrontEndConcurrentMap.CHUNK_SIZE));
        }
        this.get(currentListId).add(document);
    }


    Integer getCurrentListId() {
        return currentListId;
    }

    Integer getLastInsertedListId() {
        return lastInsertedListId;
    }

    void setLastInsertedListId(Integer lastInsertedListId) {
        this.lastInsertedListId = lastInsertedListId;
    }
}
