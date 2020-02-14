#include <jni.h>
#include <string>
#include "rtmp.h"

#include "x264.h"
#include "SafeQueue.h"
#include <pthread.h>
#include "VideoChannel.h"
#include "common.h"
#include "AudioChannel.h"

static pthread_t pid;
static SafeQueue<RTMPPacket*> packets;
static VideoChannel* videoChannel = NULL;
static AudioChannel* audioChannel = NULL;
static int readyPushing = 0;
uint32_t startTime = 0;

/**
 * 释放RTMPPacket
 * @param packet
 */
void releasePackets(RTMPPacket* &packet) {
    if( packet ) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = NULL;
    }
}

void callback(RTMPPacket* &packet) {
    if( packet ) {
        packet->m_nTimeStamp = RTMP_GetTime() - startTime;
        packets.push(packet);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_nativeInit(JNIEnv *env, jobject thiz) {
    videoChannel = new VideoChannel();
    videoChannel->setVideoCallback(callback);
    //准备一个队列,打包好的数据 放入队列，在线程中统一的取出数据再发送给服务器
    packets.setReleaseCallback(releasePackets);

    audioChannel = new AudioChannel();
    audioChannel->setAudioCallback(callback);
}


static void* rtmpStart(void* args) {
    char* url = static_cast<char *>(args);
    RTMP* rtmp = NULL;
    do {
        rtmp = RTMP_Alloc();
        if( !rtmp ) {
            printMsg("rtmp创建失败");
            break;
        }
        RTMP_Init(rtmp);
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp, url);
        if( !ret ) {
            printMsg("rtmp设置地址失败");
            break;
        }
        // 输出模式
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, NULL);
        if( !ret ) {
            printMsg("rtmp连接地址失败");
            break;
        }
        ret = RTMP_ConnectStream(rtmp, NULL);
        if( !ret ) {
            printMsg("rtmp连接流失败");
            break;
        }
        // 开始推流
        readyPushing = 1;
        startTime = RTMP_GetTime();
        packets.setWork(1);
        RTMPPacket* packet = NULL;
        printMsg("开始推流啦.....");
        if( audioChannel && readyPushing ) {
            RTMPPacket* pkt = audioChannel->getAudioTag();
            callback(pkt);
        }
        while( readyPushing ) {
            packets.pop(packet);
            if (!readyPushing) {
                break;
            }
            if (!packet) {
                continue;
            }
            // rtmp流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                printMsg("发送数据失败");
                break;
            }
        }
        releasePackets(packet);
    }while (0);
    if( rtmp ) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete(url);
    return NULL;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_nativeStart(JNIEnv *env, jobject thiz,
                                                          jstring live_url) {
    if( readyPushing ) {
        return;
    }
    jboolean isCopy = JNI_FALSE;
    const char* url = env->GetStringUTFChars(live_url, &isCopy);
    char* urlBak = new char[strlen(url)];
    strcpy(urlBak, url);
    readyPushing = true;
    pthread_create(&pid, NULL, rtmpStart, urlBak);
    env->ReleaseStringUTFChars(live_url, url);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_nativePushVideo(JNIEnv *env, jobject thiz,
                                                              jbyteArray data) {

    if( !videoChannel || !readyPushing ) {
        return;
    }
    jbyte* _data = env->GetByteArrayElements(data, NULL);
    videoChannel->encodeData(_data);
    env->ReleaseByteArrayElements(data, _data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_nativeStop(JNIEnv *env, jobject thiz) {
    readyPushing = 0;
    packets.setWork(0);
    pthread_join(pid, NULL);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_nativeSetVideoEncInfo(JNIEnv *env, jobject thiz,
                                                                    jint width, jint height,
                                                                    jint fps, jint bitrate) {
    if( videoChannel ) {
        videoChannel->setVideoEncInfo(width, height, fps, bitrate);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_nativeSetAudioEncInfo(JNIEnv *env, jobject thiz,
                                                                    jint sample_in_hz,
                                                                    jint channels) {
    if( audioChannel ) {
        audioChannel->setAudioEncInfo(sample_in_hz, channels);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_realease(JNIEnv *env, jobject thiz) {
    if( videoChannel ) {
        delete(videoChannel);
        videoChannel = NULL;
    }
    if( audioChannel ) {
        delete(audioChannel);
        audioChannel = NULL;
    }

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_getInputSample(JNIEnv *env, jobject thiz) {
    if( audioChannel ) {
        return audioChannel->getInputSamples();
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_godot_rtmppushstream_live_LivePusher_nativePushAudio(JNIEnv *env, jobject thiz,
                                                              jbyteArray data, jint len) {
    if( !audioChannel || !readyPushing ) {
        return;
    }
    jbyte* _data = new jbyte[len];
    env->GetByteArrayRegion(data, 0, len, _data);
    audioChannel->encodeData(reinterpret_cast<uint8_t *>(_data));
    delete _data;
}

