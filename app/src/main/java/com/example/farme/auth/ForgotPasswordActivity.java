package com.example.farme.auth;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.farme.R;

public class ForgotPasswordActivity extends com.example.farme.BaseActivity {

    private TextView btnBack, tvForgotError;
    private EditText etEmail;
    private LinearLayout btnSendReset, successCard;
    private ProgressBar pbForgot;

    private FirebaseAuth mAuth;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        btnBack       = findViewById(R.id.btnBack);
        etEmail       = findViewById(R.id.etEmail);
        tvForgotError = findViewById(R.id.tvForgotError);
        btnSendReset  = findViewById(R.id.btnSendReset);
        successCard   = findViewById(R.id.successCard);
        pbForgot      = findViewById(R.id.pbForgot);

        btnBack.setOnClickListener(v -> finish());
        btnSendReset.setOnClickListener(v -> {
            if (!isLoading) sendReset();
        });
    }

    private void sendReset() {
        clearError();
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            showError(getString(R.string.error_enter_email));
            shake(etEmail);
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email));
            shake(etEmail);
            return;
        }

        setLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(r -> {
                    setLoading(false);
                    successCard.setVisibility(View.VISIBLE);
                    btnSendReset.setVisibility(View.GONE);
                    etEmail.setEnabled(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = getString(R.string.error_send);
                    if (e.getMessage() != null && e.getMessage().contains("no user")) {
                        msg = getString(R.string.error_email_not_found);
                    }
                    showError(msg);
                });
    }

    private void setLoading(boolean l) {
        isLoading = l;
        pbForgot.setVisibility(l ? View.VISIBLE : View.GONE);
        btnSendReset.setAlpha(l ? 0.7f : 1f);
    }
    private void showError(String m) {
        tvForgotError.setText(m);
        tvForgotError.setVisibility(View.VISIBLE);
    }
    private void clearError() { tvForgotError.setVisibility(View.GONE); }
    private void shake(View v) {
        v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }
}