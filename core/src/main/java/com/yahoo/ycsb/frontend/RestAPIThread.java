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
 * The RestAPI handling thread
 *
 * @author archangelx360
 */
// TODO(archangelx360) : better architecture of this class
public class RestAPIThread extends Thread
{

  /* Configuration */
  static final int DEFAULT_PORT = 4567;

  /**
   * Creates a new RestAPI Thread.
   *
   */
  public RestAPIThread()
  {
      Spark.port(RestAPIThread.DEFAULT_PORT);
      CorsFilter.apply();
  }


  @Override
  public void run()
  {
      Spark.get("/operationType/:operationType", (request, response) -> {
        String result = Handler.getInstance().handleGetOperationInThread(request.params(":operationType"));
        return result;
      });

    boolean alldone = false;
    CountDownLatch countDownLatch = new CountDownLatch(1);
    do {
      try {
        System.err.println("Awaiting clients...");
        countDownLatch.await(5000, TimeUnit.MILLISECONDS);
      } catch( InterruptedException ie) {
        System.err.println("Interrupted.");
        //alldone = true;
      }
      // TODO : find a proper way to stop this program.
    } while (!alldone);
  }

}
