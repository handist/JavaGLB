#!/bin/bash
#####################################################################
# - launchBenchmarks.sh                                             #
# Bash script used to launch the benchmarks of the GLB on a         #
# Beowulf cluster.                                                  #
# This script will launch a series of "fixed grain" executions to   #
# establish what the performance to expect from the programs is.    #
# Then, the tuners evaluation is made.                              #
#####################################################################
# Arguments                                                         #
# $1 : Directory in which all the files will be placed and prefix   #
#      to all output files                                          #
# $2 : Hostfile indicating on which hosts the program should run    #
#####################################################################

####################### THINGS TO EVALUATE ##########################
# PARAMETERS CUSTOMIZABLE IN SCRIPT
# Fixed grain sizes to check
FIXED_GRAINS="8 32 128 512 2048 8192 32768 131072 524288 2097152"
#FIXED_GRAINS="2048"
# Tuners to evaluate
TUNERS="handist.glb.tuning.Ntuner handist.glb.tuning.Newtuner14 handist.glb.tuning.Newtuner13 handist.glb.tuning.Newtuner12 handist.glb.tuning.Newtuner11"
######################## PROGRAM ARGUMENTS ##########################
DEFAULT_REPS=5
# NQueens arguments
NQUEENS_ARGS="-n 18 -w 13"
NQUEENS_REPS=$DEFAULT_REPS
# Pentomino arguments
PENTOMINO_ARGS="-w 9 -h 10"
PENTOMINO_REPS=$DEFAULT_REPS
# TSP arguments (problem file, subset of citites)
TSP_ARGS="-f posner40.atsp -s 24"
TSP_REPS=10
# UTS arguments (branching factor and depth)
UTS_ARGS="-b 4 -d 17"
UTS_REPS=$DEFAULT_REPS
######################### GLB CONFIGURATION #########################
JAVAGLB_JARPATH=/home/patrick/apgaslibs
JAVA_NATIVE_LIBRARIES=/home/patrick/mpijava/lib
PLACES=1
WORKERS=24
CORE_RESTRICTION="0-23"
TIMEOUT=10m
PROC_PER_HOST=1
HOSTFILE=$2

# Exporting the configuration for it to be available in the scripts
echo "###################### Configuration ##########################"
echo "# Path to JARs:     $JAVAGLB_JARPATH contains the following"
ls -l $JAVAGLB_JARS | sed -e '1d'
echo "# Native libraries path: ${JAVA_NATIVE_LIBRARIES} which contains the following"
ls -l ${JAVA_NATIVE_LIBRARIES} | sed -e '1d'
echo "# Nb of hosts:      $PLACES"
echo "# Nb of workers:    $WORKERS"
echo "# Core constraints: $CORE_RESTRICTION"
echo "# Hostfile:         $2"
head -n $PLACES $HOSTFILE

#####################################################################
echo "############# Checking presence of script files ###############"
SCRIPTDIR=`dirname $0`

for script in grainProgram.sh tunerProgram.sh ScriptLauncher.sh
do
	if [[ -f $SCRIPTDIR/$script ]]
	then
		echo "[OK] Script $script found"
	else
		echo "[KO] Script $script not found !!!"
		echo "Exiting"
		exit
	fi
done

####################################################################
echo "#################### Creating directories ####################"
DIR=$1
PREFIX=$1
shift
echo "# - $DIR `mkdir $DIR 2>&1`"
cd $DIR
for d in Pentomino UTS TSP NQueens
do
    echo "# - $DIR/$d `mkdir $d 2>&1`"
done
cd ..

################# LAUNCHING NQUEENS BENCHMARK #######################
echo "Launching NQueens with arguments: $NQUEENS_ARGS"
MAIN=handist.glb.examples.nqueens.ParallelNQueens
ARGS=$NQUEENS_ARGS

. ./ScriptLauncher.sh grainProgram.sh $NQUEENS_REPS $DIR/NQueens/${PREFIX}_NQueens-fixedGrain $FIXED_GRAINS
. ./ScriptLauncher.sh tunerProgram.sh $NQUEENS_REPS $DIR/NQueens/${PREFIX}_NQueens-tuner $TUNERS
################ LAUNCHING PENTOMINO BENCHMARK ######################
echo "Launching Pentomino with arguments: $PENTOMINO_ARGS"
MAIN=handist.glb.examples.pentomino.ParallelPentomino
ARGS=$PENTOMINO_ARGS

. ./ScriptLauncher.sh $SCRIPTDIR/grainProgram.sh $PENTOMINO_REPS $DIR/Pentomino/${PREFIX}_Pentomino-fixedGrain $FIXED_GRAINS
. ./ScriptLauncher.sh $SCRIPTDIR/tunerProgram.sh $PENTOMINO_REPS $DIR/Pentomino/${PREFIX}_Pentomino-tuner $TUNERS
##################### LAUNCHING UTS BENCHMARK #######################
echo "Launching UTS with arguments: $UTS_ARGS"
MAIN=handist.glb.examples.uts.MultiworkerUTS
ARGS=$UTS_ARGS

. ./ScriptLauncher.sh $SCRIPTDIR/grainProgram.sh $UTS_REPS $DIR/UTS/${PREFIX}_UTS-fixedGrain $FIXED_GRAINS
. ./ScriptLauncher.sh $SCRIPTDIR/tunerProgram.sh $UTS_REPS $DIR/UTS/${PREFIX}_UTS-tuner $TUNERS
#################### LAUNCHING TSP BENCHMARK ########################
echo "Launching TSP with arguments: $TSP_ARGS"
MAIN=handist.glb.examples.tsp.GlobalTsp
ARGS=$TSP_ARGS

. ./ScriptLauncher.sh $SCRIPTDIR/grainProgram.sh $TSP_REPS $DIR/TSP/${PREFIX}_TSP-fixedGrain $FIXED_GRAINS
. ./ScriptLauncher.sh $SCRIPTDIR/tunerProgram.sh $TSP_REPS $DIR/TSP/${PREFIX}_TSP-tuner $TUNERS

