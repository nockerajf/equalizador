#include "ThreeBandFilter.h"

void ThreeBandFilter::init(float sampleRate,
                           float lowCut,
                           float midCenter,
                           float highCut,
                           float Q)
{
    low_.setup (Biquad::LOWPASS,  sampleRate, lowCut,    Q);
    mid_.setup (Biquad::BANDPASS, sampleRate, midCenter, Q);
    high_.setup(Biquad::HIGHPASS, sampleRate, highCut,   Q);
}

float ThreeBandFilter::processSample(float x)
{
    // Aqui retornamos a soma das três bandas (equalizador de 3 bandas).
    // Se quiser saídas separadas, altere esta lógica.
    return  low_.process(x) +
            mid_.process(x) +
            high_.process(x);
}

void ThreeBandFilter::processBuffer(float* samples, size_t n)
{
    if (!samples) return;
    for (size_t i = 0; i < n; ++i)
        samples[i] = processSample(samples[i]);
}

void ThreeBandFilter::reset()
{
    low_.reset();
    mid_.reset();
    high_.reset();
}
