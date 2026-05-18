package com.example.farme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences("farme_settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "ru");
        super.attachBaseContext(LocaleHelper.wrap(base, lang));
    }
}
