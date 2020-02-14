package com.godot.rtmppushstream.live.channel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.godot.rtmppushstream.live.LivePusher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-01-22
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
public class AudioChannel {
    private int channels = 2;
    private int sampleRateInHz = 44100;
    private AudioRecord audioRecord;
    private ExecutorService executorService;
    private boolean isStart;
    private LivePusher livePusher;
    private int inputSample;
    public AudioChannel(LivePusher livePusher) {
        this.livePusher = livePusher;
        int channelConfig = channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        livePusher.nativeSetAudioEncInfo(sampleRateInHz, channels);
        inputSample = livePusher.getInputSample();
        int inputSampleSize = inputSample * channels * 2;
        if( bufferSizeInBytes > inputSampleSize) {
            bufferSizeInBytes = inputSampleSize;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        executorService = Executors.newFixedThreadPool(1);
    }

    public void startLive() {
        isStart = true;
        executorService.submit(new AudioTask());
    }

    public void stopLive() {
        isStart = false;
    }

    public void release() {
        audioRecord.release();
    }

    private class AudioTask implements Runnable {

        @Override
        public void run() {
            audioRecord.startRecording();
            byte[] bytes = new byte[inputSample*2*channels];
            while ( isStart ) {
                int len = audioRecord.read(bytes, 0, bytes.length);
                Log.d("black-cat", "audioRecord.read: " + len);
                if( len > 0 ) {
                    livePusher.nativePushAudio(bytes, len);
                }
            }
            audioRecord.stop();
        }
    }
}
