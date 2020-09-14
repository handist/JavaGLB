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
FIXED_GRAINS="8 32"
# Tuners to evaluate
TUNERS=handist.glb.tuning.Ntuner
######################## PROGRAM ARGUMENTS ##########################
# NQueens arguments
NQUEENS_ARGS="-n 14 -w 12"
# Pentomino arguments
PENTOMINO_ARGS="-w 10 -h 6"
# TSP arguments (problem file, subset of citites)
TSP_ARGS="-f posner40.atsp -s 24"
# UTS arguments (branching factor and depth)
UTS_ARGS="-b 4 -d 13"
######################### GLB CONFIGURATION #########################
JAVAGLB_JARS=${JAVAGLB_JARS}
JAVA_NATIVE_LIBRARIES=${JAVAGLB_JARS}
JAVAGLB_PLACES=1
JAVAGLB_WORKERS=4
JAVAGLB_CORE_RESTRICTION="0-3"


# Exporting the configuration for it to be available in the scripts
echo "### Configuration"
echo "# Path to JARs:     $JAVAGLB_JARS contains the following"
ls -l $JAVAGLB_JARS
export JAVAGLB_JARPATH=$JAVAGLB_JARS
echo "# Native libraries path"
export JAVA_NATIVE_LIBRARIES=${JAVA_NATIVE_LIBRARIES} 
echo "# Nb of hosts:      $JAVAGLB_PLACES"
export JAVAGLB_PLACES=$JAVAGLB_PLACES
echo "# Nb of workers:    $JAVAGLB_WORKERS"
export JAVAGLB_WORKERS=$JAVAGLB_WORKERS
echo "# Core constraints: $JAVAGLB_CORE_RESTRICTION"
export JAVAGLB_CORE_RESTRICTION=$JAVAGLB_CORE_RESTRICTION
echo "# Hostfile:         $2"
export JAVAGLB_HOSTFILE=$2

#####################################################################
echo "Checking presence of script files"
SCRIPTDIR=`dirname $0`

for script in NQueens.sh NQueens-tuner.sh Pentomino.sh Pentomino-tuner.sh TSP.sh TSP-tuner.sh UTS.sh UTS-tuner.sh ScriptLauncher.sh
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

echo "Creating directories"
DIR=$1
PREFIX=$1
shift
mkdir $DIR
cd $DIR
mkdir Pentomino UTS TSP NQueens
cd ..
################# LAUNCHING NQUEENS BENCHMARK #######################
echo "Launching NQueens with arguments: $NQUEENS_ARGS"
export NQUEENS_ARGS=$NQUEENS_ARGS

# Launch fixed grains executions
echo "ScriptLauncher.sh NQueens.sh 5 ${PREFIX}_NQueens-fixedGrain $FIXED_GRAINS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/NQueens.sh 5 $DIR/NQueens/${PREFIX}_NQueens-fixedGrain $FIXED_GRAINS
# Launch tuner executions
echo "ScriptLauncher.sh NQueens-tuner.sh 5 ${PREFIX}_NQueens-tuner $TUNERS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/NQueens-tuner.sh 5 $DIR/NQueens/${PREFIX}_NQueens-tuner $TUNERS
#####################################################################
################# LAUNCHING NQUEENS BENCHMARK #######################
echo "Launching Pentomino with arguments: $PENTOMINO_ARGS"
export PENTOMINO_ARGS=$PENTOMINO_ARGS

# Launch fixed grains executions
echo "ScriptLauncher.sh Pentomino.sh 5 ${PREFIX}_Pentomino-fixedGrain $FIXED_GRAINS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/Pentomino.sh 5 $DIR/Pentomino/${PREFIX}_Pentomino-fixedGrain $FIXED_GRAINS
# Launch tuner executions
echo "ScriptLauncher.sh Pentomino-tuner.sh 5 ${PREFIX}_Pentomino-tuner $TUNERS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/Pentomino-tuner.sh 5 $DIR/Pentomino/${PREFIX}_Pentomino-tuner $TUNERS
#####################################################################

##################### LAUNCHING UTS BENCHMARK #######################
echo "Launching UTS with arguments: $UTS_ARGS"
export UTS_ARGS=$UTS_ARGS

# Launch fixed grains executions
echo "ScriptLauncher.sh UTS.sh 5 ${PREFIX}_UTS-fixedGrain $FIXED_GRAINS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/UTS.sh 5 $DIR/UTS/${PREFIX}_UTS-fixedGrain $FIXED_GRAINS
# Launch tuner executions
echo "ScriptLauncher.sh UTS-tuner.sh 5 ${PREFIX}_UTS-tuner $TUNERS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/UTS-tuner.sh 5 $DIR/UTS/${PREFIX}_UTS-tuner $TUNERS
#####################################################################

#################### LAUNCHING TSP BENCHMARK ########################
echo "Launching TSP with arguments: $TSP_ARGS"
export TSP_ARGS=$TSP_ARGS

# Launch fixed grains executions
echo "ScriptLauncher.sh TSP.sh 10 ${PREFIX}_TSP-fixedGrain $FIXED_GRAINS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/TSP.sh 10 $DIR/TSP/${PREFIX}_TSP-fixedGrain $FIXED_GRAINS
# Launch tuner executions
echo "ScriptLauncher.sh TSP-tuner.sh 10 ${PREFIX}_TSP-tuner $TUNERS"
bash $SCRIPTDIR/ScriptLauncher.sh $SCRIPTDIR/TSP-tuner.sh 10 $DIR/TSP/${PREFIX}_TSP-tuner $TUNERS
#####################################################################
