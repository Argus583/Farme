package com.example.farme.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import com.example.farme.MainActivity;
import com.example.farme.R;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Экран ввода SMS-кода (OTP).
 * Получает phone и mode (login/register) из AuthActivity.
 * Использует Firebase Phone Authentication.
 */
public class OtpActivity extends AppCompatActivity {

    private TextView btnBack, tvOtpPhone, btnChangePhone;
    private EditText[] otpFields = new EditText[6];
    private TextView tvOtpError, tvResendTimer, tvTimerCount, btnResend;
    private LinearLayout btnVerify;
    private ProgressBar pbVerify;

    private String phone;
    private String mode;          // "login" / "register"
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private CountDownTimer timer;

    private FirebaseAuth      mAuth;
    private DatabaseReference mDatabase;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        phone = getIntent().getStringExtra("phone");
        mode  = getIntent().getStringExtra("mode");
        if (phone == null) phone = "";
        if (mode  == null) mode  = "login";

        initViews();
        setupOtpInputs();
        setupClickListeners();
        startVerification();
    }

    private void initViews() {
        btnBack         = findViewById(R.id.btnBack);
        tvOtpPhone      = findViewById(R.id.tvOtpPhone);
        btnChangePhone  = findViewById(R.id.btnChangePhone);
        otpFields[0]    = findViewById(R.id.otp1);
        otpFields[1]    = findViewById(R.id.otp2);
        otpFields[2]    = findViewById(R.id.otp3);
        otpFields[3]    = findViewById(R.id.otp4);
        otpFields[4]    = findViewById(R.id.otp5);
        otpFields[5]    = findViewById(R.id.otp6);
        tvOtpError      = findViewById(R.id.tvOtpError);
        tvResendTimer   = findViewById(R.id.tvResendTimer);
        tvTimerCount    = findViewById(R.id.tvTimerCount);
        btnResend       = findViewById(R.id.btnResend);
        btnVerify       = findViewById(R.id.btnVerify);
        pbVerify        = findViewById(R.id.pbVerify);

        tvOtpPhone.setText(formatPhone(phone));
    }

    private String formatPhone(String p) {
        if (p == null || p.length() < 7) return p;
        // +996700123456 → +996 700 123 456
        StringBuilder sb = new StringBuilder();
        int cc = p.startsWith("+") ? 4 : 3;
        sb.append(p.substring(0, cc)).append(" ");
        String rest = p.substring(cc);
        for (int i = 0; i < rest.length(); i++) {
            sb.append(rest.charAt(i));
            if ((i + 1) % 3 == 0 && i + 1 < rest.length()) sb.append(" ");
        }
        return sb.toString();
    }

    // ═══ OTP поля: автопереход между ними ════════════════
    private void setupOtpInputs() {
        for (int i = 0; i < otpFields.length; i++) {
            final int idx = i;
            EditText et = otpFields[i];

            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
                @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        // Автоподстановка кода (если пользователь вставил все 6 цифр)
                        if (idx == 0 && s.length() == 1) {
                            String paste = s.toString();
                            if (paste.length() > 1) {
                                fillOtpFromString(paste);
                                return;
                            }
                        }
                        // Переход к следующему
                        if (idx < otpFields.length - 1) {
                            otpFields[idx + 1].requestFocus();
                        } else {
                            // Последнее поле — автоподтверждение (только если код уже получен)
                            hideKeyboard();
                            if (verificationId != null) verifyCode();
                        }
                    }
                }
            });

            // Backspace → возврат к предыдущему
            et.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_DEL
                        && et.getText().length() == 0
                        && idx > 0) {
                    otpFields[idx - 1].requestFocus();
                    otpFields[idx - 1].setText("");
                    return true;
                }
                return false;
            });
        }

        otpFields[0].requestFocus();
    }

    private void fillOtpFromString(String code) {
        String digits = code.replaceAll("[^0-9]", "");
        for (int i = 0; i < Math.min(6, digits.length()); i++) {
            otpFields[i].setText(String.valueOf(digits.charAt(i)));
        }
        if (digits.length() >= 6) {
            hideKeyboard();
            verifyCode();
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnChangePhone.setOnClickListener(v -> finish());
        btnResend.setOnClickListener(v -> resendCode());
        btnVerify.setOnClickListener(v -> {
            if (!isLoading) verifyCode();
        });
    }

    // ═══ Firebase Phone Auth ═════════════════════════════
    private void startVerification() {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        startTimer();
    }

    private void resendCode() {
        if (resendToken == null) {
            startVerification();
            return;
        }
        clearOtpFields();
        clearError();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .setForceResendingToken(resendToken)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        startTimer();
        Toast.makeText(this, getString(R.string.code_resent), Toast.LENGTH_SHORT).show();
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Авто-подтверждение (некоторые устройства автоматически читают SMS)
                    String code = credential.getSmsCode();
                    if (code != null) fillOtpFromString(code);
                    signInWithCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    setLoading(false);
                    String msg = "Ошибка отправки SMS";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("invalid format")) {
                            msg = "Неверный формат номера";
                        } else if (e.getMessage().contains("quota")) {
                            msg = "Слишком много запросов. Попробуйте позже";
                        }
                    }
                    showError(msg);
                }

                @Override
                public void onCodeSent(@NonNull String vid,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = vid;
                    resendToken    = token;
                    // Если пользователь уже успел ввести код — проверяем сразу
                    StringBuilder filled = new StringBuilder();
                    for (EditText et : otpFields) filled.append(et.getText().toString());
                    if (filled.length() == 6) verifyCode();
                }
            };

    private void verifyCode() {
        StringBuilder code = new StringBuilder();
        for (EditText et : otpFields) code.append(et.getText().toString());

        if (code.length() != 6) {
            showError("Введите все 6 цифр");
            return;
        }
        if (verificationId == null) {
            showError("Ожидаем отправки SMS, подождите...");
            return;
        }

        setLoading(true);
        PhoneAuthCredential credential = PhoneAuthProvider
                .getCredential(verificationId, code.toString());
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    // Проверяем существует ли уже запись пользователя
                    mDatabase.child("users").child(uid).get()
                            .addOnSuccessListener(snap -> {
                                setLoading(false);
                                // Проверяем что регистрация была ЗАВЕРШЕНА полностью
                                // (имя + регион — это значит CompleteProfile прошёл успешно)
                                String firstName = snap.child("firstName").getValue(String.class);
                                String region = snap.child("region").getValue(String.class);
                                boolean isFullyRegistered = snap.exists()
                                        && firstName != null && !firstName.trim().isEmpty()
                                        && region != null && !region.trim().isEmpty();

                                if (isFullyRegistered) {
                                    // ✅ Существующий пользователь → главный экран
                                    Boolean banned = snap.child("banned").getValue(Boolean.class);
                                    if (Boolean.TRUE.equals(banned)) {
                                        mAuth.signOut();
                                        showError("Аккаунт заблокирован");
                                        return;
                                    }
                                    Intent i = new Intent(this, MainActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(i);
                                    finish();
                                } else {
                                    // ❌ Новый пользователь или регистрация не завершена
                                    Intent i = new Intent(this, CompleteProfileActivity.class);
                                    i.putExtra("authMethod", "phone");
                                    i.putExtra("phone", phone);
                                    startActivity(i);
                                    finish();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Неверный код подтверждения");
                });
    }

    // ═══ Timer ═══════════════════════════════════════════
    private void startTimer() {
        if (timer != null) timer.cancel();
        tvResendTimer.setVisibility(View.VISIBLE);
        tvTimerCount.setVisibility(View.VISIBLE);
        btnResend.setVisibility(View.GONE);

        timer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long ms) {
                long s = ms / 1000;
                tvTimerCount.setText(String.format("00:%02d", s));
            }
            @Override
            public void onFinish() {
                tvResendTimer.setVisibility(View.GONE);
                tvTimerCount.setVisibility(View.GONE);
                btnResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    // ═══ UI helpers ══════════════════════════════════════
    private void setLoading(boolean loading) {
        isLoading = loading;
        pbVerify.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerify.setAlpha(loading ? 0.7f : 1f);
    }

    private void clearOtpFields() {
        for (EditText et : otpFields) et.setText("");
        otpFields[0].requestFocus();
    }

    private void showError(String msg) {
        tvOtpError.setText(msg);
        tvOtpError.setVisibility(View.VISIBLE);
        for (EditText et : otpFields) {
            et.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
        }
    }

    private void clearError() {
        tvOtpError.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null && imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}