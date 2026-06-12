package com.example.farme;

import android.app.Activity;

/**
 * Stub for Firebase Phone Number Verification (PNV), which requires a private
 * Google SDK not available in the public Maven repository.
 * Always reports "not supported", so AuthActivity falls back to the SMS OTP path.
 */
public class PhoneVerificationHelper {

    public interface OnSupportedListener {
        void onResult(boolean supported);
    }

    public interface OnVerifiedListener {
        void onVerified(String phoneNumber, String token);
    }

    public interface OnFailedListener {
        void onFailed(Exception e);
    }

    private static PhoneVerificationHelper instance;

    private PhoneVerificationHelper() {}

    public static synchronized PhoneVerificationHelper getInstance() {
        if (instance == null) instance = new PhoneVerificationHelper();
        return instance;
    }

    public void enableTestSession(String testToken) {
        // no-op: PNV SDK not available
    }

    public void checkSupport(OnSupportedListener listener) {
        listener.onResult(false);
    }

    public void verify(Activity activity, OnVerifiedListener onVerified, OnFailedListener onFailed) {
        onFailed.onFailed(new UnsupportedOperationException("PNV SDK not available"));
    }
}
