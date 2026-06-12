package com.example.farme.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.*;
import com.example.farme.MainActivity;
import com.example.farme.R;
import java.util.HashMap;
import java.util.Map;

/**
 * Завершение регистрации.
 * Вызывается после успешной OTP-проверки, ввода email или Google Sign-In.
 *
 * extras:
 *   authMethod = "phone" / "email" / "google"
 *   phone = +996...  (для phone)
 *   email = ...      (для email / google)
 *   name  = ...      (для google — предзаполнение имени)
 */
public class CompleteProfileActivity extends com.example.farme.BaseActivity {

    private TextView btnBack, btnTogglePass, tvCompleteError;
    private EditText etFirstName, etLastName, etPassword, etConfirmPassword;
    private Spinner spinnerRegion;
    private LinearLayout passwordGroup, confirmPasswordGroup, btnComplete;
    private CheckBox cbTerms;
    private ProgressBar pbComplete;

    private String authMethod;
    private String phone;
    private String email;
    private boolean isLoading = false;

    private FirebaseAuth      mAuth;
    private DatabaseReference mDatabase;

    private final String[] REGIONS = {
            "Выберите регион",
            "Чуйская область", "Иссык-Кульская область",
            "Ошская область", "Джалал-Абадская область",
            "Нарынская область", "Баткенская область",
            "Таласская область", "г. Бишкек", "г. Ош"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        authMethod = getIntent().getStringExtra("authMethod");
        phone      = getIntent().getStringExtra("phone");
        email      = getIntent().getStringExtra("email");
        if (authMethod == null) authMethod = "phone";

        initViews();
        setupSpinner();
        setupClickListeners();
        configureForMethod();

        // Предзаполнить имя для Google-пользователей
        if ("google".equals(authMethod)) {
            String fullName = getIntent().getStringExtra("name");
            if (fullName != null && !fullName.isEmpty()) {
                String[] parts = fullName.trim().split(" ", 2);
                etFirstName.setText(parts[0]);
                if (parts.length > 1) etLastName.setText(parts[1]);
            }
        }
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);
        btnTogglePass      = findViewById(R.id.btnTogglePass);
        tvCompleteError    = findViewById(R.id.tvCompleteError);
        etFirstName        = findViewById(R.id.etFirstName);
        etLastName         = findViewById(R.id.etLastName);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        spinnerRegion      = findViewById(R.id.spinnerRegion);
        passwordGroup      = findViewById(R.id.passwordGroup);
        confirmPasswordGroup = findViewById(R.id.confirmPasswordGroup);
        btnComplete        = findViewById(R.id.btnComplete);
        cbTerms            = findViewById(R.id.cbTerms);
        pbComplete         = findViewById(R.id.pbComplete);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, REGIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRegion.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnTogglePass.setOnClickListener(v -> togglePasswordVisibility());
        btnComplete.setOnClickListener(v -> {
            if (!isLoading) handleComplete();
        });
    }

    private void configureForMethod() {
        if ("google".equals(authMethod)) {
            // Пользователь уже залогинен через Google — пароль не нужен
            passwordGroup.setVisibility(View.GONE);
            confirmPasswordGroup.setVisibility(View.GONE);
        } else {
            passwordGroup.setVisibility(View.VISIBLE);
            confirmPasswordGroup.setVisibility(View.VISIBLE);
        }
    }

    // ═══ Validation + сохранение ═════════════════════════
    private void handleComplete() {
        clearError();

        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String password  = etPassword.getText().toString();
        String confirm   = etConfirmPassword.getText().toString();
        int regionIdx    = spinnerRegion.getSelectedItemPosition();

        if (firstName.isEmpty()) {
            showError(getString(R.string.error_enter_name));
            etFirstName.requestFocus();
            shake(etFirstName);
            return;
        }
        if (lastName.isEmpty()) {
            showError(getString(R.string.error_enter_last_name));
            etLastName.requestFocus();
            shake(etLastName);
            return;
        }
        if (regionIdx == 0) {
            showError(getString(R.string.select_region));
            shake(spinnerRegion);
            return;
        }
        if (!"google".equals(authMethod)) {
            if (password.length() < 6) {
                showError(getString(R.string.error_short_password));
                shake(etPassword);
                return;
            }
            if (!password.equals(confirm)) {
                showError(getString(R.string.error_passwords_match));
                shake(etConfirmPassword);
                return;
            }
        }
        if (!cbTerms.isChecked()) {
            showError(getString(R.string.error_accept_terms));
            shake(cbTerms);
            return;
        }

        String region = REGIONS[regionIdx];
        setLoading(true);

        if ("phone".equals(authMethod)) {
            savePhoneUser(firstName, lastName, region, password);
        } else if ("google".equals(authMethod)) {
            saveGoogleUser(firstName, lastName, region);
        } else {
            createEmailUser(firstName, lastName, region, password);
        }
    }

    // ─── Phone-flow: пользователь уже залогинен ─────────
    private void savePhoneUser(String firstName, String lastName,
                               String region, String password) {
        if (mAuth.getCurrentUser() == null) {
            setLoading(false);
            showError(getString(R.string.error_session_lost));
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("phone", phone);
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("name", firstName + " " + lastName);
        data.put("region", region);
        data.put("role", "user");
        data.put("status", "active");
        data.put("verifiedPhone", true);
        data.put("verifiedEmail", false);
        data.put("rating", 0);
        data.put("createdAt", System.currentTimeMillis());

        // Сохраняем пароль локально + создаём связку email→pass для будущих email-логинов
        // Конвертируем телефон в "fake" email чтобы можно было войти и через email/pass
        String fakeEmail = phone.replaceAll("[^0-9]", "") + "@farme.kg";

        // Обновляем профиль в Auth
        mAuth.getCurrentUser().updateProfile(
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(firstName + " " + lastName)
                        .build()
        );

        // ВАЖНО: Firebase Phone Auth не позволяет напрямую установить пароль.
        // Решение: связать с email/password провайдером отдельно
        // Но это требует дополнительной логики (linkWithCredential)
        // Для MVP сохраняем пароль в БД (хешировать на проде!)
        data.put("authMethod", "phone");

        mDatabase.child("users").child(uid).setValue(data)
                .addOnSuccessListener(r -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.account_created), Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(getString(R.string.error_save) + ": " + e.getMessage());
                });
    }

    // ─── Google-flow: пользователь уже залогинен ────────────
    private void saveGoogleUser(String firstName, String lastName, String region) {
        if (mAuth.getCurrentUser() == null) {
            setLoading(false);
            showError(getString(R.string.error_session_lost));
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();

        mAuth.getCurrentUser().updateProfile(
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(firstName + " " + lastName)
                        .build()
        );

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", email != null ? email : "");
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("name", firstName + " " + lastName);
        data.put("region", region);
        data.put("role", "user");
        data.put("status", "active");
        data.put("verifiedPhone", false);
        data.put("verifiedEmail", true);
        data.put("authMethod", "google");
        data.put("rating", 0);
        data.put("createdAt", System.currentTimeMillis());

        mDatabase.child("users").child(uid).setValue(data)
                .addOnSuccessListener(r -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.account_created), Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(getString(R.string.error_save) + ": " + e.getMessage());
                });
    }

    // ─── Email-flow: создаём аккаунт + сохраняем профиль ─
    private void createEmailUser(String firstName, String lastName,
                                 String region, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    // Отправляем письмо для верификации
                    result.getUser().sendEmailVerification();

                    // Обновляем displayName
                    result.getUser().updateProfile(
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(firstName + " " + lastName)
                                    .build()
                    );

                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("email", email);
                    data.put("firstName", firstName);
                    data.put("lastName", lastName);
                    data.put("name", firstName + " " + lastName);
                    data.put("region", region);
                    data.put("role", "user");
                    data.put("status", "active");
                    data.put("verifiedPhone", false);
                    data.put("verifiedEmail", false);
                    data.put("authMethod", "email");
                    data.put("rating", 0);
                    data.put("createdAt", System.currentTimeMillis());

                    mDatabase.child("users").child(uid).setValue(data)
                            .addOnSuccessListener(r -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        getString(R.string.account_created_email),
                                        Toast.LENGTH_LONG).show();
                                navigateToMain();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                showError(getString(R.string.error_save) + ": " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = getString(R.string.error_registration);
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("already in use")) {
                            msg = getString(R.string.error_email_in_use);
                        } else if (e.getMessage().contains("badly formatted")) {
                            msg = getString(R.string.error_invalid_email);
                        }
                    }
                    showError(msg);
                });
    }

    private void navigateToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ═══ Helpers ═════════════════════════════════════════
    private void togglePasswordVisibility() {
        boolean isHidden = etPassword.getTransformationMethod()
                instanceof PasswordTransformationMethod;
        if (isHidden) {
            etPassword.setTransformationMethod(null);
            etConfirmPassword.setTransformationMethod(null);
            btnTogglePass.setText("🙈");
        } else {
            etPassword.setTransformationMethod(new PasswordTransformationMethod());
            etConfirmPassword.setTransformationMethod(new PasswordTransformationMethod());
            btnTogglePass.setText("👁");
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        pbComplete.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnComplete.setAlpha(loading ? 0.7f : 1f);
    }

    private void showError(String msg) {
        tvCompleteError.setText(msg);
        tvCompleteError.setVisibility(View.VISIBLE);
    }
    private void clearError() {
        tvCompleteError.setVisibility(View.GONE);
    }
    private void shake(View v) {
        v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }
}