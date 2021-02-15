#!/bin/bash

timeout -k ${TIMEOUT} -s 9 ${TIMEOUT} \
mpirun -np ${PLACES} --map-by ppr:${PROC_PER_HOST}:node --hostfile ${HOSTFILE} \
taskset -ca ${CORE_RESTRICTION} \
java -cp ${JAVAGLB_JARPATH}/*:. \
-Djava.library.path=${JAVA_NATIVE_LIBRARIES} \
-Dglb.workunit=$1 \
-Dglb.workers=${WORKERS} \
apgas.mpi.MPILauncher \
${MAIN} ${ARGS}
