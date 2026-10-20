#!/bin/sh

#
# Gradle start up script for POSIX shells.
#

set -e

app_path=$0

while [ -h "$app_path" ] ; do
    ls=$(ls -ld "$app_path")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        app_path=$link
    else
        app_path=$(dirname "$app_path")/$link
    fi
done

APP_HOME=$(cd -P "$(dirname "$app_path")" > /dev/null && pwd -P) || exit
APP_BASE_NAME=${0##*/}

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

MAX_FD=maximum

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    command -v java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ] ; then
    die "ERROR: Cannot find $CLASSPATH

Make sure you have run the build once or downloaded the Gradle wrapper jar."
fi

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

if [ ! -z "$GRADLE_OPTS" ] ; then
    eval "set -- $GRADLE_OPTS \"\$@\""
fi

eval "set -- $DEFAULT_JVM_OPTS \"\$@\""

exec "$JAVACMD" "$@"
