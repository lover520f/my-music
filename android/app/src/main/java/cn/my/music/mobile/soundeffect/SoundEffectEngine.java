package cn.my.music.mobile.soundeffect;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.util.Log;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

public class SoundEffectEngine {
    private static final String TAG = "SoundEffectEngine";

    private final Context context;
    private SoundEffectConfig config = new SoundEffectConfig();
    private boolean isActive = false;

    private Equalizer equalizer;
    private PresetReverb reverb;
    private Virtualizer virtualizer;

    private int audioSessionId = 0;
    private int lastInitializedSessionId = -1;

    public SoundEffectEngine(Context context) {
        this.context = context;
    }

    public void setAudioSessionId(int sessionId) {
        Log.d(TAG, "setAudioSessionId called with: " + sessionId + ", current: " + this.audioSessionId);
        this.audioSessionId = sessionId;
        initEffects();
        applyConfig();
    }

    private void initEffects() {
        try {
            if (audioSessionId == 0) {
                Log.d(TAG, "Audio session ID is 0, skipping effect initialization");
                return;
            }

            if (audioSessionId == lastInitializedSessionId && equalizer != null) {
                Log.d(TAG, "Same audio session, no need to reinitialize");
                return;
            }

            releaseEffects();
            lastInitializedSessionId = audioSessionId;

            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            Log.d(TAG, "Equalizer created for session: " + audioSessionId);

            reverb = new PresetReverb(1, audioSessionId);
            reverb.setEnabled(true);
            Log.d(TAG, "PresetReverb created for session: " + audioSessionId);

            virtualizer = new Virtualizer(0, audioSessionId);
            virtualizer.setEnabled(true);
            Log.d(TAG, "Virtualizer created for session: " + audioSessionId);

            Log.d(TAG, "Audio effects initialized successfully for session: " + audioSessionId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize audio effects", e);
        }
    }

    private void releaseEffects() {
        try {
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
                Log.d(TAG, "Equalizer released");
            }
            if (reverb != null) {
                reverb.release();
                reverb = null;
                Log.d(TAG, "Reverb released");
            }
            if (virtualizer != null) {
                virtualizer.release();
                virtualizer = null;
                Log.d(TAG, "Virtualizer released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing effects", e);
        }
    }

    public void updateConfig(ReadableMap configMap) {
        Log.d(TAG, "updateConfig called");
        SoundEffectConfig newConfig = parseConfig(configMap);
        this.config = newConfig;
        this.isActive = newConfig.isActive();
        applyConfig();
    }

    private void applyConfig() {
        try {
            if (audioSessionId == 0) {
                Log.d(TAG, "Audio session not set, skipping config apply");
                return;
            }

            if (equalizer != null) {
                int numBands = equalizer.getNumberOfBands();
                short[] range = equalizer.getBandLevelRange();
                short minLevel = range[0];
                short maxLevel = range[1];

                for (int i = 0; i < numBands && i < config.equalizerGains.length; i++) {
                    float gain = config.equalizerGains[i];
                    short level = (short) (gain * 100);
                    level = (short) Math.max(minLevel, Math.min(maxLevel, level));
                    equalizer.setBandLevel((short) i, level);
                }
                Log.d(TAG, "Equalizer config applied, " + numBands + " bands");
            }

            if (reverb != null && config.hasConvolution()) {
                short preset = PresetReverb.PRESET_NONE;
                String fileName = config.convolutionFileName.toLowerCase();

                if (fileName.contains("hall")) {
                    preset = PresetReverb.PRESET_LARGEHALL;
                } else if (fileName.contains("large")) {
                    preset = PresetReverb.PRESET_LARGEROOM;
                } else if (fileName.contains("medium")) {
                    preset = PresetReverb.PRESET_MEDIUMROOM;
                } else if (fileName.contains("small") || fileName.contains("telephone")) {
                    preset = PresetReverb.PRESET_SMALLROOM;
                } else if (fileName.contains("plate")) {
                    preset = PresetReverb.PRESET_PLATE;
                }

                reverb.setPreset(preset);
                Log.d(TAG, "Reverb preset applied: " + preset + " for file: " + fileName);
            }

            if (virtualizer != null && config.hasPanner()) {
                short strength = (short) (config.pannerSoundR * 100);
                strength = (short) Math.max(0, Math.min(1000, strength));
                virtualizer.setStrength(strength);
                Log.d(TAG, "Virtualizer strength applied: " + strength);
            }

            Log.d(TAG, "Sound effect config applied successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply sound effect config", e);
        }
    }

    private SoundEffectConfig parseConfig(ReadableMap configMap) {
        SoundEffectConfig config = new SoundEffectConfig();

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

        ReadableMap convConfig = configMap.hasKey("convolution") ? configMap.getMap("convolution") : null;
        if (convConfig != null) {
            config.convolutionFileName = convConfig.hasKey("fileName") ? convConfig.getString("fileName") : "";
            config.convolutionAssetUri = convConfig.hasKey("assetUri") ? convConfig.getString("assetUri") : "";
            config.convolutionMainGain = convConfig.hasKey("mainGain") ? (float) convConfig.getDouble("mainGain") : 10.0f;
            config.convolutionSendGain = convConfig.hasKey("sendGain") ? (float) convConfig.getDouble("sendGain") : 0.0f;
        }

        ReadableMap pannerConfig = configMap.hasKey("panner") ? configMap.getMap("panner") : null;
        if (pannerConfig != null) {
            config.pannerEnabled = pannerConfig.hasKey("enabled") && pannerConfig.getBoolean("enabled");
            config.pannerSoundR = pannerConfig.hasKey("soundR") ? (float) pannerConfig.getDouble("soundR") : 5.0f;
            config.pannerSpeed = pannerConfig.hasKey("speed") ? (float) pannerConfig.getDouble("speed") : 25.0f;
        }

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

    public void release() {
        releaseEffects();
        lastInitializedSessionId = -1;
        Log.d(TAG, "Audio effects released");
    }
}
