package com.example.farme;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.messaging.FirebaseMessaging;

public class FarmeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Тёмная / светлая тема
        SharedPreferences prefs = getSharedPreferences("farme_settings", MODE_PRIVATE);
        boolean dark = prefs.getBoolean("dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // Создаём notification-каналы (Android 8+)
        NotificationHelper.createChannels(this);

        // FCM: обновляем токен если пользователь уже авторизован
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> FarmeFcmService.saveToken(token));

    }
}
