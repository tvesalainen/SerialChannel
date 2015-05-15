/*
 * Copyright (C) 2015 Timo Vesalainen
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
#define ERRORRETURNV fprintf(stderr, "Error %s at %d\n", strerror(errno), __LINE__);
#define ERRORRETURN fprintf(stderr, "Error %s at %d\n", strerror(errno), __LINE__);return 0;
#define DEBUG(s) if (debug) fprintf(stderr, "%s at %d\n", (s), __LINE__);fflush(stderr);

static int debug;
static pthread_t selectThread;
static sigset_t origmask;

static void sighdl(int sig)
{
    DEBUG("signal");
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_setDebug
  (JNIEnv *env, jobject obj, jboolean on)
{
    debug = on;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_staticInit
  (JNIEnv *env, jclass cls)
{
    sigset_t mask;
    struct sigaction act;
    
    bzero(&act, sizeof(act));
    
    act.sa_handler = sighdl;
    
    if (sigaction(SIGUSR1, &act, NULL) < 0)
    {
        exception(env, "java/io/IOException", "sigaction");
        ERRORRETURNV;
    }
        
    sigemptyset(&mask);
    sigaddset(&mask, SIGUSR1);
    
    if (pthread_sigmask(SIG_BLOCK, &mask, &origmask) < 0)
    {
        exception(env, "java/io/IOException", "sigprocmask");
        ERRORRETURNV;
    }
    
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doSelect
  (JNIEnv *env, jclass cls, jint readCount, jint writeCount, jlongArray reads, jlongArray writes, jint timeout)
{
    jlong *readArr;
    jlong *writeArr;
    
    int nfds = 0;
    fd_set readfds;
    fd_set writefds;
    struct timespec ts;
    sigset_t sigmask;
    int ii, rc;
    
    bzero(&ts, sizeof(ts));
    
    selectThread = pthread_self();
    
    readArr = (*env)->GetLongArrayElements(env, reads, NULL);
    if (readArr == NULL)
    {
        exception(env, "java/io/IOException", "GetLongArrayElements");
        ERRORRETURN;
    }
    writeArr = (*env)->GetLongArrayElements(env, writes, NULL);
    if (writeArr == NULL)
    {
        exception(env, "java/io/IOException", "GetLongArrayElements");
        ERRORRETURN;
    }
    FD_ZERO(&readfds);
    FD_ZERO(&writefds);
    for (ii=0;ii<readCount;ii++)
    {
        CTX *c = (CTX*)readArr[ii];
        FD_SET(c->fd, &readfds);
        nfds = MAX(nfds, c->fd);
    }
    for (ii=0;ii<writeCount;ii++)
    {
        CTX *c = (CTX*)writeArr[ii];
        FD_SET(c->fd, &writefds);
        nfds = MAX(nfds, c->fd);
    }

    if (timeout <0)
    {
        timeout = 0xFFFFFF;
    }
    ts.tv_sec = timeout / 1000;
    ts.tv_nsec = (timeout % 1000)*1000000;
    rc = pselect(nfds+1, &readfds, &writefds, NULL, &ts, &origmask);
    if (rc < 0 && errno != EINTR)
    {
        (*env)->ReleaseLongArrayElements(env, reads, readArr, 0);
        (*env)->ReleaseLongArrayElements(env, writes, writeArr, 0);
        exception(env, "java/io/IOException", "pselect");
        ERRORRETURN;
    }
    for (ii=0;ii<readCount;ii++)
    {
        CTX *c = (CTX*)readArr[ii];
        readArr[ii] = FD_ISSET(c->fd, &readfds);
    }
    for (ii=0;ii<writeCount;ii++)
    {
        CTX *c = (CTX*)writeArr[ii];
        writeArr[ii] = FD_ISSET(c->fd, &writefds);
    }
    (*env)->ReleaseLongArrayElements(env, reads, readArr, 0);
    (*env)->ReleaseLongArrayElements(env, writes, writeArr, 0);
    selectThread = 0;
    return rc;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_wakeupSelect
  (JNIEnv *env, jclass cls)
{

    if (selectThread)
    {
        if (pthread_kill(selectThread, SIGUSR1) < 0)
        {
            exception(env, "java/io/IOException", "pthread_kill");
            ERRORRETURNV
        }
    }
}

JNIEXPORT jboolean JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_connected
  (JNIEnv *env, jobject obj, jlong ctx)
{
    exception(env, "java/lang/UnsupportedOperationException", NULL);
    ERRORRETURN;
}
/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_version
  (JNIEnv *env, jobject obj)
{
    return org_vesalainen_comm_channel_linux_LinuxSerialChannel_VERSION;
}
/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    initialize
 * Signature: ([BIZII)I
 */
