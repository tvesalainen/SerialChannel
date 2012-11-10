/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_vesalainen_comm_channel_winx_WinSerialChannel */

#ifndef _Included_org_vesalainen_comm_channel_winx_WinSerialChannel
#define _Included_org_vesalainen_comm_channel_winx_WinSerialChannel
#ifdef __cplusplus
extern "C" {
#endif
#undef org_vesalainen_comm_channel_winx_WinSerialChannel_VERSION
#define org_vesalainen_comm_channel_winx_WinSerialChannel_VERSION 4L
/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    initialize
 * Signature: ([BIIIII)J
 */
JNIEXPORT jlong JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_initialize
  (JNIEnv *, jobject, jbyteArray, jint, jint, jint, jint, jint);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_version
  (JNIEnv *, jobject);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    setEventMask
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_setEventMask
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    waitEvent
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_waitEvent
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    doGetError
 * Signature: (JLorg/vesalainen/comm/channel/winx/WinCommStat;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doGetError
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    setDebug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_setDebug
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    connected
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_connected
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    doWaitOnline
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doWaitOnline
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    doEnumPorts
 * Signature: (Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doEnumPorts
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    commStatus
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_commStatus
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    doRead
 * Signature: (JLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doRead
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    doClose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doClose
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    doFlush
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doFlush
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_winx_WinSerialChannel
 * Method:    doWrite
 * Signature: (JLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_winx_WinSerialChannel_doWrite
  (JNIEnv *, jobject, jlong, jobject);

#ifdef __cplusplus
}
#endif
#endif
