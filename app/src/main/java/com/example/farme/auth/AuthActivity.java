package com.example.farme.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.farme.MainActivity;
import com.example.farme.R;

/**
 * Главный экран авторизации Farme.
 * Режимы:
 *  - mode = "login" / "register"
 *  - method = "phone" / "email"
 *
 * Phone-логика → переход на OtpActivity.
 * Email-логика → стандартный signIn / createUser.
 */
public class AuthActivity extends com.example.farme.BaseActivity {

    private TextView tabLogin, tabRegister;
    private LinearLayout methodPhone, methodEmail;
    private TextView methodPhoneLabel, methodEmailLabel;

    private LinearLayout loginPhoneGroup, loginEmailGroup,
            loginPasswordGroup, rememberGroup;
    private LinearLayout countryCodeBtn;
    private TextView tvCountryFlag, tvCountryCode;

    private EditText etLoginPhone, etLoginEmail, etLoginPassword;
    private TextView tvLoginPhoneError, tvLoginEmailError,
            tvLoginPasswordError, tvFormError, tvFormTitle,
            tvFormSubtitle, tvPhoneHint;
    private TextView btnTogglePass, btnForgotPassword;
    private LinearLayout btnPrimary;
    private TextView tvBtnPrimary;
    private ProgressBar pbBtnLoading;
    private CheckBox cbRemember;

    private String mode   = "login";
    private String method = "phone";
    private String selectedCountryCode = "+996";
    private String selectedCountryFlag = "🇰🇬";
    private boolean isLoading = false;

