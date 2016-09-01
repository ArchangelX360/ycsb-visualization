Yahoo! Cloud System Benchmark (YCSB)
====================================

Links
-----
**This is a fork + visualization of:**
http://wiki.github.com/brianfrankcooper/YCSB/

Getting Started
---------------

1. Download the [latest release of YCSB](https://github.com/brianfrankcooper/YCSB/releases/latest):

    ```sh
    curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.10.0/ycsb-0.10.0.tar.gz
    tar xfvz ycsb-0.10.0.tar.gz
    cd ycsb-0.10.0
    ```
    
2. Set up a database to benchmark. There is a README file under each binding 
   directory.

3. Run YCSB command. 

    On Linux:
    ```sh
    bin/ycsb.sh load basic -P workloads/workloada
    bin/ycsb.sh run basic -P workloads/workloada
    ```

    On Windows:
    ```bat
    bin/ycsb.bat load basic -P workloads\workloada
    bin/ycsb.bat run basic -P workloads\workloada
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
------------------

#### Presentation

This YCSB module extension stores YCSB measurements in real-time into a MongoDB database.
It is designed to be used with [Web Dataset Visualizer](https://bitbucket.org/r0bcrane/ycsb-visualization) out of the box.

![Architecture photo with Storage DB and YCSB selected.](/doc/images/archi-software.png "Place in the overall project architecture")

**Place in the overall project architecture**

This module provides a new thread that periodically checks if there is new measurements and if any stores them into a storage DB.
We created a new MeasurementType "frontend" which is using a concurrent map of lists of MongoDB Documents instead of basic(operation, latency) couples. 
Choosing this MeasurementType will automatically launch the whole DB storage process. The MongoDB connexion will be initialized based on specified parameters or the default local ones and the periodic fetching process will be started.

#### New parameter & measurement

We added some parameters to YCSB:

* **frontend.collection.benchmark=<my_bench_name>** where "my_bench_name" is the name of the benchmark's collection in the storage database. Be careful, if you use this name for two different benchmarks, the result of the second will be considered as new results for the first one.
* **frontend.collection.counters=<my_counters_collection_name>** where "my_counters_collection_name" is the name of the counters collection of the storage database. One you chose one, try not to change it because you will have to reconfigure your client depending on which benchmark you want to watch.
* **frontend.db.uri=<my_db_uri>** where "my_db_uri" is the MongoDB URI without the database name.
* **frontend.db.name=<my_db_name>** where "my_db_name" is the name of the MongoDB Database where to store benchmark results.
* **frontend.frequency=<my_frequency>** where "my_frequency" is the number of milliseconds between two storage actions in MongoDB. (5000 by default)

All the above parameters have default values thus are not mandatory.

As we said above, we also created another type of measurement:

* **measurementtype=frontend** will activate the frontend DB hook for measurement and allows the graph visualisation

#### Limitations

##### Makes YCSB a bit slower

// **TODO: determine how much & fill this !**

##### Database

The storage process, as it is implemented, should work regardless of the database thus you can use your custom adapters.
However, it has been tested with memcached only.