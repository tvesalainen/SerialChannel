/*
 * Copyright (C) 2011 Timo Vesalainen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
// SerialChannel.cpp : Defines the exported functions for the DLL application.
//
#undef __cplusplus

#include "SerialChannel.h"

void hexdump(int count, char* buf, int len, int bufsize);

#define MIN(x,y)	(x) < (y) ? (x) : (y);
#define MAX(x,y)	(x) > (y) ? (x) : (y);
#define ERRORRETURNV fprintf(stderr, "Error at %d\n", __LINE__);
#define ERRORRETURN fprintf(stderr, "Error at %d\n", __LINE__);return 0;
#define DEBUG(s) if (debug) fprintf(stderr, "%s at %d\n", (s), __LINE__);fflush(stderr);

static int debug;

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_setDebug
  (JNIEnv *env, jobject obj, jboolean on)
{
    debug = on;
}

/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    connected
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_connected
  (JNIEnv *env, jobject obj, jlong handle)
{
    exception(env, "java/lang/UnsupportedOperationException", NULL);
    ERRORRETURN;
}
/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_version
  (JNIEnv *env, jobject obj)
{
    return org_vesalainen_comm_channel_winx_WinSerialChannel_VERSION;
}
/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    initialize
 * Signature: ([BIZII)I
 */
JNIEXPORT jlong JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_initialize(
    JNIEnv *env, 
    jobject obj, 
    jbyteArray port, 
    jint bauds, 
    jint parity, 
    jint databits, 
    jint stopbits, 
    jint flow,
    jint readIntervalTimeout,
    jint readTotalTimeoutMultiplier,
    jint readTotalTimeoutConstant,
    jint writeTotalTimeoutMultiplier,
    jint writeTotalTimeoutConstant
	)
{
    int fd;
    char szPort[32];
    jsize size;
    jbyte* sPort; 
    char* err;

    DEBUG("initialize");
    sPort = (*env)->GetByteArrayElements(env, port, NULL);
    if (sPort == NULL)
    {
        ERRORRETURN
    }
    size = (*env)->GetArrayLength(env, port);
    memset(szPort, 0, sizeof(szPort));
    if (strncpy(szPort, sPort, size))
    {
        exception(env, "java/io/IOException", "copy com port");
        ERRORRETURN
    }

    DEBUG("open");
    fd = open(sPort, O_RDWR | O_NOCTTY);
    if (fd < 0)
    {
        exception(env, "java/io/IOException", sPort);
        (*env)->ReleaseByteArrayElements(env, port, sPort, 0);
        ERRORRETURN
    }
    (*env)->ReleaseByteArrayElements(env, port, sPort, 0);

    err = configure(
        env, 
        fd, 
        bauds, 
        parity, 
        databits, 
        stopbits, 
        flow
        );
    if (err != NULL)
    {
        exception(env, "java/io/IOException", err);
        ERRORRETURN;
    }
    return (jlong)fd;
}

