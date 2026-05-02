package com.my.music.moblie.soundeffect;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.audiofx.EnvironmentalReverb;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConvolutionProcessor {
    private static final int BLOCK_SIZE = 512;
    private static final int FFT_SIZE = 2048;
    private static final int HOP_SIZE = 512;

    private Context context;
    private float dryGain = 1.0f;
    private float wetGain = 0.0f;
    private boolean enabled = false;

    private EnvironmentalReverb environmentalReverb;
    private float currentMainGain = 10.0f;
    private float currentSendGain = 0.0f;

    // 真实卷积混响相关
    private float[] impulseResponse;
    private float[] inputBuffer;
    private float[] outputBuffer;
    private float[] overlapBuffer;
    private int bufferIndex = 0;
    private boolean useNativeReverb = true;

    public ConvolutionProcessor(Context context) {
        this.context = context;
        initBuffers();
    }

    private void initBuffers() {
        inputBuffer = new float[FFT_SIZE];
        outputBuffer = new float[FFT_SIZE];
        overlapBuffer = new float[FFT_SIZE];
    }

    public void updateConfig(SoundEffectConfig config) {
        this.enabled = config.hasConvolution();
        this.dryGain = config.convolutionMainGain / 10.0f;
        this.wetGain = config.convolutionSendGain / 10.0f;
        this.currentMainGain = config.convolutionMainGain;
        this.currentSendGain = config.convolutionSendGain;

        if (enabled && !config.convolutionFileName.isEmpty()) {
            loadImpulseResponse(config.convolutionFileName);
            useNativeReverb = false;
        } else {
            useNativeReverb = true;
        }
    }

    private void loadImpulseResponse(String fileName) {
        try {
            String fullPath = "medias/filters/" + fileName;
            impulseResponse = WavLoader.loadWavFromAssets(context, fullPath);
            if (impulseResponse == null || impulseResponse.length == 0) {
                useNativeReverb = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            useNativeReverb = true;
        }
    }

    public void setAudioSessionId(int audioSessionId) {
        release();
        
        try {
            if (useNativeReverb || impulseResponse == null) {
                environmentalReverb = new EnvironmentalReverb(0, audioSessionId);
                applyReverbSettings();
                environmentalReverb.setEnabled(enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
            environmentalReverb = null;
        }
    }

    private void applyReverbSettings() {
        if (environmentalReverb == null) return;

        try {
            float roomLevel = mapGainToRoomLevel(currentSendGain);
            environmentalReverb.setRoomLevel((short) roomLevel);
            
            float reverbLevel = mapGainToReverbLevel(currentSendGain);
            environmentalReverb.setReverbLevel((short) reverbLevel);
            
            environmentalReverb.setDecayHFRatio((short) 80);
            environmentalReverb.setDecayTime(2.0f + currentSendGain * 0.2f);
            
            environmentalReverb.setDiffusion((short) 100);
            environmentalReverb.setDensity((short) 100);
            
            environmentalReverb.setEnabled(enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private short mapGainToRoomLevel(float gain) {
        float normalized = Math.max(0, Math.min(1, gain / 50));
        return (short) (-6000 + normalized * 6000);
    }

    private short mapGainToReverbLevel(float gain) {
        float normalized = Math.max(0, Math.min(1, gain / 50));
        return (short) (-960 + normalized * 960);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (environmentalReverb != null) {
            try {
                environmentalReverb.setEnabled(enabled);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void process(float[] samples, int channels) {
        if (!enabled) {
            if (dryGain != 1.0f) {
                for (int i = 0; i < samples.length; i++) {
                    samples[i] *= dryGain;
                }
            }
            return;
        }

        // 使用原生 EnvironmentalReverb
        if (useNativeReverb || environmentalReverb != null) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] *= dryGain;
            }
        } 
        // 使用真实卷积混响
        else if (impulseResponse != null && impulseResponse.length > 0) {
            processWithConvolution(samples, channels);
        }
    }

    private void processWithConvolution(float[] samples, int channels) {
        int numFrames = samples.length / channels;
        
        for (int frame = 0; frame < numFrames; frame++) {
            // 填充输入缓冲区
            inputBuffer[bufferIndex] = samples[frame * channels];
            bufferIndex++;
            
            // 当缓冲区满时进行卷积
            if (bufferIndex >= FFT_SIZE) {
                performOverlapAddConvolution();
                bufferIndex = FFT_SIZE / 2;
                
                // 移动后半部分到前面
                System.arraycopy(inputBuffer, FFT_SIZE / 2, inputBuffer, 0, FFT_SIZE / 2);
            }
            
            // 输出（使用 overlap-add）
            int outputIndex = frame % (FFT_SIZE / 2);
            if (outputIndex < outputBuffer.length) {
                float out = outputBuffer[outputIndex] + (bufferIndex > 0 ? inputBuffer[bufferIndex - 1] : 0);
                samples[frame * channels] = out * dryGain + out * wetGain;
                if (channels > 1) {
                    samples[frame * channels + 1] = out * dryGain + out * wetGain;
                }
            }
        }
    }

    private void performOverlapAddConvolution() {
        // 清空输出缓冲区
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = 0;
        }
        
        // 简单的 overlap-add 卷积
        int irLength = Math.min(impulseResponse.length, FFT_SIZE);
        for (int i = 0; i < FFT_SIZE; i++) {
            if (i < inputBuffer.length) {
                float inputSample = inputBuffer[i];
                for (int j = 0; j < irLength && i + j < FFT_SIZE; j++) {
                    outputBuffer[i + j] += inputSample * impulseResponse[j] * 0.1f;
                }
            }
        }
    }

    public void release() {
        if (environmentalReverb != null) {
            try {
                environmentalReverb.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            environmentalReverb = null;
        }
    }
}
