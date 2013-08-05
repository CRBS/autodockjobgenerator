#!/bin/bash

if [ $# -lt 2 ] ; then
  echo "$0 <auto dock job directory>"
  echo "This program runs the Auto Dock Vina jobs defined in"
  echo "autodock.sh.config file through Panfish.  This program"
  echo "First runs a test job locally then batches jobs"
  echo "based on the run time before uploading the data"
  echo "and running the jobs on remote clusters"
  exit 1
fi

CLUSTER=$1
AUTODOCKDIR=$2

# output info on where job is running
echo "SGEJobId: ${JOB_ID}.${SGE_TASK_ID}"

#fail if the autodockdir is not a directory
if [ ! -d $AUTODOCKDIR ] ; then
  echo "$AUTODOCKDIR not valid directory"
  exit 1
fi

#
# jobFailed(<message>)
# 
# Prints <message> to standard error and standard out and to
# $AUTODOCKDIR/KILL.JOB.REQUEST file before exiting with exit code of 1
#
function jobFailed {
  echo "$1"
  echo "$1" 1>&2
  echo "$1" > $AUTODOCKDIR/KILL.JOB.REQUEST
  exit 1
}

#
# Checks for KILL.JOB.REQUEST or DELETE.JOB.REQUEST file and if
# found code chums that file to remote clusters and exits
#
function check_for_kill_file {

      KILL_FILE=""
      if [ -e $AUTODOCKDIR/KILL.JOB.REQUEST ] ; then
          KILL_FILE="$AUTODOCKDIR/KILL.JOB.REQUEST"
      fi

      if [ "$KILL_FILE" != "" ] ; then
          echo "$KILL_FILE dectected. Chumming kill file to remote cluster and exiting..."
          echo "Running $CHUMBINARY --path $KILL_FILE > $AUTODOCKDIR/killed.chum.out"
          $CHUMBINARY --path $KILL_FILE > $AUTODOCKDIR/killed.chum.out 2>&1
          
          echo "Running qdel"
          # lets also kill any running jobs if we see them by calling qdel
          qdel `cat $AUTODOCKDIR/cast*out  sed "s/^.*job-array //" | sed "s/\..*//"` 
          exit 1
      fi
}

#
# function called when USR2 signal is caught
#
on_usr2() {
  jobFailed "External request for job termination"
}

# trap usr2 signal cause its what gets sent by SGE when qdel is called
trap 'on_usr2' USR2


function log_start {
  echo ""
  echo "$1 (iteration $2) start time: `date +%s`"
  echo ""
}


function log_end {
  echo ""
  echo "$1 (iteration $2) end time: `date +%s`"
  echo ""
}

# Get the camera.properties file which resides in the same directory as the script
PROP="`dirname $0`/autodock.properties"

MAX_RETRIES=5
RETRY_SLEEP=100

LOCALCLUSTER=`egrep "^localcluster" $CAMPROP | sed "s/^.*= *//"`
RUNLOCALCUTOFF=`egrep "^runlocalcutoff" $CAMPROP | sed "s/^.*= *//"`
CASTBINARY=`egrep "^panfishcast" $CAMPROP | sed "s/^.*= *//"`
CHUMBINARY=`egrep "^panfishchum" $CAMPROP | sed "s/^.*= *//"`
LANDBINARY=`egrep "^panfishland" $CAMPROP | sed "s/^.*= *//"`
PANFISHSTATBINARY=`egrep "^panfishstat" $CAMPROP | sed "s/^.*= *//"`
JOBUSER=`echo $BLASTDIR | sed "s/^.*blast-data\///" | sed "s/\/.*//" | sed "s/^[0-9]/X/" | sed "s/\\W/X/g"`

JOBNAME="${JOBUSER}_panblast"

LASTLINE=`tail -n 1 $BLASTDIR/blastConfiguration.config`
LASTJOBID=`echo $LASTLINE | sed "s/:::.*//"`

DESIRED_RUN_TIME=7200 
FIRSTQUERYLINE=`head -1 ${BLASTDIR}/blastConfiguration.config | sed "s/^1::://"`
QUERYSUBDIR=`dirname $FIRSTQUERYLINE`
QUERYDIR=`dirname $QUERYSUBDIR`
CHUMMEDLIST=""
BLAST_BATCHER_CONFIG="blastbatchercmd.sh.config"
BLAST_BATCHER_CMD="blastbatchercmd.sh"
BLAST_BATCHER_CMD_TEMPLATE="`dirname $0`/blastbatchercmd.sh.template"
TASK_ARGUMENT="task.argument"
BATCH_FACTOR="batch.factor"
GEN_BLAST_BATCHER="`dirname $0`/generate_blastbatcher_config.pl"


# creates blast batcher command
# 
function generate_blastbatcher {
   log_start "Generate blastbatcher command" $1
   /bin/cp $BLAST_BATCHER_CMD_TEMPLATE $BLASTDIR/$BLAST_BATCHER_CMD

   if [ $? != 0 ] ; then
      log_end "Generate blastbatcher command" $1
      jobFailed "Error copying $BLAST_BATCHER_CMD_TEMPLATE to $BLASTDIR/$BLAST_BATCHER_CMD"
   fi
   log_end "Generate blastbatcher command" $1
}

# creates blast batcher config
# 
function generate_blastbatcher_config {
    log_start "Generate blastbatcher config" $1
    # if its the first iteration run the first job otherwise use the blastbatcher_config_generator program
    # which sets BLAST_BATCHER_CONFIG file based on failed.jobs file
    # and BATCH_FACTOR file
    RUN_BENCHMARK=""
    if [ ! -e $BLASTDIR/$BATCH_FACTOR ] ; then
       echo "No $BLASTDIR/$BATCH_FACTOR running benchmark"
       GEN_BATCHER_ARGS="--runbenchmark"
    else
       FACTOR=`cat $BLASTDIR/$BATCH_FACTOR`
       GEN_BATCHER_ARGS="--batchfactor $FACTOR"
    fi
    echo "Running $GEN_BLAST_BATCHER --blastdir $BLASTDIR $GEN_BATCHER_ARGS"
    $GEN_BLAST_BATCHER --blastdir $BLASTDIR $GEN_BATCHER_ARGS
       
    if [ $? != 0 ] ; then
        log_end "Generate blastbatcher config" $1
        jobFailed "Error running $GEN_BLAST_BATCHER --blastdir $BLASTDIR $GEN_BATCHER_ARGS"
    fi

    log_end "Generate blastbatcher config" $1
}


# Run the first blast job locally
# get the runtime.  Use that time to
# calculate optimal job runtime.
# Write out value to batch.factor file
# used by gen_batch_config
function benchmark_blast {
   log_start "Benchmark Blast" $1

   # run 1 job to get a benchmark time
   generate_blastbatcher_config $1

   # it is assumed benchmark is only run the very first time
   # so we are setting chummedlist to just the local cluster
   # so the benchmark job runs locally
   CHUMMEDLIST=$LOCALCLUSTER
   # Cast the job to panfish
   cast_job $1

   # Wait for job to complete
   wait_for_job_completion $1

   # Check for success and get job run time
   check_subblast 1

   # check if blast failed.  If it did try once more
   if [ $? != 0 ] ; then
      echo "Initial benchmark blast failed.  Trying again"
      # Cast the job to panfish
      cast_job $1

      # Wait for job to complete
      wait_for_job_completion $1

      check_subblast 1

      if [ $? != 0 ] ; then
         echo "Second benchmark blast failed"
         log_end "Benchmark blast" $1
         return 1
      fi

   fi

   echo "Blast in Benchmark was successful.  Getting runtime"

   # If we are here we had a successful blast job run
   # lets get the time and determine a batch factor which
   # we need to write to the file $BATCH_FACTOR with that
   # value
   START_TIME=`cat $BLASTDIR/blastDir/stdout/blastOutput.1 | grep "Start Time:" | head -n 1 | sed "s/^.*: //"`
   END_TIME=`cat $BLASTDIR/blastDir/stdout/blastOutput.1 | grep "End Time:" | tail -n 1 | sed "s/^.*: //"`

   

   let DURATION=$END_TIME-$START_TIME

   if [ $DURATION -lt 1 ] ; then
      DURATION=1
   fi

   echo "Blast End Time: $END_TIME Start Time: $START_TIME: Duration: $DURATION seconds"


   let FACTOR=$DESIRED_RUN_TIME/$DURATION

   if [ $FACTOR -lt 1 ] ; then
      FACTOR=1
   fi

   if [ $FACTOR -gt 1000 ] ; then
      echo "Factor calculated $FACTOR exceeds 1000.  Setting to 1000"
      FACTOR=1000
   fi

   echo "Desired run time: $DESIRED_RUN_TIME Calculated Factor:  $FACTOR"
   
   echo "$FACTOR" > $BLASTDIR/$BATCH_FACTOR

   log_end "Benchmark blast" $1
   return 0
}


# Chum the blast directory to remote clusters deleting the directory
# on the remote clusters before starting the transfer.
# This function assumes the following variables are set
# CHUMBINARY
# BLASTDIR
# and this function sets CHUMMEDLIST with the list of clusters that
# received the blast directory.
function chum_blast_directory {
    log_start "Chum blast dir" $1


    # check on # of jobs to run if its below the cutoff just set --cluster in the next command to
    # the local cluster cause we don't want the overhead of remote running for little jobs 
    CLUSTERARG=""

    NUMJOBS=`tail -n 1 $BLASTDIR/blastConfiguration.config | sed "s/:::.*//"`
    
    if [ $NUMJOBS -lt $RUNLOCALCUTOFF ] ; then
       echo "Number of jobs in blast $NUMJOBS is less then cut off of $RUNLOCALCUTOFF.  Running jobs on local cluster only"
       CLUSTERARG="--cluster $LOCALCLUSTER"

    fi

    # get the reference sequence id
    REF_SEQUENCE_DIR=`head -n 2 $BLASTDIR/blastConfiguration.config | tail -n 1 | sed "s/^.*::://" | sed "s/\p_0.*//"`

    # get list of clusters that have the reference sequence directory
    echo "Running $CHUMBINARY --listexists $CLUSTERARG --path $REF_SEQUENCE_DIR"
    
    $CHUMBINARY --listexists $CLUSTERARG --path $REF_SEQUENCE_DIR > $BLASTDIR/check_for_ref_sequences.out 2>&1
   
    if [ $? != 0 ] ; then
       log_end "Chum blast dir" $1
       jobFailed "Error running $CHUMBINARY --listexists $CLUSTERARG --path $REF_SEQUENCE_DIR"
    fi

    RAWCLUSTERLIST=`cat $BLASTDIR/check_for_ref_sequences.out | egrep "^chummed.clusters" | sed "s/^chummed.clusters=//"`

    echo "Running $CHUMBINARY --listchummed --cluster $RAWCLUSTERLIST --path $BLASTDIR --deletebefore --exclude panfish_remoteblast.out --exclude panfish_remoteblast.err --exclude chum.out --exclude 'Collate*' --exclude 'persist*' --exclude 'blastGridMerge*' --exclude '*.oos' --exclude 'blastOutput.*' --exclude 'blastError.*' --exclude 'totalBlastHits' --exclude 'seqCount' --exclude '*.xml' --exclude '*.zip' --exclude '*.old' --verbose --verbose > $BLASTDIR/chum.out 2>&1"

    # Chum the blast directory removing it from the remote side if it already exists
    $CHUMBINARY --listchummed --cluster $RAWCLUSTERLIST --path $BLASTDIR --deletebefore --exclude panfish_remoteblast.out --exclude panfish_remoteblast.err --exclude chum.out --exclude 'Collate*' --exclude 'persist*' --exclude 'blastGridMerge*' --exclude '*.oos' --exclude 'blastOutput.*' --exclude 'blastError.*' --exclude 'totalBlastHits' --exclude 'seqCount' --exclude '*.xml' --exclude '*.zip' --exclude '*.old' --verbose --verbose > $BLASTDIR/chum.out 2>&1

    if [ $? != 0 ] ; then
        log_end "Chum blast dir" $1
        jobFailed "Error running $CHUMBINARY --listchummed --cluster $RAWCLUSTERLIST --path $BLASTDIR --deletebefore --exclude panfish_remoteblast.out --exclude panfish_remoteblast.err --exclude chum.out --exclude 'Collate*' --exclude 'persist*' --exclude 'blastGridMerge*' --exclude '*.oos' --exclude 'blastOutput.*' --exclude 'blastError.*' --exclude 'totalBlastHits' --exclude 'seqCount' --exclude '*.xml' --exclude '*.zip' --exclude '*.old' --verbose --verbose 2>&1"
    fi

    # gets the chum output to standard out
    cat $BLASTDIR/chum.out

    CHUMMEDLIST=`cat $BLASTDIR/chum.out | egrep "^chummed.clusters" | sed "s/^chummed.clusters=//"`

    echo "Cluster list: $CHUMMEDLIST"

    log_end "Chum blast dir" $1
}

# Chum the query sequence directory to remote clusters specified by CHUMMEDLIST.
# To find the directory for the query sequence by looking at the first line of blastConfiguration.config
# and calling dirname twice to get to the actual sequence directory since sequences are stored in
# (path)/(al-#)/seq1.234
# format and we need to remove the filename and the (al-#) directory
# this function assumes the following variables are set
# BLASTDIR
# CHUMBINARY
# CHUMMEDLIST
# QUERYDIR
function chum_query_sequence {
   log_start "Chum query sequence" $1
   
   echo "Running $CHUMBINARY --listchummed --cluster $CHUMMEDLIST --path $QUERYDIR --exclude '*.fasta'"
   $CHUMBINARY --listchummed --cluster $CHUMMEDLIST --path $QUERYDIR --exclude '*.fasta'

   if [ $? != 0 ] ; then
       log_end "Chum query sequence" $1
       jobFailed "Error running $CHUMBINARY --listchummed --cluster $CHUMMEDLIST --path $QUERYDIR --exclude '*.fasta'"
   fi

   CHUMMEDLIST=`cat $BLASTDIR/chum.out | egrep "^chummed.clusters" | sed "s/^chummed.clusters=//"`

   log_end "Chum query sequence" $1
}

#
# Using land command remove remote query and blast directories 
#
function remove_remote_directories {
   log_start "Remove remote directories"
   
   echo "Running $LANDBINARY --directory $BLASTDIR --cluster $CHUMMEDLIST --deleteonly 2>&1"
   $LANDBINARY --directory $BLASTDIR --cluster $CHUMMEDLIST --deleteonly 2>&1
   if [ $? != 0 ] ; then
      echo "Error running land command to remove blast directory $BLASTDIR"
   fi

   echo "Running $LANDBINARY --directory $QUERYDIR --cluster $CHUMMEDLIST --deleteonly 2>&1"
   $LANDBINARY --directory $QUERYDIR --cluster $CHUMMEDLIST --deleteonly 2>&1
   
   log_end "Remove remote directories"
}

function cast_job {
   log_start "Cast job" $1

   LAST_JOB_ID=`tail -n 1 ${BLASTDIR}/$BLAST_BATCHER_CONFIG | sed "s/:::.*//"`

   # move the old cast file out of the way if it exists
   if [ -e $BLASTDIR/cast.out ] ; then
      /bin/mv $BLASTDIR/cast.out $BLASTDIR/cast.$(( $1 - 1 )).out
   fi

   echo "Running $CASTBINARY -t 1-${LAST_JOB_ID} -q $CHUMMEDLIST -N $JOBNAME --writeoutputlocal -e /dev/null -o /dev/null ${BLASTDIR}/$BLAST_BATCHER_CMD"

   $CASTBINARY -t 1-${LAST_JOB_ID} -q $CHUMMEDLIST -N $JOBNAME --writeoutputlocal -e /dev/null -o /dev/null ${BLASTDIR}/$BLAST_BATCHER_CMD > $BLASTDIR/cast.out

   if [ $? != 0 ] ; then
      log_end "Cast job" $1
      jobFailed "Error running $CASTBINARY -t 1-${LAST_JOB_ID} -q $CHUMMEDLIST -N $JOBNAME --writeoutputlocal -e /dev/null -o /dev/null ${BLASTDIR}/$BLAST_BATCHER_CMD > $BLASTDIR/cast.out"
   fi

   log_end "Cast job" $1
   # output in cast.out file will look like this
   # Your job-array 142.1-1:1 ("line") has been submitted
   # Your job-array 143.3-3:1 ("line") has been submitted
   # Your job-array 144.5-11:1 ("line") has been submitted
}

function land_results {
   log_start "Land job" $1
   echo "Running $LANDBINARY --directory $BLASTDIR --cluster $CHUMMEDLIST --exclude panfish_remoteblast.out --exclude panfish_remoteblast.err"
   $LANDBINARY --directory $BLASTDIR --cluster $CHUMMEDLIST --exclude panfish_remoteblast.out --exclude panfish_remoteblast.err

   if [ $? != 0 ] ; then
      log_end "Land job" $1
      jobFailed "Error running $LANDBINARY --directory $BLASTDIR --cluster $CHUMMEDLIST --exclude panfish_remoteblast.out --exclude panfish_remoteblast.err"
   fi
   log_end "Land job" $1
}

function check_subblast {
    Y=$1
    if [ ! -s "$BLASTDIR/blastDir/stdout/blastOutput.$Y" ] ; then
        echo "No blastDir/stdout/blastOutput.$Y" 1>&2
        return 1
    fi

    BLASTOUTFILE=`egrep "^$Y:::" $BLASTDIR/blastConfiguration.config | tail -n 1 | sed "s/^.*::://"`
    if [ ! -s "${BLASTOUTFILE}.oos" ] ; then
        echo "For job $Y ${BLASTOUTFILE}.oos missing" 1>&2
        return 1
    fi

    # check the exit codes and make sure they are all 0   
    for Z in `cat $BLASTDIR/blastDir/stdout/blastOutput.$Y | grep "Exit Code:" | sed "s/^.*Exit Code: *//" | sort | uniq` ; do
        if [ $Z != "0" ] ; then
            echo "Nonzero exit code in job $Y" 1>&2
            return 1
        fi
    done
}

function verify_results {

   log_start "Verify blasts" $1
   ANYFAILED="NO"
   FAILEDJOBSFILE="$BLASTDIR/failed.jobs"
   
   #move the old failed jobs file out of the way if it exists
   if [ -e "$FAILEDJOBSFILE" ] ; then
       /bin/mv $FAILEDJOBSFILE ${FAILEDJOBSFILE}.$(( $1 - 1 ))
   fi

   for Y in `seq 1 $LASTJOBID` ; do

      # Check blast job for failure
      check_subblast $Y

      if [ $? != 0 ] ; then
         ANYFAILED="YES"
         echo $Y >> $FAILEDJOBSFILE
         continue
      fi
   done

   if [ "$ANYFAILED" == "YES" ] ; then
        echo ""
        echo "# of failed jobs: `wc -l $FAILEDJOBSFILE`"
        echo ""
   fi

   log_end "Verify blasts" $1
}

#
# Moves any cluster folders out of the way by renaming them with
# a #.old suffix
#
function move_old_cluster_folders {
  if [ -n $CHUMMEDLIST ] ; then
   
     for Y in `echo "$CHUMMEDLIST" | sed "s/,/ /g"` ; do
         if [ -d $Y ] ; then
            /bin/mv $Y ${Y}.$1.old
         fi
     done
  fi
}

#
# If failed.jobs file is non zero size go through the file and
# remove any corresponding blastDir/blastError.# files
#
function remove_failed_blast_job_error_files {
  log_start "Remove Failed Blast Job Files" $1
  if [ -s $BLASTDIR/failed.jobs ] ; then
     for Y in `cat $BLASTDIR/failed.jobs` ; do
       if [ -e "$BLASTDIR/blastDir/stderr/blastError.${Y}" ] ; then
          echo "Removing blastDir/stderr/blastError.${Y}"
          /bin/rm -f $BLASTDIR/blastDir/stderr/blastError.${Y}
          if [ $? != 0 ] ; then
             log_end "Remove Failed Blast Job Files" $1
             jobFailed "ERROR unable to remove blastDir/stderr/blastError.${Y}"
          fi 
       fi
     done
  fi
  log_end "Remove Failed Blast Job Files" $1

}

function wait_for_job_completion {
   log_start "Wait for job(s)" $1
   JOBSTATUS="NA"
   while [ "$JOBSTATUS" != "done" ] 
   do

      check_for_kill_file

      echo "Job status is $JOBSTATUS.  Sleeping 60 seconds"
      sleep 60

      # need to call a program to see if the jobs have completed
      # Output in cast.out file will look like this
      # Your job-array 142.1-1:1 ("line") has been submitted
      # Your job-array 143.3-3:1 ("line") has been submitted
      # Your job-array 144.5-11:1 ("line") has been submitted

      # ideally lets call panfishstat, pass it cast.out and it can tell us 
      # if we are done or not.  
      OUT=`$PANFISHSTATBINARY --statusofjobs $BLASTDIR/cast.out`
      if [ $? != 0 ] ; then
          echo "Error calling $PANFISHSTATBINARY --statusofjobs $BLASTDIR/cast.out will just keep waiting"
          JOBSTATUS="unknown"
      else
          JOBSTATUS=`echo $OUT | egrep "^status=" | sed "s/^status=//" | tr \[:upper:\] \[:lower:\]`
      fi
   done
   log_end "Wait for job(s)" $1
}

###############################################################################
#
# Start of actual script.  First code checks if there is a RESTART.JOB file
# which indicates the job needs to be continued.
#
###############################################################################

X=1

#
# Check for RESTART.JOB file if found it means we should start up at the specified
# iteration
#
if [ -s "$BLASTDIR/RESTART.JOB" ] ; then
   echo "RESTART.JOB file detected"
   X=`cat $BLASTDIR/RESTART.JOB`
   if [ $X -le 1 ] ; then
      jobFailed "Unable to restart $BLASTDIR/RESTART.JOB must have an iteration number greater then 1"
   fi
   echo "From contents of RESTART.JOB setting iteration to $X"

   # Performing a check on results
   verify_results $X
   if [ "$ANYFAILED" == "NO" ] ; then
      echo "Restart verification found no errors.  Setting X above MAX_RETRIES to skip loop"
      let X=$MAX_RETRIES+1
   fi
fi


###############################################################################
# 
# The following section is the loop used to run blast.  All steps take
# place in a giant while loop which will attempt to rerun the blast MAX_RETRIES
# before giving up
#
###############################################################################

while [ $X -le $MAX_RETRIES ] 
do

   # if no BATCH_FACTOR file is found.  Run the benchmark
   if [ ! -e "$BLASTDIR/$BATCH_FACTOR" ] ; then
      generate_blastbatcher $X
      benchmark_blast $X
   fi
   
   # generate blastbatcher config   
   generate_blastbatcher_config $X
    
   move_old_cluster_folders $(( $X - 1 ))

   if [ $X -gt 1 ] ; then
       check_for_kill_file
       echo "Iteration $X.  Jobs failed in previous iteration. sleeping $RETRY_SLEEP before trying again"
       sleep $RETRY_SLEEP
   fi

   remove_failed_blast_job_error_files $X

   check_for_kill_file

   # Chum the blast dir
   chum_blast_directory $X

   check_for_kill_file

   # Chum the query sequence
   chum_query_sequence $X

   check_for_kill_file

   # Cast the job to panfish
   cast_job $X
  
   # Wait for job to complete
   wait_for_job_completion $X

   # Downloads the completed job results from remote clusters
   land_results $X

   check_for_kill_file

   # Check the results look valid
   verify_results $X

   check_for_kill_file

   # If nothing failed we are good.  Get out of the loop
   if [ "$ANYFAILED" == "NO" ] ; then
     break
   fi

   # increment X by 1
   X=$(( $X + 1 ))

done

# if we get here and still have failed blasts then something went wrong
# so just fail the job.
if [ "$ANYFAILED" == "YES" ] ; then
   jobFailed "Blast Failed"
fi


###############################################################################
# 
# Job has completed successfully so lets adjust the status file and clean up
# directories on remote clusters
#
###############################################################################

remove_remote_directories 

if [ "$REMOTEBUSYFILE" != "DISABLED" ] ; then
  if [ -e $REMOTEBUSYFILE ] ; then

     OUT=`/bin/mv -f $REMOTEBUSYFILE $REMOTEFILE 2>&1`

     if [ $? != 0 ] ; then
        echo "Unable to run /bin/mv -f $REMOTEBUSYFILE $REMOTEFILE 2>&1 : $OUT"
        echo "Wont consider this an error, but something may be wrong"
     fi
  fi
fi

echo "Script Completed.  Have a nice day."

exit 0
