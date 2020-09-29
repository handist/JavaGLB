#!/bin/bash
################################################################################
# SCRIPT PARSING THE EXECUTION FILES TO GENERATE A TABLE OF THE EXEUCTION      #
# TIMES AND THE 
################################################################################
# $1: Name of the problem                                                      #
# $2: Directory in which the files to parse are located                        #
# $3: Number of runs to parse                                                  #
# $4: FixedGrains                                                              #
# $5: Tuners                                                                   #
################################################################################
# OUTPUT: in the current directory, file "$1-ExecutionTimes,csv"               #
################################################################################
PROBLEM=$1
shift
DIR=$1
shift
OUTPUT="$DIR/${PROBLEM}_ExecutionTimesAndQueueAccesses.csv"
RUNS=$1
shift
#FIXED_GRAINS=$4
#TUNERS=$5

echo "Parsing files in $DIR/$PROBLEM to generate $OUTPUT"
touch ${OUTPUT}

# First, recreate a full table of the total computation.
# In order, the columns represent the | Fixed grains ... | Tuners ... |
# On each line we put the different Runs

########################### HEADER OF CSV FILE #################################
# The columns of the table created by this script are:
# - The Parameter the program was run with (grain size / tuner)
# - The run number of the execution considered
# - The host number of the queue events of this line
# - The execution time of the program (identical for all hosts of the same run
# - The number of times the intra-bag of the host was "split"
# - The number of times the intra-bag of the host was "merged"
# - The number of times the intra-bag of the host was "emptied"
# - The number of times the inter-bag of the host was "split"
# - The number of times the inter-bag of the host was "merged"
# - The number of times the inter-bag of the host was "emptied"
LINE="Program;Run;Host;Exec Time;Intra-bag split;Intra-bag merge;Intra-bag empty;Inter-bag split;Inter-bag merge;Inter-bag empty"
echo $LINE > ${OUTPUT}

#echo $LINE
#################### LINE BY LINE WE FILL THE OUTPUT FILE ######################
for g in $@
do
    for (( r = 1; r <= $RUNS; r++ ))
    do
	FILE=`ls ${DIR}/$PROBLEM/*-param${g}_Run${r}.txt`
	COMPTIME=`grep "COMPUTATION TIME" $FILE | sed -r -e 's/.*?COMPUTATION TIME;(.*?);/\1/'`
	sed -e '1,/Place;Worker/d; /WORKER DATA/,$d' $FILE | head -n $HOSTS | sed -n -e "s/^\([0-9][0-9]*\);[0-9]*;\([0-9]*;[0-9]*;[0-9]*;[0-9]*;[0-9]*;[0-9]*\);.*/ $g ; $r ; \1 ; $COMPTIME ; \2/p" >> ${OUTPUT}
    done
done


