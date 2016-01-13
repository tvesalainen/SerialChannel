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

#include "stdafx.h"
#include "SerialChannel.h"
void hexdump(int count, char* buf, int len, int bufsize);


#define MIN(x,y)	(x) < (y) ? (x) : (y);
#define MAX(x,y)	(x) > (y) ? (x) : (y);

#define ERRORRETURNV if (debug) fprintf(stderr, "Error at %d\n", __LINE__);return;
#define ERRORRETURN if (debug) fprintf(stderr, "Error at %d\n", __LINE__);return 0;
#define DEBUG(s) if (debug) {fprintf(stderr, "%s at %d\n", (s), __LINE__);fflush(stderr)};

#define CHECK(p)	if (!(p)) {ERRORRETURN;}
#define CHECKV(p)	if (!(p)) {ERRORRETURNV;}
#define CHECKEXC if ((*env)->ExceptionCheck(env)) {ERRORRETURN;};
#define CHECKEXCV if ((*env)->ExceptionCheck(env)) {ERRORRETURNV;};

#define EXCEPTION(m) exception(env, "java/io/IOException", m, __FILE__, __LINE__);ERRORRETURN;
#define EXCEPTIONV(m) exception(env, "java/io/IOException", m, __FILE__, __LINE__);ERRORRETURNV;

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

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_staticInit
(JNIEnv *env, jclass cls)
{
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
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doClearBuffers
(JNIEnv *env, jobject obj, jlong ctx)
{
	CTX *c = (CTX*)ctx;
	DEBUG("PurgeComm");
	if (!PurgeComm(c->hComm, PURGE_RXCLEAR | PURGE_TXCLEAR))
	{
		EXCEPTIONV("PurgeComm");
	}
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_setDebug
  (JNIEnv *env, jobject obj, jboolean on)
{
	debug = on;
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_version
  (JNIEnv *env, jobject obj)
{
	return org_vesalainen_comm_channel_WinSerialChannel_VERSION;
}
JNIEXPORT jlong JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doOpen(
	JNIEnv *env, 
	jobject obj, 
	jbyteArray port
	)
{
	char szPort[32];
	char buf[256];
	jsize size;
	jbyte* sPort; 
	CTX *c = NULL;

	c = (CTX*)calloc(1, sizeof(CTX));

	DEBUG("initialize");
	sPort = (*env)->GetByteArrayElements(env, port, NULL);
	if (sPort == NULL)
	{
		EXCEPTION("GetByteArrayElements");
	}
	size = (*env)->GetArrayLength(env, port);
	FillMemory(szPort, sizeof(szPort), 0);
	if (strncpy_s(szPort, sizeof(szPort), sPort, size))
	{
		EXCEPTION("copy com port");
	}
	if (sprintf_s(buf, sizeof(buf), "\\\\.\\%s", szPort) == -1)
	{
		EXCEPTION("create port name");
	}

	DEBUG("CreateFile");
	c->hComm = CreateFile( buf,  
				GENERIC_READ | GENERIC_WRITE, 
				0, 
				0, 
				OPEN_EXISTING,
				FILE_FLAG_OVERLAPPED,
				0);
	if (c->hComm == INVALID_HANDLE_VALUE)
	{
		free(c);
		(*env)->ReleaseByteArrayElements(env, port, sPort, 0);
		EXCEPTION(buf);
	}
	(*env)->ReleaseByteArrayElements(env, port, sPort, 0);

	if (!GetCommState(c->hComm, &c->dcb))
	{
		EXCEPTION("GetCommState failed");
	}

	DEBUG("PurgeComm");
	if (!PurgeComm(c->hComm, PURGE_RXCLEAR | PURGE_TXCLEAR))
	{
		EXCEPTION("PurgeComm");
	}
	return (jlong)c;
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doConfigure
(
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
	int framesize = 1;	// start bit

	c->dcb.fBinary = TRUE;
	c->dcb.fDtrControl = DTR_CONTROL_DISABLE;
	c->dcb.fDsrSensitivity = FALSE;
	c->dcb.fTXContinueOnXoff = FALSE;
	c->dcb.fOutX = FALSE;
	c->dcb.fInX = FALSE;
	c->dcb.fErrorChar = FALSE;
	c->dcb.fNull = FALSE;
	c->dcb.fRtsControl = RTS_CONTROL_DISABLE;
	c->dcb.fAbortOnError = FALSE;
	c->dcb.fOutxCtsFlow = FALSE;
	c->dcb.fOutxDsrFlow = FALSE;

	c->dcb.BaudRate = bauds;

	switch (parity)
	{
	case 0:	// NONE
		c->dcb.fParity = FALSE;
		c->dcb.Parity = NOPARITY;
		break;
	case 1:	// ODD
		c->dcb.fParity = TRUE;
		c->dcb.Parity = ODDPARITY;
		break;
	case 2:	// EVEN
		c->dcb.fParity = TRUE;
		c->dcb.Parity = EVENPARITY;
		break;
	case 3:	// MARK
		c->dcb.fParity = TRUE;
		c->dcb.Parity = MARKPARITY;
		break;
	case 4:	// SPACE
		c->dcb.fParity = TRUE;
		c->dcb.Parity = SPACEPARITY;
		break;
	default:
		EXCEPTIONV("illegal parity value");
		break;
}
	if (c->dcb.Parity != NOPARITY)
	{
		framesize++;
	}
	switch (databits)
	{
	case 0:	// 4
		c->dcb.ByteSize = 4;
		break;
	case 1:	// 5
		c->dcb.ByteSize = 5;
		break;
	case 2:	// 6
		c->dcb.ByteSize = 6;
		break;
	case 3:	// 7
		c->dcb.ByteSize = 7;
		break;
	case 4:	// 8
		c->dcb.ByteSize = 8;
		break;
	default:
		EXCEPTIONV("illegal databits value");
		break;
	}
	framesize += c->dcb.ByteSize;
	switch (stopbits)
	{
	case 0:	// 1
		c->dcb.StopBits = ONESTOPBIT;
		framesize += 1;
		break;
	case 1:	// 1.5
		c->dcb.StopBits = ONE5STOPBITS;
		framesize += 2;
		break;
	case 2:	// 2
		c->dcb.StopBits = TWOSTOPBITS;
		framesize += 2;
		break;
	default:
		EXCEPTIONV("illegal stopbits value");
		break;
	}
	switch (flow)
	{
	case 0:	// NONE
		break;
	case 1:	// XONXOFF
		c->dcb.fInX = TRUE;
		c->dcb.fOutX = TRUE;
		break;
	case 2:	// RTSCTS
		c->dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
		c->dcb.fOutxCtsFlow = TRUE;
		break;
	case 3:	// DSRDTR
		c->dcb.fDtrControl = DTR_CONTROL_HANDSHAKE;
		c->dcb.fOutxDsrFlow = TRUE;
		break;
	default:
		EXCEPTIONV("illegal flow control value");
		break;
	}
	if (replaceError)
	{
		c->dcb.fErrorChar = TRUE;
		c->dcb.ErrorChar = 0xff;
	}
	if (!SetCommState(c->hComm, &c->dcb))
	{
		EXCEPTIONV("SetCommState failed");
	}
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_timeouts(
	JNIEnv *env,
	jobject obj,
	jlong ctx,
	jint readIntervalTimeout,
	jint readTotalTimeoutMultiplier,
	jint readTotalTimeoutConstant,
	jint writeTotalTimeoutMultiplier,
	jint writeTotalTimeoutConstant
)
{
	/*
	If an application sets ReadIntervalTimeout and ReadTotalTimeoutMultiplier to MAXDWORD
	and sets ReadTotalTimeoutConstant to a value greater than zero and less than MAXDWORD,
	one of the following occurs when the ReadFile function is called:
	- If there are any bytes in the input buffer, ReadFile returns immediately with the
	bytes in the buffer.
	- If there are no bytes in the input buffer, ReadFile waits until a byte arrives
	and then returns immediately.
	- If no bytes arrive within the time specified by ReadTotalTimeoutConstant, ReadFile times out.
	*/
	COMMTIMEOUTS timeouts;
	CTX *c = (CTX*)ctx;

	timeouts.ReadIntervalTimeout = readIntervalTimeout;
	timeouts.ReadTotalTimeoutMultiplier = readTotalTimeoutMultiplier;
	timeouts.ReadTotalTimeoutConstant = readTotalTimeoutConstant;
	timeouts.WriteTotalTimeoutMultiplier = writeTotalTimeoutMultiplier;
	timeouts.WriteTotalTimeoutConstant = writeTotalTimeoutConstant;
	if (!SetCommTimeouts(c->hComm, &timeouts))
	{
		EXCEPTIONV("SetCommTimeouts failed");
	}

}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doClose
  (JNIEnv *env, jobject obj, jlong ctx)
{
	CTX *c = (CTX*)ctx;
	/*
	DEBUG("PurgeComm");
	if (!PurgeComm(c->hComm, PURGE_RXABORT | PURGE_RXCLEAR | PURGE_TXABORT ))
	{
		exception(env, "java/io/IOException", "PurgeComm");
		ERRORRETURNV
	}
	*/
	if (!FlushFileBuffers(c->hComm))
	{
		EXCEPTIONV("FlushFileBuffers");
	}

	DEBUG("CloseHandle");
	if (!CloseHandle(c->hComm))
	{
		free(c);
		EXCEPTIONV("CloseHandle failed");
	}
	free(c);
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_setEventMask
  (JNIEnv *env, jobject obj, jlong ctx, jint mask)
{
	CTX *c = (CTX*)ctx;

	DEBUG("SetCommMask");
	if (!SetCommMask(c->hComm, mask))
	{
		EXCEPTIONV("SetCommMask");
	}
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_waitEvent
(JNIEnv *env, jobject obj, jlong ctx, jint mask)
{
	DWORD dwCommEvent = 0;
	DWORD dwRes;
	OVERLAPPED osStatus = {0};
	CTX *c = (CTX*)ctx;

	if (mask)
	{
		DEBUG("SetCommMask");
		if (!SetCommMask(c->hComm, mask))
		{
			EXCEPTION("SetCommMask");
		}
	}
	osStatus.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

	if (osStatus.hEvent == NULL)
	{
		EXCEPTION(NULL);
	}
	DEBUG("WaitCommEvent");
	if (!WaitCommEvent(c->hComm, &dwCommEvent, &osStatus))
	{
		if (GetLastError() != ERROR_IO_PENDING)
		{
			CloseHandle(osStatus.hEvent);
			EXCEPTION(NULL);
		}
	DEBUG("WaitForSingleObject");
		dwRes = WaitForSingleObject(osStatus.hEvent, INFINITE);
		switch(dwRes)
		{
		  case WAIT_OBJECT_0:
			if (!GetOverlappedResult(c->hComm, &osStatus, &dwCommEvent, FALSE))
			{
				CloseHandle(osStatus.hEvent);
				EXCEPTION(NULL);
			}

			break;
		  default:
			CloseHandle(osStatus.hEvent);
			EXCEPTION(NULL);
			break;
		}
	}
	CloseHandle(osStatus.hEvent);
	return dwCommEvent;
}
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doSelect
(JNIEnv *env, jobject obj, jint len, jobject ctxs, jint timeout)
{
	jlong *ctxArr;
	jint count = 0;
	DWORD dwCommEvent[MAXIMUM_WAIT_OBJECTS];
	DWORD dwRes;
	OVERLAPPED osStatus[MAXIMUM_WAIT_OBJECTS] = { 0 };
	HANDLE waits[MAXIMUM_WAIT_OBJECTS];
	DWORD to = timeout;
	int waitCount = 0;
	int index[MAXIMUM_WAIT_OBJECTS] = { 0 };
	int ii;

	if (timeout < 0)
	{
		to = INFINITE;
	}
	if (len > MAXIMUM_WAIT_OBJECTS)
	{
		EXCEPTION("too many channels");
	}
	ctxArr = (*env)->GetDirectBufferAddress(env, ctxs);
	CHECK(ctxArr);
	for (ii = 0; ii < len; ii++)
	{
		CTX *ctx = (CTX*)ctxArr[ii];
		DEBUG("SetCommMask");
		if (!SetCommMask(ctx->hComm, EV_RXCHAR))
		{
			EXCEPTION("SetCommMask");
		}
		osStatus[ii].hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

		if (osStatus[ii].hEvent == NULL)
		{
			EXCEPTION("CreateEvent");
		}
		DEBUG("WaitCommEvent");
		if (!WaitCommEvent(ctx->hComm, &dwCommEvent[ii], &osStatus[ii]))
		{
			switch (GetLastError())
			{
			case ERROR_IO_PENDING:
				index[waitCount] = ii;
				waits[waitCount++] = osStatus[ii].hEvent;
				break;
			case ERROR_INVALID_PARAMETER:
				// wakeup is implemented by calling SetCommMask from a different thread
				// which causes this error
				for (ii = 0; ii < waitCount; ii++)
				{
					CloseHandle(waits[ii]);
				}
				return 0;
				break;
			default:
				EXCEPTION("WaitCommEvent");
				break;
			}
		}
		else
		{
			CloseHandle(osStatus[ii].hEvent);
			ctxArr[ii] = 0;
			count++;
		}
	}
	if (waitCount)
	{
		if (waitCount != len)	// we already have event
		{
			to = 0;
		}
		dwRes = WaitForMultipleObjects(waitCount, waits, FALSE, to);
		switch (dwRes)
		{
		case WAIT_TIMEOUT:
			for (ii = 0; ii < waitCount; ii++)
			{
				CloseHandle(waits[ii]);
			}
			break;
		case WAIT_FAILED:
			for (ii = 0; ii < waitCount; ii++)
			{
				CloseHandle(waits[ii]);
			}
			EXCEPTION("WaitForMultipleObjects");
				break;
		default:

			for (ii = WAIT_OBJECT_0; ii < waitCount; ii++)
			{
				int jj = index[ii];
				CTX *ctx = (CTX*)ctxArr[jj];
				if (!GetOverlappedResult(ctx->hComm, &osStatus[jj], &dwCommEvent[jj], FALSE))
				{
					if (GetLastError() != ERROR_IO_INCOMPLETE)
					{
						for (ii = 0; ii < waitCount; ii++)
						{
							CloseHandle(waits[ii]);
						}
						EXCEPTION("GetOverlappedResult");
					}
					else
					{
						if (!CancelIo(ctx->hComm))
						{
							for (ii = 0; ii < waitCount; ii++)
							{
								CloseHandle(waits[ii]);
							}
							EXCEPTION("CancelIo");
						}
						CloseHandle(waits[ii]);
					}
				}
				else
				{
					CloseHandle(waits[ii]);
					ctxArr[jj] = 0;
					count++;
				}
			}
			break;
		}
	}
	return count;
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doRead
  (JNIEnv *env, jobject obj, jlong ctx, jobject bb)
{
	int pos;
	int lim;
	char* addr;
	jbyteArray barr = NULL;
	jint len;
	jbyte* arr = NULL;
	jint newPos;

	DWORD timeout = INFINITE;
	OVERLAPPED osReader = {0};
	DWORD dwRead;
	DWORD dwRes;
	BOOL fRes;
	CTX *c = (CTX*)ctx;

	DEBUG("read");
	pos = GETPOSITION(bb);
	lim = GETLIMIT(bb);
	len = lim - pos;
	addr = (*env)->GetDirectBufferAddress(env, bb);
	if (addr == NULL)
	{
		barr = (*env)->NewByteArray(env, len);
		CHECK(barr);
		arr = (*env)->GetByteArrayElements(env, barr, NULL);
		CHECK(arr);
		addr = arr;
	}
	else
	{
		addr += pos;
	}

	// Create the overlapped event. Must be closed before exiting
	// to avoid a handle leak.
	osReader.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

	if (osReader.hEvent == NULL)
	{
		if (barr != NULL)
		{
			(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
		}
		EXCEPTION(NULL);
	}

	DEBUG("ReadFile");
	if (!ReadFile(c->hComm, addr, len, &dwRead, &osReader)) 
	{
		if (GetLastError() != ERROR_IO_PENDING)     // read not delayed?
		{
			if (barr != NULL)
			{
				(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
			}
			EXCEPTION(NULL);
		}
		else
		{
			DEBUG("WaitForSingleObject");
			dwRes = WaitForSingleObject(osReader.hEvent, timeout);
			switch(dwRes)
			{
			  // Read completed.
			  case WAIT_OBJECT_0:
				if (!GetOverlappedResult(c->hComm, &osReader, &dwRead, FALSE))
				{
					 // Error in communications; report it.
					fRes = FALSE;
				}
				else
				{
					 // Read completed successfully.
					fRes = TRUE;
				}
				break;
			  case WAIT_TIMEOUT:
				  fRes = TRUE;
				  dwRead = 0;
				  break;
			default:
				if (barr != NULL)
				{
					(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
				}
				EXCEPTION(NULL);
				break;
			}
		}
	}
	else
	{
		 // Read completed successfully.
		fRes = TRUE;
	}

	DEBUG("CloseHandle");
	CloseHandle(osReader.hEvent);

	if (barr != NULL)
	{
		if (fRes)
		{
			(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
			PUT(bb, barr);
		}
	}
	if (fRes)
	{
		newPos = pos + dwRead;
		SETPOSITION(bb, newPos);
		return dwRead;
	}
	else
	{
		EXCEPTION(NULL);
	}
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doWrite
  (JNIEnv *env, jobject obj, jlong ctx, jobject bb)
{
	static int count = 0;
	int pos;
	int lim;
	char* addr;
	jbyteArray barr = NULL;
	jint len;
	jbyte* arr = NULL;
	jint newPos;

	OVERLAPPED osWrite = { 0 };
	DWORD dwWritten;
	DWORD dwRes;
	BOOL fRes;
	CTX *c = (CTX*)ctx;


	DEBUG("write");
	pos = GETPOSITION(bb);
	lim = GETLIMIT(bb);
	len = lim - pos;
	addr = (*env)->GetDirectBufferAddress(env, bb);
	if (addr == NULL)
	{
		barr = (*env)->NewByteArray(env, len);
		CHECK(barr);
		GET(bb, barr);
		arr = (*env)->GetByteArrayElements(env, barr, NULL);
		CHECK(arr);
		addr = arr;
	}
	else
	{
		addr += pos;
	}

	// Create this write operation's OVERLAPPED structure's hEvent.
	osWrite.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
	if (osWrite.hEvent == NULL)
	{
		// error creating overlapped event handle
		if (barr != NULL)
		{
			(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
		}
		EXCEPTION(NULL);
	}

	// Issue write.
	DEBUG("WriteFile");
	if (!WriteFile(c->hComm, addr, len, &dwWritten, &osWrite))
	{
		if (GetLastError() != ERROR_IO_PENDING) 
		{ 
			// WriteFile failed, but isn't delayed. Report error and abort.
			if (barr != NULL)
			{
				(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
			}
			EXCEPTION(NULL);
		}
		else
		{
			// Write is pending.
			dwRes = WaitForSingleObject(osWrite.hEvent, INFINITE);
			switch(dwRes)
			{
				// OVERLAPPED structure's event has been signaled. 
				case WAIT_OBJECT_0:
				if (!GetOverlappedResult(c->hComm, &osWrite, &dwWritten, FALSE))
				{
					fRes = FALSE;
				}
				else
				{
					// Write operation completed successfully.
					fRes = TRUE;
				}
				break;

				default:
				// An error has occurred in WaitForSingleObject.
				// This usually indicates a problem with the
				// OVERLAPPED structure's event handle.
				fRes = FALSE;
				break;
			}
		}
	}
	else
	{
		// WriteFile completed immediately.
		fRes = TRUE;
	}

	//hexdump(count, addr, dwWritten, len);
	count += dwWritten;

	DEBUG("CloseHandle");
	CloseHandle(osWrite.hEvent);

	if (barr != NULL)
	{
		(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
	}
	if (fRes)
	{
		newPos = pos + dwWritten;
		SETPOSITION(bb, newPos);
		return dwWritten;
	}
	else
	{
		EXCEPTION(NULL);
	}
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_WinSerialChannel_doEnumPorts
  (JNIEnv *env, jobject obj, jobject list)
{
	jstring str;
	char buf[7];
	char target[256];
	int ii;

	for (ii=1;ii<=256;ii++)
	{
		sprintf_s(buf, sizeof(buf), "COM%d", ii);
		if (QueryDosDevice(buf, target, sizeof(target)))
		{
			str = (*env)->NewStringUTF(env, buf);
			CHECKV(str);
			ADDV(list, str);
		}
	}
}

void exception(JNIEnv * env, const char* clazz, const char* message, const char* filename, int lineno)
{
	jclass exc;
	char buf[256];
	LPVOID lpMsgBuf;
	DWORD err;

	err = GetLastError();
	if (err != 0)
	{
		FormatMessage( 
			FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
			NULL,
			err,
			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
			(LPTSTR) &lpMsgBuf,
			0,
			NULL 
		);
	}

	exc = (*env)->FindClass(env, clazz);
	if (exc == NULL)
	{
		return;
	}
	if (err != 0)
	{
		if (message != NULL)
		{
			sprintf_s(buf, sizeof(buf), "%s %d err %d: %s: %s", filename, lineno, err, lpMsgBuf, message);
		}
		else
		{
			sprintf_s(buf, sizeof(buf), "%s %d err %d: %s", filename, lineno, err, lpMsgBuf);
		}
		(*env)->ThrowNew(env, exc, buf);
	}
	else
	{
		(*env)->ThrowNew(env, exc, message);
	}
	// Free the buffer.

	if (err != 0)
	{
		LocalFree( lpMsgBuf );
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

