package cn.my.music.mobile.soundeffect;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import androidx.annotation.NonNull;
import java.util.List;

public class AudioSessionModule extends ReactContextBaseJavaModule {
    private static final String TAG = "AudioSessionModule";
    private static int cachedSessionId = 0;
    private AudioManager audioManager;

    public AudioSessionModule(ReactApplicationContext reactContext) {
        super(reactContext);
        audioManager = (AudioManager) reactContext.getSystemService(reactContext.AUDIO_SERVICE);
    }

    @NonNull
    @Override
    public String getName() {
        return "LXAudioSession";
    }

    @ReactMethod
    public void getAudioSessionId(Promise promise) {
        try {
            int sessionId = findActiveAudioSession();
            Log.d(TAG, "getAudioSessionId returning: " + sessionId);
            promise.resolve(sessionId);
        } catch (Exception e) {
            Log.e(TAG, "getAudioSessionId error: " + e.getMessage());
            promise.resolve(0);
        }
    }

    private int findActiveAudioSession() {
        if (cachedSessionId != 0) {
            return cachedSessionId;
        }

        try {
            if (audioManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                List<AudioPlaybackConfiguration> configs = audioManager.getActivePlaybackConfigurations();
                for (AudioPlaybackConfiguration config : configs) {
                    AudioAttributes attrs = config.getAudioAttributes();
                    if (attrs != null) {
                        int session = getAudioSessionIdFromAttributes(attrs);
                        if (session > 0) {
                            cachedSessionId = session;
                            Log.d(TAG, "Found active audio session: " + session);
                            return session;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting active audio sessions: " + e.getMessage());
        }

        Log.d(TAG, "No active audio session found, returning 0");
        return 0;
    }

    private int getAudioSessionIdFromAttributes(AudioAttributes attrs) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                java.lang.reflect.Method method = attrs.getClass().getMethod("getSessionId");
                Object result = method.invoke(attrs);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Could not get session ID from AudioAttributes: " + e.getMessage());
        }
        return 0;
    }

    @ReactMethod
    public void clearCachedSessionId() {
        cachedSessionId = 0;
        Log.d(TAG, "Cleared cached session ID");
    }
}
