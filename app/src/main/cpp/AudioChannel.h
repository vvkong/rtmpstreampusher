//
// Created by wangrenxing on 2020-02-14.
//

#ifndef RTMPPUSHSTREAM_AUDIOCHANNEL_H
#define RTMPPUSHSTREAM_AUDIOCHANNEL_H

#include "faac.h"
#include "rtmp.h"

class AudioChannel {
    typedef void (*AudioCallback)(RTMPPacket* &);
public:
    AudioChannel();
    ~AudioChannel();

    void setAudioEncInfo(int sampleInHz, int channels);

    void setAudioCallback(AudioCallback callback);

    void encodeData(uint8_t* data);

    RTMPPacket* getAudioTag();

    unsigned long getInputSamples() const;

private:
    AudioCallback callback = NULL;
    int sampleInHz;
    int channels;
    // 输入样本数 编码器的样本 一次编码的最大样本数
    unsigned long inputSamples;
    // 最大输出字节数
    unsigned long maxOutputBytes;
    faacEncHandle audioCodec = NULL;
    uint8_t* outputBuffer = NULL;
};


#endif //RTMPPUSHSTREAM_AUDIOCHANNEL_H
