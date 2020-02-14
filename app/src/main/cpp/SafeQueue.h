//
// Created by wangrenxing on 2020-02-11.
//

#ifndef RTMPPUSHSTREAM_SAFEQUEUE_H
#define RTMPPUSHSTREAM_SAFEQUEUE_H

#include <queue>
#include <pthread.h>

using namespace std;

template<typename T>
class SafeQueue {
    typedef void (*ReleaseCallback)(T&);
    typedef void (*SyncHandler)(queue<T>&);

private:
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    ReleaseCallback releaseCallback = NULL;
    SyncHandler syncHandler = NULL;
    queue<T> innerQueue;
    int work;
public:
    SafeQueue() {
        pthread_mutex_init(&mutex, NULL);
        pthread_cond_init(&cond, NULL);
    }
    ~SafeQueue() {
        pthread_mutex_destroy(&mutex);
        pthread_cond_destroy(&cond);
    }

    void push(T newValue) {
        pthread_mutex_lock(&mutex);
        if( work ) {
            innerQueue.push(newValue);
            pthread_cond_signal(&cond);
        } else {
            if( releaseCallback ) {
                releaseCallback(newValue);
            }
        }
        pthread_mutex_unlock(&mutex);
    }

    int pop(T& value) {
        int ret = 0;
        pthread_mutex_lock(&mutex);
        while( work && innerQueue.empty() ) {
            pthread_cond_wait(&cond, &mutex);
        }
        if( !innerQueue.empty() ) {
            value = innerQueue.front();
            innerQueue.pop();
            ret = 1;
        }
        pthread_mutex_unlock(&mutex);
        return ret;
    }

    void setWork(int work) {
        pthread_mutex_lock(&mutex);
        SafeQueue::work = work;
        pthread_cond_signal(&cond);
        pthread_mutex_unlock(&mutex);
    }

    int empty() {
        return innerQueue.empty();
    }

    int size() {
        return innerQueue.size();
    }

    void clear() {
        pthread_mutex_lock(&mutex);
        int size = innerQueue.size();
        for( int i=0; i<size; i++ ) {
            T v = innerQueue.front();
            if( releaseCallback ){
                releaseCallback(v);
            }
            innerQueue.pop();
        }
        pthread_mutex_unlock(&mutex);
    }

    void sync() {
        pthread_mutex_lock(&mutex);
        syncHandler(innerQueue);
        pthread_mutex_unlock(&mutex);
    }

    void setReleaseCallback(void (*releaseCallback)(T &)) {
        SafeQueue::releaseCallback = releaseCallback;
    }

    void setSyncHandler(void (*syncHandler)(queue<T> &)) {
        SafeQueue::syncHandler = syncHandler;
    }
};
#endif //RTMPPUSHSTREAM_SAFEQUEUE_H
