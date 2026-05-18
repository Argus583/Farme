package com.example.farme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.farme.auth.AuthActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DURATION = 2000; // 2 секунды

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Анимация появления лого
        View logo = findViewById(R.id.tvSplashLogo);
        View appName = findViewById(R.id.tvSplashAppName);
        View tagline = findViewById(R.id.tvSplashTagline);

        if (logo != null) {
            AlphaAnimation anim = new AlphaAnimation(0f, 1f);
            anim.setDuration(800);
            logo.startAnimation(anim);
        }
        if (appName != null) {
            AlphaAnimation anim = new AlphaAnimation(0f, 1f);
            anim.setDuration(800);
            anim.setStartOffset(300);
            appName.startAnimation(anim);
        }
        if (tagline != null) {
            AlphaAnimation anim = new AlphaAnimation(0f, 1f);
            anim.setDuration(800);
            anim.setStartOffset(600);
            tagline.startAnimation(anim);
        }

        // Переход через 2 секунды
        new Handler(Looper.getMainLooper()).postDelayed(
                this::navigate, SPLASH_DURATION);
    }

    private void navigate() {
        // 1. Проверяем onboarding
        SharedPreferences prefs = getSharedPreferences(
                "farme_prefs", MODE_PRIVATE);
        boolean onboardingDone = prefs.getBoolean("onboarding_done", false);

        if (!onboardingDone) {
            goTo(OnboardingActivity.class);
            return;
        }

        // 2. Проверяем авторизацию
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goTo(AuthActivity.class);
            return;
        }

        // 3. Пользователь авторизован — проверяем роль и блокировку
        FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Проверяем блокировку
                        Boolean banned = snapshot.child("banned")
                                .getValue(Boolean.class);
                        if (Boolean.TRUE.equals(banned)) {
                            FirebaseAuth.getInstance().signOut();
                            goTo(AuthActivity.class);
                            return;
                        }

                        goTo(MainActivity.class);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // При ошибке сети — всё равно входим
                        goTo(MainActivity.class);
                    }
                });
    }

    private void goTo(Class<?> target) {
        Intent i = new Intent(this, target);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        // Плавный переход
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        finish();
    }
}
