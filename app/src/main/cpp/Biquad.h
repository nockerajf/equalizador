#pragma once
#include <cstdint>

/*
 * Biquad – filtro IIR de 2ª ordem
 * Implementa low-pass, high-pass e band-pass.
 *
 * Exemplo rápido:
 *     Biquad biq;
 *     biq.setup(Biquad::LOWPASS, 48000, 2000.0f, 0.707f);
 *     float y = biq.process(x);
 */
class Biquad {
public:
    enum Type : uint8_t { LOWPASS, HIGHPASS, BANDPASS };

    // Inicializa coeficientes (f0 em Hz, Q sem dimensão)
    void setup(Type type,
               float sampleRate,
               float f0,
               float Q = 0.707f); // 0.707 → Butterworth

    // Processa uma única amostra
    float process(float x);

    // Zera o histórico interno
    void reset();

private:
    // Coeficientes normalizados (a0 já =1 no livro-texto; aqui usamos notação própria)
    float a0 = 1.0f, a1 = 0.0f, a2 = 0.0f; // numerador
    float b1 = 0.0f, b2 = 0.0f;            // denominador (‐b1, ‐b2 na equação canônica)

    // Memória (delays)
    float z1 = 0.0f, z2 = 0.0f;
};
