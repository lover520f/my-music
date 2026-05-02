package com.my.music.moblie.soundeffect;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavLoader {
    
    public static float[] loadWavFromAssets(Context context, String fileName) throws IOException {
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = context.getAssets().open(fileName);
            bis = new BufferedInputStream(is);
            return loadWav(bis);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static float[] loadWav(BufferedInputStream is) throws IOException {
        byte[] header = new byte[44];
        int bytesRead = is.read(header);
        if (bytesRead < 44) {
            throw new IOException("Invalid WAV file: header too short");
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // RIFF header
        buffer.position(0);
        byte[] riff = new byte[4];
        buffer.get(riff);
        if (!"RIFF".equals(new String(riff))) {
            throw new IOException("Invalid WAV file: missing RIFF header");
        }
        
        // Wave format
        buffer.position(8);
        byte[] wave = new byte[4];
        buffer.get(wave);
        if (!"WAVE".equals(new String(wave))) {
            throw new IOException("Invalid WAV file: missing WAVE format");
        }
        
        // Find data chunk
        buffer.position(12);
        int channels = 0;
        int sampleRate = 0;
        int bitsPerSample = 0;
        int dataSize = 0;
        
        while (buffer.position() < 44) {
            byte[] chunkId = new byte[4];
            buffer.get(chunkId);
            String chunkIdStr = new String(chunkId);
            
            if ("fmt ".equals(chunkIdStr)) {
                buffer.position(buffer.position() + 4);
                int fmtSize = buffer.getInt();
                short audioFormat = buffer.getShort();
                channels = buffer.getShort();
                sampleRate = buffer.getInt();
                buffer.position(buffer.position() + 6);
                bitsPerSample = buffer.getShort();
            } else if ("data".equals(chunkIdStr)) {
                dataSize = buffer.getInt();
                break;
            } else {
                if (buffer.position() + 4 < header.length) {
                    int chunkSize = buffer.getInt();
                    buffer.position(buffer.position() + chunkSize);
                }
            }
        }
        
        // Read audio data
        int bytesPerSample = bitsPerSample / 8;
        int numSamples = dataSize / bytesPerSample;
        float[] samples = new float[numSamples];
        
        byte[] data = new byte[dataSize];
        int totalRead = 0;
        int read;
        while (totalRead < dataSize && (read = is.read(data, totalRead, dataSize - totalRead)) != -1) {
            totalRead += read;
        }
        
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        if (bitsPerSample == 16) {
            for (int i = 0; i < numSamples; i++) {
                short sample = dataBuffer.getShort();
                samples[i] = sample / 32768.0f;
            }
        } else if (bitsPerSample == 24) {
            for (int i = 0; i < numSamples; i++) {
                int b0 = dataBuffer.get() & 0xFF;
                int b1 = dataBuffer.get() & 0xFF;
                int b2 = dataBuffer.get() & 0xFF;
                int sample = (b2 << 16) | (b1 << 8) | b0;
                if ((sample & 0x800000) != 0) {
                    sample |= 0xFF000000;
                }
                samples[i] = sample / 8388608.0f;
            }
        } else if (bitsPerSample == 32) {
            for (int i = 0; i < numSamples; i++) {
                int sample = dataBuffer.getInt();
                samples[i] = sample / 2147483648.0f;
            }
        } else {
            throw new IOException("Unsupported bits per sample: " + bitsPerSample);
        }
        
        // Convert stereo to mono if needed
        if (channels == 2) {
            float[] mono = new float[numSamples / 2];
            for (int i = 0; i < mono.length; i++) {
                mono[i] = (samples[i * 2] + samples[i * 2 + 1]) / 2.0f;
            }
            return mono;
        }
        
        return samples;
    }
}
