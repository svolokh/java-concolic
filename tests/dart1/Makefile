JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
CLASSPATH=../../instrumenter/instrumentation/bin

.PHONY: mkdirs

all: sootOutput

clean:
	rm -fr bin sootOutput
	rm pathConstraints.txt || true

sootOutput: bin/MyClass.class
	$(JAVA_HOME)/bin/java -Xmx4g -cp "../../instrumenter/target/java-concolic-1.0-SNAPSHOT.jar:../../instrumenter/graphs.jar" csci699cav.Main \
		-prepend-classpath -soot-class-path "$(CLASSPATH):bin" -keep-offset -process-dir bin
	
bin/%.class: %.java
	mkdir -p bin
	$(JAVA_HOME)/bin/javac -d bin -cp "$(CLASSPATH)" $<
