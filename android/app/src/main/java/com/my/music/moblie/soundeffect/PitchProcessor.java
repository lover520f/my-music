package com.my.music.moblie.soundeffect;

public class PitchProcessor {
    private float pitchFactor = 1.0f;
    private boolean enabled = false;
    private float sampleRate = 44100.0f;

    private int channels = 2;
    private PhaseVocoder[] vocoders;
    private ChannelState[] channelStates;

    public PitchProcessor() {
        this.channels = 2;
        initChannels();
    }

    public void updateConfig(SoundEffectConfig config) {
        this.enabled = config.hasPitchShift();
        this.pitchFactor = config.pitchPlaybackRate;
        
        if (vocoders != null) {
            for (PhaseVocoder vocoder : vocoders) {
                if (vocoder != null) {
                    vocoder.setPitchFactor(pitchFactor);
                }
            }
        }
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    private void initChannels() {
        vocoders = new PhaseVocoder[channels];
        for (int i = 0; i < channels; i++) {
            vocoders[i] = new PhaseVocoder();
            vocoders[i].setPitchFactor(pitchFactor);
        }
        channelStates = new ChannelState[channels];
        for (int i = 0; i < channels; i++) {
            channelStates[i] = new ChannelState();
        }
    }

    public void process(float[] samples, int channels) {
        int usedChannels = Math.min(channels, this.channels);

        if (!enabled) return;

        for (int i = 0; i < samples.length; i += channels) {
            for (int ch = 0; ch < usedChannels; ch++) {
                if (vocoders != null && ch < vocoders.length && vocoders[ch] != null) {
                    samples[i + ch] = vocoders[ch].processSample(samples[i + ch]);
                }
            }
        }
    }

    private static class ChannelState {
        float[] hopInput = new float[128];
        float[] outputQueue = new float[128];
        int hopFill = 0;
        int outputReadIndex = 0;
        int timeCursor = 0;
    }
}
