SerialChannel
Java library for serial communication

Features:
Selectable channel implementing GatheringByteChannel and ScatteringByteChannel
Support for direct buffers (recommended)
Efficient Support for InputStream and OutputStream
Minimal implementation of javax.comm as a separate jar

Supported platforms:

Linux x64 and arm (Raspberry Pi)

- native support for GatheringByteChannel and ScatteringByteChannel
- select support for read and write

Windows x64 and x32

- support for GatheringByteChannel and ScatteringByteChannel as java code.
- select support for read

When started the native libraries are installed at temporary directory. 
From where the libraries can be installed to proper place.

Example:

Builder builder = new Builder("/dev/ttyUSB0", Speed.B1200)
        .setParity(Parity.SPACE);
try (SerialChannel c = builder.get();
        )
{
...

