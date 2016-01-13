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
#define MAX_BUFFERS org_vesalainen_comm_channel_LinuxSerialChannel_MaxBuffers

void hexdump(int count, char* buf, int len, int bufsize);

#define MIN(x,y)	(x) < (y) ? (x) : (y);
#define MAX(x,y)	(x) > (y) ? (x) : (y);

#define ERRORRETURNV if (debug) fprintf(stderr, "Error at %d\n", __LINE__);return;
#define ERRORRETURN if (debug) fprintf(stderr, "Error at %d\n", __LINE__);return 0;
#define DEBUG(s) if (debug) {fprintf(stderr, "%s at %d\n", (s), __LINE__);fflush(stderr);}

#define CHECK(p)	if (!(p)) {ERRORRETURN;}
#define CHECKV(p)	if (!(p)) {ERRORRETURNV;}
#define CHECKEXC if ((*env)->ExceptionCheck(env)) {ERRORRETURN;};
#define CHECKEXCV if ((*env)->ExceptionCheck(env)) {ERRORRETURNV;};

#define EXCEPTION(m) exception(env, "java/io/IOException", m);ERRORRETURN;
#define EXCEPTIONV(m) exception(env, "java/io/IOException", m);ERRORRETURNV;

#define GETPOSITION(bb) (*env)->CallIntMethod(env, bb, midByteBuffer_GetPosition);CHECKEXC;
#define GETLIMIT(bb) (*env)->CallIntMethod(env, bb, midByteBuffer_GetLimit);CHECKEXC;
#define SETPOSITION(bb, newPos) (*env)->CallObjectMethod(env, bb, midByteBuffer_SetPosition, newPos);CHECKEXC;

#define PUT(bb, barr) (*env)->CallObjectMethod(env, bb, midByteBuffer_PutByteArr, barr);CHECKEXC;
#define GET(bb, barr) (*env)->CallObjectMethod(env, bb, midByteBuffer_GetByteArr, barr);CHECKEXC;

#define ADD(list, item) (*env)->CallObjectMethod(env, list, midList_Add, item);CHECKEXC;
#define ADDV(list, item) (*env)->CallObjectMethod(env, list, midList_Add, item);CHECKEXCV;

static int debug;

static jclass clsByteBuffer;
static jmethodID midByteBuffer_GetPosition;
static jmethodID midByteBuffer_GetLimit;
static jmethodID midByteBuffer_SetPosition;
static jmethodID midByteBuffer_PutByteArr;
static jmethodID midByteBuffer_GetByteArr;
static jclass clsList;
static jmethodID midList_Add;

static pthread_t selectThread;
static pthread_mutex_t  selectMutex = PTHREAD_MUTEX_INITIALIZER;
static sigset_t origmask;

