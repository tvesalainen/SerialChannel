MACHINE=$(shell uname -m)
JAVAH=$(JAVA_HOME)/bin/javah
PROJECTDIR=..
JAVASRC=$(PROJECTDIR)/comm/src/main/java
RESOURCES=$(PROJECTDIR)/comm/src/main/resources
CFLAGS=$(shell dpkg-buildflags --get CFLAGS) -I . #-g -pthread -fPIC -I .
LDFLAGS=$(shell dpkg-buildflags --get LDFLAGS)
SOURCEDIR=$(PROJECTDIR)/linux
CLASSPATH=/opt/org.vesalainen.nmea/nmea-router/jar/org/vesalainen/comm/1.0.4/comm-1.0.4.jar
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

header:
	javah -d . -v -cp $(CLASSPATH) $(CLASS)
