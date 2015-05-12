MACHINE=$(shell uname -m)
JAVAH=$(JAVA_HOME)/bin/javah
PROJECTDIR=/home/tkv/NetBeansProjects/SerialChannel
JAVASRC=$(PROJECTDIR)/src/main/java
RESOURCES=$(PROJECTDIR)/src/main/resources
CFLAGS= -fPIC -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux
SOURCEDIR=$(PROJECTDIR)/LinuxSerialChannel
CLASSPATH=$(PROJECTDIR)/target/classes
HEADER=org_vesalainen_comm_channel_linux_LinuxSerialChannel.h
CLASSFILE=$(CLASSPATH)/org/vesalainen/comm/channel/linux/LinuxSerialChannel.class
CLASS=org.vesalainen.comm.channel.linux.LinuxSerialChannel

$(HEADER):	$(CLASSFILE)
	$(JAVAH) -d $(SOURCEDIR) -cp $(CLASSPATH) $(CLASS)

SerialChannel.o:	SerialChannel.c $(HEADER)
SerialChannel.so:	SerialChannel.o
	ld -shared -o SerialChannel.so SerialChannel.o -lc

install:	SerialChannel.so
	cp SerialChannel.so $(RESOURCES)/SerialChannel$(MACHINE).so

all:	install

clean:
	rm *.o
	rm *.so