    private FirebaseAuth      mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupClickListeners();
        updateUI();
    }

    private void initViews() {
        tabLogin    = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);
        methodPhone = findViewById(R.id.methodPhone);
        methodEmail = findViewById(R.id.methodEmail);
        methodPhoneLabel = findViewById(R.id.methodPhoneLabel);
        methodEmailLabel = findViewById(R.id.methodEmailLabel);

        loginPhoneGroup    = findViewById(R.id.loginPhoneGroup);
        loginEmailGroup    = findViewById(R.id.loginEmailGroup);
        loginPasswordGroup = findViewById(R.id.loginPasswordGroup);
        rememberGroup      = findViewById(R.id.rememberGroup);
        countryCodeBtn     = findViewById(R.id.countryCodeBtn);
        tvCountryFlag      = findViewById(R.id.tvCountryFlag);
        tvCountryCode      = findViewById(R.id.tvCountryCode);

        etLoginPhone     = findViewById(R.id.etLoginPhone);
        etLoginEmail     = findViewById(R.id.etLoginEmail);
        etLoginPassword  = findViewById(R.id.etLoginPassword);

        tvLoginPhoneError    = findViewById(R.id.tvLoginPhoneError);
        tvLoginEmailError    = findViewById(R.id.tvLoginEmailError);
        tvLoginPasswordError = findViewById(R.id.tvLoginPasswordError);
        tvFormError    = findViewById(R.id.tvFormError);
        tvFormTitle    = findViewById(R.id.tvFormTitle);
        tvFormSubtitle = findViewById(R.id.tvFormSubtitle);
        tvPhoneHint    = findViewById(R.id.tvPhoneHint);

        btnTogglePass     = findViewById(R.id.btnTogglePass);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnPrimary        = findViewById(R.id.btnPrimary);
        tvBtnPrimary      = findViewById(R.id.tvBtnPrimary);
        pbBtnLoading      = findViewById(R.id.pbBtnLoading);
        cbRemember        = findViewById(R.id.cbRemember);
    }

    private void setupClickListeners() {
        tabLogin.setOnClickListener(v -> setMode("login"));
        tabRegister.setOnClickListener(v -> setMode("register"));
        methodPhone.setOnClickListener(v -> setMethod("phone"));
        methodEmail.setOnClickListener(v -> setMethod("email"));

        countryCodeBtn.setOnClickListener(v -> showCountryPicker());

        btnTogglePass.setOnClickListener(v -> togglePasswordVisibility());

        btnForgotPassword.setOnClickListener(v -> {
            Intent i = new Intent(this, ForgotPasswordActivity.class);
            startActivity(i);
        });

        btnPrimary.setOnClickListener(v -> {
            if (isLoading) return;
            handlePrimaryAction();
        });
    }

    // ═══ Переключение режимов ═══════════════════════════
    private void setMode(String newMode) {
        mode = newMode;
        if ("login".equals(mode)) {
            tabLogin.setBackgroundResource(R.drawable.bg_tab_active);
            tabLogin.setTextColor(0xFFFFFFFF);
            tabRegister.setBackgroundResource(android.R.color.transparent);
            tabRegister.setTextColor(0xFF6B7280);
        } else {
            tabRegister.setBackgroundResource(R.drawable.bg_tab_active);
            tabRegister.setTextColor(0xFFFFFFFF);
            tabLogin.setBackgroundResource(android.R.color.transparent);
            tabLogin.setTextColor(0xFF6B7280);
        }
        updateUI();
        clearErrors();
    }

    private void setMethod(String newMethod) {
        method = newMethod;
        if ("phone".equals(method)) {
            methodPhone.setBackgroundResource(R.drawable.bg_method_active);
            methodPhoneLabel.setTextColor(0xFF0A1F14);
            methodPhoneLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            methodEmail.setBackgroundResource(R.drawable.bg_method_inactive);
            methodEmailLabel.setTextColor(0xFF6B7280);
            methodEmailLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            methodEmail.setBackgroundResource(R.drawable.bg_method_active);
            methodEmailLabel.setTextColor(0xFF0A1F14);
            methodEmailLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            methodPhone.setBackgroundResource(R.drawable.bg_method_inactive);
            methodPhoneLabel.setTextColor(0xFF6B7280);
            methodPhoneLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        updateUI();
        clearErrors();
    }

    private void updateUI() {
        boolean isPhone = "phone".equals(method);
        boolean isLogin = "login".equals(mode);

        // Заголовки
        if (isLogin) {
            tvFormTitle.setText(getString(R.string.welcome));
            tvFormSubtitle.setText(getString(R.string.login_subtitle));
            tvBtnPrimary.setText(isPhone ? getString(R.string.btn_get_code) : getString(R.string.login));
        } else {
            tvFormTitle.setText(getString(R.string.create_account_title));
            tvFormSubtitle.setText(isPhone
                    ? getString(R.string.verify_phone_subtitle)
                    : getString(R.string.register_email_subtitle));
            tvBtnPrimary.setText(isPhone ? getString(R.string.btn_get_code) : getString(R.string.btn_continue));
        }

        // Видимость полей
        loginPhoneGroup.setVisibility(isPhone ? View.VISIBLE : View.GONE);
        loginEmailGroup.setVisibility(isPhone ? View.GONE : View.VISIBLE);

        // Пароль показываем только при email + login
        // (для register email — пароль будет на следующем экране)
        boolean showPassword = !isPhone && isLogin;
        loginPasswordGroup.setVisibility(showPassword ? View.VISIBLE : View.GONE);
        rememberGroup.setVisibility(showPassword ? View.VISIBLE : View.GONE);

        // Подсказка
        if (isPhone) {
            tvPhoneHint.setVisibility(View.VISIBLE);
            tvPhoneHint.setText(getString(R.string.phone_verification_hint));
        } else if (!isLogin) {
            tvPhoneHint.setVisibility(View.VISIBLE);
            tvPhoneHint.setText(getString(R.string.email_verification_hint));
        } else {
            tvPhoneHint.setVisibility(View.GONE);
        }
    }

    // ═══ Обработка нажатия главной кнопки ════════════════
    private void handlePrimaryAction() {
        clearErrors();
        if ("phone".equals(method)) {
            handlePhone();
        } else {
            if ("login".equals(mode)) handleEmailLogin();
            else handleEmailRegister();
        }
    }

    // ─── Phone: отправка OTP ─────────────────────────────
    private void handlePhone() {
        String rawPhone = etLoginPhone.getText().toString().trim();
        String digits   = rawPhone.replaceAll("[^0-9]", "");

        if (digits.isEmpty()) {
            showFieldError(tvLoginPhoneError, etLoginPhone, getString(R.string.error_enter_phone));
            return;
        }
        if (digits.length() < 7 || digits.length() > 12) {
            showFieldError(tvLoginPhoneError, etLoginPhone, getString(R.string.error_invalid_phone_number));
            return;
        }

        String fullPhone = selectedCountryCode + digits;

        // Переход на OtpActivity
        Intent i = new Intent(this, OtpActivity.class);
        i.putExtra("phone", fullPhone);
        i.putExtra("mode", mode); // login / register
        startActivity(i);
    }

    // ─── Email: вход ─────────────────────────────────────
    private void handleEmailLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String pass  = etLoginPassword.getText().toString();

        if (email.isEmpty()) {
            showFieldError(tvLoginEmailError, etLoginEmail, getString(R.string.error_enter_email));
            return;
        }
        if (!isValidEmail(email)) {
            showFieldError(tvLoginEmailError, etLoginEmail, getString(R.string.error_invalid_email));
            return;
        }
        if (pass.length() < 6) {
            showFieldError(tvLoginPasswordError, etLoginPassword,
                    getString(R.string.error_short_password));
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> checkUserAndNavigate())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showFormError(getString(R.string.error_wrong_credentials));
                });
    }

    // ─── Email: регистрация — переход на CompleteProfile ─
    private void handleEmailRegister() {
        String email = etLoginEmail.getText().toString().trim();

        if (email.isEmpty()) {
            showFieldError(tvLoginEmailError, etLoginEmail, getString(R.string.error_enter_email));
            return;
        }
        if (!isValidEmail(email)) {
            showFieldError(tvLoginEmailError, etLoginEmail, getString(R.string.error_invalid_email));
            return;
        }

        Intent i = new Intent(this, CompleteProfileActivity.class);
        i.putExtra("authMethod", "email");
        i.putExtra("email", email);
        startActivity(i);
    }

    private void checkUserAndNavigate() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(uid).get()
                .addOnSuccessListener(snap -> {
                    setLoading(false);
                    Boolean banned = snap.child("banned").getValue(Boolean.class);
                    if (Boolean.TRUE.equals(banned)) {
                        mAuth.signOut();
                        showFormError(getString(R.string.error_account_banned));
                        return;
                    }
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                });
    }

    // ═══ Helpers ═════════════════════════════════════════
    private void togglePasswordVisibility() {
        boolean isHidden = etLoginPassword.getTransformationMethod()
                instanceof PasswordTransformationMethod;
        etLoginPassword.setTransformationMethod(isHidden
                ? null : new PasswordTransformationMethod());
        btnTogglePass.setText(isHidden ? "🙈" : "👁");
        etLoginPassword.setSelection(etLoginPassword.getText().length());
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        if (loading) {
            pbBtnLoading.setVisibility(View.VISIBLE);
            tvBtnPrimary.setText(getString(R.string.loading));
            btnPrimary.setAlpha(0.7f);
        } else {
            pbBtnLoading.setVisibility(View.GONE);
            updateUI();
            btnPrimary.setAlpha(1f);
        }
    }

    private void showFieldError(TextView errorView, View input, String msg) {
        errorView.setText(msg);
        errorView.setVisibility(View.VISIBLE);
        input.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }

    private void showFormError(String msg) {
        tvFormError.setText(msg);
        tvFormError.setVisibility(View.VISIBLE);
        btnPrimary.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }

    private void clearErrors() {
        tvLoginPhoneError.setVisibility(View.GONE);
        tvLoginEmailError.setVisibility(View.GONE);
        tvLoginPasswordError.setVisibility(View.GONE);
        tvFormError.setVisibility(View.GONE);
    }

    // ═══ Country picker ═════════════════════════════════
    private void showCountryPicker() {
        String[] countries = {
                "🇰🇬  Кыргызстан (+996)",
                "🇰🇿  Казахстан (+7)",
                "🇷🇺  Россия (+7)",
                "🇺🇿  Узбекистан (+998)",
                "🇹🇯  Таджикистан (+992)",
                "🇹🇲  Туркменистан (+993)",
                "🇨🇳  Китай (+86)",
                "🇹🇷  Турция (+90)",
                "🇺🇸  США (+1)"
        };
        String[] codes = {"+996", "+7", "+7", "+998", "+992", "+993", "+86", "+90", "+1"};
        String[] flags = {"🇰🇬", "🇰🇿", "🇷🇺", "🇺🇿", "🇹🇯", "🇹🇲", "🇨🇳", "🇹🇷", "🇺🇸"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_country))
                .setItems(countries, (dialog, which) -> {
                    selectedCountryCode = codes[which];
                    selectedCountryFlag = flags[which];
                    tvCountryCode.setText(selectedCountryCode);
                    tvCountryFlag.setText(selectedCountryFlag);
                })
                .show();
    }
}