package cn.my.music.mobile.soundeffect;

import android.media.MediaPlayer;
import android.content.Context;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import androidx.annotation.NonNull;

public class SoundEffectModule extends ReactContextBaseJavaModule {
    private static final String NAME = "LXSoundEffect";
    private final SoundEffectEngine engine;
    private int currentAudioSessionId = 0;

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
        Log.d(NAME, "updateConfig called");
        engine.updateConfig(config);
    }

    @ReactMethod
    public void isSupported(Promise promise) {
        promise.resolve(true);
    }

    @ReactMethod
    public void setAudioSessionId(int sessionId, Promise promise) {
        Log.d(NAME, "setAudioSessionId called with: " + sessionId);
        this.currentAudioSessionId = sessionId;
        engine.setAudioSessionId(sessionId);
        promise.resolve(true);
    }

    @ReactMethod
    public void release() {
        engine.release();
        this.currentAudioSessionId = 0;
    }

    @ReactMethod
    public void getCurrentAudioSessionId(Promise promise) {
        promise.resolve(this.currentAudioSessionId);
    }
}
