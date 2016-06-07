package com.yahoo.ycsb.frontend;

import org.javalite.activejdbc.Model;

/**
 * Database model for one measure
 */
public class Measure extends Model {

    public String getOperationType(){
        return getString("operationType");
    }

    public String getCreatedAt(){
        return getString("createdAt");
    }

    public Double getTime(){
        return getDouble("time");
    }

    public Double getLatency(){
        return getDouble("latency");
    }

}
