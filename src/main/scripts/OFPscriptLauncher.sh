#!/bin/bash

##############################################################
# Script for launching a script multiple times               #
#                                                            #
# 1st argument: script to launch                             #
# 2nd argument: number of launches for the above program     #
# 3rd argument: directory from which to launch the program   #
# 4th argument: output file prefix for each individual       #
#               program execution (used for both stdout and  #
#               stderr)                                      #
#                                                            #
#                                                            #
##############################################################


SCRIPT=$1 # Job script (OFPtunerProgram / OFPgrainProgram) with absolute path
shift
REPETITIONS=$1 # Number of time the execution is desired
shift
LAUNCHDIR=$1
shift
PREFIX=$1 #Prefix for the output files (-o $PREFIX_RunX.txt -e $PREFIX_RunX.err.txt
shift

cd $LAUNCHDIR

# We submit $REPETITIONS times the specified script/main/args for each parameter
for param in $@ 
do
	export PARAM=$param
	STAMP=`date +"%m-%d-%y %R"`
	echo "# [$STAMP] Submitting $SCRIPT with main: $MAIN $ARGS and parameter $param"
	for (( run=1; run<=$REPETITIONS; run++))
	do
	    echo "[Run ${run}]`pjsub -X --rsc-list rscgrp=regular-flat,node=${JAVAGLB_PLACES},elapse=${TIMEOUT} -o ${PREFIX}-param${param}_Run${run}.txt -e ${PREFIX}-param${param}_Run${run}.err.txt ${SCRIPT} 2>&1`"
	done
done

cd ../..
