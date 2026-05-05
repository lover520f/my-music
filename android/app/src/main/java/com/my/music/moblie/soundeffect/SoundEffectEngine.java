package com.my.music.moblie.soundeffect;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.util.Log;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import java.util.ArrayList;
import java.util.List;

public class SoundEffectEngine {
    private static final String TAG = "SoundEffectEngine";
    
    private final Context context;
    private SoundEffectConfig config = new SoundEffectConfig();
    private boolean isActive = false;

    // Android 系统音效对象
    private Equalizer equalizer;
    private PresetReverb reverb;
    private Virtualizer virtualizer;
    
    // 当前音频会话 ID（将在播放时设置）
    private int audioSessionId = 0;
    
    // 预设混响名称映射
    private static final String[] PRESET_NAMES = {
        "none",
        "small",
        "medium",
        "large",
        "hall"
    };

    public SoundEffectEngine(Context context) {
        this.context = context;
        // 初始化时会在设置 audioSessionId 后创建对象
    }

    public void setAudioSessionId(int sessionId) {
        this.audioSessionId = sessionId;
        initEffects();
        applyConfig();
    }

    private void initEffects() {
        try {
            if (audioSessionId != 0) {
                // 初始化 Equalizer
                if (equalizer != null) {
                    equalizer.release();
                }
                equalizer = new Equalizer(0, audioSessionId);
                equalizer.setEnabled(true);
                
                // 初始化 PresetReverb
                if (reverb != null) {
                    reverb.release();
                }
                reverb = new PresetReverb(1, audioSessionId);
                reverb.setEnabled(true);
                
                // 初始化 Virtualizer（3D环绕）
                if (virtualizer != null) {
                    virtualizer.release();
                }
                virtualizer = new Virtualizer(2, audioSessionId);
                virtualizer.setEnabled(true);
                
                Log.d(TAG, "Audio effects initialized for session: " + audioSessionId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize audio effects", e);
        }
    }

    public void updateConfig(ReadableMap configMap) {
        SoundEffectConfig newConfig = parseConfig(configMap);
        this.config = newConfig;
        this.isActive = newConfig.isActive();
        applyConfig();
    }

    private void applyConfig() {
        try {
            // 应用 Equalizer 设置
            if (equalizer != null && config.hasEqualizer()) {
                int numBands = equalizer.getNumberOfBands();
                short[] range = equalizer.getBandLevelRange();
                short minLevel = range[0];
                short maxLevel = range[1];
                
                for (int i = 0; i < numBands && i < config.equalizerGains.length; i++) {
                    // 将 dB 转换为 Equalizer 期望的范围
                    float gain = config.equalizerGains[i];
                    short level = (short) (gain * 100);
                    // 限制在有效范围内
                    level = (short) Math.max(minLevel, Math.min(maxLevel, level));
                    equalizer.setBandLevel((short) i, level);
                }
            }
            
            // 应用 PresetReverb 设置
            if (reverb != null && config.hasConvolution()) {
                // 尝试将文件名映射到预设
                short preset = PresetReverb.PRESET_NONE;
                String fileName = config.convolutionFileName.toLowerCase();
                
                if (fileName.contains("hall")) preset = PresetReverb.PRESET_HALL;
                else if (fileName.contains("large")) preset = PresetReverb.PRESET_LARGEROOM;
                else if (fileName.contains("medium")) preset = PresetReverb.PRESET_MEDIUMROOM;
                else if (fileName.contains("small") || fileName.contains("telephone")) preset = PresetReverb.PRESET_SMALLROOM;
                
                reverb.setPreset(preset);
            }
            
            // 应用 Virtualizer（3D环绕）设置
            if (virtualizer != null && config.hasPanner()) {
                short strength = (short) (config.pannerSoundR * 100);
                strength = (short) Math.max(0, Math.min(1000, strength));
                virtualizer.setStrength(strength);
            }
            
            Log.d(TAG, "Sound effect config applied");
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply sound effect config", e);
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
        try {
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
            }
            if (reverb != null) {
                reverb.release();
                reverb = null;
            }
            if (virtualizer != null) {
                virtualizer.release();
                virtualizer = null;
            }
            Log.d(TAG, "Audio effects released");
        } catch (Exception e) {
            Log.e(TAG, "Failed to release audio effects", e);
        }
    }
}
