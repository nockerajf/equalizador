#include "Biquad.h"
#include <cmath>

#ifndef NDEBUG
#define LOG_TAG "Biquad"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...) ((void)0)
#define LOGE(...) ((void)0)
#endif
void Biquad::setup(Type type, float Fs, float f0, float Q)
{
    if (Fs <= 0.0f || f0 <= 0.0f || f0 >= Fs * 0.5f || Q <= 0.0f) {

        a0 = 1.0f; a1 = 0.0f; a2 = 0.0f;
        b1 = 0.0f; b2 = 0.0f;
        return;
    }
    const float w0    = 2.0f * static_cast<float>(M_PI) * f0 / Fs;
    const float cosw0 = cosf(w0);
    const float sinw0 = sinf(w0);
    const float alpha = sinw0 / (2.0f * Q); // Bandwidth

    float b0_std, b1_std, b2_std; // Standard numerator coefficients
    float a0_std, a1_std, a2_std; // Standard denominator coefficients

    switch (type) {
        case LOWPASS:
            b0_std  =  (1.0f - cosw0) * 0.5f;
            b1_std =   1.0f - cosw0;
            b2_std  =  (1.0f - cosw0) * 0.5f;
            a0_std =   1.0f + alpha;
            a1_std =  -2.0f * cosw0;
            a2_std =   1.0f - alpha;
            break;

        case HIGHPASS:
            b0_std  =  (1.0f + cosw0) * 0.5f;
            b1_std = -(1.0f + cosw0);
            b2_std  =  (1.0f + cosw0) * 0.5f;
            a0_std =   1.0f + alpha;
            a1_std =  -2.0f * cosw0;
            a2_std =   1.0f - alpha;
            break;

        case BANDPASS:
            b0_std  =   alpha;
            b1_std =   0.0f;
            b2_std  =  -alpha;
            a0_std =   1.0f + alpha;
            a1_std =  -2.0f * cosw0;
            a2_std =   1.0f - alpha;
            break;
    }
    if (a0_std == 0.0f) {
        a0 = 1.0f; a1 = 0.0f; a2 = 0.0f;
        b1 = 0.0f; b2 = 0.0f;
    } else {
        const float invA0_std = 1.0f / a0_std;

        a0 = b0_std * invA0_std; // Your b0
        a1 = b1_std * invA0_std; // Your b1
        a2 = b2_std * invA0_std; // Your b2
        b1 = a1_std * invA0_std; // Your a1
        b2 = a2_std * invA0_std; // Your a2

        if (isnan(this->a0) || isinf(this->a0) || isnan(this->a1) || isinf(this->a1) ||
            isnan(this->a2) || isinf(this->a2) || isnan(this->b1) || isinf(this->b1) ||
            isnan(this->b2) || isinf(this->b2)) {
            LOGE("Biquad::setup: DETECTED NaN or INF in final coefficients!");
        }
    }
    reset(); // Reset states after setup
}