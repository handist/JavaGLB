#!/bin/bash

timeout -k 10m -s 9 10m  \
mpirun -np ${JAVAGLB_PLACES} --map-by ppr:1:node --hostfile {JAVAGLB_HOSTFILE} \
taskset -ca ${JAVAGLB_CORE_RESTRICTION} \
java -cp ${JAVAGLB_JARPATH}/*:. \
-Djava.library.path=${JAVA_NATIVE_LIBRARIES} \
-Dglb.workunit=10 \
-Dglb.workers=${JAVAGLB_WORKERS} \
-Dglb.tuner=$1 \
apgas.mpi.MPILauncher \
handist.glb.examples.uts.MultiworkerUTS \
${UTS_ARGS}
