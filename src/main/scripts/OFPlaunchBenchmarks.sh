#!/bin/bash
#####################################################################
# - OFPlaunchBenchmarks.sh                                          #
# Bash script used to launch the benchmarks of the GLB on the       #
# OakForest-PACS supercomputer                                      #
# This script will launch a series of "fixed grain" executions to   #
# establish what the performance to expect from the programs is.    #
# Then, the tuners evaluation is made.                              #
#####################################################################
# Arguments                                                         #
# $1 : Directory in which all the files will be placed and prefix   #
#      to all output files. This should represent the cluster under #
#      consideration for the tests                                  #
#####################################################################

####################### THINGS TO EVALUATE ##########################
# PARAMETERS CUSTOMIZABLE IN SCRIPT
# Fixed grain sizes to check
FIXED_GRAINS=""
#FIXED_GRAINS="8 32 128 512 2048 8192 32768 131072 524288 2097152"
# FIXED_GRAINS="8 32 128 512 2048"
#FIXED_GRAINS="131072 524288 2097152"
# Tuners to evaluate
#TUNERS=""
#TUNERS="handist.glb.tuning.Ntuner handist.glb.tuning.Newtuner14 handist.glb.tuning.Newtuner13 handist.glb.tuning.Newtuner12 handist.glb.tuning.Newtuner11" 
# handist.glb.tuning.KamadaTuner"
#TUNERS="handist.glb.tuning.Newtuner14 handist.glb.tuning.Newtuner13 handist.glb.tuning.Newtuner12 handist.glb.tuning.Newtuner11" 
#TUNERS="handist.glb.tuning.SpuriousTuner14 handist.glb.tuning.SpuriousTuner13 handist.glb.tuning.SpuriousTuner12 handist.glb.tuning.SpuriousTuner11" 
TUNERS="handist.glb.tuning.Newtuner13 handist.glb.tuning.SpuriousTuner13v2"
######################## PROGRAM ARGUMENTS ##########################
# NQueens arguments
NQUEENS_ARGS="-n 19 -w 16"
# Pentomino arguments
PENTOMINO_ARGS="-w 9 -h 10"
# TSP arguments (problem file, subset of citites)
TSP_ARGS="-f /work/gp43/p43001/posner40.atsp -s 35"
# UTS arguments (branching factor and depth)
UTS_ARGS="-b 4 -d 18"

######################### GLB CONFIGURATION #########################
JAVAGLB_JARS=/work/gp43/p43001/apgaslibs
JAVA_NATIVE_LIBRARIES=/work/gp43/p43001/mpijava/lib
JAVAGLB_PLACES=4
JAVAGLB_WORKERS=68
JAVAGLB_CORE_RESTRICTION="0-67"
TIMEOUT="40:00"


# Exporting the configuration for it to be available in OFPscriptLauncher
echo "############## Configuration ###############"
echo "# Path to JARs:     $JAVAGLB_JARS contains the following"
ls -l $JAVAGLB_JARS | sed -e '1d;s/\(.*\)/# \1/'
export JAVAGLB_JARPATH=$JAVAGLB_JARS
echo "# Native libraries path: ${JAVA_NATIVE_LIBRARIES} which contains the following"
ls -l ${JAVA_NATIVE_LIBRARIES} | sed -e '1d;s/\(.*\)/# \1/'
export JAVA_NATIVE_LIBRARIES=${JAVA_NATIVE_LIBRARIES} 
echo "# Nb of hosts:      $JAVAGLB_PLACES"
export JAVAGLB_PLACES=$JAVAGLB_PLACES
echo "# Nb of workers:    $JAVAGLB_WORKERS"
export JAVAGLB_WORKERS=$JAVAGLB_WORKERS
echo "# Core constraints: $JAVAGLB_CORE_RESTRICTION"
export JAVAGLB_CORE_RESTRICTION=$JAVAGLB_CORE_RESTRICTION
echo "# Hostfile:    UNUSED ON OFP    "
echo "# Timeout for indivudual executions: $TIMEOUT"
export TIMEOUT=$TIMEOUT
#####################################################################
echo "#### Checking presence of script files #####"
SCRIPTDIR=`dirname $0`

for script in OFPscriptLauncher.sh OFPgrainProgram.sh OFPtunerProgram.sh
do
	if [[ -f $SCRIPTDIR/$script ]]
	then
		echo "# [OK] Script $script found"
	else
		echo "# [KO] Script $script not found !!!"
		echo "Exiting"
		exit
	fi
done
#####################################################################
echo "############## Creating directories ###########"
DIR=$1
PREFIX=$1
shift
echo "# - $DIR `mkdir $DIR 2>&1`"
cd $DIR
for d in Pentomino UTS TSP NQueens UTSsplit1 UTSsplit2 UTSsplit3
do
    echo "# - $DIR/$d `mkdir $d 2>&1`"
