#pragma once
#include <cmath>
#include <android/log.h> // Keep include for logs outside hot loops

#define LOG_TAG "Biquad"
// Define LOGD/LOGE to compile away if NDEBUG is defined (add -DNDEBUG to compile flags)
#ifndef NDEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...) ((void)0)
#define LOGE(...) ((void)0)
#endif


/*
 * Biquad filter - Direct Form II Transposed structure.
 * Implements y[n] = (b0/a0)*x[n] + (b1/a0)*x[n-1] + (b2/a0)*x[n-2]
 *                       - (a1/a0)*y[n-1] - (a2/a0)*y[n-2]
 * (assuming a0 is normalized to 1)
 *
 * In your code's variable names:
 * y[n] = a0*x[n] + a1*x[n-1] + a2*x[n-2] - b1*y[n-1] - b2*y[n-2]
 * Where your member variables 'a0, a1, a2' are standard 'b0, b1, b2' (normalized by a0_std),
 * and your member variables 'b1, b2' are standard 'a1, a2' (normalized by a0_std).
 *
 */
class Biquad {
public:
    enum Type {
        LOWPASS, HIGHPASS, BANDPASS
    };

    // Coefficients (standard names for clarity below, map to your members in .cpp)
    // float b0, b1, b2, a1, a2;

    // State variables for Direct Form II Transposed
    float z1 = 0.0f;
    float z2 = 0.0f;

    // Your member coefficients
    float a0 = 0.0f, a1 = 0.0f, a2 = 0.0f; // These are your b0, b1, b2
    float b1 = 0.0f, b2 = 0.0f;           // These are your a1, a2


    // Setup filter coefficients. Normalize so standard a0=1.
    // Uses your variable naming internally.
    void setup(Type type, float Fs, float f0, float Q);

    // Processes a single sample. Make it inline.
    // This function is the inner loop and needs to be fast.
    inline float process(float x) {
        // Based on your code's DF2T implementation and variable mapping
        float y = a0 * x + z1; // a0 is your b0_std

        // Update states
        z1 = a1 * x - b1 * y + z2; // a1 is your b1_std, b1 is your a1_std
        z2 = a2 * x - b2 * y;      // a2 is your b2_std, b2 is your a2_std

        // Basic check for NaN/Inf (consider removing this check in release build for performance)
        // if (isnan(y) || isinf(y)) {
        //    LOGE("Biquad::process: DETECTED NaN or INF! y=%.6f, x=%.6f, z1=%.6f, z2=%.6f", y, x, z1, z2);
        // You might want to return 0.0f or handle this error
        //    return 0.0f; // Example handling
        // }


        return y;
    }


    // Reset state variables
    inline void reset() {
        z1 = z2 = 0.0f;
        // LOGD("Biquad::reset: States reset to 0.0"); // Keep log if desired, outside hot path
    }
};