#!/bin/bash

# 1. Create ProgressBar function
# 1.1 Input is currentState($1) and totalState($2)
ProgressBar() {
# Process data
    _progress_percent=$(($((${1}*100/${2}*100))/100))
    _done=$((${_progress_percent}*4/10))
    _left=$((40-${_done}))
    _fill=$(printf "%${_done}s")
    _empty=$(printf "%${_left}s")

    printf "\rProgress : [${_fill// /#}${_empty// /-}] ${_progress_percent}%%"
}

launch() {
    # Launching benchmark
    output="$(${4}bin/ycsb ${2} memcached -P ${4}workloads/${3} -p measurementtype=${1} -p threadcount=${6} -p frontend.collection.benchmark=${5} -p memcached.hosts=${7} -s)"
    res="$(echo ${output} | sed 's/.* Throughput(ops\/sec), \([0-9.,]*\).*/\1/g')"
}

# YCSB & evaluation parameters initialization
iterations=${1}
thread_count=${2}
workload=${3}
memcached_address=${4}
ycsb_root="/home/titouan/Documents/ycsb-web-app/ycsb-0.11.1-custom-release/"
unique_id="results-I${1}-T${2}-W${3}-M${4}"
output_file="/home/titouan/Documents/ycsb-web-app/public/evaluations/${unique_id}.json"


# Evaluation variables initialization
adjusted_iterations=$((${iterations} * 10))
total_adjusted_iterations=$((${adjusted_iterations} * 2))
res_frontend_load=0
res_frontend_run=0
res_raw_load=0
res_raw_run=0
i=0

ProgressBar ${i} ${total_adjusted_iterations}

# FRONTEND CYCLE
echo "{" > ${output_file}
echo "    \"data\": [" >> ${output_file}
while [ "$i" -lt "$adjusted_iterations" ]
 do
    FRONTEND_CYCLE_START=$(date +%s.%N)
    # Preparing memcached environment
    echo 'flush_all' | nc ${memcached_address} 11211  >> /dev/null 2>&1

    launch frontend load ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_address} >> /dev/null 2>&1
    current_res_frontend_load=$res
    res_frontend_load=$(echo "$res_frontend_load + $res" | bc)

    ProgressBar $(($i + 5)) ${total_adjusted_iterations}

    launch frontend run ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_address} >> /dev/null 2>&1
    current_res_frontend_run=$res
    res_frontend_run=$(echo "$res_frontend_run + $res" | bc)

    i=$(($i + 10))
    ProgressBar ${i} ${total_adjusted_iterations}

    echo "        {" >> ${output_file}
    echo "            \"iteration\": $(($i / 10))," >> ${output_file}
    echo "            \"type\": \"frontend\"," >> ${output_file}
    echo "            \"load\": $current_res_frontend_load," >> ${output_file}
    echo "            \"run\": $current_res_frontend_run" >> ${output_file}
    echo "        }," >> ${output_file}

    FRONTEND_CYCLE_END=$(date +%s.%N)
    FRONTEND_EXECUTION_TIME=$(echo "$FRONTEND_CYCLE_END - $FRONTEND_CYCLE_START" | bc)
done
average_frontend_load=$(echo "$res_frontend_load / ${iterations}" | bc)
average_frontend_run=$(echo "$res_frontend_run / ${iterations}" | bc)

# RAW CYCLE
while [ "$i" -lt "${total_adjusted_iterations}" ]
 do
    RAW_CYCLE_START=$(date +%s.%N)
 # Preparing memcached environment
    echo 'flush_all' | nc ${memcached_address} 11211 >> /dev/null 2>&1

    launch raw load ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_address} >> /dev/null 2>&1
    current_res_raw_load=$res
    res_raw_load=$(echo "$res_raw_load + $res" | bc)

    ProgressBar $(($i + 5)) ${total_adjusted_iterations}

    launch raw run ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_address} >> /dev/null 2>&1
    current_res_raw_run=$res
    res_raw_run=$(echo "$res_raw_run + $res" | bc)

    i=$(($i + 10))
    ProgressBar ${i} ${total_adjusted_iterations}

    echo "        {" >> ${output_file}
    echo "            \"iteration\": $(($i / 10 / 2))," >> ${output_file}
    echo "            \"type\": \"raw\"," >> ${output_file}
    echo "            \"load\": $current_res_raw_load," >> ${output_file}
    echo "            \"run\": $current_res_raw_run" >> ${output_file}

    if [ "$i" -lt "${total_adjusted_iterations}" ]
    then
        echo "        }," >> ${output_file}
    else
        echo "        }" >> ${output_file}
    fi

    RAW_CYCLE_END=$(date +%s.%N)
    RAW_EXECUTION_TIME=$(echo "$RAW_CYCLE_END - $RAW_CYCLE_START" | bc)
done
average_raw_load=$(echo "$res_raw_load / ${iterations}" | bc)
average_raw_run=$(echo "$res_raw_run / ${iterations}" | bc)

percent_average_load=$(echo "$average_frontend_load * 100 / $average_raw_load" | bc)
percent_average_run=$(echo "$average_frontend_run * 100 / $average_raw_run" | bc)
percent_execution_time=$(echo "${RAW_EXECUTION_TIME} * 100 / ${FRONTEND_EXECUTION_TIME}" | bc)

echo "    ]," >> ${output_file}
echo "    \"results\": {" >> ${output_file}
echo "        \"averages_load\": {" >> ${output_file}
echo "            \"raw\": $average_raw_load," >> ${output_file}
echo "            \"frontend\": $average_frontend_load" >> ${output_file}
echo "        }," >> ${output_file}
echo "        \"averages_run\": {" >> ${output_file}
echo "            \"raw\": $average_raw_run," >> ${output_file}
echo "            \"frontend\": $average_frontend_run" >> ${output_file}
echo "        }," >> ${output_file}
echo "        \"execution_time\": {" >> ${output_file}
echo "            \"raw\": ${RAW_EXECUTION_TIME}," >> ${output_file}
echo "            \"frontend\": ${FRONTEND_EXECUTION_TIME}" >> ${output_file}
echo "        }," >> ${output_file}
echo "        \"percents\": {" >> ${output_file}
echo "            \"average_run\": ${percent_average_load}," >> ${output_file}
echo "            \"average_load\": ${percent_average_run}," >> ${output_file}
echo "            \"execution_time\": ${percent_execution_time}" >> ${output_file}
echo "        }" >> ${output_file}
echo "    }" >> ${output_file}
echo "}" >> ${output_file}

exit 0
