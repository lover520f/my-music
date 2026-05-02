package com.my.music.moblie.soundeffect;

public class SoundEffectConfig {
    // 均衡器配置
    public boolean equalizerEnabled = false;
    public float[] equalizerGains = new float[10];

    // 混响配置
    public String convolutionFileName = "";
    public String convolutionAssetUri = "";
    public float convolutionMainGain = 10.0f;
    public float convolutionSendGain = 0.0f;

    // 3D环绕配置
    public boolean pannerEnabled = false;
    public float pannerSoundR = 5.0f;
    public float pannerSpeed = 25.0f;

    // 变调配置
    public float pitchPlaybackRate = 1.0f;

    public boolean hasEqualizer() {
        if (!equalizerEnabled) return false;
        for (float gain : equalizerGains) {
            if (Math.abs(gain) >= 0.01f) {
                return true;
            }
        }
        return false;
    }

    public boolean hasConvolution() {
        return !convolutionFileName.isEmpty();
    }

    public boolean hasPanner() {
        return pannerEnabled;
    }

    public boolean hasPitchShift() {
        return Math.abs(pitchPlaybackRate - 1.0f) >= 0.01f;
    }

    public boolean isActive() {
        return hasEqualizer() || hasConvolution() || hasPanner() || hasPitchShift();
    }
}
