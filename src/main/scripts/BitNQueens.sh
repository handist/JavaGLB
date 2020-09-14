#!/bin/bash

#JAR=/home/patrick/apr2020/varyingNbWorkers/JAR
#JAR=/home/patrick/apgaslibs
JAR=${JAVAGLB_JARPATH}

#-Djava.library.path=/home/patrick/mpijava/lib \
# mpirun -n 4 \
# apgas.mpi.MPILauncher \

timeout -k 10m -s 9 10m  \
taskset -ca ${JAVAGLB_CORE_RESTRICTION} \
java -cp $JAR/*:. \
-Dapgas.java="taskset -ca ${JAVAGLB_CORE_RESTRICTION} java" \
-Dapgas.hostfile=${JAVAGLB_HOSTFILE} \
-Dapgas.places=${JAVAGLB_PLACES} \
-Dglb.workunit=$1 \
-Dglb.workers=${JAVAGLB_WORKERS} \
handist.glb.examples.nqueens.ParallelBitNQueens \
18


