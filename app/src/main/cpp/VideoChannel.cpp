//
// Created by wangrenxing on 2020-01-22.
//

#include <include/x264/x264.h>
#include "VideoChannel.h"

VideoChannel::VideoChannel() {
    pthread_mutex_init(&mutex, NULL);
}

VideoChannel::~VideoChannel() {
    pthread_mutex_destroy(&mutex);
    if( videoCodec ) {
        x264_encoder_close(videoCodec);
        videoCodec = NULL;
    }
    if( picIn ) {
        x264_picture_clean(picIn);
        delete(picIn);
        picIn = NULL;
    }
}

void VideoChannel::setVideoCallback(VideoChannel::VideoCallback cb) {
    pthread_mutex_lock(&mutex);
    callback = cb;
    pthread_mutex_unlock(&mutex);
}

void VideoChannel::setVideoEncInfo(int w, int h, int fps, int bitrate) {
    pthread_mutex_lock(&mutex);

    VideoChannel::width = w;
    VideoChannel::height = h;
    VideoChannel::fps = fps;
    VideoChannel::bitrate = bitrate;

    ySize = width * height;
    uvSize = ySize / 4;

    if( videoCodec ) {
        x264_encoder_close(videoCodec);
        videoCodec = NULL;
    }
    if( picIn ) {
        x264_picture_clean(picIn);
        delete(picIn);
        picIn = NULL;
    }

    // 打开x264编码器
    x264_param_t param;
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
    // 编码规格
    param.i_level_idc = 32;
    // 输入数据格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    // 无b帧
    param.i_bframe = 0;
    // 码率控制 CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    param.rc.i_rc_method = X264_RC_ABR;
    param.rc.i_bitrate = bitrate / 1000;
    // 瞬时最大码率
    param.rc.i_vbv_max_bitrate = bitrate / 1000 * 1.2;
    // 设置了i_vbv_max_bitrate必须设置此参数，码率控制区大小,单位kbps
    param.rc.i_vbv_buffer_size = bitrate / 1000;

    // 帧率
    param.i_fps_num = fps;
    param.i_fps_den = 1;
    param.i_timebase_num = param.i_fps_den;
    param.i_timebase_den = param.i_fps_num;
    // 用fps而不是时间戳来计算帧间距离
    param.b_vfr_input = 0;
    //帧距离(关键帧)  2s一个关键帧
    param.i_keyint_max = fps * 2;
    // 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
    param.b_repeat_headers = 1;
    param.i_threads = 1;

    x264_param_apply_profile(&param, "baseline");
    // 打开编码器
    videoCodec = x264_encoder_open(&param);
    picIn = new x264_picture_t;
    x264_picture_alloc(picIn, X264_CSP_I420, width, height);

    pthread_mutex_unlock(&mutex);
}

void VideoChannel::encodeData(int8_t *data) {
    printMsg("encodeData");
    pthread_mutex_lock(&mutex);
    memcpy(picIn->img.plane[0], data, ySize);
    for( int i=0; i<uvSize; ++i ) {
        // u
        *(picIn->img.plane[1]+i) = *(data+ySize+i*2+1);
        // v
        *(picIn->img.plane[2]+i) = *(data+ySize+i*2+2);
    }
    picIn->i_pts = index++;
    // 编码后的数据
    x264_nal_t* ppNal;
    // nalu数量
    int piNal;
    x264_picture_t picOut;
    // 编码
    int ret = x264_encoder_encode(videoCodec, &ppNal, &piNal, picIn, &picOut);
    if( ret < 0 ) {
        printMsg("编码失败");
        pthread_mutex_unlock(&mutex);
        return;
    }
    int spsLen, ppsLen;
    uint8_t sps[100];
    uint8_t pps[100];
    for( int i=0; i<piNal; ++i ) {
        if( ppNal[i].i_type == NAL_SPS ) {
            // 序列参数集
            // 丢掉00 00 00 01
            spsLen = ppNal[i].i_payload - 4;
            memcpy(sps, ppNal[i].p_payload+4, spsLen);
        } else if( ppNal[i].i_type == NAL_PPS ) {
            // 图像参数集
            ppsLen = ppNal[i].i_payload - 4;
            memcpy(pps, ppNal[i].p_payload+4, ppsLen);
            sendSpsPps(sps, pps, spsLen, ppsLen);
        } else {
            // 关键帧、非关键帧
            sendFrame(ppNal[i].i_type, ppNal[i].i_payload, ppNal[i].p_payload);
        }
    }
    pthread_mutex_unlock(&mutex);
}

void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int spsLen, int ppsLen) {
    printMsg("sendSpsPps");
    RTMPPacket* packet = new RTMPPacket;
    int bodySize = 13 + spsLen + 3 + ppsLen;
    RTMPPacket_Alloc(packet, bodySize);
    int i = 0;
    // 固定头
    packet->m_body[i++] = 0x17;
    // 类型
    packet->m_body[i++] = 0x00;
    // composition time 0x000000
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    // 版本
    packet->m_body[i++] = 0x01;
    // 编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xff;

    // 整个sps
    packet->m_body[i++] = 0xe1;
    // sps长度
    packet->m_body[i++] = (spsLen>>8) & 0xff;
    packet->m_body[i++] = (spsLen) & 0xff;
    // sps数据
    memcpy(&packet->m_body[i], sps, spsLen);
    i += spsLen;

    // pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (ppsLen >> 8) & 0xff;
    packet->m_body[i++] = (ppsLen) & 0xff;
    memcpy(&packet->m_body[i], pps, ppsLen);

    // 视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    // 随意分配一个管道(尽量避开rtmp.c中使用的)
    packet->m_nChannel = 0x10;
    // sps pps没有时间戳
    packet->m_nTimeStamp = 0;
    // 不是有绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    callback(packet);
}

void VideoChannel::sendFrame(int type, int payloadLen, uint8_t *pPayload) {
    printMsg("sendFrame");
    // 去掉00 00 00 01 / 00 00 01
    if( pPayload[2] == 0x0 ) {
        payloadLen -= 4;
        pPayload += 4;
    } else if( pPayload[2] == 0x01 ) {
        payloadLen -= 3;
        pPayload += 3;
    }
    RTMPPacket* packet = new RTMPPacket;

    int bodySize = 9 + payloadLen;
    RTMPPacket_Alloc(packet, bodySize);
    RTMPPacket_Reset(packet);

    packet->m_body[0] = 0x27;
    // 关键帧
    if( type == NAL_SLICE_IDR ) {
        printMsg("关键帧");
        packet->m_body[0] = 0x17;
    }
    // 类型
    packet->m_body[1] = 0x01;
    // 时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    // 数据长度 4 字节
    packet->m_body[5] = (payloadLen>>24)&0xff;
    packet->m_body[6] = (payloadLen>>16)&0xff;
    packet->m_body[7] = (payloadLen>>8)&0xff;
    packet->m_body[8] = (payloadLen)&0xff;

    // 图片数据
    memcpy(&packet->m_body[9], pPayload, payloadLen);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callback(packet);
}