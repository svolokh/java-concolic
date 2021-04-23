JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
CLASSPATH=../../instrumenter/instrumentation/bin

.PHONY: mkdirs

all: sootOutput/rt.jar

clean:
	rm -fr bin sootOutput
	rm pathConstraints.txt || true
	rm rt-deps.txt || true

rt-deps.txt: bin/MyClass.class
	LIBRARY_DEPS_OUT=deps.tmp.txt $(JAVA_HOME)/bin/java -Xmx4g -cp "../../instrumenter/target/java-concolic-1.0-SNAPSHOT.jar:../../instrumenter/graphs.jar" csci699cav.Main \
		-prepend-classpath -soot-class-path "$(CLASSPATH):bin" -keep-offset -process-dir bin
	cat deps.tmp.txt | sed 1d > rt-deps.txt
	rm deps.tmp.txt

sootOutput/rt.jar: rt-deps.txt
	ENTRYPOINTS=rt-deps.txt $(JAVA_HOME)/bin/java -Xmx4g -cp "../../instrumenter/target/java-concolic-1.0-SNAPSHOT.jar:../../instrumenter/graphs.jar" csci699cav.Main \
		-prepend-classpath -soot-class-path "$(CLASSPATH):bin" -keep-offset  -process-dir "$(JAVA_HOME)/jre/lib/rt.jar" -allow-phantom-refs
	
bin/%.class: %.java
	mkdir -p bin
	$(JAVA_HOME)/bin/javac -d bin -cp "$(CLASSPATH)" $<
