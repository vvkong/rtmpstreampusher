//
// Created by wangrenxing on 2020-02-14.
//

#include <cstring>
#include "AudioChannel.h"
#include "rtmp/rtmp.h"
#include "common.h"

AudioChannel::AudioChannel() {

}

AudioChannel::~AudioChannel() {
    if( audioCodec ) {
        faacEncClose(audioCodec);
        audioCodec = NULL;
    }
    if( outputBuffer ) {
        delete []outputBuffer;
    }
}

void AudioChannel::setAudioEncInfo(int sampleInHz, int channels) {
    AudioChannel::sampleInHz = sampleInHz;
    AudioChannel::channels = channels;
    // 打开编码器
    audioCodec = faacEncOpen(sampleInHz, channels, &inputSamples, &maxOutputBytes);
    // 设置编码器参数
    faacEncConfigurationPtr configPtr = faacEncGetCurrentConfiguration(audioCodec);

    // mpeg版本
    configPtr->mpegVersion = MPEG4;
    // aac规格
    configPtr->aacObjectType = LOW;
    configPtr->inputFormat = FAAC_INPUT_16BIT;
    //编码输出原始数据
    configPtr->outputFormat = 0;
    faacEncSetConfiguration(audioCodec, configPtr);

    // 输出缓冲区
    outputBuffer = new uint8_t[maxOutputBytes];
}

void AudioChannel::setAudioCallback(AudioCallback callback) {
    AudioChannel::callback = callback;
}

unsigned long AudioChannel::getInputSamples() const {
    return inputSamples;
}

void AudioChannel::encodeData(uint8_t *data) {
    printMsg("encodeData");

    int len = faacEncEncode(audioCodec, reinterpret_cast<int32_t *>(data), inputSamples, outputBuffer, maxOutputBytes);
    RTMPPacket* packet = new RTMPPacket;
    int bodySize = 2 + len;
    RTMPPacket_Alloc(packet, bodySize);

    packet->m_body[0] = channels == 1 ? 0xae : 0xaf;

    // 编码输出声音 都是0x01
    packet->m_body[1] = 0x01;

    memcpy(&packet->m_body[2], outputBuffer, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    if( callback ) {
        callback(packet);
    }
}

RTMPPacket* AudioChannel::getAudioTag() {
    unsigned char * buffer;
    unsigned long size;
    faacEncGetDecoderSpecificInfo(audioCodec, &buffer, &size);

    RTMPPacket* packet = new RTMPPacket;
    int bodySize = 2 + size;
    RTMPPacket_Alloc(packet, bodySize);

    packet->m_body[0] = channels == 1 ? 0xae : 0xaf;

    packet->m_body[1] = 0x00;

    memcpy(&packet->m_body[2], buffer, size);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    return packet;
}