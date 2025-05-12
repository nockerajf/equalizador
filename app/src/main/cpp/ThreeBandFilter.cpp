#include <math.h>
#include "ThreeBandFilter.h"
#include <android/log.h>
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
    // 1º: atualiza os ganhos suavizados
    gLowSmooth_  = alpha_ * gLowSmooth_  + (1.0f - alpha_) * gLow_;
    gMidSmooth_  = alpha_ * gMidSmooth_  + (1.0f - alpha_) * gMid_;
    gHighSmooth_ = alpha_ * gHighSmooth_ + (1.0f - alpha_) * gHigh_;

    LOGD("processSample: Input x=%.6f", x);
    LOGD("processSample: Smooth Gains - Low=%.6f, Mid=%.6f, High=%.6f", gLowSmooth_, gMidSmooth_, gHighSmooth_);

    // 2º: aplica aos três biquads e soma
    float lowOut = low_.process(x);
    float midOut = mid_.process(x);
    float highOut = high_.process(x);

    //LOGD("processSample: Biquad Outputs - Low=%.6f, Mid=%.6f, High=%.6f", lowOut, midOut, highOut);

    // Add checks here!
    if (isnan(lowOut) || isinf(lowOut) || // Removido 'std::'
        isnan(midOut) || isinf(midOut) || // Removido 'std::'
        isnan(highOut) || isinf(highOut)) { // Removido 'std::'
        //LOGE("processSample: DETECTED NaN or INF in Biquad output!");
        // You found the source! Now need to debug Biquad::process
    }


    float result = gLowSmooth_  * lowOut +
                   gMidSmooth_  * midOut +
                   gHighSmooth_ * highOut;

    //LOGD("processSample: Final Result = %.6f", result);

    if (isnan(result) || isinf(result)) { // Removido 'std::'
        //LOGE("processSample: DETECTED NaN or INF in final result!");
        // This should match the NaN seen in Kotlin
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

