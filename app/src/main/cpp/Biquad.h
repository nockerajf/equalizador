#pragma once
#include <cmath>
#include <android/log.h> // Keep include for logs outside hot loops

#define LOG_TAG "Biquad"
#ifndef NDEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...) ((void)0)
#define LOGE(...) ((void)0)
#endif



class Biquad {
public:
    enum Type {
        LOWPASS, HIGHPASS, BANDPASS
    };

    float z1 = 0.0f;
    float z2 = 0.0f;

    // Your member coefficients
    float a0 = 0.0f, a1 = 0.0f, a2 = 0.0f; // These are your b0, b1, b2
    float b1 = 0.0f, b2 = 0.0f;           // These are your a1, a2

    void setup(Type type, float Fs, float f0, float Q);

    inline float process(float x) {
        float y = a0 * x + z1; // a0 is your b0_std
        z1 = a1 * x - b1 * y + z2; // a1 is your b1_std, b1 is your a1_std
        z2 = a2 * x - b2 * y;      // a2 is your b2_std, b2 is your a2_std

        // Basic check for NaN/Inf (consider removing this check in release build for performance)
         if (isnan(y) || isinf(y)) {
           LOGE("Biquad::process: DETECTED NaN or INF! y=%.6f, x=%.6f, z1=%.6f, z2=%.6f", y, x, z1, z2);
         }

        return y;
    }

    // Reset state variables
    inline void reset() {
        z1 = z2 = 0.0f;
    }
};