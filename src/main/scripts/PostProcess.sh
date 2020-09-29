#!/bin/bash

##########################################################
# Script in charge of compiling the results of the       #
# benchmarks into csv files. It will also generate plots #
# of the grain chosen by the tuner for every tuner       #
# execution.                                             #
##########################################################
# ARGUMENTS                                              #
# $1: directory name in which the files to process lie   #
##########################################################
# CONFIGURATION
# Fixed grain sizes to process
FIXED_GRAINS="8 32 128 512 2048 8192 32768 131072 524288 2097152"
# Tuners to process
#TUNERS=""
TUNERS="handist.glb.tuning.Ntuner handist.glb.tuning.KamadaTuner"
##########################################################
PREFIX=${1%/}
DIR=$1
NB_HOSTS=7 #Is actually 4 but for gnuplot script, it's [0,3]
let HOSTS=$NB_HOSTS+1
##########################################################
echo "Checking directory structure and file presence"
cd $DIR
for problem in NQueens Pentomino UTS TSP
do
	if [[ -d $problem ]]
	then
		echo "[OK] Directory $problem present"
		cd $problem
		runs=5
		if [ $problem == "TSP" ]
		then
		    runs=10
		fi
		for (( r = 1; r <= $runs; r++ ))
		do
			for g in $FIXED_GRAINS
			do
				FILE=${PREFIX}_${problem}-fixedGrain-param${g}_Run${r}.txt
				if [[ -f $FILE ]]
				then
					echo "[OK] file ${problem}/$FILE found"
				else
				    echo "[KO] file ${problem}/$FILE not found"
				    exit
				fi
			done
			for t in $TUNERS
			do
				FILE=${PREFIX}_${problem}-tuner-param${t}_Run${r}.txt
				if [[ -f $FILE ]]
				then
				    echo "[OK] file ${problem}/$FILE found"
				else
				    echo "[KO] file ${problem}/$FILE not found"
				    exit
				fi
			done
		done
		cd ..
	else
		echo "[KO] Directory $problem not found"
		exit
	fi
done
cd ..
##########################################################
# PARSING each problem's files                           #
##########################################################

for problem in Pentomino UTS TSP NQueens
do
	runs=5
	if [ $problem == "TSP" ]
	then
	    runs=10
	fi
	# Generating execution times only, in a handy table
	./ExecutionTimes.sh $problem ${DIR} $runs "${FIXED_GRAINS}" "${TUNERS}"
	# Extracting more detailed information on queue accesses
	. ./ExecTimesAndQueueAccesses.sh $problem ${DIR} $runs $FIXED_GRAINS $TUNERS
done


#############################################################
# GENERATING Grain size / Time plots                        #
#############################################################

mkdir -p TunerPlots

for problem in Pentomino NQueens Pentomino UTS TSP
do
	runs=5
	if [ $problem == "TSP" ]
	then
	    runs=10
	fi

	for t in $TUNERS
	do
		for (( r = 1; r <= $runs; r++ ))
		do
			FILE=$DIR/${problem}/${PREFIX}_${problem}-tuner-param${t}_Run${r}.txt

			EXEC_TIME=`grep COMPUTATION $FILE | sed -r -e 's/COMPUTATION TIME;([0-9]*)\.([0-9]*);/\1/'`
			sed -n '/Place(0) stamp/,$p' $FILE > data.csv
			#echo $EXEC_TIME
			#cat data.csv

			# Call the gnuplot script to generate image
			OUTPUT=$DIR/$problem/${PREFIX}_${problem}-${t}-GrainEvolution_Run${r}.png
			echo "Creating $OUTPUT"
			gnuplot -c TunerGrain.plt $EXEC_TIME $NB_HOSTS data.csv > $OUTPUT
		done
	done

	cp $DIR/${problem}/*.png TunerPlots
done
