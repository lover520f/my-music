package com.my.music.moblie.soundeffect;

public class PannerProcessor {
    private float sampleRate = 44100.0f;
    private float soundR = 5.0f;
    private float speed = 25.0f;
    private boolean enabled = false;

    private double processedSamples = 0.0;
    private DelayLine leftDelay;
    private DelayLine rightDelay;

    public PannerProcessor() {
        leftDelay = new DelayLine(1000);
        rightDelay = new DelayLine(1000);
    }

    public void updateConfig(SoundEffectConfig config) {
        this.enabled = config.hasPanner();
        this.soundR = Math.max(1.0f, Math.min(config.pannerSoundR / 10.0f, 3.0f));
        this.speed = Math.max(1.0f, Math.min(config.pannerSpeed, 50.0f));
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void process(float[] samples, int channels) {
        if (!enabled || channels < 2) return;

        double phaseStep = (Math.PI / 180.0) / (Math.max((double) speed * 0.01, 0.1) * sampleRate);

        for (int i = 0; i < samples.length; i += channels) {
            double angle = processedSamples * phaseStep;
            float x = (float) Math.sin(angle) * soundR;
            float normalizedX = Math.max(-1.0f, Math.min(1.0f, x / Math.max(soundR, 0.0001f)));

            float leftGain = (float) Math.sqrt(0.5 * (1.0 - normalizedX));
            float rightGain = (float) Math.sqrt(0.5 * (1.0 + normalizedX));

            int itdSamples = Math.abs((int) Math.round(normalizedX * 300.0f));

            float inputLeft = samples[i];
            float inputRight = samples[i + 1];
            float mid = 0.5f * (inputLeft + inputRight);
            float side = 0.5f * (inputLeft - inputRight);

            float delayedLeft = leftDelay.pushAndRead(mid * leftGain, normalizedX > 0 ? itdSamples : 0);
            float delayedRight = rightDelay.pushAndRead(mid * rightGain, normalizedX < 0 ? itdSamples : 0);

            samples[i] = delayedLeft + side * 0.28f;
            samples[i + 1] = delayedRight - side * 0.28f;
            processedSamples++;
        }
    }

    private static class DelayLine {
        private float[] buffer;
        private int writeIndex = 0;

        DelayLine(int size) {
            buffer = new float[Math.max(size, 1)];
        }

        float pushAndRead(float input, int delaySamples) {
            int clampedDelay = Math.max(0, Math.min(delaySamples, buffer.length - 1));
            buffer[writeIndex] = input;
            int readIndex = (writeIndex - clampedDelay + buffer.length) % buffer.length;
            float output = buffer[readIndex];
            writeIndex++;
            if (writeIndex >= buffer.length) {
                writeIndex = 0;
            }
            return output;
        }
    }
}
