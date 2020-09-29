#!/bin/bash

##############################################################
# Script for launching a script multiple times               #
#                                                            #
# 1st argument: script to launch                             #
# 2nd argument: repetitions                                  #
# 3rd argument: output file prefix for each individual       #
#               program execution                            #
#                                                            #
#                                                            #
##############################################################

PROGRAM=$1
shift
REPETITIONS=$1
shift
PREFIX=$1
shift

for param in $@
do
	STAMP=`date +"%m-%d-%y %R"`
	echo "[$STAMP] Starting executions with parameter $param"

	for (( run=1; run<=$REPETITIONS; run++))
	do
		STAMP=`date +"%m-%d-%y %R"`
		echo "[$STAMP] Launching $PROGRAM $param run ${run}"
		nohup bash $PROGRAM $param > ${PREFIX}-param${param}_Run${run}.txt 2> ${PREFIX}-param${param}_Run${run}.err.txt
	done

	STAMP=`date +"%m-%d-%y %R"`
	echo "[$STAMP] Executions with parameter $param completed"
done
