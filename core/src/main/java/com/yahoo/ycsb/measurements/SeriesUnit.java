package com.yahoo.ycsb.measurements;

import org.javalite.activejdbc.Model;

import java.io.Serializable;

public class SeriesUnit extends Model implements Serializable
{
  static {
    // TODO : validate all important parameters
    validatePresenceOf("operationType");
  }

  // TODO(archangelx360) : the following is a workaround for serialization to reconsider
  public SeriesUnit(){

  }

  /**
   * @param time
   * @param average
   */
  public SeriesUnit(long time, double average) {
    this.time = time;
    this.average = average;
  }
  public long time;
  public double average;
}
