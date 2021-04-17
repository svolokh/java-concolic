JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
JAVAC=$(JAVA_HOME)/bin/javac

SRC_DIR=src
BIN_DIR=bin

CLASSPATH=$(SRC_DIR)

CLASSES=csci699cav/Assignment.class csci699cav/PathConstraint.class csci699cav/ShutdownHook.class csci699cav/VariableType.class csci699cav/Variable.class csci699cav/ConcolicState.class csci699cav/Concolic.class

.PHONY: dirs

all: dirs $(patsubst %.class,$(BIN_DIR)/%.class,$(CLASSES))

dirs:
	mkdir -p "$(BIN_DIR)"/csci699cav

clean: 
	rm -fr "$(BIN_DIR)"

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	$(JAVAC) -cp "$(CLASSPATH)" -d "$(BIN_DIR)" $<
