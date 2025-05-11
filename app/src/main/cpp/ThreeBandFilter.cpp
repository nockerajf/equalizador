#include <math.h>
#include "ThreeBandFilter.h"

void ThreeBandFilter::init(float sampleRate,
                           float lowCut,
                           float midCenter,
                           float highCut,
                           float Q)
{
    if (sampleRate != 0)
        sampleRate_ = sampleRate;

    low_.setup (Biquad::LOWPASS,  sampleRate_, lowCut,    Q);
    mid_.setup (Biquad::BANDPASS, sampleRate_, midCenter, Q);
    high_.setup(Biquad::HIGHPASS, sampleRate_, highCut,   Q);

    low_.reset();
    mid_.reset();
    high_.reset();

    /* garante que, numa primeira chamada, o smoothed comece já no alvo */
    gLowSmooth_  = gLow_;
    gMidSmooth_  = gMid_;
    gHighSmooth_ = gHigh_;
    setRampMs(50.f);
}

float ThreeBandFilter::processSample(float x)
{
    // 1º: atualiza os ganhos suavizados (filtro exponencial de 1ª ordem)
    gLowSmooth_  = alpha_ * gLowSmooth_  + (1.0f - alpha_) * gLow_;
    gMidSmooth_  = alpha_ * gMidSmooth_  + (1.0f - alpha_) * gMid_;
    gHighSmooth_ = alpha_ * gHighSmooth_ + (1.0f - alpha_) * gHigh_;

    // 2º: aplica aos três biquads e soma
    return  gLowSmooth_  * low_.process(x) +
            gMidSmooth_  * mid_.process(x) +
            gHighSmooth_ * high_.process(x);
}
void ThreeBandFilter::setRampMs(float ms)
{
    alpha_ = expf(-1.0f / ( (ms/1000.0f) * sampleRate_ ));
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

