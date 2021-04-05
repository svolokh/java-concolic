#!/bin/bash

JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
JAVA="$JAVA_HOME"/bin/java

"$JAVA" -cp "sootOutput:../instrumentation/bin" MyClass
