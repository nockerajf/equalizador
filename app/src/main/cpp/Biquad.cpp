//
// Created by asilv562@sa1.ford.com on 5/9/25.
//
#include "Biquad.h"
#include <cmath>

void Biquad::setup(Type type, float Fs, float f0, float Q)
{
    const float w0    = 2.0f * static_cast<float>(M_PI) * f0 / Fs;
    const float cosw0 = cosf(w0);
    const float sinw0 = sinf(w0);
    const float alpha = sinw0 / (2.0f * Q);

    float b0, b1n, b2;
    float a0n, a1n, a2n;

    switch (type) {
        case LOWPASS:
            b0  =  (1.0f - cosw0) * 0.5f;
            b1n =   1.0f - cosw0;
            b2  =  (1.0f - cosw0) * 0.5f;
            a0n =   1.0f + alpha;
            a1n =  -2.0f * cosw0;
            a2n =   1.0f - alpha;
            break;

        case HIGHPASS:
            b0  =  (1.0f + cosw0) * 0.5f;
            b1n = -(1.0f + cosw0);
            b2  =  (1.0f + cosw0) * 0.5f;
            a0n =   1.0f + alpha;
            a1n =  -2.0f * cosw0;
            a2n =   1.0f - alpha;
            break;

        case BANDPASS:
            b0  =   sinw0 * 0.5f;
            b1n =   0.0f;
            b2  =  -sinw0 * 0.5f;
            a0n =   1.0f + alpha;
            a1n =  -2.0f * cosw0;
            a2n =   1.0f - alpha;
            break;
    }

    // Normaliza para que a0 = 1
    const float invA0 = 1.0f / a0n;
    a0 = b0  * invA0;
    a1 = b1n * invA0;
    a2 = b2  * invA0;
    b1 = a1n * invA0;
    b2 = a2n * invA0;
}

float Biquad::process(float x)
{
    // s1 & s2 correspondem ao estado interno (z^-1)
    // y[n] = b0*x + s1
    float y = a0 * x + z1;

    // Atualiza estados
    z1 = a1 * x - b1 * y + z2;
    z2 = a2 * x - b2 * y;

    return y;
}

// Separado para evitar simbolização cruzada quando for inline


void Biquad::reset()
{
    z1 = z2 = 0.0f;
}
