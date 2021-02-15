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
OUTPUT_PREFIX=$1
shift

for param in $@
do
	STAMP=`date +"%m-%d-%y %R"`
	echo "[$STAMP] Starting $PROGRAM $MAIN $ARGS with parameter $param"

	for (( run=1; run<=$REPETITIONS; run++))
	do
		STAMP=`date +"%m-%d-%y %R"`
		echo "[$STAMP] Run $run"
		. ./$PROGRAM $param > ${OUTPUT_PREFIX}-param${param}_Run${run}.txt 2> ${OUTPUT_PREFIX}-param${param}_Run${run}.err.txt
	done

	STAMP=`date +"%m-%d-%y %R"`
done
