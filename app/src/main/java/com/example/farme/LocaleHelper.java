package com.example.farme;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public final class LocaleHelper {

    private LocaleHelper() {}

    public static Context wrap(Context base, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }
}
