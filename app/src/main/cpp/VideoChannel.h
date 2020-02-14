//
// Created by wangrenxing on 2020-01-22.
//

#ifndef RTMPPUSHSTREAM_VIDEOCHANNEL_H
#define RTMPPUSHSTREAM_VIDEOCHANNEL_H

#include <rtmp/rtmp.h>
#include "SafeQueue.h"
#include "x264.h"
#include "common.h"

class VideoChannel {
    typedef void (* VideoCallback)(RTMPPacket *&);
private:
    pthread_mutex_t mutex;
    int width;
    int height;
    int fps;
    int bitrate;
    x264_t* videoCodec = NULL;
    x264_picture_t* picIn = NULL;
    int ySize;
    int uvSize;
    int index = 0;
    VideoCallback callback;

    void sendSpsPps(uint8_t* sps, uint8_t* pps, int spsLen, int ppsLen);
    void sendFrame(int type, int payloadLen, uint8_t* pPayload);

public:
    VideoChannel();
    virtual ~VideoChannel();

    void setVideoEncInfo(int w, int h, int fps, int bitrate);
    void encodeData(int8_t* data);
    void setVideoCallback(VideoCallback cb);
};
#endif //RTMPPUSHSTREAM_VIDEOCHANNEL_H
