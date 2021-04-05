JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
JAVAC=$(JAVA_HOME)/bin/javac

CLASSPATH=""

CLASSES=MyClass.class

all: $(CLASSES)

clean:
	rm $(CLASSES)

%.class: %.java
	$(JAVAC) -cp $(CLASSPATH) $<