done
cd ..

echo "############## LAUNCHING PROGRAMS ###############"

HERE=`pwd`
echo "We are here: $HERE"
################# LAUNCHING NQUEENS BENCHMARK #######################
echo "# Launching NQueens with arguments: $NQUEENS_ARGS"
# export ARGS=$NQUEENS_ARGS
export MAIN=handist.glb.examples.nqueens.ParallelNQueens
export ARGS=$NQUEENS_ARGS

#. ./OFPscriptLauncher.sh $HERE/OFPgrainProgram.sh 5 ${DIR}/NQueens ${DIR}_NQueens-fixedGrain $FIXED_GRAINS
. ./OFPscriptLauncher.sh $HERE/OFPtunerProgram.sh 5 ${DIR}/NQueens ${DIR}_NQueens-tuner $TUNERS
################ LAUNCHING PENTOMINO BENCHMARK ######################
echo "Launching Pentomino with arguments: $PENTOMINO_ARGS"
export MAIN=handist.glb.examples.pentomino.ParallelPentomino
export ARGS=$PENTOMINO_ARGS

#. ./OFPscriptLauncher.sh $HERE/OFPgrainProgram.sh 5 ${DIR}/Pentomino ${DIR}_Pentomino-fixedGrain $FIXED_GRAINS
. ./OFPscriptLauncher.sh $HERE/OFPtunerProgram.sh 5 ${DIR}/Pentomino ${DIR}_Pentomino-tuner $TUNERS
##################### LAUNCHING UTS BENCHMARK #######################
echo "Launching UTS with arguments: $UTS_ARGS"
export MAIN=handist.glb.examples.uts.MultiworkerUTS
export ARGS=$UTS_ARGS

#. ./OFPscriptLauncher.sh $HERE/OFPgrainProgram.sh 5 ${DIR}/UTS ${DIR}_UTS-fixedGrain $FIXED_GRAINS
. ./OFPscriptLauncher.sh $HERE/OFPtunerProgram.sh 5 ${DIR}/UTS ${DIR}_UTS-tuner $TUNERS
#################### LAUNCHING TSP BENCHMARK ########################
echo "Launching TSP with arguments: $TSP_ARGS"
export MAIN=handist.glb.examples.tsp.GlobalTsp
export ARGS=$TSP_ARGS


#. ./OFPscriptLauncher.sh $HERE/OFPgrainProgram.sh 10 ${DIR}/TSP ${DIR}_TSP-fixedGrain $FIXED_GRAINS
. ./OFPscriptLauncher.sh $HERE/OFPtunerProgram.sh 10 ${DIR}/TSP ${DIR}_TSP-tuner $TUNERS
##################### LAUNCHING UTS split 1 #######################
echo "Launching UTS with arguments: $UTS_ARGS"
export MAIN=handist.glb.examples.uts.MultiworkerUTSsplit1
export ARGS=$UTS_ARGS

#. ./OFPscriptLauncher.sh $HERE/OFPgrainProgram.sh 5 ${DIR}/UTSsplit1 ${DIR}_UTSsplit1-fixedGrain $FIXED_GRAINS
. ./OFPscriptLauncher.sh $HERE/OFPtunerProgram.sh 5 ${DIR}/UTSsplit1 ${DIR}_UTSsplit1-tuner $TUNERS

##################### LAUNCHING UTS split 2 #######################
#echo "Launching UTS with arguments: $UTS_ARGS"
#export MAIN=handist.glb.examples.uts.MultiworkerUTSsplit2
#export ARGS=$UTS_ARGS

#. ./OFPscriptLauncher.sh $HERE/OFPgrainProgram.sh 10 ${DIR}/UTSsplit2 ${DIR}_UTSsplit2-fixedGrain $FIXED_GRAINS
#. ./OFPscriptLauncher.sh $HERE/OFPtunerProgram.sh 10 ${DIR}/UTSsplit2 ${DIR}_UTSsplit2-tuner $TUNERS

##################### LAUNCHING UTS split 3 #######################
echo "Launching UTS with arguments: $UTS_ARGS"
export MAIN=handist.glb.examples.uts.MultiworkerUTSsplit3
export ARGS=$UTS_ARGS

#. ./OFPscriptLauncher.sh $HERE/OFPgrainProgram.sh 5 ${DIR}/UTSsplit3 ${DIR}_UTSsplit3-fixedGrain $FIXED_GRAINS
. ./OFPscriptLauncher.sh $HERE/OFPtunerProgram.sh 5 ${DIR}/UTSsplit3 ${DIR}_UTSsplit3-tuner $TUNERS
