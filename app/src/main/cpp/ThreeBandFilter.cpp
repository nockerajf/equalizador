#include <math.h>
#include "ThreeBandFilter.h"
#include <android/log.h>
void ThreeBandFilter::init(float sampleRate,float lowCut,float midCenter,float highCut,float Q)
{
    if (sampleRate != 0)
        sampleRate_ = sampleRate;

    low_.setup (Biquad::LOWPASS,  sampleRate_, lowCut,    Q);
    mid_.setup (Biquad::BANDPASS, sampleRate_, midCenter, Q);
    high_.setup(Biquad::HIGHPASS, sampleRate_, highCut,   Q);
    low_.reset();
    mid_.reset();
    high_.reset();
    gLowSmooth_  = gLow_;
    gMidSmooth_  = gMid_;
    gHighSmooth_ = gHigh_;
    setRampMs(50.f);
}
float ThreeBandFilter::processSample(float x)
{
    gLowSmooth_  = alpha_ * gLowSmooth_  + (1.0f - alpha_) * gLow_;
    gMidSmooth_  = alpha_ * gMidSmooth_  + (1.0f - alpha_) * gMid_;
    gHighSmooth_ = alpha_ * gHighSmooth_ + (1.0f - alpha_) * gHigh_;

    float lowOut = low_.process(x);
    float midOut = mid_.process(x);
    float highOut = high_.process(x);

    if (isnan(lowOut) || isinf(lowOut) ||
        isnan(midOut) || isinf(midOut) ||
        isnan(highOut) || isinf(highOut)) {
        LOGE("processSample: DETECTED NaN or INF in Biquad output!");
    }
    float result = gLowSmooth_  * lowOut +
                   gMidSmooth_  * midOut +
                   gHighSmooth_ * highOut;
    if (isnan(result) || isinf(result)) {
        LOGE("processSample: DETECTED NaN or INF in final result!");
    }
    return result;
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

