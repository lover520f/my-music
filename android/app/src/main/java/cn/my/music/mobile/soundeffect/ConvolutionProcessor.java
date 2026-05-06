package cn.my.music.mobile.soundeffect;

import android.content.Context;
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

    // 简单的卷积实现
    private float[][] impulseResponse;
    private float[][] inputBuffers;
    private float[][] outputBuffers;
    private float[][] overlapBuffers;
    private int inputFill = 0;
    private int outputReadIndex = 0;
    private int outputFrameCount = 0;
    private int channels = 2;

    public ConvolutionProcessor(Context context) {
        this.context = context;
    }

    public void updateConfig(SoundEffectConfig config) {
        this.enabled = config.hasConvolution();
        this.dryGain = config.convolutionMainGain / 10.0f;
        this.wetGain = config.convolutionSendGain / 10.0f;

        if (enabled && impulseResponse == null) {
            loadImpulseResponse(config);
        }
    }

    private void loadImpulseResponse(SoundEffectConfig config) {
        // 这里应该实际加载混响样本，暂时使用简单的脉冲响应
        // 真实应用中需要从 assets 或资源文件加载 WAV 格式的混响样本
        this.channels = 2;
        this.impulseResponse = new float[channels][1024];
        for (int ch = 0; ch < channels; ch++) {
            for (int i = 0; i < impulseResponse[ch].length; i++) {
                float t = i / 44100.0f;
                impulseResponse[ch][i] = (float) (Math.exp(-t * 10.0) * Math.sin(2 * Math.PI * 1000 * t));
            }
        }
        initBuffers();
    }

    private void initBuffers() {
        inputBuffers = new float[channels][BLOCK_SIZE];
        outputBuffers = new float[channels][BLOCK_SIZE];
        overlapBuffers = new float[channels][impulseResponse[0].length];
    }

    public void process(float[] samples, int channels) {
        int usedChannels = Math.min(channels, this.channels);

        if (!enabled) {
            if (dryGain != 1.0f) {
                for (int i = 0; i < samples.length; i++) {
                    samples[i] *= dryGain;
                }
            }
            return;
        }

        for (int i = 0; i < samples.length; i += channels) {
            // 填充输入缓冲区
            for (int ch = 0; ch < usedChannels; ch++) {
                if (inputBuffers != null && ch < inputBuffers.length) {
                    inputBuffers[ch][inputFill] = samples[i + ch];
                }
            }

            inputFill++;
            if (inputFill >= BLOCK_SIZE && impulseResponse != null) {
                processBlock();
                inputFill = 0;
            }

            // 输出处理
            if (outputFrameCount > 0 && outputReadIndex < outputFrameCount) {
                for (int ch = 0; ch < usedChannels; ch++) {
                    float wet = 0.0f;
                    if (outputBuffers != null && ch < outputBuffers.length) {
                        wet = outputBuffers[ch][outputReadIndex];
                    }
                    float dry = samples[i + ch] * dryGain;
                    samples[i + ch] = dry + wet * wetGain;
                }
                outputReadIndex++;
                if (outputReadIndex >= outputFrameCount) {
                    outputFrameCount = 0;
                    outputReadIndex = 0;
                }
            } else {
                for (int ch = 0; ch < usedChannels; ch++) {
                    samples[i + ch] *= dryGain;
                }
            }
        }
    }

    private void processBlock() {
        outputReadIndex = 0;
        outputFrameCount = BLOCK_SIZE;

        if (impulseResponse == null || outputBuffers == null || overlapBuffers == null) return;

        // 简单的时域卷积
        for (int ch = 0; ch < channels; ch++) {
            float[] input = inputBuffers[ch];
            float[] ir = impulseResponse[ch];
            float[] output = outputBuffers[ch];
            float[] overlap = overlapBuffers[ch];

            for (int n = 0; n < BLOCK_SIZE; n++) {
                output[n] = overlap[n];
                overlap[n] = 0.0f;
                for (int k = 0; k < ir.length && n + k < overlap.length; k++) {
                    if (n < input.length) {
                        overlap[n + k] += input[n] * ir[k];
                    }
                }
            }
        }
    }
}
