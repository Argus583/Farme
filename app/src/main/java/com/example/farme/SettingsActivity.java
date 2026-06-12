package com.example.farme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    private static final String PREFS = "farme_settings";
    private SharedPreferences prefs;
    private DatabaseReference mDatabase;
    private String currentUid;

    private SwitchCompat switchPush, switchMsg, switchPrices;
    private TextView tvLanguageValue;

    private static final String[] LANG_NAMES = {"Русский", "English", "Кыргызча"};
    private static final String[] LANG_CODES = {"ru", "en", "ky"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs     = getSharedPreferences(PREFS, MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        loadRegion();
        if (currentUid != null) loadNotifSettings();
    }

    private void initViews() {
        TextView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Dark theme
        SwitchCompat switchDark = findViewById(R.id.switchDarkTheme);
        if (switchDark != null) {
            switchDark.setChecked(prefs.getBoolean("dark_theme", false));
            switchDark.setOnCheckedChangeListener((btn, checked) -> {
                prefs.edit().putBoolean("dark_theme", checked).apply();
                AppCompatDelegate.setDefaultNightMode(
                        checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                recreate();
            });
        }

        // Region
        View itemRegion = findViewById(R.id.itemRegion);
        if (itemRegion != null) itemRegion.setOnClickListener(v -> showRegionDialog());

        // Language
        tvLanguageValue = findViewById(R.id.tvLanguageValue);
        updateLanguageLabel(prefs.getString("language", "ru"));
        View itemLanguage = findViewById(R.id.itemLanguage);
        if (itemLanguage != null) itemLanguage.setOnClickListener(v -> showLanguageDialog());

        // Push notifications
        switchPush = findViewById(R.id.switchPush);
        if (switchPush != null) {
            switchPush.setChecked(prefs.getBoolean("push_enabled", true));
            switchPush.setOnCheckedChangeListener((b, checked) -> {
                prefs.edit().putBoolean("push_enabled", checked).apply();
                saveNotifSetting("pushEnabled", checked);
            });
        }

        // New messages
        switchMsg = findViewById(R.id.switchMessages);
        if (switchMsg != null) {
            switchMsg.setChecked(prefs.getBoolean("msg_enabled", true));
            switchMsg.setOnCheckedChangeListener((b, checked) -> {
                prefs.edit().putBoolean("msg_enabled", checked).apply();
                saveNotifSetting("msgEnabled", checked);
            });
        }

        // Price changes in favorites
        switchPrices = findViewById(R.id.switchPrices);
        if (switchPrices != null) {
            switchPrices.setChecked(prefs.getBoolean("prices_enabled", false));
            switchPrices.setOnCheckedChangeListener((b, checked) -> {
                prefs.edit().putBoolean("prices_enabled", checked).apply();
                saveNotifSetting("pricesEnabled", checked);
            });
        }

        // Blacklist
        View itemBlacklist = findViewById(R.id.itemBlacklist);
        if (itemBlacklist != null)
            itemBlacklist.setOnClickListener(v ->
                    startActivity(new Intent(this, BlocklistActivity.class)));

        // Logout
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void saveNotifSetting(String key, boolean value) {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("settings").child(key).setValue(value);
    }

    private void loadNotifSettings() {
        mDatabase.child("users").child(currentUid).child("settings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        Boolean push   = snap.child("pushEnabled").getValue(Boolean.class);
                        Boolean msg    = snap.child("msgEnabled").getValue(Boolean.class);
                        Boolean prices = snap.child("pricesEnabled").getValue(Boolean.class);
                        if (push != null && switchPush != null) {
                            prefs.edit().putBoolean("push_enabled", push).apply();
                            switchPush.setChecked(push);
                        }
                        if (msg != null && switchMsg != null) {
                            prefs.edit().putBoolean("msg_enabled", msg).apply();
                            switchMsg.setChecked(msg);
                        }
                        if (prices != null && switchPrices != null) {
                            prefs.edit().putBoolean("prices_enabled", prices).apply();
                            switchPrices.setChecked(prices);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadRegion() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).child("region")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        String region = snap.getValue(String.class);
                        TextView tvRegion = findViewById(R.id.tvRegionValue);
                        if (tvRegion != null && region != null) tvRegion.setText(region);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void showRegionDialog() {
        String[] regions = {
                "Все регионы", "Чуйская область", "Иссык-Кульская область",
                "Ошская область", "Джалал-Абадская область", "Нарынская область",
                "Баткенская область", "Таласская область", "г. Бишкек", "г. Ош"
        };
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_region))
                .setItems(regions, (d, i) -> {
                    TextView tvRegion = findViewById(R.id.tvRegionValue);
                    if (tvRegion != null) tvRegion.setText(regions[i]);
                    if (currentUid != null)
                        mDatabase.child("users").child(currentUid).child("region").setValue(regions[i]);
                    prefs.edit().putString("region", regions[i]).apply();
                })
                .show();
    }

    private void showLanguageDialog() {
        String current = prefs.getString("language", "ru");
        int selected = 0;
        for (int i = 0; i < LANG_CODES.length; i++) {
            if (LANG_CODES[i].equals(current)) { selected = i; break; }
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_language))
                .setSingleChoiceItems(LANG_NAMES, selected, null)
                .setPositiveButton("OK", null)
                .setNegativeButton("Отмена / Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int choice = dialog.getListView().getCheckedItemPosition();
                    if (choice >= 0 && !LANG_CODES[choice].equals(current)) {
                        dialog.dismiss();
                        applyLanguage(LANG_CODES[choice]);
                    } else {
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    private void applyLanguage(String langCode) {
        prefs.edit().putString("language", langCode).apply();
        Intent i = new Intent(this, SplashActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void updateLanguageLabel(String code) {
        if (tvLanguageValue == null) return;
        switch (code) {
            case "en": tvLanguageValue.setText("English");   break;
            case "ky": tvLanguageValue.setText("Кыргызча"); break;
            default:   tvLanguageValue.setText("Русский");  break;
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(getString(R.string.action_logout), (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(this, com.example.farme.auth.AuthActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }
}
