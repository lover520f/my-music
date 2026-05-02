package com.my.music.moblie.soundeffect;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import java.util.HashMap;
import java.util.Map;

public class SoundEffectModule extends ReactContextBaseJavaModule {
    private static final String NAME = "LXSoundEffect";
    private final SoundEffectEngine engine;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;

    public SoundEffectModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.engine = new SoundEffectEngine(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void updateConfig(ReadableMap config) {
        engine.updateConfig(config);
        applyEffectsToAudio();
    }

    @ReactMethod
    public void isSupported(Promise promise) {
        promise.resolve(true);
    }

    @ReactMethod
    public void setAudioSessionId(int audioSessionId) {
        try {
            releaseEffects();
            
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            
            bassBoost = new BassBoost(0, audioSessionId);
            bassBoost.setEnabled(true);
            
            virtualizer = new Virtualizer(0, audioSessionId);
            virtualizer.setEnabled(true);
            
            applyEffectsToAudio();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyEffectsToAudio() {
        SoundEffectConfig config = engine.getConfig();
        if (config == null) return;

        if (equalizer != null) {
            applyEqualizer(config);
        }
        
        if (bassBoost != null) {
            applyBassBoost(config);
        }
        
        if (virtualizer != null) {
            applyVirtualizer(config);
        }
    }

    private void applyEqualizer(SoundEffectConfig config) {
        if (equalizer == null || !config.hasEqualizer()) {
            if (equalizer != null) {
                equalizer.setEnabled(false);
            }
            return;
        }

        try {
            short numBands = equalizer.getNumberOfBands();
            short[] levelRange = equalizer.getBandLevelRange();
            short minLevel = levelRange[0];
            short maxLevel = levelRange[1];

            float[] gains = config.equalizerGains;
            int bands = Math.min((int) numBands, gains.length);
            
            for (int i = 0; i < bands; i++) {
                short bandLevel = (short) (minLevel + (maxLevel - minLevel) * (gains[i] + 15) / 30);
                equalizer.setBandLevel((short) i, bandLevel);
            }
            
            equalizer.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyBassBoost(SoundEffectConfig config) {
        if (bassBoost == null) return;

        try {
            if (config.hasEqualizer()) {
                float bassGain = 0;
                if (config.equalizerGains.length > 0) {
                    bassGain = Math.max(config.equalizerGains[0], 0);
                }
                int strength = (int) (bassGain / 15.0f * 1000);
                bassBoost.setStrength((short) strength);
                bassBoost.setEnabled(true);
            } else {
                bassBoost.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyVirtualizer(SoundEffectConfig config) {
        if (virtualizer == null) return;

        try {
            if (config.hasPanner()) {
                int strength = (int) (config.pannerSoundR / 30.0f * 1000);
                virtualizer.setStrength((short) strength);
                virtualizer.setEnabled(true);
            } else {
                virtualizer.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseEffects() {
        try {
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
            }
            if (bassBoost != null) {
                bassBoost.release();
                bassBoost = null;
            }
            if (virtualizer != null) {
                virtualizer.release();
                virtualizer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Required for RN event emitter
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Required for RN event emitter
    }
}
