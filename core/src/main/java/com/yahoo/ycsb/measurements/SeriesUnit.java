package com.yahoo.ycsb.measurements;

public class SeriesUnit
{

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
