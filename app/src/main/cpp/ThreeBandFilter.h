#pragma once
#include "Biquad.h"
#include <cstddef>
#define LOG_TAG "ThreeBandFilter" // Replace with a relevant tag
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
/*
 * ThreeBandFilter – empacota três Biquads
 * low-pass, band-pass (centro), high-pass.
 */
class ThreeBandFilter {
public:
    // Configurações iniciais
    void init(float sampleRate,
              float lowCut    = 200.0f,   // Hz
              float midCenter = 1000.0f,  // Hz
              float highCut   = 5000.0f,  // Hz
              float Q         = 0.707f);  // mesmo Q para as três bandas
    void setLowGain (float lin) { gLow_  = lin; }
    void setMidGain (float lin) { gMid_  = lin; }
    void setHighGain(float lin) { gHigh_ = lin; }

    // Processa um buffer de 'n' amostras mono em ponto-flutuante
    void processBuffer(float* samples, size_t n);

    // Se preferir amostra-a-amostra
    float processSample(float x);

    void setRampMs(float ms);

    // Reseta estados internos
    void reset();

private:
    Biquad low_, mid_, high_;
    float gLow_  = 1.0f;
    float gMid_  = 1.0f;
    float gHigh_ = 1.0f;
    float gLowSmooth_  = 1.0f;
    float gMidSmooth_  = 1.0f;
    float gHighSmooth_ = 1.0f;
    float sampleRate_ = 48000.f;
    float alpha_ = 0.999f;
};
