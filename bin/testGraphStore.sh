#!/bin/bash

PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SCRIPTDIR=`dirname "$PRG"`
ROOTDIR=`cd "$SCRIPTDIR/.."; pwd -P`

java -cp "$ROOTDIR"/'lib/*':"$ROOTDIR"/miscfastcompact.jar com.jpetrak.miscfastcompact.graph.GraphStore "$@"

