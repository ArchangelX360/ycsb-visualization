package com.yahoo.ycsb.frontend;

import spark.Filter;
import spark.Spark;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows CORS operation on server
 */
final class CorsFilter {

  private static final Map<String, String> corsHeaders = new HashMap<String, String>();

  static {
    CorsFilter.corsHeaders.put("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
    CorsFilter.corsHeaders.put("Access-Control-Allow-Origin", "*");
    CorsFilter.corsHeaders.put("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
    CorsFilter.corsHeaders.put("Access-Control-Allow-Credentials", "true");
  }

  static void apply() {
    Filter filter = (request, response) -> CorsFilter.corsHeaders.forEach(response::header);
    Spark.after(filter);
  }
}
