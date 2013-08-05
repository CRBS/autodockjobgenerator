#!/bin/sh

###########################################################
#
# This top section defines functions used by the main
# program below
#
###########################################################

#
# This function outputs an error message to standard error and exits with an exit code of 1
# The message is format of ERROR: (first argument passed to function)
#
function jobFailed {
  echo "ERROR: $1" 1>&2
  exit 1
}

#
# This function outputs a warning message to standard error
#
function logWarning {
  echo "WARNING: $1" 1>&2
}

#
# This function outputs a log message to standard out
# logMessage ($1 is message)
#
function logMessage {
  echo "    $1"
}

#
# This function runs du on path given and sets NUM_BYTES variable to size 
# of file or -1 if file is not found
#
function getSizeOfPath {
  if [ ! -e $1 ] ; then
     NUM_BYTES="-1"
  fi

  NUM_BYTES=`du $1 -bs | sed "s/\W.*//"`  
}

#
# Creates directory passed in via first argument.
# If creation fails script will exit with 1 code.
#
function makeDirectory {
  /bin/mkdir -p $1
  if [ $? != 0 ] ; then
    jobFailed "ERROR: Unable to run /bin/mkdir -p $1"
  fi
}

#
# Compresses outputs and copies back to shared file system
# After copy scratch is deleted.
# 
function compressAndUploadResults {

  logStartTime "CompressAndUploadResults"

  cd $SCRATCH/outputs

  if [ $? != 0 ] ; then
     jobFailed "Unable to run: cd $SCRATCH/outputs"
  fi

  tar -cz $SGE_TASK_ID > ${SGE_TASK_ID}.tar.gz

  if [ $? != 0 ] ; then
     jobFailed "Unable to run: tar -cz $SGE_TASK_ID > ${SGE_TASK_ID}.tar.gz"
  fi

  getSizeOfPath ${SGE_TASK_ID}.tar.gz

  logMessage "Moving  $NUM_BYTES bytes to outputs directory on shared filesystem"


  /bin/mv $SCRATCH/outputs/${SGE_TASK_ID}.tar.gz $BASEDIR/outputs/. 

  if [ $? != 0 ] ; then
     jobFailed "Unable to run: /bin/mv $SCRATCH/outputs/${SGE_TASK_ID}.tar.gz $BASEDIR/outputs/."
  fi

  logEndTime "CompressAndUploadResults" $START_TIME 0
}


#
# Copies gzip tarball of inputs to $SCRATCH/inputs and gunzips and untars
# the file
#
function copyInputsToScratch {
  logStartTime "CopyInputsToScratch"

  getSizeOfPath $BASEDIR/inputs/${SGE_TASK_ID}.tar.gz

  logMessage "Copying $NUM_BYTES bytes to scratch"

  /bin/cp $BASEDIR/inputs/${SGE_TASK_ID}.tar.gz $1

  if [ $? != 0 ] ; then
     jobFailed "Unable to run: /bin/cp $BASEDIR/inputs/${SGE_TASK_ID}.tar.gz $1"
  fi

  cd $1

  if [ $? != 0 ] ; then
    jobFailed "Unable to run: cd $1"
  fi

  tar -zxf ${SGE_TASK_ID}.tar.gz
  
  if [ $? != 0 ] ; then
     jobFailed "Unable to run: tar -zxf ${SGE_TASK_ID}.tar.gz"
  fi

  /bin/rm -f ${SGE_TASK_ID}.tar.gz

  if [ $? != 0 ] ; then
     logWarning "Unable to run /bin/rm -f ${SGE_TASK_ID}.tar.gz"
  fi


  logEndTime "CopyInputsToScratch" $START_TIME 0
}

#
# Logs start time in seconds since epoch. Start time is also
# stored in START_TIME variable
#
# logStartTime($1 is program)
#
# Example:  
#
#   logStartTime "hello"
#
# Output:
#
#   hello 123354354
#
function logStartTime {
  START_TIME=`date +%s`
  echo "$1 Start Time: $START_TIME"
}

#
# Logs end time duration and exit code.  
# logEndTime($1 is program, $2 is start time, $3 is the exit code to log)
#
function logEndTime {
  END_TIME=`date +%s`
  DURATION=`echo "$END_TIME-$2" | bc -l`
  echo "$1 End Time: $END_TIME Duration: $DURATION Exit Code: $3"
}

ARGUMENTS="--center_x 41.1100 --center_y 34.9382 --center_z 35.8160 --size_x 25.0000 --size_y 25.0000 --size_z 25.0000 --cpu 16"

###########################################################
#
# Start of program
#
###########################################################

# Dont allow job to run if SGE_TASK_ID is NOT set
if [ -z "$SGE_TASK_ID" ] ; then
  jobFailed "Variable SGE_TASK_ID must be set to a numeric value"
fi

# set SCRATCH variable to /tmp/vina.# or to whatever PANFISH_SCRATCH/vina.#
# if PANFISH_SCRATCH variable is not empty
UUID=`uuidgen`
SCRATCH="/tmp/vina.${UUID}"
if [ -n "$PANFISH_SCRATCH" ] ; then
  SCRATCH="$PANFISH_SCRATCH/vina.${UUID}"
fi

# Auto Dock Vina program
VINA="$PANFISH_BASEDIR/home/churas/bin/autodock_vina_1_1_2/bin/vina"

# the base directory will be the directory where this script resides
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR
BASEDIR=`pwd -P`

logStartTime "autodock.sh" 
AUTODOCK_START_TIME=$START_TIME

# remove vina folder just in case it exists
if [ -e $SCRATCH ] ; then
  echo "$SCRATCH exists"
  /bin/rm -rf $SCRATCH
  if [ $? != 0 ] ; then
     jobFailed "Unable to run: /bin/rm -rf $SCRATCH"
  fi
fi

makeDirectory $SCRATCH/inputs
makeDirectory $SCRATCH/outputs/${SGE_TASK_ID}/logs
makeDirectory $SCRATCH/outputs/${SGE_TASK_ID}/pdbqt

copyInputsToScratch $SCRATCH/inputs

let CNTR=1

cd $SCRATCH

if [ $? != 0 ] ; then
  jobFailed "Unable to run: cd $SCRATCH"
fi

NUM_JOBS=`wc -l $SCRATCH/inputs/${SGE_TASK_ID}/${SGE_TASK_ID}.autodock.sh.config | sed "s/ .*//"`
logMessage "Found $NUM_JOBS to run"

for Y in `seq 1 $NUM_JOBS` ; do

   OTHERARGS=`egrep "^$Y:::" $SCRATCH/inputs/${SGE_TASK_ID}/${SGE_TASK_ID}.autodock.sh.config | sed "s/^.*::://"`

   logStartTime "Vina $Y"
   $VINA $ARGUMENTS $OTHERARGS > $SCRATCH/outputs/${SGE_TASK_ID}/logs/${SGE_TASK_ID}.$Y.log 2>&1
   EXIT_CODE=$?

   logEndTime "Vina $Y" $START_TIME $EXIT_CODE
done

compressAndUploadResults

logStartTime "Remove Scratch"
# remove the scratch directory 
/bin/rm -rf $SCRATCH

EXIT_CODE=$?

if [ $EXIT_CODE != 0 ] ; then
  jobWarning "Unable to run: /bin/rm -rf $SCRATCH"
fi
logEndTime "Remove Scratch" $START_TIME $EXIT_CODE

logEndTime "autodock.sh" $AUTODOCK_START_TIME 0
exit 0
