#!/bin/bash

JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
JAVA="$JAVA_HOME"/bin/java

pushd ..
mvn package -DskipTests=true
popd

"$JAVA" -Xmx4g -cp "../target/java-concolic-1.0-SNAPSHOT.jar" csci699cav.Main \
	-prepend-classpath -soot-class-path "../instrumentation/bin:." -keep-offset MyClass