/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    doClose
 * Signature: ()I
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doClose
  (JNIEnv *env, jobject obj, jlong fd)
{
    DEBUG("close");
    if (close(fd) < 0)
    {
        exception(env, "java/io/IOException", "CloseHandle failed");
        ERRORRETURNV
    }
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doFlush
  (JNIEnv *env, jobject obj, jlong fd)
{
}
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_commStatus
  (JNIEnv *env, jobject obj, jlong fd)
{
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_setEventMask
  (JNIEnv *env, jobject obj, jlong fd, jint mask)
{
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_waitEvent
  (JNIEnv *env, jobject obj, jlong handle)
{
    return 0;
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doGetError
  (JNIEnv *env, jobject obj, jlong handle, jobject commStat)
{
    return 0;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doWaitOnline
  (JNIEnv *env, jobject obj, jlong handle)
{
}
/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    doRead
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doRead
  (JNIEnv *env, jobject obj, jlong fd, jobject bb)
{
    int pos;
    int lim;
    void* addr;
    jclass cls;
    jmethodID pmid;
    jmethodID lmid;
    jmethodID putmid;
    jmethodID spmid;
    jbyteArray barr = NULL;
    jint len;
    jbyte* arr;
    jint newPos;

    ssize_t rc;
    DEBUG("read");
    cls = (*env)->GetObjectClass(env, bb);
    pmid = (*env)->GetMethodID(env, cls, "position", "()I");
    if (pmid == NULL)
    {
        ERRORRETURN
    }
    pos = (*env)->CallIntMethod(env, bb, pmid);
    if ((*env)->ExceptionCheck(env))
    {
        ERRORRETURN
    }
    lmid = (*env)->GetMethodID(env, cls, "limit", "()I");
    if (lmid == NULL)
    {
        ERRORRETURN
    }
    lim = (*env)->CallIntMethod(env, bb, lmid);
    if ((*env)->ExceptionCheck(env))
    {
        ERRORRETURN
    }
    len = lim - pos;
    addr = (*env)->GetDirectBufferAddress(env, bb);
    if (addr == NULL)
    {
        barr = (*env)->NewByteArray(env, len);
        if (barr == NULL)
        {
            ERRORRETURN
        }
        arr = (*env)->GetByteArrayElements(env, barr, NULL);
        if (arr == NULL)
        {
            (*env)->DeleteLocalRef(env, barr);
            ERRORRETURN
        }
        addr = arr;
    }
    else
    {
        addr += pos;
    }

    DEBUG("read");
    rc = read(fd, addr, len);
    if (rc < 0)
    {
        if (barr != NULL)
        {
            (*env)->ReleaseByteArrayElements(env, barr, arr, 0);
            (*env)->DeleteLocalRef(env, barr);
        }
        exception(env, "java/io/IOException", NULL);
        ERRORRETURN
    }
    if (barr != NULL)
    {
        (*env)->ReleaseByteArrayElements(env, barr, arr, 0);
        putmid = (*env)->GetMethodID(env, cls, "put", "([B)Ljava/nio/ByteBuffer;");
        if (putmid == NULL)
        {
            (*env)->DeleteLocalRef(env, barr);
            ERRORRETURN
        }
        (*env)->CallObjectMethod(env, bb, putmid, barr);
        if ((*env)->ExceptionCheck(env))
        {
            (*env)->DeleteLocalRef(env, barr);
            ERRORRETURN
        }
        (*env)->DeleteLocalRef(env, barr);
    }
    spmid = (*env)->GetMethodID(env, cls, "position", "(I)Ljava/nio/Buffer;");
    if (spmid == NULL)
    {
        ERRORRETURN
    }
    newPos = pos + rc;
    (*env)->CallObjectMethod(env, bb, spmid, newPos);
    if ((*env)->ExceptionCheck(env))
    {
        ERRORRETURN
    }
    return rc;
}

/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    doWrite
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doWrite
  (JNIEnv *env, jobject obj, jlong fd, jobject bb)
{
    static int count = 0;
    int pos;
    int lim;
    char* addr;
    jclass cls;
    jmethodID pmid;
    jmethodID lmid;
    jmethodID gmid;
    jmethodID spmid;
    jbyteArray barr = NULL;
    jint len;
    jbyte* arr;
    jint newPos;

    ssize_t rc;

    DEBUG("write");
    cls = (*env)->GetObjectClass(env, bb);
    pmid = (*env)->GetMethodID(env, cls, "position", "()I");
    if (pmid == NULL)
    {
        ERRORRETURN
    }
    pos = (*env)->CallIntMethod(env, bb, pmid);
    if ((*env)->ExceptionCheck(env))
    {
        ERRORRETURN
    }
    lmid = (*env)->GetMethodID(env, cls, "limit", "()I");
    if (lmid == NULL)
    {
        ERRORRETURN
    }
    lim = (*env)->CallIntMethod(env, bb, lmid);
    if ((*env)->ExceptionCheck(env))
    {
        ERRORRETURN
    }
    len = lim - pos;
    addr = (*env)->GetDirectBufferAddress(env, bb);
    if (addr == NULL)
    {
        barr = (*env)->NewByteArray(env, len);
        if (barr == NULL)
        {
            ERRORRETURN
        }
        gmid = (*env)->GetMethodID(env, cls, "get", "([B)Ljava/nio/ByteBuffer;");
        if (gmid == NULL)
        {
            (*env)->DeleteLocalRef(env, barr);
            ERRORRETURN
        }
        (*env)->CallObjectMethod(env, bb, gmid, barr);
        if ((*env)->ExceptionCheck(env))
        {
            (*env)->DeleteLocalRef(env, barr);
            ERRORRETURN
        }
        arr = (*env)->GetByteArrayElements(env, barr, NULL);
        if (arr == NULL)
        {
            (*env)->DeleteLocalRef(env, barr);
            ERRORRETURN
        }
        addr = arr;
    }
    else
    {
        addr += pos;
    }

    rc = write(fd, addr, len);
    if (rc < 0)
    {
        (*env)->ReleaseByteArrayElements(env, barr, arr, 0);
        (*env)->DeleteLocalRef(env, barr);
        exception(env, "java/io/IOException", NULL);
        ERRORRETURN
    }

    //hexdump(count, addr, dwWritten, len);
    count += rc;

    if (barr != NULL)
    {
        (*env)->ReleaseByteArrayElements(env, barr, arr, 0);
        (*env)->DeleteLocalRef(env, barr);
    }
    spmid = (*env)->GetMethodID(env, cls, "position", "(I)Ljava/nio/Buffer;");
    if (spmid == NULL)
    {
        ERRORRETURN
    }
    newPos = pos + fd;
    (*env)->CallObjectMethod(env, bb, spmid, newPos);
    if ((*env)->ExceptionCheck(env))
    {
        ERRORRETURN
    }
    return rc;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doEnumPorts
  (JNIEnv *env, jobject obj, jobject list)
{
    jclass cls;
    jstring str;
    jmethodID addid;
    char buf[256];
    char target[256];
    DIR *dp;
    struct dirent *de = NULL;

    cls = (*env)->GetObjectClass(env, list);
    addid = (*env)->GetMethodID(env, cls, "add", "(Ljava/lang/Object;)Z");
    if (addid == NULL)
    {
        ERRORRETURNV;
    }

    dp = opendir("/dev");
    if (dp == NULL)
    {
        exception(env, "java/io/IOException", NULL);
        ERRORRETURNV;
    }
    de = readdir(dp);
    while (de != NULL)
    {
        if (de->d_type == DT_CHR)
        {
            str = (*env)->NewStringUTF(env, de->d_name);
            if (str == NULL)
            {
                    ERRORRETURNV;
            }
            (*env)->CallObjectMethod(env, list, addid, str);
            if ((*env)->ExceptionCheck(env))
            {
                    ERRORRETURNV;
            }
        }
        de = readdir(dp);
    }
}

void exception(JNIEnv * env, const char* clazz, const char* message)
{
    jclass exc;
    char buf[256];

    exc = (*env)->FindClass(env, clazz);
    if (exc == NULL)
    {
        return;
    }
    if (errno != 0)
    {
        if (message != NULL)
        {
            sprintf(buf, "%s: %s", strerror(errno), message);
        }
        else
        {
            sprintf(buf, "%s", strerror(errno));
        }
        (*env)->ThrowNew(env, exc, buf);
    }
    else
    {
        (*env)->ThrowNew(env, exc, message);
    }
}

char* configure(
    JNIEnv *env, 
    int fd, 
    int bauds, 
    int parity, 
    int databits, 
    int stopbits, 
    int flow
	)
{
    struct termios tio;
    int baudrate = 0;
    int bits = 0;
    int stop = 0;
    int par = 0;
    int fctrl = 0;
    
    bzero(&tio, sizeof(tio));
    
    switch (bauds)
    {
        case 50:
            baudrate = B50;
            break;
        case 75:
            baudrate = B75;
            break;
        case 110:
            baudrate = B110;
            break;
        case 134:
            baudrate = B134;
            break;
        case 150:
            baudrate = B150;
            break;
        case 200:
            baudrate = B200;
            break;
        case 300:
            baudrate = B300;
            break;
        case 600:
            baudrate = B600;
            break;
        case 1200:
            baudrate = B1200;
            break;
        case 2400:
            baudrate = B2400;
            break;
        case 4800:
            baudrate = B4800;
            break;
        case 9600:
            baudrate = B9600;
            break;
        case 19200:
            baudrate = B19200;
            break;
        case 38400:
            baudrate = B38400;
            break;
        case 57600:
            baudrate = B57600;
            break;
        case 115200:
            baudrate = B115200;
            break;
        case 230400:
            baudrate = B230400;
            break;
        case 460800:
            baudrate = B460800;
            break;
        case 500000:
            baudrate = B500000;
            break;
        case 576000:
            baudrate = B576000;
            break;
        case 921600:
            baudrate = B921600;
            break;
        case 1000000:
            baudrate = B1000000;
            break;
        case 1152000:
            baudrate = B1152000;
            break;
        case 1500000:
            baudrate = B1500000;
            break;
        case 2000000:
            baudrate = B2000000;
            break;
        case 2500000:
            baudrate = B2500000;
            break;
        case 3000000:
            baudrate = B3000000;
            break;
        case 3500000:
            baudrate = B3500000;
            break;
        case 4000000:
            baudrate = B4000000;
            break;
        default:
            return "unknown baudrate";
            break;
    }
    
    switch (parity)
    {
    case 0:	// NONE
        break;
    case 1:	// ODD
        par = PARENB | PARODD;
        break;
    case 2:	// EVEN
        par = PARENB;
        break;
    case 3:	// MARK
        par = CMSPAR | PARODD;
        break;
    case 4:	// SPACE
        par = CMSPAR;
        break;
    default:
        return "illegal parity value";
        break;
    }
    switch (databits)
    {
    case 1:	// 5
        bits = CS5;
        break;
    case 2:	// 6
        bits = CS6;
        break;
    case 3:	// 7
        bits = CS7;
        break;
    case 4:	// 8
        bits = CS8;
        break;
    default:
        return "illegal databits value";
        break;
    }
    switch (stopbits)
    {
    case 0:	// 1
        break;
    case 2:	// 2
        stop = CSTOPB;
        break;
    default:
        return "illegal stopbits value";
        break;
    }
    switch (flow)
    {
    case 0:	// NONE
        break;
    default:
        return "illegal flow control value";
        break;
    }
    tio.c_cflag = baudrate | bits | stop | par;
    tio.c_lflag = ICANON;
    tio.c_cc[VMIN] = 1;
    
    tcsetattr(fd, TCSANOW, &tio);
    return NULL;
}

void hexdump(int count, char* buf, int len, int bufsize)
{
    int ii;
    unsigned char cc;

    fprintf(stderr, "%d %d/%d: ", count, len, bufsize);
    for (ii=0;ii<len;ii++)
    {
        cc = buf[ii] & 0xff;
        fprintf(stderr, "%02x ", cc);
    }
    fprintf(stderr, "\n");
}

