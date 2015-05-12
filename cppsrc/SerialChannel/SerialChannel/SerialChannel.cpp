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
  (JNIEnv *env, jobject obj, jlong ctx)
{
	DWORD dwModemStat;
	OVERLAPPED osStatus = {0};
	CTX *c = (CTX*)ctx;

	DEBUG("GetCommModemStatus");
	if (GetCommModemStatus(c->hComm, &dwModemStat))
	{
		if ((dwModemStat & MS_RLSD_ON) == 0)
		{
			return JNI_FALSE;
		}
		else
		{
			return JNI_TRUE;
		}
	}
	else
	{
		CloseHandle(osStatus.hEvent);
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN;
	}
}
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
	jint flow
	)
{
	char szPort[32];
	char buf[256];
	jsize size;
	jbyte* sPort; 
	char* err;
	CTX *c = NULL;

	c = (CTX*)calloc(1, sizeof(CTX));

	DEBUG("initialize");
	sPort = (*env)->GetByteArrayElements(env, port, NULL);
	if (sPort == NULL)
	{
		ERRORRETURN
	}
	size = (*env)->GetArrayLength(env, port);
	FillMemory(szPort, sizeof(szPort), 0);
	if (strncpy_s(szPort, sizeof(szPort), sPort, size))
	{
		exception(env, "java/io/IOException", "copy com port");
		ERRORRETURN
	}
	if (sprintf_s(buf, sizeof(buf), "\\\\.\\%s", szPort) == -1)
	{
		exception(env, "java/io/IOException", "create port name");
		ERRORRETURN
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
		exception(env, "java/io/IOException", buf);
		(*env)->ReleaseByteArrayElements(env, port, sPort, 0);
		ERRORRETURN
	}
	(*env)->ReleaseByteArrayElements(env, port, sPort, 0);

	err = configure(
		env, 
		c->hComm, 
		bauds, 
		parity, 
		databits, 
		stopbits, 
		flow
		);
	if (err != NULL)
	  // Error in SetCommState. Possibly a problem with the communications 
	  // port handle or a problem with the DCB structure itself.
	{
		CloseHandle(c->hComm);
		free(c);
		exception(env, "java/io/IOException", err);
		ERRORRETURNV
	}
	return (jlong)c;
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_timeouts(
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
		exception(env, "java/io/IOException", "SetCommTimeouts failed");
		ERRORRETURNV
	}

}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doClose
  (JNIEnv *env, jobject obj, jlong ctx)
{
	CTX *c = (CTX*)ctx;

	DEBUG("PurgeComm");
	PurgeComm(c->hComm, PURGE_RXABORT | PURGE_RXCLEAR | PURGE_TXABORT | PURGE_TXCLEAR);

	DEBUG("CloseHandle");
	if (!CloseHandle(c->hComm))
	{
		free(c);
		exception(env, "java/io/IOException", "CloseHandle failed");
		ERRORRETURNV
	}
	free(c);
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doFlush
  (JNIEnv *env, jobject obj, jlong ctx)
{
	CTX *c = (CTX*)ctx;

	DEBUG("FlushFileBuffers");
	if (!FlushFileBuffers(c->hComm))
	{
		exception(env, "java/io/IOException", "FlushFileBuffers failed");
		ERRORRETURNV
	}
}
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_commStatus
  (JNIEnv *env, jobject obj, jlong ctx)
{
	DWORD dwModemStat;
	CTX *c = (CTX*)ctx;

	DEBUG("GetCommModemStatus");
	if (GetCommModemStatus(c->hComm, &dwModemStat))
	{
		return dwModemStat;
	}
	else
	{
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
	}
}
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_setEventMask
  (JNIEnv *env, jobject obj, jlong ctx, jint mask)
{
	CTX *c = (CTX*)ctx;

	DEBUG("SetCommMask");
	if (!SetCommMask(c->hComm, mask))
	{
		exception(env, "java/io/IOException", NULL);
		ERRORRETURNV
	}
}

JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_waitEvent
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
			exception(env, "java/io/IOException", NULL);
			ERRORRETURNV
		}
	}
	osStatus.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

	if (osStatus.hEvent == NULL)
	{
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
	}
	DEBUG("WaitCommEvent");
	if (!WaitCommEvent(c->hComm, &dwCommEvent, &osStatus))
	{
		if (GetLastError() != ERROR_IO_PENDING)
		{
			CloseHandle(osStatus.hEvent);
			exception(env, "java/io/IOException", NULL);
			ERRORRETURN
		}
	DEBUG("WaitForSingleObject");
		dwRes = WaitForSingleObject(osStatus.hEvent, INFINITE);
		switch(dwRes)
		{
		  case WAIT_OBJECT_0:
			if (!GetOverlappedResult(c->hComm, &osStatus, &dwCommEvent, FALSE))
			{
				CloseHandle(osStatus.hEvent);
				exception(env, "java/io/IOException", NULL);
				ERRORRETURN
			}

			break;
		  default:
			CloseHandle(osStatus.hEvent);
			exception(env, "java/io/IOException", NULL);
			ERRORRETURN
			break;
		}
	}
	CloseHandle(osStatus.hEvent);
	return dwCommEvent;
}
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doSelect
(JNIEnv *env, jobject obj, jlongArray ctxs, jintArray masks, jint timeout)
{
	jsize len;
	jlong *ctxArr;
	jint *pMask;
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
	len = (*env)->GetArrayLength(env, ctxs);
	if (len > MAXIMUM_WAIT_OBJECTS)
	{
		exception(env, "java/io/IOException", "too many channels");
		ERRORRETURN
	}
	ctxArr = (*env)->GetLongArrayElements(env, ctxs, NULL);
	pMask = (*env)->GetIntArrayElements(env, masks, NULL);
	for (ii = 0; ii < len; ii++)
	{
		int mask = pMask[ii];
		pMask[ii] = 0;
		CTX *ctx = (CTX*)ctxArr[ii];
		if ((mask & EV_RXCHAR) != 0)
		{
			DEBUG("SetCommMask");
			if (!SetCommMask(ctx->hComm, EV_RXCHAR))
			{
				(*env)->ReleaseLongArrayElements(env, ctxs, ctxArr, 0);
				(*env)->ReleaseIntArrayElements(env, masks, pMask, 0);
				exception(env, "java/io/IOException", NULL);
				ERRORRETURNV
			}
			osStatus[ii].hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

			if (osStatus[ii].hEvent == NULL)
			{
				(*env)->ReleaseLongArrayElements(env, ctxs, ctxArr, 0);
				(*env)->ReleaseIntArrayElements(env, masks, pMask, 0);
				exception(env, "java/io/IOException", NULL);
				ERRORRETURN
			}
			DEBUG("WaitCommEvent");
			if (!WaitCommEvent(ctx->hComm, dwCommEvent + ii, osStatus + ii))
			{
				if (GetLastError() != ERROR_IO_PENDING)
				{
					(*env)->ReleaseLongArrayElements(env, ctxs, ctxArr, 0);
					(*env)->ReleaseIntArrayElements(env, masks, pMask, 0);
					CloseHandle(osStatus[ii].hEvent);
					exception(env, "java/io/IOException", NULL);
					ERRORRETURN
				}
				index[waitCount] = ii;
				waits[waitCount++] = osStatus[ii].hEvent;
			}
			else
			{
				CloseHandle(osStatus[ii].hEvent);
				pMask[ii] |= EV_RXCHAR;
				count++;
			}
		}
	}
	if (waitCount == len)
	{
		timeout = INFINITE;
	}
	dwRes = WaitForMultipleObjects(waitCount, waits, FALSE, to);
	switch (dwRes)
	{
	case WAIT_TIMEOUT:
		break;
	case WAIT_FAILED:
		(*env)->ReleaseLongArrayElements(env, ctxs, ctxArr, 0);
		(*env)->ReleaseIntArrayElements(env, masks, pMask, 0);
		for (ii = 0; ii < waitCount; ii++)
		{
			CloseHandle(waits[ii]);
		}
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
		break;
	default:

		for (ii = dwRes - WAIT_OBJECT_0; ii < waitCount; ii++)
		{
			int jj = index[ii];
			CTX *ctx = (CTX*)ctxArr[jj];
			if (!GetOverlappedResult(ctx->hComm, osStatus + jj, dwCommEvent+jj, FALSE))
			{
				if (GetLastError() != ERROR_IO_INCOMPLETE)
				{
					(*env)->ReleaseLongArrayElements(env, ctxs, ctxArr, 0);
					(*env)->ReleaseIntArrayElements(env, masks, pMask, 0);
					for (ii = 0; ii < waitCount; ii++)
					{
						CloseHandle(waits[ii]);
					}
					exception(env, "java/io/IOException", NULL);
					ERRORRETURN
				}
			}
			else
			{
				CloseHandle(waits[ii]);
				pMask[jj] |= EV_RXCHAR;
				count++;
			}
		}
		break;
	}
	(*env)->ReleaseLongArrayElements(env, ctxs, ctxArr, 0);
	(*env)->ReleaseIntArrayElements(env, masks, pMask, 0);
	return count;
}


JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doGetError
  (JNIEnv *env, jobject obj, jlong ctx, jobject commStat)
{
	DWORD errors;
	COMSTAT comstat;
	CTX *c = (CTX*)ctx;

	if (!ClearCommError(c->hComm, &errors, &comstat))
	{
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
	}
	return errors;
}

/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    doRead
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doRead
  (JNIEnv *env, jobject obj, jlong ctx, jobject bb)
{
	int pos;
	int lim;
	char* addr;
	jclass cls;
	jmethodID pmid;
	jmethodID lmid;
	jmethodID putmid;
	jmethodID spmid;
	jbyteArray barr = NULL;
	jint len;
	jbyte* arr;
	jint newPos;

	DWORD timeout = INFINITE;
	OVERLAPPED osReader = {0};
	DWORD dwRead;
	DWORD dwRes;
	BOOL fRes;
	CTX *c = (CTX*)ctx;

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

	// Create the overlapped event. Must be closed before exiting
	// to avoid a handle leak.
	osReader.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

	if (osReader.hEvent == NULL)
	{
		if (barr != NULL)
		{
			(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
			(*env)->DeleteLocalRef(env, barr);
		}
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
	}

	DEBUG("ReadFile");
	if (!ReadFile(c->hComm, addr, len, &dwRead, &osReader)) 
	{
		if (GetLastError() != ERROR_IO_PENDING)     // read not delayed?
		{
			if (barr != NULL)
			{
				(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
				(*env)->DeleteLocalRef(env, barr);
			}
			exception(env, "java/io/IOException", NULL);
			ERRORRETURN
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
					(*env)->DeleteLocalRef(env, barr);
				}
				exception(env, "java/io/IOException", NULL);
				ERRORRETURN
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
		}
		(*env)->DeleteLocalRef(env, barr);
	}
	if (fRes)
	{
		spmid = (*env)->GetMethodID(env, cls, "position", "(I)Ljava/nio/Buffer;");
		if (spmid == NULL)
		{
			ERRORRETURN
		}
		newPos = pos + dwRead;
		(*env)->CallObjectMethod(env, bb, spmid, newPos);
		if ((*env)->ExceptionCheck(env))
		{
			ERRORRETURN
		}
		return dwRead;
	}
	else
	{
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
	}
}

/*
 * Class:     fi_sw_0005fnets_comm_channel_SerialChannel
 * Method:    doWrite
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doWrite
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

	OVERLAPPED osWrite = { 0 };
	DWORD dwWritten;
	DWORD dwRes;
	BOOL fRes;
	CTX *c = (CTX*)ctx;


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

	// Create this write operation's OVERLAPPED structure's hEvent.
	osWrite.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
	if (osWrite.hEvent == NULL)
	{
		// error creating overlapped event handle
		if (barr != NULL)
		{
			(*env)->ReleaseByteArrayElements(env, barr, arr, 0);
			(*env)->DeleteLocalRef(env, barr);
		}
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
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
				(*env)->DeleteLocalRef(env, barr);
			}
			exception(env, "java/io/IOException", NULL);
			ERRORRETURN
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
		(*env)->DeleteLocalRef(env, barr);
	}
	if (fRes)
	{
		spmid = (*env)->GetMethodID(env, cls, "position", "(I)Ljava/nio/Buffer;");
		if (spmid == NULL)
		{
			ERRORRETURN
		}
		newPos = pos + dwWritten;
		(*env)->CallObjectMethod(env, bb, spmid, newPos);
		if ((*env)->ExceptionCheck(env))
		{
			ERRORRETURN
		}
		return dwWritten;
	}
	else
	{
		exception(env, "java/io/IOException", NULL);
		ERRORRETURN
	}
}

JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doEnumPorts
  (JNIEnv *env, jobject obj, jobject list)
{
	jclass cls;
	jstring str;
	jmethodID addid;
	char buf[7];
	char target[256];
	int ii;

	cls = (*env)->GetObjectClass(env, list);
	addid = (*env)->GetMethodID(env, cls, "add", "(Ljava/lang/Object;)Z");
	if (addid == NULL)
	{
		ERRORRETURNV;
	}

	for (ii=1;ii<=256;ii++)
	{
		sprintf_s(buf, sizeof(buf), "COM%d", ii);
		if (QueryDosDevice(buf, target, sizeof(target)))
		{
			str = (*env)->NewStringUTF(env, buf);
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

void exception(JNIEnv * env, const char* clazz, const char* message)
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
			sprintf_s(buf, sizeof(buf), "%s: %s", lpMsgBuf, message);
		}
		else
		{
			sprintf_s(buf, sizeof(buf), "%s", lpMsgBuf);
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

char* configure(
	JNIEnv *env, 
	HANDLE hComm, 
	int bauds, 
	int parity, 
	int databits, 
	int stopbits, 
	int flow
	)
{
	DCB dcb;
	int framesize = 1;	// start bit

	if (!GetCommState(hComm, &dcb))
	{
		return "GetCommState failed";
	}

	dcb.fBinary = TRUE;
	dcb.fDtrControl = DTR_CONTROL_ENABLE;
	dcb.fDsrSensitivity = FALSE;
	dcb.fTXContinueOnXoff = FALSE;
	dcb.fOutX = FALSE;
	dcb.fInX = FALSE;
	dcb.fErrorChar = FALSE;
	dcb.fNull = FALSE;
	dcb.fRtsControl = RTS_CONTROL_ENABLE;
	dcb.fAbortOnError = FALSE;
	dcb.fOutxCtsFlow = FALSE;
	dcb.fOutxDsrFlow = FALSE;

	dcb.BaudRate = bauds;

	switch (parity)
	{
	case 0:	// NONE
		dcb.Parity = NOPARITY;
			break;
	case 1:	// ODD
		dcb.Parity = ODDPARITY;
			break;
	case 2:	// EVEN
		dcb.Parity = EVENPARITY;
			break;
	case 3:	// MARK
		dcb.Parity = MARKPARITY;
			break;
	case 4:	// SPACE
		dcb.Parity = SPACEPARITY;
			break;
	default:
		return "illegal parity value";
		break;
	}
	if (dcb.Parity != NOPARITY)
	{
		framesize++;
	}
	switch (databits)
	{
	case 0:	// 4
		dcb.ByteSize = 4;
		break;
	case 1:	// 5
		dcb.ByteSize = 5;
		break;
	case 2:	// 6
		dcb.ByteSize = 6;
		break;
	case 3:	// 7
		dcb.ByteSize = 7;
		break;
	case 4:	// 8
		dcb.ByteSize = 8;
		break;
	default:
		return "illegal databits value";
		break;
	}
	framesize += dcb.ByteSize;
	switch (stopbits)
	{
	case 0:	// 1
		dcb.StopBits = ONESTOPBIT;
		framesize += 1;
		break;
	case 1:	// 1.5
		dcb.StopBits = ONE5STOPBITS;
		framesize += 2;
		break;
	case 2:	// 2
		dcb.StopBits = TWOSTOPBITS;
		framesize += 2;
		break;
	default:
		return "illegal stopbits value";
		break;
	}
	switch (flow)
	{
	case 0:	// NONE
			break;
	case 1:	// XONXOFF
		dcb.fInX = TRUE;
		dcb.fOutX = TRUE;
			break;
	case 2:	// RTSCTS
	    dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
	    dcb.fOutxCtsFlow = TRUE;
			break;
	case 3:	// DSRDTR
	    dcb.fDtrControl = DTR_CONTROL_HANDSHAKE;
	    dcb.fOutxDsrFlow = TRUE;
		break;
	default:
		return "illegal flow control value";
		break;
	}

	if (!SetCommState(hComm, &dcb))
	{
		return "SetCommState failed";
	}
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

