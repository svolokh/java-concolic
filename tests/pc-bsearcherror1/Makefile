JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
CLASSPATH=../../instrumenter/instrumentation/bin

.PHONY: mkdirs

all: sootOutput/Main.class

clean:
	( rm -fr bin sootOutput || true ) 2>/dev/null
	( rm pathConstraints.txt || true ) 2>/dev/null
	( rm deps.txt rt-deps.txt conc-deps.txt || true ) 2>/dev/null

sootOutput/Main.class: bin/Main.class
	LIBRARY_DEPS_OUT=deps.txt $(JAVA_HOME)/bin/java -Xmx4g -cp "../../instrumenter/target/java-concolic-1.0-SNAPSHOT.jar" csci699cav.Main \
		-prepend-classpath -soot-class-path "$(CLASSPATH):bin" -keep-offset -process-dir bin
	cat deps.txt | sed 1d > rt-deps.txt
	rm deps.txt
	ENTRYPOINTS=rt-deps.txt $(JAVA_HOME)/bin/java -Xmx4g -cp "../../instrumenter/target/java-concolic-1.0-SNAPSHOT.jar" csci699cav.Main \
		-prepend-classpath -soot-class-path "$(CLASSPATH):bin" -keep-offset  -process-dir "$(JAVA_HOME)/jre/lib/rt.jar" -allow-phantom-refs
	cp -r "$(CLASSPATH)"/* sootOutput
	echo '<csci699cav.Concolic: void assume(boolean)>' > conc-deps.txt
	echo '<csci699cav.Concolic: void assertTrue(boolean)>' >> conc-deps.txt
	echo '<csci699cav.Concolic: void assertFalse(boolean)>' >> conc-deps.txt
	ENTRYPOINTS=conc-deps.txt $(JAVA_HOME)/bin/java -Xmx4g -cp "../../instrumenter/target/java-concolic-1.0-SNAPSHOT.jar" csci699cav.Main \
		-prepend-classpath -soot-class-path "$(CLASSPATH):bin" -keep-offset csci699cav.Concolic
	
bin/%.class: %.java
	mkdir -p bin
	$(JAVA_HOME)/bin/javac -d bin -cp "$(CLASSPATH)" $<
