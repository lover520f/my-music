package com.my.music.moblie.soundeffect;

public class PhaseVocoder {
    private static final int FFT_SIZE = 2048;
    private static final int HOP_SIZE = 512;
    
    private float pitchFactor = 1.0f;
    
    private float[] inputBuffer;
    private float[] outputBuffer;
    private float[] window;
    private int writeIndex = 0;
    private int readIndex = 0;
    
    // FFT 相关
    private float[] realBuffer;
    private float[] imagBuffer;
    
    public PhaseVocoder() {
        initBuffers();
        generateWindow();
    }
    
    private void initBuffers() {
        inputBuffer = new float[FFT_SIZE * 4];
        outputBuffer = new float[FFT_SIZE * 4];
        realBuffer = new float[FFT_SIZE];
        imagBuffer = new float[FFT_SIZE];
    }
    
    private void generateWindow() {
        window = new float[FFT_SIZE];
        for (int i = 0; i < FFT_SIZE; i++) {
            window[i] = (float) (0.5 * (1 - Math.cos(2 * Math.PI * i / (FFT_SIZE - 1))));
        }
    }
    
    public void setPitchFactor(float factor) {
        this.pitchFactor = factor;
    }
    
    public float processSample(float input) {
        inputBuffer[writeIndex] = input;
        writeIndex++;
        
        float output = 0.0f;
        if ((writeIndex - readIndex) >= FFT_SIZE) {
            processFrame();
            output = outputBuffer[readIndex];
            readIndex++;
        }
        
        if (writeIndex >= inputBuffer.length) {
            writeIndex = 0;
        }
        if (readIndex >= outputBuffer.length) {
            readIndex = 0;
        }
        
        return output;
    }
    
    private void processFrame() {
        // 1. 提取一帧并加窗
        for (int i = 0; i < FFT_SIZE; i++) {
            int idx = (readIndex + i) % inputBuffer.length;
            realBuffer[i] = inputBuffer[idx] * window[i];
            imagBuffer[i] = 0.0f;
        }
        
        // 2. FFT
        fft(realBuffer, imagBuffer);
        
        // 3. 相位累积
        float[] magnitudes = new float[FFT_SIZE];
        float[] phases = new float[FFT_SIZE];
        
        for (int i = 0; i < FFT_SIZE / 2; i++) {
            float real = realBuffer[i];
            float imag = imagBuffer[i];
            magnitudes[i] = (float) Math.sqrt(real * real + imag * imag);
            phases[i] = (float) Math.atan2(imag, real);
        }
        
        // 4. 频率移位
        float[] newPhases = new float[FFT_SIZE / 2];
        for (int i = 0; i < FFT_SIZE / 2; i++) {
            int newBin = (int) (i * pitchFactor);
            if (newBin < FFT_SIZE / 2) {
                newPhases[newBin] = phases[i];
            }
        }
        
        // 5. 逆变换
        for (int i = 0; i < FFT_SIZE / 2; i++) {
            realBuffer[i] = (float) (magnitudes[i] * Math.cos(newPhases[i]));
            imagBuffer[i] = (float) (magnitudes[i] * Math.sin(newPhases[i]));
        }
        
        // 清除高频部分
        for (int i = FFT_SIZE / 2; i < FFT_SIZE; i++) {
            realBuffer[i] = 0;
            imagBuffer[i] = 0;
        }
        
        // 6. IFFT
        ifft(realBuffer, imagBuffer);
        
        // 7. 加窗并输出
        for (int i = 0; i < FFT_SIZE; i++) {
            int idx = (writeIndex + i) % outputBuffer.length;
            outputBuffer[idx] += realBuffer[i] * window[i] / (FFT_SIZE / 2);
        }
    }
    
    private void fft(float[] real, float[] imag) {
        int n = real.length;
        int bits = (int) Math.log(n) / (int) Math.log(2);
        
        // 位反转排序
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - bits);
            if (i < j) {
                float tempReal = real[i];
                float tempImag = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tempReal;
                imag[j] = tempImag;
            }
        }
        
        // Cooley-Tukey FFT
        for (int size = 2; size <= n; size *= 2) {
            int halfsize = size / 2;
            float angle = (float) (-2 * Math.PI / size);
            
            for (int i = 0; i < n; i += size) {
                for (int j = 0; j < halfsize; j++) {
                    int idx = i + j;
                    int idx2 = idx + halfsize;
                    
                    float tReal = (float) (Math.cos(angle * j) * real[idx2] - Math.sin(angle * j) * imag[idx2]);
                    float tImag = (float) (Math.sin(angle * j) * real[idx2] + Math.cos(angle * j) * imag[idx2]);
                    
                    real[idx2] = real[idx] - tReal;
                    imag[idx2] = imag[idx] - tImag;
                    real[idx] = real[idx] + tReal;
                    imag[idx] = imag[idx] + tImag;
                }
            }
        }
    }
    
    private void ifft(float[] real, float[] imag) {
        int n = real.length;
        
        // 取共轭
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i];
        }
        
        // FFT
        fft(real, imag);
        
        // 再次取共轭并除以 N
        for (int i = 0; i < n; i++) {
            real[i] /= n;
            imag[i] = -imag[i] / n;
        }
    }
    
    public void reset() {
        for (int i = 0; i < inputBuffer.length; i++) {
            inputBuffer[i] = 0;
        }
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = 0;
        }
        writeIndex = 0;
        readIndex = 0;
    }
}
