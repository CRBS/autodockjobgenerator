#!/bin/bash

if [ $# -ne 1 ] ; then
   echo "$0 <environment dev|prod>"
   echo "Deploys this jar via ssh to remote host correctly setting symlinks etc.."
   exit 1
fi

DEPLOY_ENV=$1

BASEDIR=`dirname $0`

TIMESTAMP=`date +%m.%d.%Y.%H.%M.%S`

TMPINSTALLDIR="/tmp/$INSTALL_DIR_NAME"

cd $BASEDIR

mvn clean package assembly:single

if [ $? != 0 ] ; then
   echo "Build failed"
   exit 1
fi

HOST="NOTSET"
SCP_ARG="NOTSET"

if [ "$DEPLOY_ENV" == "dev" ] ; then
   HOST="tomcat@cylume.camera.calit2.net"
   DEPLOY_BASE_DIR="/camera/cam-dev/camera/release/bin"
   SCP_ARG="${HOST}:${DEPLOY_BASE_DIR}/."
fi

if [ "$DEPLOY_ENV" == "prod" ] ; then
   HOST="tomcat@cylume.camera.calit2.net"
   DEPLOY_BASE_DIR="/home/validation/camera/release/bin"
   SCP_ARG="${HOST}:${DEPLOY_BASE_DIR}/."
fi

if [ "$HOST" == "NOTSET" ] ; then
  echo "Please setup $DEPLOY_ENV in this script $0"
  exit 1
fi

JOB_GENERATOR_JAR=`find target -maxdepth 1 -name "autodockjobgenerator*-jar-with-dependencies.jar" -type f`

if [ ! -e "$JOB_GENERATOR_JAR" ] ; then
   echo "Compiled jar not found in $BASEDIR/target wtf"
   exit 1
fi

scp $JOB_GENERATOR_JAR $SCP_ARG

if [ $? != 0 ] ; then
   echo "Error running: scp $JOB_GENERATOR_JAR $SCP_ARG"
   exit 1
fi

# change symlink by removing first then creating
ssh $HOST "/bin/rm $DEPLOY_BASE_DIR/autodockjobgenerator.jar"


JOB_GENERATOR_JAR_NAME=`basename $JOB_GENERATOR_JAR`

ssh $HOST "/bin/ln -s $DEPLOY_BASE_DIR/$JOB_GENERATOR_JAR_NAME $DEPLOY_BASE_DIR/autodockjobgenerator.jar"
if [ $? != 0 ] ; then
  echo "Error running: ssh $HOST \"/bin/ln -s $DEPLOY_BASE_DIR/$JOB_GENERATOR_JAR_NAME $DEPLOY_BASE_DIR/autodockjobgenerator.jar\""
  exit 1
fi

echo "Build completed.  Have a nice day."
exit 0
