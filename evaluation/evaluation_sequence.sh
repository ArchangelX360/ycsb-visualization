#!/bin/bash

MEMCACHED_ADDRESSES=( 127.0.0.1 141.76.44.145 )
WORKLOADS=( workloada workloadaR workloada100k workloadaR100k workloada1M workloadaR1M workloada10M workloadaR10M )
THREADS=( 1 4 )
MEASUREMENTS=( raw )

for workload in ${WORKLOADS[@]}; do
    for address in ${MEMCACHED_ADDRESSES[@]}; do
        for thread in ${THREADS[@]}; do
            for measurement in ${MEASUREMENTS[@]}; do
                echo "Launching : 100 ${thread} ${workload} ${address}"
                ./evaluation_script.sh 100 ${thread} ${workload} ${address}
            done
        done
    done
done