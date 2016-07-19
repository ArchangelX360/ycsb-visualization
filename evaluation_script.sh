#!/usr/bin/env bash

# 1. Create ProgressBar function
# 1.1 Input is currentState($1) and totalState($2)
function ProgressBar() {
# Process data
    let _progress=(${1}*100/${2}*100)/100
    let _done=(${_progress}*4)/10
    let _left=40-$_done
    _fill=$(printf "%${_done}s")
    _empty=$(printf "%${_left}s")

    printf "\rProgress : [${_fill// /#}${_empty// /-}] ${_progress}%%"
}

launch() {
    # Launching benchmark
    output="$(${4}bin/ycsb ${2} memcached -P ${4}workloads/${3} -p measurementtype=${1} -p threadcount=${6} -p frontend.collection.benchmark=${5} -s)"
    res="$(echo ${output} | sed 's/.* Throughput(ops\/sec), \([0-9.,]*\).*/\1/g')"
}

# YCSB & evaluation parameters initialization
iterations=${1}
threadcount=${2}
workload=${3}
ycsbexec="/home/titouan/Documents/ycsb-web-app/ycsb-0.11.1-custom-release/"


# Evaluation variables initialization
adjustedIterations=$((${iterations} * 10))
totalAdjustedIterations=$((${adjustedIterations} * 2))
res_frontend_load=0
res_frontend_run=0
res_raw_load=0
res_raw_run=0
i=0

# Killing any memcached instance
killall -q memcached >> /dev/null 2>&1
sleep 1

ProgressBar ${i} ${totalAdjustedIterations}

# FRONTEND CYCLE
echo "iteration frontend_load frontend_run" > result.txt
while [ "$i" -lt "$adjustedIterations" ]; do
    # Preparing memcached environment
    /usr/bin/memcached -m 10240 -p 11211 -u titouan -l 127.0.0.1 &
    foo_pid=$!
    sleep 1

    launch frontend load ${workload} ${ycsbexec} bash${i} ${threadcount} >> /dev/null 2>&1
    res_frontend_load=$(echo "$res_frontend_load + $res" | bc)

    ProgressBar $(($i + 5)) ${totalAdjustedIterations}

    launch frontend run ${workload} ${ycsbexec} bash${i} ${threadcount} >> /dev/null 2>&1
    res_frontend_run=$(echo "$res_frontend_run + $res" | bc)

    i=$(($i + 10))
    ProgressBar ${i} ${totalAdjustedIterations}

    echo "$(($i / 10)) $res_frontend_load $res_frontend_run" >> result.txt

    kill -9 ${foo_pid} 2>/dev/null
    wait ${foo_pid} 2>/dev/null
done
average_frontend_load=$(echo "$res_frontend_load / ${iterations}" | bc)
average_frontend_run=$(echo "$res_frontend_run / ${iterations}" | bc)

# RAW CYCLE
echo "iteration raw_load raw_run" >> result.txt
while [ "$i" -lt "${totalAdjustedIterations}" ]; do
 # Preparing memcached environment
    /usr/bin/memcached -m 10240 -p 11211 -u titouan -l 127.0.0.1 &
    foo_pid=$!
    sleep 1

    launch raw load ${workload} ${ycsbexec} bash${i} ${threadcount} >> /dev/null 2>&1
    res_raw_load=$(echo "$res_raw_load + $res" | bc)

    ProgressBar $(($i + 5)) ${totalAdjustedIterations}

    launch raw run ${workload} ${ycsbexec} bash${i} ${threadcount} >> /dev/null 2>&1
    res_raw_run=$(echo "$res_raw_run + $res" | bc)

    i=$(($i + 10))
    ProgressBar ${i} ${totalAdjustedIterations}

    echo "$(($i / 10)) $res_raw_load $res_raw_run" >> result.txt

    kill -9 ${foo_pid} 2>/dev/null
    wait ${foo_pid} 2>/dev/null
done
average_raw_load=$(echo "$res_raw_load / ${iterations}" | bc)
average_raw_run=$(echo "$res_raw_run / ${iterations}" | bc)

# Displaying load results
echo "average_frontend_load average_raw_load" >> result.txt
echo "$average_frontend_load $average_raw_load" >> result.txt

# Displaying run results
echo "average_frontend_run average_raw_run" >> result.txt
echo "$average_frontend_run $average_raw_run" >> result.txt

# Displaying percentage results
echo "Percentage :"  >> result.txt
echo "$average_frontend_load * 100 / $average_raw_load" | bc  >> result.txt
echo "$average_frontend_run * 100 / $average_raw_run" | bc  >> result.txt

exit 0
