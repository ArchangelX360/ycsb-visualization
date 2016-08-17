#!/bin/bash

ITERATION=20

workload=workloada
MEMCACHED_URIS=( 127.0.0.1:11211 10.0.0.3:11211 )
#MEMCACHED_URIS=( 127.0.0.1:11211 )
MONGO_URIS=( 127.0.0.1:27017 10.0.0.3:27017 )
#MONGO_URIS=( 127.0.0.1:27017 )
POINTS=( 1000 2500 5000 7500 10000 15000 30000 45000 60000 75000 90000 105000 120000 135000 150000 200000 250000 300000 350000 400000 450000 500000 600000 700000 800000 900000 1000000)
THREADS=( 1 4 )
#THREADS=( 1 )
MEASUREMENTS=( raw ) # No other measurement supported at the moment

#for workload in ${WORKLOADS[@]}; do
for point in ${POINTS[@]}; do
    for mongo_uri in ${MONGO_URIS[@]}; do
        for memcached_uri in ${MEMCACHED_URIS[@]}; do
            for thread in ${THREADS[@]}; do
                for measurement in ${MEASUREMENTS[@]}; do
                    echo -e "\n$(date +"[%d-%m-%YT%T] ")Launching : ${ITERATION} ${thread} ${workload} ${memcached_uri} ${mongo_uri} ${point}"
                    ./evaluation_script.sh ${ITERATION} ${thread} ${workload} ${memcached_uri} ${mongo_uri} ${point}
                done
            done
        done
    done
done
