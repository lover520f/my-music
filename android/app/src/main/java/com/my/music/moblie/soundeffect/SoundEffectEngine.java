package com.my.music.moblie.soundeffect;

import android.content.Context;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import java.util.ArrayList;
import java.util.List;

public class SoundEffectEngine {
    private final Context context;
    private SoundEffectConfig config = new SoundEffectConfig();
    private boolean isActive = false;

    // 音效处理器
    private EqualizerProcessor equalizer;
    private ConvolutionProcessor convolution;
    private PannerProcessor panner;
    private PitchProcessor pitch;
    private DynamicsProcessor dynamics;

    public SoundEffectEngine(Context context) {
        this.context = context;
        initializeProcessors();
    }

    private void initializeProcessors() {
        equalizer = new EqualizerProcessor();
        convolution = new ConvolutionProcessor(context);
        panner = new PannerProcessor();
        pitch = new PitchProcessor();
        dynamics = new DynamicsProcessor();
    }

    public void updateConfig(ReadableMap configMap) {
        SoundEffectConfig newConfig = parseConfig(configMap);
        this.config = newConfig;
        this.isActive = newConfig.isActive();

        // 更新各个处理器
        if (equalizer != null) {
            equalizer.updateConfig(newConfig);
        }
        if (convolution != null) {
            convolution.updateConfig(newConfig);
        }
        if (panner != null) {
            panner.updateConfig(newConfig);
        }
        if (pitch != null) {
            pitch.updateConfig(newConfig);
        }
    }

    public void setAudioSessionId(int audioSessionId) {
        if (equalizer != null) {
            // Android Equalizer API 不需要手动设置 session ID
        }
        if (convolution != null) {
            convolution.setAudioSessionId(audioSessionId);
        }
    }

    private SoundEffectConfig parseConfig(ReadableMap configMap) {
        SoundEffectConfig config = new SoundEffectConfig();

        // 解析均衡器配置
        ReadableMap eqConfig = configMap.hasKey("equalizer") ? configMap.getMap("equalizer") : configMap;
        if (eqConfig != null) {
            config.equalizerEnabled = eqConfig.hasKey("enabled") && eqConfig.getBoolean("enabled");
            if (eqConfig.hasKey("gains")) {
                ReadableArray gains = eqConfig.getArray("gains");
                if (gains != null) {
                    float[] gainArray = new float[gains.size()];
                    for (int i = 0; i < gains.size(); i++) {
                        gainArray[i] = (float) gains.getDouble(i);
                    }
                    config.equalizerGains = gainArray;
                }
            }
        }

        // 解析混响配置
        ReadableMap convConfig = configMap.hasKey("convolution") ? configMap.getMap("convolution") : null;
        if (convConfig != null) {
            config.convolutionFileName = convConfig.hasKey("fileName") ? convConfig.getString("fileName") : "";
            config.convolutionAssetUri = convConfig.hasKey("assetUri") ? convConfig.getString("assetUri") : "";
            config.convolutionMainGain = convConfig.hasKey("mainGain") ? (float) convConfig.getDouble("mainGain") : 10.0f;
            config.convolutionSendGain = convConfig.hasKey("sendGain") ? (float) convConfig.getDouble("sendGain") : 0.0f;
        }

        // 解析3D环绕配置
        ReadableMap pannerConfig = configMap.hasKey("panner") ? configMap.getMap("panner") : null;
        if (pannerConfig != null) {
            config.pannerEnabled = pannerConfig.hasKey("enabled") && pannerConfig.getBoolean("enabled");
            config.pannerSoundR = pannerConfig.hasKey("soundR") ? (float) pannerConfig.getDouble("soundR") : 5.0f;
            config.pannerSpeed = pannerConfig.hasKey("speed") ? (float) pannerConfig.getDouble("speed") : 25.0f;
        }

        // 解析变调配置
        ReadableMap pitchConfig = configMap.hasKey("pitchShifter") ? configMap.getMap("pitchShifter") : null;
        if (pitchConfig != null) {
            config.pitchPlaybackRate = pitchConfig.hasKey("playbackRate") ? (float) pitchConfig.getDouble("playbackRate") : 1.0f;
        }

        return config;
    }

    public SoundEffectConfig getConfig() {
        return config;
    }

    public boolean isActive() {
        return isActive;
    }

    // 音频处理方法（将在音频播放器中调用）
    public float[] processAudio(float[] samples, int channels) {
        if (!isActive || samples == null || samples.length == 0) {
            return samples;
        }

        float[] processed = samples.clone();

        // 1. 均衡器处理
        if (config.hasEqualizer() && equalizer != null) {
            equalizer.process(processed, channels);
        }

        // 2. 变调处理
        if (config.hasPitchShift() && pitch != null) {
            pitch.process(processed, channels);
        }

        // 3. 混响处理
        if (config.hasConvolution() && convolution != null) {
            convolution.process(processed, channels);
        }

        // 4. 动态处理器（限幅器）
        if (dynamics != null) {
            dynamics.process(processed, channels);
        }

        // 5. 3D环绕处理
        if (config.hasPanner() && panner != null) {
            panner.process(processed, channels);
        }

        // 限幅到 [-1, 1] 范围
        for (int i = 0; i < processed.length; i++) {
            processed[i] = Math.max(-1.0f, Math.min(1.0f, processed[i]));
        }

        return processed;
    }

    public void release() {
        if (convolution != null) {
            convolution.release();
        }
    }
}
