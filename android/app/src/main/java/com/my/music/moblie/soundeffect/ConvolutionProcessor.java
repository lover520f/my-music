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

    private Context context;
    private float dryGain = 1.0f;
    private float wetGain = 0.0f;
    private boolean enabled = false;

    private EnvironmentalReverb environmentalReverb;
    private float currentMainGain = 10.0f;
    private float currentSendGain = 0.0f;

    public ConvolutionProcessor(Context context) {
        this.context = context;
    }

    public void updateConfig(SoundEffectConfig config) {
        this.enabled = config.hasConvolution();
        this.dryGain = config.convolutionMainGain / 10.0f;
        this.wetGain = config.convolutionSendGain / 10.0f;
        this.currentMainGain = config.convolutionMainGain;
        this.currentSendGain = config.convolutionSendGain;
    }

    public void setAudioSessionId(int audioSessionId) {
        release();
        
        try {
            environmentalReverb = new EnvironmentalReverb(0, audioSessionId);
            
            applyReverbSettings();
            environmentalReverb.setEnabled(enabled);
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

        for (int i = 0; i < samples.length; i++) {
            samples[i] *= dryGain;
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
