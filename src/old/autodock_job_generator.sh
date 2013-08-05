#!/bin/bash

if [ $# -ne 3 ] ; then
  echo "$0 <ligand directory> <receptor directory> <job directory>"
  echo "This program takes the ligand and receptor directory"
  echo "and generates jobs combining all files found within"
  echo "The output of those files end up in the <job directory>"
  exit 1
fi

INPUT_LIGAND_DIR=$1
INPUT_RECEPTOR_DIR=$2
OUT_DIR=$3

BASEDIR=`dirname $0`

AUTODOCK_CONFIG="$OUT_DIR/autodock.sh.config"

ARGUMENTS="--center_x 41.1100 --center_y 34.9382 --center_z 35.8160 --size_x 25.0000 --size_y 25.0000 --size_z 25.0000 --cpu 16"

if [ ! -d $INPUT_LIGAND_DIR ] ; then
  echo "$LIGAND_DIR is not a directory"
  exit 1
fi

if [ ! -d $INPUT_RECEPTOR_DIR ] ; then
  echo "$RECEPTOR_DIR is not a directory"
  exit 1
fi

/bin/mkdir -p "$OUT_DIR/config"

if [ $? != 0 ] ; then
  echo "Unable to run /bin/mkdir -p $OUT_DIR/config"
  exit 1
fi


LIGAND_DIR="$OUT_DIR/ligand"

/bin/mkdir -p $LIGAND_DIR

if [ $? != 0 ] ; then
  echo "Unable to run /bin/mkdir -p $LIGAND_DIR"
  exit 1
fi

/bin/cp -a $INPUT_LIGAND_DIR $LIGAND_DIR

if [ $? != 0 ] ; then
   echo "Unable to run /bin/cp -a $INPUT_LIGAND_DIR/* $LIGAND_DIR"
   exit 1
fi

RECEPTOR_DIR="$OUT_DIR/receptor"

/bin/mkdir -p $RECEPTOR_DIR

if [ $? != 0 ] ; then
  echo "Unable to run /bin/mkdir -p $RECEPTOR_DIR"
  exit 1
fi



/bin/cp -a $INPUT_RECEPTOR_DIR/* $RECEPTOR_DIR

if [ $? != 0 ] ; then
   echo "Unable to run /bin/cp -a $INPUT_RECEPTOR_DIR $RECEPTOR_DIR"
   exit 1
fi


/bin/mkdir -p $OUT_DIR

if [ $? != 0 ] ; then
  echo "Unable to run /bin/mkdir -p $OUT_DIR"
  exit 1
fi

PDBQT_OUT_DIR="$OUT_DIR/output_pdbqt"

/bin/mkdir -p $PDBQT_OUT_DIR

if [ $? != 0 ] ; then
  echo "Unable to run /bin/mkdir -p $PDBQT_OUT_DIR="
  exit 1
fi

RECEPTOR_LIST_FILE="$OUT_DIR/receptor.list"

find $RECEPTOR_DIR -name "*.pdbqt" -type f > $RECEPTOR_LIST_FILE

LIGAND_LIST_FILE="$OUT_DIR/ligand.list"

find $LIGAND_DIR -name "*.pdbqt" -type f > $LIGAND_LIST_FILE

NUM_RECEPTOR_FILES=`wc -l $RECEPTOR_LIST_FILE | sed "s/ .*//"`

NUM_LIGAND_FILES=`wc -l $LIGAND_LIST_FILE | sed "s/ .*//"`

echo "Found $NUM_RECEPTOR_FILES receptor files and $NUM_LIGAND_FILES"
NUM_JOBS=`echo "$NUM_RECEPTOR_FILES*$NUM_LIGAND_FILES" | bc -l`
echo "This command will generate $NUM_JOBS jobs"


$BASEDIR/generate_autodock_config2.pl $OUT_DIR $RECEPTOR_LIST_FILE $LIGAND_LIST_FILE > $AUTODOCK_CONFIG 

