/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_vesalainen_comm_channel_linux_LinuxSerialChannel */

#ifndef _Included_org_vesalainen_comm_channel_linux_LinuxSerialChannel
#define _Included_org_vesalainen_comm_channel_linux_LinuxSerialChannel
#ifdef __cplusplus
extern "C" {
#endif
#undef org_vesalainen_comm_channel_linux_LinuxSerialChannel_VERSION
#define org_vesalainen_comm_channel_linux_LinuxSerialChannel_VERSION 1L
#undef org_vesalainen_comm_channel_linux_LinuxSerialChannel_MaxSelectors
#define org_vesalainen_comm_channel_linux_LinuxSerialChannel_MaxSelectors 64L
/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doClearBuffers
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doClearBuffers
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doConfigure
 * Signature: (JIIIIIZ)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doConfigure
  (JNIEnv *, jobject, jlong, jint, jint, jint, jint, jint, jboolean);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_version
  (JNIEnv *, jobject);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doOpen
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doOpen
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doRead
 * Signature: (JLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doRead
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doWrite
 * Signature: (JLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doWrite
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    setDebug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_setDebug
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doEnumPorts
 * Signature: (Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doEnumPorts
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doClose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doClose
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    staticInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_staticInit
  (JNIEnv *, jclass);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    doSelect
 * Signature: (IILjava/nio/LongBuffer;Ljava/nio/LongBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_doSelect
  (JNIEnv *, jclass, jint, jint, jobject, jobject, jint);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    timeouts
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_timeouts
  (JNIEnv *, jobject, jlong, jint, jint);

/*
 * Class:     org_vesalainen_comm_channel_linux_LinuxSerialChannel
 * Method:    wakeupSelect
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_linux_LinuxSerialChannel_wakeupSelect
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
