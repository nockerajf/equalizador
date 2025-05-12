#include "Biquad.h"
#include <cmath>
// Adicione include para logging
#include <android/log.h>

// Defina o TAG para logs desta classe (pode usar o mesmo do ThreeBandFilter ou um específico)
#define LOG_TAG "Biquad"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


void Biquad::setup(Type type, float Fs, float f0, float Q)
{
    // ... (código existente de setup) ...

    const float w0    = 2.0f * static_cast<float>(M_PI) * f0 / Fs;
    const float cosw0 = cosf(w0);
    const float sinw0 = sinf(w0);
    const float alpha = sinw0 / (2.0f * Q);

    float b0_temp, b1n, b2_temp; // Use nomes temporários para evitar conflito com membros da classe
    float a0n, a1n, a2n;

    switch (type) {
        case LOWPASS:
            b0_temp  =  (1.0f - cosw0) * 0.5f;
            b1n =   1.0f - cosw0;
            b2_temp  =  (1.0f - cosw0) * 0.5f;
            a0n =   1.0f + alpha;
            a1n =  -2.0f * cosw0;
            a2n =   1.0f - alpha;
            break;

        case HIGHPASS:
            b0_temp  =  (1.0f + cosw0) * 0.5f;
            b1n = -(1.0f + cosw0);
            b2_temp  =  (1.0f + cosw0) * 0.5f;
            a0n =   1.0f + alpha;
            a1n =  -2.0f * cosw0;
            a2n =   1.0f - alpha;
            break;

        case BANDPASS:
            b0_temp  =   sinw0 * 0.5f;
            b1n =   0.0f;
            b2_temp  =  -sinw0 * 0.5f;
            a0n =   1.0f + alpha;
            a1n =  -2.0f * cosw0;
            a2n =   1.0f - alpha;
            break;
    }

    // Normaliza para que a0 = 1
    // Cuidado: se a0n for 0, invA0 será Infinity e a0, a1, a2, b1, b2 se tornarão NaN/Inf
    if (a0n == 0.0f) {
        LOGE("Biquad::setup: Critical error: a0n is zero! f0=%f, Q=%f, Fs=%f", f0, Q, Fs);
        // Handle this error appropriately - perhaps set coefficients to a known safe state (e.g., passthrough)
        a0 = 1.0f; a1 = 0.0f; a2 = 0.0f;
        b1 = 0.0f; b2 = 0.0f;
    } else {
        const float invA0 = 1.0f / a0n;
        a0 = b0_temp  * invA0;
        a1 = b1n * invA0; // Note: This looks like a typo based on standard biquad forms (should be b1_temp?)
        a2 = b2_temp  * invA0; // Note: This looks like a typo (should be b2_temp?)
        b1 = a1n * invA0; // Note: This looks like a typo (should be a1n?)
        b2 = a2n * invA0; // Note: This looks like a typo (should be a2n?)

        // Based on the provided code's variable names:
        // Your code has a0, a1, a2, b1, b2 as member variables.
        // Standard biquad coefficients are usually called b0, b1, b2 (numerator)
        // and a1, a2 (denominator, a0 is normalized to 1).
        // It seems your member variables 'a0', 'a1', 'a2' correspond to standard 'b0', 'b1', 'b2'
        // and your member variables 'b1', 'b2' correspond to standard 'a1', 'a2'.
        // Let's add logs with this understanding.

        a0 = b0_temp  * invA0; // Corresponds to standard b0
        a1 = b1n * invA0;     // Corresponds to standard b1
        a2 = b2_temp  * invA0; // Corresponds to standard b2
        float a1_std = a1n * invA0; // Corresponds to standard a1
        float a2_std = a2n * invA0; // Corresponds to standard a2

        // Update your member variables based on your code's naming
        // Assuming your member variable 'b1' is standard 'a1' and 'b2' is standard 'a2'
        b1 = a1_std;
        b2 = a2_std;

        LOGD("Biquad::setup: Type=%d, Fs=%.1f, f0=%.1f, Q=%.3f", type, Fs, f0, Q);
        LOGD("Biquad::setup: w0=%.4f, cosw0=%.4f, sinw0=%.4f, alpha=%.4f", w0, cosw0, sinw0, alpha);
        LOGD("Biquad::setup: Coeffs (temp) - b0_temp=%.4f, b1n=%.4f, b2_temp=%.4f, a0n=%.4f, a1n=%.4f, a2n=%.4f",
             b0_temp, b1n, b2_temp, a0n, a1n, a2n);

        // Log final coefficients (using your member variable names)
        LOGD("Biquad::setup: Final Coeffs - a0=%.6f, a1=%.6f, a2=%.6f, b1=%.6f (std a1), b2=%.6f (std a2)",
             a0, a1, a2, this->b1, this->b2); // Access b1/b2 explicitly as members if needed

        // Add NaN/Inf checks for final coefficients
        if (isnan(a0) || isinf(a0) || isnan(this->a1) || isinf(this->a1) ||
            isnan(this->a2) || isinf(this->a2) || isnan(this->b1) || isinf(this->b1) || // Checking members b1, b2
            isnan(this->b2) || isinf(this->b2)) {
            LOGE("Biquad::setup: DETECTED NaN or INF in final coefficients!");
            // This is a critical error and explains NaNs in process.
        }
    }
}