static void sighdl(int sig)
{
    DEBUG("signal");
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_setDebug
  (JNIEnv *env, jobject obj, jboolean on)
{
    debug = on;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doClearBuffers
(JNIEnv *env, jobject obj, jlong ctx)
{
	CTX *c = (CTX*)ctx;
	if (tcflush(c->fd, TCIOFLUSH) < 0)
	{
		EXCEPTIONV("tcflush failed");
	}
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_staticInit
  (JNIEnv *env, jclass cls)
{
    sigset_t mask;
    struct sigaction act;
    
    bzero(&act, sizeof(act));
    
    act.sa_handler = sighdl;
    
    if (sigaction(SIGUSR1, &act, NULL) < 0)
    {
        EXCEPTIONV("sigaction");
    }
        
    sigemptyset(&mask);
    sigaddset(&mask, SIGUSR1);
    
    if (pthread_sigmask(SIG_BLOCK, &mask, &origmask) < 0)
    {
		EXCEPTIONV("sigprocmask");
    }
    
	clsByteBuffer = (*env)->FindClass(env, "java/nio/ByteBuffer");
	CHECKV(clsByteBuffer);
	midByteBuffer_GetPosition = (*env)->GetMethodID(env, clsByteBuffer, "position", "()I");
	CHECKV(midByteBuffer_GetPosition);
	midByteBuffer_GetLimit = (*env)->GetMethodID(env, clsByteBuffer, "limit", "()I");
	CHECKV(midByteBuffer_GetLimit);
	midByteBuffer_SetPosition = (*env)->GetMethodID(env, clsByteBuffer, "position", "(I)Ljava/nio/Buffer;");
	CHECKV(midByteBuffer_SetPosition);
	midByteBuffer_PutByteArr = (*env)->GetMethodID(env, clsByteBuffer, "put", "([B)Ljava/nio/ByteBuffer;");
	CHECKV(midByteBuffer_PutByteArr);
	midByteBuffer_GetByteArr = (*env)->GetMethodID(env, clsByteBuffer, "get", "([B)Ljava/nio/ByteBuffer;");
	CHECKV(midByteBuffer_GetByteArr);

	clsList = (*env)->FindClass(env, "java/util/List");
	CHECKV(clsList);
	midList_Add = (*env)->GetMethodID(env, clsList, "add", "(Ljava/lang/Object;)Z");
	CHECKV(midList_Add);
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doSelect
  (JNIEnv *env, jclass cls, jint readCount, jint writeCount, jobject reads, jobject writes, jint timeout)
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
    
	readArr = (*env)->GetDirectBufferAddress(env, reads);
	CHECK(readArr);
	writeArr = (*env)->GetDirectBufferAddress(env, writes);
	CHECK(writeArr);

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
	if (debug) fprintf(stderr, "sec=%d nsec=%d rc=%d\n", ts.tv_sec, ts.tv_nsec, rc);
	if (rc < 0)
    {
		if (errno != EINTR)
		{
			EXCEPTION("pselect");
		}
		else
		{
			rc = 0;
		}
    }
	else
	{
		for (ii = 0; ii < readCount; ii++)
		{
			CTX *c = (CTX*)readArr[ii];
			readArr[ii] = FD_ISSET(c->fd, &readfds);
		}
		for (ii = 0; ii < writeCount; ii++)
		{
			CTX *c = (CTX*)writeArr[ii];
			writeArr[ii] = FD_ISSET(c->fd, &writefds);
		}
	}
	if (pthread_mutex_lock(&selectMutex) < 0)
	{
		EXCEPTION("pthread_mutex_lock");
	}
	selectThread = 0;
	if (pthread_mutex_unlock(&selectMutex) < 0)
	{
		EXCEPTION("pthread_mutex_unlock");
	}
	return rc;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_wakeupSelect
  (JNIEnv *env, jclass cls)
{

	if (pthread_mutex_lock(&selectMutex) < 0)
	{
		EXCEPTIONV("pthread_mutex_lock");
	}
	if (selectThread)
    {
        if (pthread_kill(selectThread, SIGUSR1) < 0)
        {
			EXCEPTIONV("pthread_kill");
        }
    }
	if (pthread_mutex_unlock(&selectMutex) < 0)
	{
		EXCEPTIONV("pthread_mutex_unlock");
	}
}

/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_version
  (JNIEnv *env, jobject obj)
{
    return org_vesalainen_comm_channel_LinuxSerialChannel_VERSION;
}
JNIEXPORT jlong JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doOpen(
    JNIEnv *env, 
    jobject obj, 
    jbyteArray port
 	)
{
    jsize size;
    jbyte* sPort; 
    
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
		(*env)->ReleaseByteArrayElements(env, port, sPort, 0);
		EXCEPTION(c->szPort);
    }
    if (tcgetattr(c->fd, &c->oldtio) < 0)
    {
        (*env)->ReleaseByteArrayElements(env, port, sPort, 0);
		EXCEPTION("tcgetattr failed");
	}
    if (tcflush(c->fd, TCIOFLUSH) < 0)
    {
		EXCEPTION("tcflush failed");
    }
    (*env)->ReleaseByteArrayElements(env, port, sPort, 0);

    return (jlong)c;
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doConfigure(
    JNIEnv *env, 
    jobject obj,
    jlong ctx, 
    jint bauds, 
    jint parity, 
    jint databits, 
    jint stopbits, 
    jint flow,
    jboolean replaceError
	)
{
    CTX *c = (CTX*)ctx;
    
    bzero(&c->newtio, sizeof(c->newtio));
    
    switch (bauds)
    {
        case 50:
            c->newtio.c_cflag |= B50;
            break;
        case 75:
            c->newtio.c_cflag |= B75;
            break;
        case 110:
            c->newtio.c_cflag |= B110;
            break;
        case 134:
            c->newtio.c_cflag |= B134;
            break;
        case 150:
            c->newtio.c_cflag |= B150;
            break;
        case 200:
            c->newtio.c_cflag |= B200;
            break;
        case 300:
            c->newtio.c_cflag |= B300;
            break;
        case 600:
            c->newtio.c_cflag |= B600;
            break;
        case 1200:
            c->newtio.c_cflag |= B1200;
            break;
        case 2400:
            c->newtio.c_cflag |= B2400;
            break;
        case 4800:
            c->newtio.c_cflag |= B4800;
            break;
        case 9600:
            c->newtio.c_cflag |= B9600;
            break;
        case 19200:
            c->newtio.c_cflag |= B19200;
            break;
        case 38400:
            c->newtio.c_cflag |= B38400;
            break;
        case 57600:
            c->newtio.c_cflag |= B57600;
            break;
        case 115200:
            c->newtio.c_cflag |= B115200;
            break;
        case 230400:
            c->newtio.c_cflag |= B230400;
            break;
        default:
			EXCEPTIONV("unknown baudrate");
            break;
    }
    
    switch (parity)
    {
    case 0:	// NONE
        c->newtio.c_iflag |= IGNPAR;
        break;
    case 1:	// ODD
        c->newtio.c_cflag |= PARENB | PARODD;
        break;
    case 2:	// EVEN
        c->newtio.c_cflag |= PARENB;
        break;
    case 3:	// MARK
        c->newtio.c_cflag |= PARENB | CMSPAR | PARODD;
        break;
    case 4:	// SPACE
        c->newtio.c_cflag |= PARENB | CMSPAR;
        break;
    default:
		EXCEPTIONV("illegal parity value");
        break;
    }
    switch (databits)
    {
    case 1:	// 5
        c->newtio.c_cflag |= CS5;
        break;
    case 2:	// 6
        c->newtio.c_cflag |= CS6;
        break;
    case 3:	// 7
        c->newtio.c_cflag |= CS7;
        break;
    case 4:	// 8
        c->newtio.c_cflag |= CS8;
        break;
    default:
		EXCEPTIONV("illegal datac->newtio.c_cflag |value");
        break;
    }
    switch (stopbits)
    {
    case 0:	// 1
        break;
    case 2:	// 2
        c->newtio.c_cflag |= CSTOPB;
        break;
    default:
		EXCEPTIONV("illegal stopc->newtio.c_cflag |value");
        break;
    }
    switch (flow)
    {
    case 0:	// NONE
        break;
    case 1:	// XONXOFF
        c->newtio.c_iflag |= IXON | IXOFF;
        break;
    case 2:	// RTSCTS
        c->newtio.c_cflag |= CRTSCTS;
        break;
    case 3:	// DSRDTR
    default:
		EXCEPTIONV("illegal flow control value");
        break;
    }
    if (replaceError)
    {
        c->newtio.c_iflag |= PARMRK | INPCK;    // mark parity
    }
    c->newtio.c_cflag |= CLOCAL | CREAD;
    c->newtio.c_cc[VSTART] = 0x11;
    c->newtio.c_cc[VSTOP] = 0x13;
    c->newtio.c_cc[VMIN] = 1;   // block is default
    
    if (tcsetattr(c->fd, TCSAFLUSH, &c->newtio) < 0)
    {
		EXCEPTIONV("tcsetattr failed");
    }
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doClose
  (JNIEnv *env, jobject obj, jlong ctx)
{
    CTX* c = (CTX*)ctx;
    DEBUG("close");
    /*
    if (tcflush(c->fd, TCIFLUSH) < 0)
    {
        exception(env, "java/io/IOException", "tcflush failed");
        ERRORRETURNV;
    }
    if (tcdrain(c->fd) < 0)
    {
        exception(env, "java/io/IOException", "tcflush failed");
        ERRORRETURNV;
    }
    if (tcsetattr(c->fd, TCSADRAIN, &c->oldtio) < 0)
    {
        exception(env, "java/io/IOException", "tcsetattr failed");
        ERRORRETURNV;
    }
    */
    if (close(c->fd) < 0)
    {
		EXCEPTIONV("CloseHandle failed");
    }
    free(c);
}
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doRead
(JNIEnv *env, jobject obj, jlong ctx, jobjectArray bba, jint offset, jint length)
{
	int pos[MAX_BUFFERS];
	int lim[MAX_BUFFERS];
	void* addr;
	jbyteArray barr[MAX_BUFFERS] = { 0 };
	jbyte* arr[MAX_BUFFERS];
	jobject bb[MAX_BUFFERS];
	jsize len[MAX_BUFFERS];
    jint newPos;
	struct iovec vec[MAX_BUFFERS];
	int ii;
	int res;

    ssize_t rc;
    CTX* c = (CTX*)ctx;
    DEBUG("init read");
	for (ii = 0; ii < length; ii++)
	{
		bb[ii] = (*env)->GetObjectArrayElement(env, bba, ii+offset);
		CHECKEXC;
		pos[ii] = GETPOSITION(bb[ii]);
		lim[ii] = GETLIMIT(bb[ii]);
		len[ii] = lim[ii] - pos[ii];
		addr = (*env)->GetDirectBufferAddress(env, bb[ii]);
		if (addr == NULL)
		{
			barr[ii] = (*env)->NewByteArray(env, len[ii]);
			CHECK(barr[ii]);
			arr[ii] = (*env)->GetByteArrayElements(env, barr[ii], NULL);
			CHECK(arr[ii]);
			addr = arr[ii];
		}
		else
		{
			addr += pos[ii];
		}
		vec[ii].iov_base = addr;
		vec[ii].iov_len = len[ii];
	}

    DEBUG("read");
    rc = readv(c->fd, vec, length);
    if (rc < 0 && errno == EAGAIN)
    {
        rc = 0;
    }
    if (rc < 0)
    {
		for (ii = 0; ii < length; ii++)
        {
			if (barr[ii])
			{
				(*env)->ReleaseByteArrayElements(env, barr[ii], arr[ii], 0);
			}
        }
		EXCEPTION(NULL);
    }
	res = rc;
	for (ii = 0; ii < length; ii++)
	{
		int r = 0;
		if (res >= len[ii])
		{
			r = len[ii];
			res -= len[ii];
		}
		else
		{
			r = res;
			res = 0;
		}
		if (barr[ii] != NULL)
		{
			(*env)->ReleaseByteArrayElements(env, barr[ii], arr[ii], 0);
			PUT(bb[ii], barr[ii]);
		}
		newPos = pos[ii] + r;
		SETPOSITION(bb[ii], newPos);
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doWrite
(JNIEnv *env, jobject obj, jlong ctx, jobjectArray bba, jint offset, jint length)
{
    static int count = 0;
	int pos[MAX_BUFFERS];
	int lim[MAX_BUFFERS];
    char* addr;
	jbyteArray barr[MAX_BUFFERS] = { 0 };
	jbyte* arr[MAX_BUFFERS];
	jobject bb[MAX_BUFFERS];
	jint len[MAX_BUFFERS];
	jint newPos;
	struct iovec vec[MAX_BUFFERS];
	int ii;
	int res;

    ssize_t rc;

    CTX* c = (CTX*)ctx;
    
    DEBUG("write");
	for (ii = 0; ii < length; ii++)
	{
		bb[ii] = (*env)->GetObjectArrayElement(env, bba, ii + offset);
		pos[ii] = GETPOSITION(bb[ii]);
		lim[ii] = GETLIMIT(bb[ii]);
		len[ii] = lim[ii] - pos[ii];
		addr = (*env)->GetDirectBufferAddress(env, bb[ii]);
		if (addr == NULL)
		{
			barr[ii] = (*env)->NewByteArray(env, len[ii]);
			CHECK(barr[ii]);
			GET(bb[ii], barr[ii]);
			arr[ii] = (*env)->GetByteArrayElements(env, barr[ii], NULL);
			CHECK(arr[ii]);
			addr = arr[ii];
		}
		else
		{
			addr += pos[ii];
		}
		vec[ii].iov_base = addr;
		vec[ii].iov_len = len[ii];
	}

    rc = writev(c->fd, vec, length);
    if (rc < 0 && errno == EAGAIN)
    {
        rc = 0;
    }
    if (rc < 0)
    {
		for (ii = 0; ii < length;ii++)
        {
			if (barr[ii])
			{
				(*env)->ReleaseByteArrayElements(env, barr[ii], arr[ii], 0);
			}
        }
		EXCEPTION(NULL);
    }

    //hexdump(count, addr, rc, len);
    count += rc;

	res = rc;
	for (ii = 0; ii < length; ii++)
	{
		int r = 0;
		if (res >= len[ii])
		{
			r = len[ii];
			res -= len[ii];
		}
		else
		{
			r = res;
			res = 0;
		}
		if (barr[ii] != NULL)
		{
			(*env)->ReleaseByteArrayElements(env, barr[ii], arr[ii], 0);
		}
		if (r)
		{
			newPos = pos[ii] + r;
			SETPOSITION(bb[ii], newPos);
		}
	}
	return rc;
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doEnumPorts
  (JNIEnv *env, jobject obj, jobject list)
{
    jstring str;
    char buf[PATH_MAX];
	char target[PATH_MAX];
    DIR *dp;
    struct dirent *de = NULL;

    dp = opendir("/dev");
    if (dp == NULL)
    {
		EXCEPTIONV(NULL);
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
					CHECKV(str);
					ADDV(list, str);
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
			sprintf(buf, "%d: %s: %s", errno, strerror(errno), message);
        }
        else
        {
			sprintf(buf, "%d: %s", errno, strerror(errno));
        }
        (*env)->ThrowNew(env, exc, buf);
    }
    else
    {
        (*env)->ThrowNew(env, exc, message);
    }
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_timeouts
  (JNIEnv *env, jobject obj, jlong ctx, jint min, jint time)
{
    CTX* c = (CTX*)ctx;
    int flags = 0;
    
    c->newtio.c_cc[VMIN] = min;
    c->newtio.c_cc[VTIME] = time;
    
    if (tcsetattr(c->fd, TCSANOW, &c->newtio) < 0)
    {
        exception(env, "java/io/IOException", "tcsetattr failed");
        ERRORRETURNV;
    }
    flags = fcntl(c->fd, F_GETFL, 0);
    if (flags < 0)
    {
        exception(env, "java/io/IOException", "fcntl failed");
        ERRORRETURNV;
    }
    if (!min)
    {
        if (fcntl(c->fd, F_SETFL, flags | O_NONBLOCK) < 0)
        {
            exception(env, "java/io/IOException", "fcntl failed");
            ERRORRETURNV;
        }
    }
    else
    {
        if (fcntl(c->fd, F_SETFL, flags & ~O_NONBLOCK) < 0)
        {
            exception(env, "java/io/IOException", "fcntl failed");
            ERRORRETURNV;
        }
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

