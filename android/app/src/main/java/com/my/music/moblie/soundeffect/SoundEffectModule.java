package com.my.music.moblie.soundeffect;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class SoundEffectModule extends ReactContextBaseJavaModule {
    private static final String NAME = "LXSoundEffect";
    private final SoundEffectEngine engine;

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
    }

    @ReactMethod
    public void isSupported(Promise promise) {
        promise.resolve(true);
    }

    @ReactMethod
    public void setAudioSessionId(int sessionId, Promise promise) {
        engine.setAudioSessionId(sessionId);
        promise.resolve(true);
    }

    @ReactMethod
    public void release() {
        engine.release();
    }
}
