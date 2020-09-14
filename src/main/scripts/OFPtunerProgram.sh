#!/bin/bash
#PJM -g gp43

# module load mpijava #/1.2.7_ojdk1.8
export I_MPI_PIN=0

mpiexec.hydra -n ${PJM_MPI_PROC} \
taskset -ca ${JAVAGLB_CORE_RESTRICTION} \
java -cp ${JAVAGLB_JARPATH}/*:. \
-Djava.library.path=${JAVA_NATIVE_LIBRARIES} \
-Dglb.workunit=10 \
-Dglb.tuner=${PARAM} \
-Dglb.workers=${JAVAGLB_WORKERS} \
apgas.mpi.MPILauncher \
${MAIN} ${ARGS}
