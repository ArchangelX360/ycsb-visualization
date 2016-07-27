#!/bin/bash


ITERATION=50

MEMCACHED_URIS=( 127.0.0.1:11211 141.76.44.145:11211 )
MONGO_URIS=( 127.0.0.1:27017 141.76.44.145:27017 )
WORKLOADS=( a1k a5k a10k a20k a50k a100k a1000k a2000k a3000k a4000k a5000k a10000k )
THREADS=( 1 4 )
MEASUREMENTS=( raw ) # No other measurement supported at the moment

for workload in ${WORKLOADS[@]}; do
    for mongo_uri in ${MONGO_URIS[@]}; do
        for memcached_uri in ${MEMCACHED_URIS[@]}; do
            for thread in ${THREADS[@]}; do
                for measurement in ${MEASUREMENTS[@]}; do
                    echo -e "\nLaunching : ${ITERATION} ${thread} ${workload} ${memcached_uri} ${mongo_uri}"
                    ./evaluation_script.sh ${ITERATION} ${thread} ${workload} ${memcached_uri} ${mongo_uri}
                done
            done
        done
    done
done
