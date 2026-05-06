package cn.my.music.mobile.soundeffect;

public class PitchProcessor {
    private static final int BLOCK_SIZE = 4096;
    private static final int HOP_SIZE = 128;

    private float pitchFactor = 1.0f;
    private boolean enabled = false;
    private float sampleRate = 44100.0f;

    private int channels = 2;
    private ChannelState[] channelStates;

    public PitchProcessor() {
        this.channels = 2;
        initChannels();
    }

    public void updateConfig(SoundEffectConfig config) {
        this.enabled = config.hasPitchShift();
        this.pitchFactor = config.pitchPlaybackRate;
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    private void initChannels() {
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
                if (channelStates != null && ch < channelStates.length) {
                    ChannelState state = channelStates[ch];
                    state.hopInput[state.hopFill] = samples[i + ch];
                    state.hopFill++;

                    if (state.outputReadIndex < HOP_SIZE) {
                        samples[i + ch] = state.outputQueue[state.outputReadIndex];
                        state.outputReadIndex++;
                    } else {
                        samples[i + ch] = 0.0f;
                    }

                    if (state.hopFill >= HOP_SIZE) {
                        processHop(state);
                        state.hopFill = 0;
                        state.outputReadIndex = 0;
                        state.timeCursor += HOP_SIZE;
                    }
                }
            }
        }
    }

    private void processHop(ChannelState state) {
        // 移动输入缓冲区
        System.arraycopy(state.inputBuffer, HOP_SIZE, state.inputBuffer, 0, BLOCK_SIZE - HOP_SIZE);
        System.arraycopy(state.hopInput, 0, state.inputBuffer, BLOCK_SIZE - HOP_SIZE, HOP_SIZE);

        // 简单的 pitch shifting（真实应用需要更复杂的算法）
        // 这里我们只做简单的重采样作为演示
        float speed = pitchFactor;
        for (int i = 0; i < HOP_SIZE; i++) {
            int srcIndex = (int) (i / speed);
            if (srcIndex < BLOCK_SIZE) {
                state.outputQueue[i] = state.inputBuffer[srcIndex];
            } else {
                state.outputQueue[i] = 0.0f;
            }
        }
    }

    private static class ChannelState {
        float[] inputBuffer = new float[BLOCK_SIZE];
        float[] hopInput = new float[HOP_SIZE];
        float[] outputQueue = new float[HOP_SIZE];
        int hopFill = 0;
        int outputReadIndex = 0;
        int timeCursor = 0;
    }
}
