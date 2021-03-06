#!/bin/bash

abs_path() {
    SOURCE="${BASH_SOURCE[0]}"
    while [ -h "$SOURCE" ]; do
        DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$( cd -P "$( dirname "$SOURCE" )" && pwd )"
}

BIN=`abs_path`
TOP="$(cd $BIN/../ && pwd)"

. $BIN/util.sh

# The maximum and minium heap memory that service can use
MAX_MEM=$[32*1024]
MIN_MEM=512
EXPECT_JDK_VERSION=1.8

# ${BASH_SOURCE[0]} is the path to this file
SOURCE="${BASH_SOURCE[0]}"
# Set $BIN to the absolute, symlinkless path to $SOURCE's parent
while [ -h "$SOURCE" ]; do
    BIN="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
    SOURCE="$(readlink "$SOURCE")"
    [[ $SOURCE != /* ]] && SOURCE="$BIN/$SOURCE"
done
BIN="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
# Set $CFG to $BIN/../conf/
cd -P $BIN/../conf
CFG=$(pwd)
# Set $LIB to $BIN/../lib
cd -P $BIN/../lib
LIB=$(pwd)
# Set $LIB to $BIN/../ext
cd -P $BIN/../ext
EXT=$(pwd)
# Initialize classpath to $CFG
CP="$CFG"
# Add the slf4j-log4j12 binding
CP="$CP":$(find -L $LIB -name 'slf4j-log4j12*.jar' | sort | tr '\n' ':')
# Add the jars in $BIN/../lib that start with "hugegraph"
CP="$CP":$(find -L $LIB -name 'hugegraph*.jar' | sort | tr '\n' ':')
# Add the remaining jars in $BIN/../lib.
CP="$CP":$(find -L $LIB -name '*.jar' \
                \! -name 'hugegraph*' \
                \! -name 'slf4j-log4j12*.jar' | sort | tr '\n' ':')
# Add the jars in $BIN/../ext (at any subdirectory depth)
CP="$CP":$(find -L $EXT -name '*.jar' | sort | tr '\n' ':')

# (Cygwin only) Use ; classpath separator and reformat paths for Windows ("C:\foo")
[[ $(uname) = CYGWIN* ]] && CP="$(cygpath -p -w "$CP")"

export CLASSPATH="${CLASSPATH:-}:$CP"

# Change to $BIN's parent
cd $BIN/..

export HUGEGRAPH_LOGDIR="$BIN/../logs"

if [ ! -d $HUGEGRAPH_LOGDIR ]; then
    mkdir $HUGEGRAPH_LOGDIR
fi

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    JAVA="java -server"
else
    JAVA="$JAVA_HOME/bin/java -server"
fi

JAVA_VERSION=`$JAVA -version 2>&1 | awk 'NR==1{gsub(/"/,""); print $3}' \
              | awk -F'_' '{print $1}'`
if [[ $? -ne 0 || $JAVA_VERSION < $EXPECT_JDK_VERSION ]]; then
    echo "Please make sure that the JDK is installed and the version >= $EXPECT_JDK_VERSION" \
    >> $HUGEGRAPH_LOGDIR/hugegraph-server.log
    exit 1
fi

# Set Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
    XMX=`calc_xmx $MIN_MEM $MAX_MEM`
    if [ $? -ne 0 ]; then
        echo "Failed to start HugeGraphServer, requires at least ${MIN_MEM}m free memory" \
        >> $HUGEGRAPH_LOGDIR/hugegraph-server.log
        exit 1
    fi
    JAVA_OPTIONS="-Xms256m -Xmx${XMX}m -javaagent:$LIB/jamm-0.3.0.jar"
fi

# Execute the application and return its exit code
set -x
ARGS="$@"
if [ $# = 0 ] ; then
    ARGS="conf/gremlin-server.yaml conf/rest-server.properties"
fi
exec $JAVA -Dname="HugeGraphServer" -Dhugegraph.logdir="HUGEGRAPH_LOGDIR" \
-Dlog4j.configurationFile=conf/graph-server-log4j.xml \
$JAVA_OPTIONS -cp $CP:$CLASSPATH com.baidu.hugegraph.dist.HugeGraphServer $ARGS
