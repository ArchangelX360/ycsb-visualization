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
[![Build Status](https://travis-ci.org/brianfrankcooper/YCSB.png?branch=master)](https://travis-ci.org/brianfrankcooper/YCSB)

Links
-----
http://wiki.github.com/brianfrankcooper/YCSB/  
https://labs.yahoo.com/news/yahoo-cloud-serving-benchmark/
ycsb-users@yahoogroups.com  

Getting Started
---------------

1. Download the [latest release of YCSB](https://github.com/brianfrankcooper/YCSB/releases/latest):

    ```sh
    curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.9.0/ycsb-0.9.0.tar.gz
    tar xfvz ycsb-0.9.0.tar.gz
    cd ycsb-0.9.0
    ```
    
2. Set up a database to benchmark. There is a README file under each binding 
   directory.

3. Run YCSB command. 
    
    ```sh
    bin/ycsb load basic -P workloads/workloada
    bin/ycsb run basic -P workloads/workloada
    ```

  Running the `ycsb` command without any argument will print the usage. 
   
  See https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload
  for a detailed documentation on how to run a workload.

  See https://github.com/brianfrankcooper/YCSB/wiki/Core-Properties for 
  the list of available workload properties.

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
* **frontendhook=boolean** true for activating the storage database hook and fill it with benchmark results, if false the frontend application won't make graphs of your results.

#### Storage database

In the _com.yahoo.ycsb.frontend.MongoHandler_ class you will find MongoDB parameters for YCSB:

``` java
    private static final String DB_NAME = "db_name";
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 27017;
```

For now, only local MongoDB have been tested. But it should work fine with a remote one.
