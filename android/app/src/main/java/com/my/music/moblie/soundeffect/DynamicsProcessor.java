package com.my.music.moblie.soundeffect;

public class DynamicsProcessor {
    private float attackCoeff;
    private float releaseCoeff;
    private static final float LIMITER_THRESHOLD = 0.98f;
    private float currentGain = 1.0f;
    private float sampleRate = 44100.0f;

    public DynamicsProcessor() {
        updateCoefficients();
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
        updateCoefficients();
    }

    private void updateCoefficients() {
        attackCoeff = (float) Math.exp(-1.0 / (0.001f * sampleRate));
        releaseCoeff = (float) Math.exp(-1.0 / (0.08f * sampleRate));
    }

    public void process(float[] samples, int channels) {
        for (int i = 0; i < samples.length; i += channels) {
            float peak = 0.0f;
            for (int ch = 0; ch < channels && i + ch < samples.length; ch++) {
                peak = Math.max(peak, Math.abs(samples[i + ch]));
            }

            float targetGain = 1.0f;
            if (peak > LIMITER_THRESHOLD) {
                targetGain = LIMITER_THRESHOLD / peak;
            }

            float coeff = targetGain < currentGain ? attackCoeff : releaseCoeff;
            currentGain = coeff * currentGain + (1.0f - coeff) * targetGain;
            currentGain = Math.max(0.0f, Math.min(1.0f, currentGain));

            for (int ch = 0; ch < channels && i + ch < samples.length; ch++) {
                samples[i + ch] *= currentGain;
            }
        }
    }
}
