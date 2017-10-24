MACHINE=$(shell uname -m)
JAVAH=$(JAVA_HOME)/bin/javah
PROJECTDIR=..
JAVASRC=$(PROJECTDIR)/comm/src/main/java
RESOURCES=$(PROJECTDIR)/comm/src/main/resources
CFLAGS=$(shell dpkg-buildflags --get CFLAGS) -I . #-g -pthread -fPIC -I .
LDFLAGS=$(shell dpkg-buildflags --get LDFLAGS)
SOURCEDIR=$(PROJECTDIR)/linux
CLASSPATH=$(PROJECTDIR)/comm/target/classes
HEADER=org_vesalainen_comm_channel_LinuxSerialChannel.h
CLASSFILE=$(CLASSPATH)/org/vesalainen/comm/channel/LinuxSerialChannel.class
CLASS=org.vesalainen.comm.channel.LinuxSerialChannel

install:	SerialChannel.so
	install -m 0755 SerialChannel.so $(RESOURCES)/libSerialChannel$(MACHINE).so

SerialChannel.o:	SerialChannel.c
SerialChannel.so:	SerialChannel.o
	ld -shared -o SerialChannel.so SerialChannel.o -lc

distclean:
	rm *.o
	rm *.so

