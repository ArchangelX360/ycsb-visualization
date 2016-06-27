package com.yahoo.ycsb.frontend;

import java.util.ArrayList;

public class FrontEndList<T> extends ArrayList<T> {

    private int nextIndexToInsert;

    public FrontEndList(int initialCapacity) {
        super(initialCapacity);
        this.nextIndexToInsert = 0;
    }

    FrontEndList(FrontEndList<T> l) {
        super(l);
        this.nextIndexToInsert = l.getNextIndexToInsert();
    }

    int getNextIndexToInsert() {
        return nextIndexToInsert;
    }

    void setNextIndexToInsert(int nextIndexToInsert) {
        this.nextIndexToInsert = nextIndexToInsert;
    }
}
