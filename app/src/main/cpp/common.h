//
// Created by wangrenxing on 2020-02-12.
//

#ifndef RTMPPUSHSTREAM_COMMON_H
#define RTMPPUSHSTREAM_COMMON_H

#include <android/log.h>

#define DELETE(p) if(p) { delete p; }

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "black-cat", __VA_ARGS__)

void printMsg(char* msg);

#endif //RTMPPUSHSTREAM_COMMON_H
