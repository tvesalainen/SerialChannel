/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_vesalainen_comm_channel_LinuxSerialChannel */

#ifndef _Included_org_vesalainen_comm_channel_LinuxSerialChannel
#define _Included_org_vesalainen_comm_channel_LinuxSerialChannel
#ifdef __cplusplus
extern "C" {
#endif
#undef org_vesalainen_comm_channel_LinuxSerialChannel_MaxSelectors
#define org_vesalainen_comm_channel_LinuxSerialChannel_MaxSelectors 64L
#undef org_vesalainen_comm_channel_LinuxSerialChannel_VERSION
#define org_vesalainen_comm_channel_LinuxSerialChannel_VERSION 4L
#undef org_vesalainen_comm_channel_LinuxSerialChannel_MaxBuffers
#define org_vesalainen_comm_channel_LinuxSerialChannel_MaxBuffers 16L
/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doClearBuffers
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doClearBuffers
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doConfigure
 * Signature: (JIIIIIZZBBBBB)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doConfigure
  (JNIEnv *, jobject, jlong, jint, jint, jint, jint, jint, jboolean, jboolean, jbyte, jbyte, jbyte, jbyte, jbyte);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_version
  (JNIEnv *, jobject);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doOpen
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doOpen
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doRead
 * Signature: (J[Ljava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doRead
  (JNIEnv *, jobject, jlong, jobjectArray, jint, jint);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doWrite
 * Signature: (J[Ljava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doWrite
  (JNIEnv *, jobject, jlong, jobjectArray, jint, jint);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    setDebug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_setDebug
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doEnumPorts
 * Signature: (Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doEnumPorts
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doClose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doClose
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_free
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    staticInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_staticInit
  (JNIEnv *, jclass);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doSelect
 * Signature: (IILjava/nio/LongBuffer;Ljava/nio/LongBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doSelect
  (JNIEnv *, jclass, jint, jint, jobject, jobject, jint);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    doBlocking
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_doBlocking
  (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     org_vesalainen_comm_channel_LinuxSerialChannel
 * Method:    wakeupSelect
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_vesalainen_comm_channel_LinuxSerialChannel_wakeupSelect
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
