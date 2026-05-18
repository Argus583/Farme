package com.example.farme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileActivity extends BaseActivity {

    private TextView tvAvatarInitials, tvProfileName, tvProfileRegion;
    private TextView tvListingsCount, tvReviewsCount, tvRating;
    private TextView tvMenuListingsCount, tvMenuFavCount;
    private ImageView ivAvatar;

    private DatabaseReference mDatabase;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, com.example.farme.auth.AuthActivity.class));
            finish();
            return;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase  = FirebaseDatabase.getInstance().getReference();

        initViews();
        loadProfile();
        loadCounts();
    }

    private void initViews() {
        tvAvatarInitials    = findViewById(R.id.tvAvatarInitials);
        ivAvatar            = findViewById(R.id.ivAvatar);
        tvProfileName       = findViewById(R.id.tvProfileName);
        tvProfileRegion     = findViewById(R.id.tvProfileRegion);
        tvListingsCount     = findViewById(R.id.tvListingsCount);
        tvReviewsCount      = findViewById(R.id.tvReviewsCount);
        tvRating            = findViewById(R.id.tvRating);
        tvMenuListingsCount = findViewById(R.id.tvMenuListingsCount);
        tvMenuFavCount      = findViewById(R.id.tvMenuFavCount);

        // Настройки (шестерёнка вверху)
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null)
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, EditProfileActivity.class)));

        // Меню пункты
        setupMenuItem(R.id.menuMyListings, MyListingsActivity.class);
        setupMenuItem(R.id.menuFavorites,  FavoritesActivity.class);
        setupMenuItem(R.id.menuReviews,    ReviewsActivity.class);
        setupMenuItem(R.id.menuNotifications, NotificationsActivity.class);
        setupMenuItem(R.id.menuEditProfile,   EditProfileActivity.class);
        setupMenuItem(R.id.menuSettingsItem,  SettingsActivity.class);

        // Помощь
        View menuHelp = findViewById(R.id.menuHelp);
        if (menuHelp != null)
            menuHelp.setOnClickListener(v ->
                    Toast.makeText(this, "support@farme.kg",
                            Toast.LENGTH_LONG).show());

        // Баннер продвижение
        View bannerPromote = findViewById(R.id.bannerPromote);
        if (bannerPromote != null)
            bannerPromote.setOnClickListener(v ->
                    Toast.makeText(this, getString(R.string.feature_coming_soon),
                            Toast.LENGTH_SHORT).show());

        // Выход
        View menuLogout = findViewById(R.id.menuLogout);
        if (menuLogout != null)
            menuLogout.setOnClickListener(v -> confirmLogout());
    }

    private void setupMenuItem(int id, Class<?> target) {
        View item = findViewById(id);
        if (item != null)
            item.setOnClickListener(v ->
                    startActivity(new Intent(this, target)));
    }

    // ── Загрузка профиля ─────────────────────────────────
    private void loadProfile() {
        mDatabase.child("users").child(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name   = snapshot.child("name").getValue(String.class);
                        String region = snapshot.child("region").getValue(String.class);
                        String avatar = snapshot.child("avatar").getValue(String.class);
                        Long   ca     = snapshot.child("createdAt").getValue(Long.class);

                        // Имя
                        if (name != null && !name.isEmpty()) {
                            if (tvProfileName != null) tvProfileName.setText(name);
                            // Инициалы (макс 2 буквы)
                            String[] parts = name.trim().split("\\s+");
                            String initials = parts.length >= 2
                                    ? String.valueOf(parts[0].charAt(0)).toUpperCase()
                                    + String.valueOf(parts[1].charAt(0)).toUpperCase()
                                    : String.valueOf(name.charAt(0)).toUpperCase();
                            if (tvAvatarInitials != null)
                                tvAvatarInitials.setText(initials);
                        }

                        // Регион + год
                        String regionText = region != null ? region : "Кыргызстан";
                        if (ca != null) {
                            String year = new SimpleDateFormat("yyyy",
                                    Locale.getDefault()).format(new Date(ca));
                            regionText += " • с " + year;
                        }
                        if (tvProfileRegion != null)
                            tvProfileRegion.setText(regionText);

                        // Аватар
                        if (avatar != null && !avatar.isEmpty()) {
                            try {
                                String data = avatar.contains(",")
                                        ? avatar.substring(avatar.indexOf(",") + 1) : avatar;
                                byte[] bytes = android.util.Base64.decode(
                                        data, android.util.Base64.DEFAULT);
                                if (ivAvatar != null) {
                                    ivAvatar.setVisibility(View.VISIBLE);
                                    tvAvatarInitials.setVisibility(View.GONE);
                                    Glide.with(ProfileActivity.this)
                                            .load(bytes).circleCrop().into(ivAvatar);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Счётчики ─────────────────────────────────────────
    private void loadCounts() {
        // Объявления
        mDatabase.child("listings")
                .orderByChild("uid").equalTo(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = 0;
                        for (DataSnapshot child : snapshot.getChildren())
                            if (Boolean.TRUE.equals(child.child("active")
                                    .getValue(Boolean.class))) count++;
                        if (tvListingsCount != null)
                            tvListingsCount.setText(String.valueOf(count));
                        if (tvMenuListingsCount != null && count > 0) {
                            tvMenuListingsCount.setText(String.valueOf(count));
                            tvMenuListingsCount.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // Отзывы + рейтинг
        mDatabase.child("reviews").child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        double total = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Double r = child.child("rating").getValue(Double.class);
                            if (r != null) total += r;
                        }
                        if (tvReviewsCount != null)
                            tvReviewsCount.setText(String.valueOf(count));
                        if (tvRating != null && count > 0)
                            tvRating.setText(String.format(
                                    Locale.getDefault(), "%.1f", total / count));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // Избранное
        mDatabase.child("favorites").child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (tvMenuFavCount != null && count > 0) {
                            tvMenuFavCount.setText(String.valueOf(count));
                            tvMenuFavCount.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Выход ─────────────────────────────────────────────
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(getString(R.string.action_logout), (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(this, com.example.farme.auth.AuthActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }
}