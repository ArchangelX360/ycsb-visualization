<!--
Copyright (c) 2010 Yahoo! Inc., 2012 - 2015 YCSB contributors. 
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You
may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. See accompanying
LICENSE file.
-->

Yahoo! Cloud System Benchmark (YCSB)
====================================
**This is a fork + visualization of:** 
[![Build Status](https://travis-ci.org/brianfrankcooper/YCSB.png?branch=master)](https://travis-ci.org/brianfrankcooper/YCSB)

Links
-----
http://wiki.github.com/brianfrankcooper/YCSB/  
https://labs.yahoo.com/news/yahoo-cloud-serving-benchmark/


Getting Started
---------------

1. Download 
    
2. Set up 

3. Run YCSB 


Building from source
--------------------

YCSB requires the use of Maven 3; if you use Maven 2, you may see [errors
such as these](https://github.com/brianfrankcooper/YCSB/issues/406).

To build the full distribution, with all database bindings:

    mvn clean package

To build a single database binding:

    mvn -pl com.yahoo.ycsb:mongodb-binding -am clean package


YCSB visualisation
--------------------

#### New parameters

We added two parameters to YCSB:

* **benchmarkname=my_bench_name** is the name of the benchmark's table in the storage database. Be careful, if you use this name for two different benchmarks, the result of the second will be considered as new results for the first one.
* **measurementtype=frontend** will activate the frontend DB hook for measurement and allows the graph visualisation

#### Storage database

In the _com.yahoo.ycsb.frontend.MongoHandler_ class you will find MongoDB parameters for YCSB:

``` java
    private static final String DB_NAME = "db_name";
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 27017;
```

For now, only local MongoDB have been tested. But it should work fine with a remote one.