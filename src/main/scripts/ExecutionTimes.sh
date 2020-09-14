#!/bin/bash
################################################################################
# SCRIPT PARSING THE EXECUTION FILES TO GENERATE A TABLE OF THE EXEUCTION      #
# TIMES                                                                        #
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
DIR=$2
OUTPUT="$DIR/${PROBLEM}ExecutionTimes.csv"
RUNS=$3
FIXED_GRAINS=$4
TUNERS=$5

echo "Parsing files in $DIR/$PROBLEM to generate $OUTPUT"
touch ${OUTPUT}

# First, recreate a full table of the total computation.
# In order, the columns represent the | Fixed grains ... | Tuners ... |
# On each line we put the different Runs

########################### HEADER OF CSV FILE #################################
LINE="Run;"
for g in $FIXED_GRAINS
do
    LINE+="$g;"
done
for t in $TUNERS
do
    LINE+="$t;"
done
#echo $LINE
echo $LINE > ${OUTPUT}
#################### LINE BY LINE WE FILL THE OUTPUT FILE ######################
for (( r = 1; r <= $RUNS; r++ ))
do
    LINE="${r};"
	for g in $FIXED_GRAINS
	do
	    FILE="${DIR}/$PROBLEM/*${problem}-fixedGrain_Param${g}_Run${r}.txt"
	    LINE+=`grep "COMPUTATION TIME" $FILE | sed -r -e 's/.*?COMPUTATION TIME;(.*?);/\1/'`
        LINE+=";"
    done
	for t in $TUNERS
	do
        FILE="${DIR}/$PROBLEM/*${problem}-tuner_Param${t}_Run${r}.txt"
        LINE+=`grep "COMPUTATION TIME" $FILE | sed -r -e 's/.*?COMPUTATION TIME;(.*?);/\1/'`
        LINE+=";"
	done
#    echo $LINE
    echo $LINE >> ${OUTPUT}
done
