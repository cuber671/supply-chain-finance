#!/bin/bash
SHELL_FOLDER=$(cd $(dirname $0);pwd)

LOG_ERROR() {
    content=${1}
    echo -e "\033[31m[ERROR] ${content}\033[0m"
}

LOG_INFO() {
    content=${1}
    echo -e "\033[32m[INFO] ${content}\033[0m"
}

fisco_bcos=${SHELL_FOLDER}/../fisco-bcos
export RUST_LOG=bcos_wasm=error
cd ${SHELL_FOLDER}
node=$(basename ${SHELL_FOLDER})
node_pid=$(docker ps |grep ${SHELL_FOLDER//\//} | grep -v grep|awk '{print $1}')
ulimit -n 1024
#start monitor
dirs=($(ls -l ${SHELL_FOLDER} | awk '/^d/ {print $NF}'))
for dir in ${dirs[*]}
do
    if [[ -f "${SHELL_FOLDER}/${dir}/node.mtail" && -f "${SHELL_FOLDER}/${dir}/start_mtail_monitor.sh" ]];then
        echo "try to start ${dir}"
        bash ${SHELL_FOLDER}/${dir}/start_mtail_monitor.sh &
    fi
done


if [ ! -z ${node_pid} ];then
    kill -USR1 ${node_pid}
    sleep 0.2
    kill -USR2 ${node_pid}
    sleep 0.2
    echo " ${node} is running, container id is $node_pid."
    exit 0
else
    docker run -d --rm --name ${SHELL_FOLDER//\//} -v ${SHELL_FOLDER}:/data --network=host -w=/data fiscoorg/fiscobcos:v3.12.1 -c config.ini -g config.genesis
    sleep 1.5
fi
try_times=4
i=0
while [ $i -lt ${try_times} ]
do
    node_pid=$(docker ps |grep ${SHELL_FOLDER//\//} | grep -v grep|awk '{print $1}')
    success_flag=success
    if [[ ! -z ${node_pid} && ! -z "${success_flag}" ]];then
        echo -e "\033[32m ${node} start successfully pid=${node_pid}\033[0m"
        exit 0
    fi
    sleep 0.5
    ((i=i+1))
done
echo -e "\033[31m  Exceed waiting time. Please try again to start ${node} \033[0m"
tail -n20 $(docker inspect --format='{{.LogPath}}' ${SHELL_FOLDER//\//})
