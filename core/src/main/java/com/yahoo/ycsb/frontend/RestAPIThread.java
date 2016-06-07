package com.yahoo.ycsb.frontend;

import com.google.gson.Gson;
import com.yahoo.ycsb.measurements.SeriesUnit;
import org.javalite.activejdbc.Base;
import spark.Spark;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A thread to periodically feed a Hazelcast DB with measurements.
 * Hazelcast exposes a RestAPI to communicate with the frontend.
 *
 * @author archangelx360
 */
public class RestAPIThread extends Thread
{

  /* Configuration */
  static final int DEFAULT_PORT = 4567;
    Gson gson = new Gson();

  /**
   * Creates a new HazelcastThread.
   *
   */
  public RestAPIThread()
  {
      Spark.port(RestAPIThread.DEFAULT_PORT);
      CorsFilter.apply();
  }

  /**
   * Run and periodically fill storage DB for frontend.
   */
  @Override
  public void run()
  {
      Spark.get("/operationType/:operationType", (request, response) -> {
        List<SeriesUnit> l = Handler.getInstance().handleGetOperationInThread(request.params(":operationType"));
        /*List<SeriesUnit> l = new ArrayList<>();
        l.add(new SeriesUnit(20, 20));*/
        return l;
      }, gson::toJson);

    boolean alldone = false;
    CountDownLatch countDownLatch = new CountDownLatch(1);
    do {
      try {
        System.err.println("Awaiting clients...");
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
      } catch( InterruptedException ie) {
        System.err.println("Interrupted.");
        //Handler.getInstance().closeConnection();
        //alldone = true;
      }
      // TODO : find a proper way to stop this program.
    } while (!alldone);
  }

}
