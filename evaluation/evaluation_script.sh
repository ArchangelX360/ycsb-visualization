#!/bin/bash

ProgressBar() {
    _progress_percent=$(($((${1}*100/${2}*100))/100))
    _done=$((${_progress_percent}*4/10))
    _left=$((40-${_done}))
    _fill=$(printf "%${_done}s")
    _empty=$(printf "%${_left}s")

    printf "\rProgress : [${_fill// /#}${_empty// /-}] ${_progress_percent}%%"
}

launch() {
    # Launching benchmark
    output="$(${4}bin/ycsb ${2} memcached -P ${4}workloads/${3} -p measurementtype=${1} -p threadcount=${6} -p frontend.collection.benchmark=${5} -p memcached.hosts=${7} -p frontend.db.uri=mongodb://${8}/ -p frontend.db.name=${9} -p frontend.collection.counters=${10} -s)"
    res="$(echo ${output} | sed 's/.* Throughput(ops\/sec), \([0-9.,]*\).*/\1/g')"
}

# YCSB & evaluation parameters initialization
iterations=${1}
thread_count=${2}
workload=${3}
memcached_host=${4}
memcached_address=$(echo ${memcached_host} | cut -f1 -d:)
memcached_port=$(echo ${memcached_host} | cut -f2 -d:)
storage_uri=${5}

# Custom parameters
storage_db_name="dbMeasurements"
storage_counters_collection_name="counters"
ycsb_root="/home/titouan/Documents/ycsb-web-app/ycsb-0.11.1-custom-release/"
unique_id="I${1}-W${3}-M${4}-T${2}-S${5}"
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
    # Preparing memcached environment
    echo 'flush_all' | nc ${memcached_address} ${memcached_port} >> /dev/null 2>&1

    FRONTEND_CYCLE_START=$(date +%s.%N)

    launch frontend load ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_host} ${storage_uri} ${storage_db_name} ${storage_counters_collection_name} >> /dev/null 2>&1
    current_res_frontend_load=$res
    res_frontend_load=$(echo "$res_frontend_load + $res" | bc)

    ProgressBar $(($i + 5)) ${total_adjusted_iterations}

    launch frontend run ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_host} ${storage_uri} ${storage_db_name} ${storage_counters_collection_name} >> /dev/null 2>&1
    current_res_frontend_run=$res
    res_frontend_run=$(echo "$res_frontend_run + $res" | bc)

    FRONTEND_CYCLE_END=$(date +%s.%N)
    FRONTEND_EXECUTION_TIME=$(echo "$FRONTEND_CYCLE_END - $FRONTEND_CYCLE_START" | bc)

    # Cleaning up storage DB
    mongo "${storage_uri}/${storage_db_name}" --eval "db[\"${unique_id}bash${i}\"].drop()" >> /dev/null 2>&1
    mongo "${storage_uri}/${storage_db_name}" --eval "db[\"${storage_counters_collection_name}\"].remove({\"collection\":\"${unique_id}bash${i}\"})" >> /dev/null 2>&1

    i=$(($i + 10))
    ProgressBar ${i} ${total_adjusted_iterations}

    echo "        {" >> ${output_file}
    echo "            \"iteration\": $(($i / 10))," >> ${output_file}
    echo "            \"type\": \"frontend\"," >> ${output_file}
    echo "            \"load\": $current_res_frontend_load," >> ${output_file}
    echo "            \"run\": $current_res_frontend_run" >> ${output_file}
    echo "        }," >> ${output_file}
done

# RAW CYCLE
while [ "$i" -lt "${total_adjusted_iterations}" ]
 do
    # Preparing memcached environment
    echo 'flush_all' | nc ${memcached_address} ${memcached_port} >> /dev/null 2>&1

    RAW_CYCLE_START=$(date +%s.%N)

    launch raw load ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_host} ${storage_uri} ${storage_db_name} ${storage_counters_collection_name} >> /dev/null 2>&1
    current_res_raw_load=$res
    res_raw_load=$(echo "$res_raw_load + $res" | bc)

    ProgressBar $(($i + 5)) ${total_adjusted_iterations}

    launch raw run ${workload} ${ycsb_root} ${unique_id}bash${i} ${thread_count} ${memcached_host} ${storage_uri} ${storage_db_name} ${storage_counters_collection_name} >> /dev/null 2>&1
    current_res_raw_run=$res
    res_raw_run=$(echo "$res_raw_run + $res" | bc)

    RAW_CYCLE_END=$(date +%s.%N)
    RAW_EXECUTION_TIME=$(echo "$RAW_CYCLE_END - $RAW_CYCLE_START" | bc)

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
done

average_frontend_load=$(echo "$res_frontend_load / ${iterations}" | bc)
average_frontend_run=$(echo "$res_frontend_run / ${iterations}" | bc)
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
echo "            \"average_load\": ${percent_average_load}," >> ${output_file}
echo "            \"average_run\": ${percent_average_run}," >> ${output_file}
echo "            \"execution_time\": ${percent_execution_time}" >> ${output_file}
echo "        }" >> ${output_file}
echo "    }" >> ${output_file}
echo "}" >> ${output_file}

exit 0
