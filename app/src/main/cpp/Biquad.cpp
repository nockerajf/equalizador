#include "Biquad.h"
#include <cmath>

// Ensure LOGD/LOGE macros are defined as in the header
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
        LOGE("Biquad::setup: Invalid parameters! Fs=%.1f, f0=%.1f, Q=%.3f", Fs, f0, Q);
        // Set coefficients to passthrough or a safe default in case of invalid input
        a0 = 1.0f; a1 = 0.0f; a2 = 0.0f;
        b1 = 0.0f; b2 = 0.0f;
        return; // Exit setup
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

        case BANDPASS: // Peak/notch gain is related to Q in standard formulas, but for bandpass magnitude |H(exp(jw0))| = Q
            // The provided formula for b0/b2 seems to be specific for a Bandpass with peak gain Q at w0.
            // For unity gain at peak, b0 = sinw0 / 2.
            // Assuming your intent is indeed a bandpass filter based on your formula:
            b0_std  =   alpha; // Standard formula for bandpass with peak gain = Q
            b1_std =   0.0f;
            b2_std  =  -alpha; // Standard formula for bandpass
            a0_std =   1.0f + alpha;
            a1_std =  -2.0f * cosw0;
            a2_std =   1.0f - alpha;
            break;
    }

    // Normalize by standard a0
    if (a0_std == 0.0f) {
        //LOGE("Biquad::setup: Critical error: a0_std is zero! f0=%f, Q=%f, Fs=%f", f0, Q, Fs);
        // Handle this error appropriately - set to passthrough
        a0 = 1.0f; a1 = 0.0f; a2 = 0.0f;
        b1 = 0.0f; b2 = 0.0f;
    } else {
        const float invA0_std = 1.0f / a0_std;
        // Map standard coefficients to your member variables based on your processing logic:
        // y[n] = a0*x[n] + a1*x[n-1] + a2*x[n-2] - b1*y[n-1] - b2*y[n-2]
        // Compared to standard: y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
        // So your a0 -> b0_std, your a1 -> b1_std, your a2 -> b2_std, your b1 -> a1_std, your b2 -> a2_std

        a0 = b0_std * invA0_std; // Your b0
        a1 = b1_std * invA0_std; // Your b1
        a2 = b2_std * invA0_std; // Your b2
        b1 = a1_std * invA0_std; // Your a1
        b2 = a2_std * invA0_std; // Your a2

        //LOGD("Biquad::setup: Type=%d, Fs=%.1f, f0=%.1f, Q=%.3f", type, Fs, f0, Q);
        //LOGD("Biquad::setup: w0=%.4f, cosw0=%.4f, sinw0=%.4f, alpha=%.4f", w0, cosw0, sinw0, alpha);
        //LOGD("Biquad::setup: Coeffs (standard) - b0=%.4f, b1=%.4f, b2=%.4f, a0=%.4f, a1=%.4f, a2=%.4f",
             b0_std, b1_std, b2_std, a0_std, a1_std, a2_std);

        // Log final coefficients (using your member variable names)
       // LOGD("Biquad::setup: Final Coeffs - a0=%.6f, a1=%.6f, a2=%.6f, b1=%.6f, b2=%.6f",
             this->a0, this->a1, this->a2, this->b1, this->b2);

        // Add NaN/Inf checks for final coefficients
        if (isnan(this->a0) || isinf(this->a0) || isnan(this->a1) || isinf(this->a1) ||
            isnan(this->a2) || isinf(this->a2) || isnan(this->b1) || isinf(this->b1) ||
            isnan(this->b2) || isinf(this->b2)) {
            // LOGE("Biquad::setup: DETECTED NaN or INF in final coefficients!");
            // This is a critical error and explains NaNs in process.
            // Could reset coefficients here as well.
        }
    }

    reset(); // Reset states after setup
}

// The 'process' function definition is now in the header as inline.
// void Biquad::process(float x) { ... } // REMOVE this from the .cpp

// The 'reset' function definition is now in the header as inline.
// void Biquad::reset() { ... } // REMOVE this from the .cpp