package com.my.music.moblie.soundeffect;

public class EqualizerProcessor {
    private static final float[] FREQUENCIES = {31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
    private static final float Q = 1.41f;
    private static final int BAND_COUNT = 10;

    private BiquadFilter[] filters;
    private float sampleRate = 44100.0f;
    private float[] gains = new float[BAND_COUNT];
    private boolean enabled = false;

    public EqualizerProcessor() {
        filters = new BiquadFilter[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            filters[i] = new BiquadFilter();
        }
    }

    public void updateConfig(SoundEffectConfig config) {
        this.enabled = config.hasEqualizer();
        this.gains = config.equalizerGains.length >= BAND_COUNT ? config.equalizerGains : new float[BAND_COUNT];
        updateCoefficients();
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
        updateCoefficients();
    }

    private void updateCoefficients() {
        for (int i = 0; i < BAND_COUNT; i++) {
            float gain = gains[i];
            if (Math.abs(gain) < 0.01f) {
                filters[i].setBypass();
            } else {
                float amplitude = (float) Math.pow(10.0, gain / 40.0);
                float omega = 2.0f * (float) Math.PI * FREQUENCIES[i] / sampleRate;
                float cosOmega = (float) Math.cos(omega);
                float sinOmega = (float) Math.sin(omega);
                float alpha = sinOmega / (2.0f * Q);

                float b0 = 1.0f + alpha * amplitude;
                float b1 = -2.0f * cosOmega;
                float b2 = 1.0f - alpha * amplitude;
                float a0 = 1.0f + alpha / amplitude;
                float a1 = -2.0f * cosOmega;
                float a2 = 1.0f - alpha / amplitude;

                filters[i].setCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0);
            }
        }
    }

    public void process(float[] samples, int channels) {
        if (!enabled) return;

        for (int channel = 0; channel < channels; channel++) {
            for (int i = 0; i < filters.length; i++) {
                filters[i].reset();
            }
            for (int i = channel; i < samples.length; i += channels) {
                float sample = samples[i];
                for (int band = 0; band < filters.length; band++) {
                    sample = filters[band].process(sample);
                }
                samples[i] = sample;
            }
        }
    }

    private static class BiquadFilter {
        private float b0, b1, b2, a1, a2;
        private float z1, z2;
        private boolean bypass = true;

        void setBypass() {
            this.bypass = true;
        }

        void setCoefficients(float b0, float b1, float b2, float a1, float a2) {
            this.b0 = b0;
            this.b1 = b1;
            this.b2 = b2;
            this.a1 = a1;
            this.a2 = a2;
            this.bypass = false;
        }

        void reset() {
            z1 = z2 = 0.0f;
        }

        float process(float input) {
            if (bypass) return input;

            float output = b0 * input + z1;
            z1 = b1 * input - a1 * output + z2;
            z2 = b2 * input - a2 * output;
            return output;
        }
    }
}