float Biquad::process(float x)
{
    // Log input and initial states
    LOGD("Biquad::process: Input x=%.6f, Initial States z1=%.6f, z2=%.6f", x, z1, z2);

    // Log coefficients being used
    LOGD("Biquad::process: Coeffs a0=%.6f, a1=%.6f, a2=%.6f, b1=%.6f, b2=%.6f",
         a0, a1, a2, b1, b2);


    // s1 & s2 correspondem ao estado interno (z^-1) na forma Direct Form II Transposed
    // y[n] = b0*x + s1  --> In your code this seems to be y[n] = a0*x + z1
    // Your code's structure seems different from standard forms. Let's log intermediate steps.

    // --- Your Original Calculation ---
    // float y = a0 * x + z1; // <caret>

    // // Atualiza estados
    // z1 = a1 * x - b1 * y + z2;
    // z2 = a2 * x - b2 * y;
    // --- End Original Calculation ---

    // Let's add logging step-by-step through your formula:

    // Step 1: Calculate output contribution from input and first state
    float term1 = a0 * x;
    LOGD("Biquad::process: Term1 (a0 * x) = %.6f", term1);
    float y = term1 + z1;
    LOGD("Biquad::process: Output y (term1 + z1) = %.6f", y);

    // Step 2: Calculate first state update
    float z1_term1 = a1 * x;
    LOGD("Biquad::process: z1_term1 (a1 * x) = %.6f", z1_term1);
    float z1_term2 = b1 * y;
    LOGD("Biquad::process: z1_term2 (b1 * y) = %.6f", z1_term2);
    float new_z1 = z1_term1 - z1_term2 + z2;
    LOGD("Biquad::process: New z1 (z1_term1 - z1_term2 + z2) = %.6f", new_z1);


    // Step 3: Calculate second state update
    float z2_term1 = a2 * x;
    LOGD("Biquad::process: z2_term1 (a2 * x) = %.6f", z2_term1);
    float z2_term2 = b2 * y;
    LOGD("Biquad::process: z2_term2 (b2 * y) = %.6f", z2_term2);
    float new_z2 = z2_term1 - z2_term2;
    LOGD("Biquad::process: New z2 (z2_term1 - z2_term2) = %.6f", new_z2);


    // Update states
    z1 = new_z1;
    z2 = new_z2;

    // Check output and states for NaN/Inf after update
    if (isnan(y) || isinf(y)) {
        LOGE("Biquad::process: DETECTED NaN or INF in output y!");
    }
    if (isnan(z1) || isinf(z1)) {
        LOGE("Biquad::process: DETECTED NaN or INF in state z1!");
    }
    if (isnan(z2) || isinf(z2)) {
        LOGE("Biquad::process: DETECTED NaN or INF in state z2!");
    }


    LOGD("Biquad::process: Final States z1=%.6f, z2=%.6f, Returning y=%.6f", z1, z2, y);

    return y;
}

// Separado para evitar simbolização cruzada quando for inline


void Biquad::reset()
{
    z1 = z2 = 0.0f;
    LOGD("Biquad::reset: States reset to 0.0");
}