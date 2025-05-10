#pragma once
#include "Biquad.h"
#include <cstddef>

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

    // Processa um buffer de 'n' amostras mono em ponto-flutuante
    void processBuffer(float* samples, size_t n);

    // Se preferir amostra-a-amostra
    float processSample(float x);

    // Reseta estados internos
    void reset();

private:
    Biquad low_;
    Biquad mid_;
    Biquad high_;
};