JNIEXPORT jlong JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_initialize(
    JNIEnv *env, 
    jobject obj, 
    jbyteArray port, 
    jint bauds, 
    jint parity, 
    jint databits, 
    jint stopbits, 
    jint flow
 	)
{
    jsize size;
    jbyte* sPort; 
    char* err;
    
    CTX *c = (CTX*)calloc(1, sizeof(CTX));

    DEBUG("initialize");
    sPort = (*env)->GetByteArrayElements(env, port, NULL);
    if (sPort == NULL)
    {
        ERRORRETURN
    }
    size = (*env)->GetArrayLength(env, port);
    strncpy(c->szPort, sPort, size);

    DEBUG("open");
    c->fd = open(c->szPort, O_RDWR | O_NOCTTY);
    if (c->fd < 0)
    {
        exception(env, "java/io/IOException", c->szPort);
        (*env)->ReleaseByteArrayElements(env, port, sPort, 0);
        ERRORRETURN
    }
    (*env)->ReleaseByteArrayElements(env, port, sPort, 0);

    err = configure(
        env, 
        c, 
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
    return (jlong)c;
}

/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    doClose
 * Signature: ()I
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doClose
  (JNIEnv *env, jobject obj, jlong ctx)
{
    CTX* c = (CTX*)ctx;
    DEBUG("close");
    if (tcdrain(c->fd) < 0)
    {
        exception(env, "java/io/IOException", "tcdrain");
        ERRORRETURNV
    }
    if (tcsetattr(c->fd, TCSANOW, &c->oldtio) < 0)
    {
        exception(env, "java/io/IOException", "tcsetattr failed");
        ERRORRETURNV;
    }
    if (close(c->fd) < 0)
    {
        exception(env, "java/io/IOException", "CloseHandle failed");
        ERRORRETURNV
    }
    free(c);
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doFlush
  (JNIEnv *env, jobject obj, jlong ctx)
{
    CTX* c = (CTX*)ctx;
    if (tcdrain(c->fd) < 0)
    {
        exception(env, "java/io/IOException", "tcdrain failed");
        ERRORRETURNV
    }
}
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_commStatus
  (JNIEnv *env, jobject obj, jlong ctx)
{
    CTX* c = (CTX*)ctx;
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_setEventMask
  (JNIEnv *env, jobject obj, jlong ctx, jint mask)
{
    CTX* c = (CTX*)ctx;
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_waitEvent
  (JNIEnv *env, jobject obj, jlong ctx)
{
    CTX* c = (CTX*)ctx;
    return 0;
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doGetError
  (JNIEnv *env, jobject obj, jlong ctx, jobject commStat)
{
    CTX* c = (CTX*)ctx;
    return 0;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doWaitOnline
  (JNIEnv *env, jobject obj, jlong ctx)
{
    CTX* c = (CTX*)ctx;
}
/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    doRead
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doRead
  (JNIEnv *env, jobject obj, jlong ctx, jobject bb)
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
    CTX* c = (CTX*)ctx;
    DEBUG("init read");
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
    rc = read(c->fd, addr, len);
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
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doWrite
  (JNIEnv *env, jobject obj, jlong ctx, jobject bb)
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

    CTX* c = (CTX*)ctx;
    
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

    rc = write(c->fd, addr, len);
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
    newPos = pos + rc;
    (*env)->CallObjectMethod(env, bb, spmid, newPos);
    if ((*env)->ExceptionCheck(env))
    {
        ERRORRETURN
    }
    return rc;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doEnumPorts
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
            int l = strlen(de->d_name);
            if (l >= 4)
            {
                if (
                        de->d_name[0] == 't' &&
                        de->d_name[1] == 't' &&
                        de->d_name[2] == 'y' &&
                        isupper(de->d_name[3])
                        )
                {
                    char b[PATH_MAX];
                    sprintf(b, "/dev/%s", de->d_name);
                    str = (*env)->NewStringUTF(env, b);
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
            }
        }
        de = readdir(dp);
    }
    closedir(dp);
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
    CTX* c, 
    int bauds, 
    int parity, 
    int databits, 
    int stopbits, 
    int flow
	)
{
    int baudrate = 0;
    int bits = 0;
    int stop = 0;
    int par = 0;
    int fctrl = 0;
    
    if (tcgetattr(c->fd, &c->oldtio) < 0)
    {
        return "tcgetattr failed";
    }
    
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
        par = PARENB | CMSPAR | PARODD;
        break;
    case 4:	// SPACE
        par = PARENB | CMSPAR;
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
        c->newtio.c_iflag |= IGNPAR;
        break;
    case 1:	// XONXOFF
        c->newtio.c_iflag |= IXON | IXOFF;
        break;
    case 2:	// RTSCTS
        fctrl = CRTSCTS;
        break;
    case 3:	// DSRDTR
    default:
        return "illegal flow control value";
        break;
    }
    c->newtio.c_iflag |= PARMRK;    // mark parity
    c->newtio.c_cflag = baudrate | bits | stop | par | fctrl | CLOCAL | CREAD;
    c->newtio.c_cc[VMIN] = 1;
    
    if (tcflush(c->fd, TCIFLUSH) < 0)
    {
        return "tcflush failed";
    }
    if (tcsetattr(c->fd, TCSANOW, &c->newtio) < 0)
    {
        return "tcsetattr failed";
    }
    return NULL;
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_timeouts
  (JNIEnv *env, jobject obj, jlong ctx, jint min, jint time)
{
    CTX* c = (CTX*)ctx;
    
    c->newtio.c_cc[VMIN] = min;
    c->newtio.c_cc[VTIME] = time;
    
    if (tcsetattr(c->fd, TCSANOW, &c->newtio) < 0)
    {
        exception(env, "java/io/IOException", "tcsetattr failed");
        ERRORRETURNV;
    }
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

