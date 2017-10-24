MACHINE=$(shell uname -m)
JAVAH=$(JAVA_HOME)/bin/javah
PROJECTDIR=..
JAVASRC=$(PROJECTDIR)/comm/src/main/java
RESOURCES=$(PROJECTDIR)/comm/src/main/resources
CFLAGS= -g -pthread -fPIC -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux
SOURCEDIR=$(PROJECTDIR)/linux
CLASSPATH=$(PROJECTDIR)/comm/target/classes
HEADER=org_vesalainen_comm_channel_LinuxSerialChannel.h
CLASSFILE=$(CLASSPATH)/org/vesalainen/comm/channel/LinuxSerialChannel.class
CLASS=org.vesalainen.comm.channel.LinuxSerialChannel

$(HEADER):	$(CLASSFILE)
	$(JAVAH) -v -d $(SOURCEDIR) -cp $(CLASSPATH) $(CLASS)

SerialChannel.o:	SerialChannel.c $(HEADER)
SerialChannel.so:	SerialChannel.o
	ld -shared -o SerialChannel.so SerialChannel.o -lc

install:	SerialChannel.so
	cp SerialChannel.so $(RESOURCES)/libSerialChannel$(MACHINE).so

all:	install

clean:
	rm *.o
	rm *.so

