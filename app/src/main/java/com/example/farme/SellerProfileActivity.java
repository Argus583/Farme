package com.example.farme;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.example.farme.model.Listing;

import java.text.SimpleDateFormat;
import java.util.*;

public class SellerProfileActivity extends BaseActivity {

    private TextView tvSellerInitial, tvSellerName, tvSellerRegion, tvSellerSince;
    private TextView tvSellerListingsCount, tvSellerRating, tvSellerReviewsCount;
    private ImageView ivSellerAvatar;
    private LinearLayout btnWriteSeller, btnCallSeller;
    private LinearLayout btnBlockSeller, btnReportSeller;
    private TextView tvBlockLabel;
    private TextView tabListings, tabReviews;
    private View tabIndicator;
    private LinearLayout containerListings, containerReviews, emptyContent;
    private TextView tvEmptyIcon, tvEmptyText;

    private DatabaseReference mDatabase;
    private String sellerUid;
    private String myUid;
    private String sellerPhone = "";
    private int currentTab = 0;
    private boolean isBlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        sellerUid = getIntent().getStringExtra("sellerUid");
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (sellerUid == null) { finish(); return; }

        initViews();
        checkBlockedBySellerThenLoad();
        checkBlocked();
        selectTab(0);
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvSellerInitial      = findViewById(R.id.tvSellerInitial);
        ivSellerAvatar       = findViewById(R.id.ivSellerAvatar);
        tvSellerName         = findViewById(R.id.tvSellerName);
        tvSellerRegion       = findViewById(R.id.tvSellerRegion);
        tvSellerSince        = findViewById(R.id.tvSellerSince);
        tvSellerListingsCount = findViewById(R.id.tvSellerListingsCount);
        tvSellerRating       = findViewById(R.id.tvSellerRating);
        tvSellerReviewsCount = findViewById(R.id.tvSellerReviewsCount);

        btnWriteSeller  = findViewById(R.id.btnWriteSeller);
        btnCallSeller   = findViewById(R.id.btnCallSeller);
        btnBlockSeller  = findViewById(R.id.btnBlockSeller);
        btnReportSeller = findViewById(R.id.btnReportSeller);
        tvBlockLabel    = findViewById(R.id.tvBlockLabel);
        tabListings     = findViewById(R.id.tabSellerListings);
        tabReviews     = findViewById(R.id.tabSellerReviews);
        tabIndicator   = findViewById(R.id.tabSellerIndicator);
        containerListings = findViewById(R.id.containerListings);
        containerReviews  = findViewById(R.id.containerReviews);
        emptyContent   = findViewById(R.id.emptyContent);
        tvEmptyIcon    = findViewById(R.id.tvEmptyIcon);
        tvEmptyText    = findViewById(R.id.tvEmptyText);

        btnWriteSeller.setOnClickListener(v -> openChat());
        btnCallSeller.setOnClickListener(v  -> callSeller());
        if (btnBlockSeller  != null) btnBlockSeller.setOnClickListener(v  -> toggleBlock());
        if (btnReportSeller != null) btnReportSeller.setOnClickListener(v -> openReportUserDialog());
        tabListings.setOnClickListener(v    -> selectTab(0));
        tabReviews.setOnClickListener(v     -> selectTab(1));
    }

    // ── Проверка: продавец заблокировал меня? ────────────────
    private void checkBlockedBySellerThenLoad() {
        if (myUid == null) {
            loadSellerProfile();
            loadSellerListings();
            return;
        }
        mDatabase.child("blocks").child(sellerUid).child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            Toast.makeText(SellerProfileActivity.this,
                                    "Профиль недоступен", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            loadSellerProfile();
                            loadSellerListings();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        loadSellerProfile();
                        loadSellerListings();
                    }
                });
    }

    // ── Загрузка профиля ─────────────────────────────────────
    private void loadSellerProfile() {
        mDatabase.child("users").child(sellerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name   = snapshot.child("name").getValue(String.class);
                        String region = snapshot.child("region").getValue(String.class);
                        String avatar = snapshot.child("avatar").getValue(String.class);
                        Double rating = snapshot.child("rating").getValue(Double.class);
                        Long   ca     = snapshot.child("createdAt").getValue(Long.class);
                        sellerPhone   = snapshot.child("phone").getValue(String.class) != null
                                ? snapshot.child("phone").getValue(String.class) : "";

                        if (name != null && !name.isEmpty()) {
                            tvSellerName.setText(name);
                            tvSellerInitial.setText(
                                    String.valueOf(name.charAt(0)).toUpperCase());
                        }
                        if (region != null)
                            tvSellerRegion.setText("📍 " + region);

                        if (ca != null) {
                            String date = new SimpleDateFormat("dd.MM.yyyy",
                                    Locale.getDefault()).format(new Date(ca));
                            tvSellerSince.setText("На сайте с " + date);
                        }

                        if (rating != null)
                            tvSellerRating.setText(String.format(
                                    Locale.getDefault(), "%.1f", rating));

                        // Аватар
                        if (avatar != null && !avatar.isEmpty()) {
                            String data = avatar.contains(",")
                                    ? avatar.substring(avatar.indexOf(",") + 1) : avatar;
                            byte[] bytes = android.util.Base64.decode(
                                    data, android.util.Base64.DEFAULT);
                            ivSellerAvatar.setVisibility(View.VISIBLE);
                            tvSellerInitial.setVisibility(View.GONE);
                            Glide.with(SellerProfileActivity.this)
                                    .load(bytes).circleCrop().into(ivSellerAvatar);
                        }

                        // Скрываем кнопки если это свой профиль
                        if (sellerUid.equals(myUid)) {
                            btnWriteSeller.setVisibility(View.GONE);
                            btnCallSeller.setVisibility(View.GONE);
                            if (btnBlockSeller  != null) btnBlockSeller.setVisibility(View.GONE);
                            if (btnReportSeller != null) btnReportSeller.setVisibility(View.GONE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Объявления продавца ───────────────────────────────────
    private void loadSellerListings() {
        mDatabase.child("listings").orderByChild("uid").equalTo(sellerUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Listing> listings = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Listing l = child.getValue(Listing.class);
                            if (l != null && l.isActive()) {
                                l.setId(child.getKey());
                                listings.add(l);
                            }
                        }
                        listings.sort((a, b) ->
                                Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                        tvSellerListingsCount.setText(String.valueOf(listings.size()));
                        buildListingsUI(listings);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // Отзывы
        mDatabase.child("reviews").child(sellerUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ReviewItem> reviews = new ArrayList<>();
                        double total = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ReviewItem r = new ReviewItem();
                            r.authorName = child.child("authorName").getValue(String.class);
                            r.text       = child.child("text").getValue(String.class);
                            r.rating     = child.child("rating").getValue(Double.class) != null
                                    ? child.child("rating").getValue(Double.class) : 0;
                            r.createdAt  = child.child("createdAt").getValue(Long.class) != null
                                    ? child.child("createdAt").getValue(Long.class) : 0;
                            reviews.add(r);
                            total += r.rating;
                        }

                        tvSellerReviewsCount.setText(String.valueOf(reviews.size()));
                        if (!reviews.isEmpty())
                            tvSellerRating.setText(String.format(
                                    Locale.getDefault(), "%.1f", total / reviews.size()));

                        reviews.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                        buildReviewsUI(reviews);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── UI объявлений ─────────────────────────────────────────
    private void buildListingsUI(List<Listing> listings) {
        if (containerListings == null) return;
        containerListings.removeAllViews();

        if (listings.isEmpty()) {
            if (currentTab == 0) showEmpty("📋", "Нет активных объявлений");
            return;
        }

        for (Listing l : listings) {
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_listing, containerListings, false);

            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvPrice = card.findViewById(R.id.tvPrice);
            TextView tvRegion = card.findViewById(R.id.tvRegion);

            if (tvTitle  != null) tvTitle.setText(l.getTitle());
            if (tvPrice  != null) tvPrice.setText(formatPrice(l.getPrice()));
            if (tvRegion != null) tvRegion.setText("📍 " + l.getRegion());

            ImageView ivPhoto = card.findViewById(R.id.ivListingPhoto);
            if (ivPhoto != null && l.hasPhotos() && !l.getPhotos().isEmpty()) {
                String raw = l.getPhotos().get(0);
                if (raw != null && !raw.isEmpty()) {
                    try {
                        String d = raw.contains(",")
                                ? raw.substring(raw.indexOf(",") + 1) : raw;
                        byte[] bytes = android.util.Base64.decode(d, android.util.Base64.DEFAULT);
                        Glide.with(this).load(bytes).centerCrop().into(ivPhoto);
                    } catch (Exception ignored) {}
                }
            }

            card.setOnClickListener(v -> {
                Intent i = new Intent(this, ListingDetailActivity.class);
                i.putExtra("listingId", l.getId());
                startActivity(i);
            });

            containerListings.addView(card);
        }

        if (currentTab == 0) emptyContent.setVisibility(View.GONE);
    }

    // ── UI отзывов ────────────────────────────────────────────
    private void buildReviewsUI(List<ReviewItem> reviews) {
        if (containerReviews == null) return;
        containerReviews.removeAllViews();

        if (reviews.isEmpty()) {
            if (currentTab == 1) showEmpty("⭐", "Отзывов пока нет");
            return;
        }

        for (ReviewItem r : reviews) {
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_review, containerReviews, false);

            TextView tvAuthor = card.findViewById(R.id.tvReviewAuthor);
            TextView tvText   = card.findViewById(R.id.tvReviewText);
            TextView tvRating = card.findViewById(R.id.tvReviewRating);
            TextView tvDate   = card.findViewById(R.id.tvReviewDate);

            if (tvAuthor != null)
                tvAuthor.setText(r.authorName != null ? r.authorName : "—");
            if (tvText   != null)
                tvText.setText(r.text != null ? r.text : "");
            if (tvRating != null)
                tvRating.setText(getStars(r.rating));
            if (tvDate   != null && r.createdAt > 0)
                tvDate.setText(new SimpleDateFormat("dd.MM.yyyy",
                        Locale.getDefault()).format(new Date(r.createdAt)));

            containerReviews.addView(card);
        }

        if (currentTab == 1) emptyContent.setVisibility(View.GONE);
    }

    // ── Вкладки ───────────────────────────────────────────────
    private void selectTab(int tab) {
        currentTab = tab;
        int active   = getResources().getColor(R.color.green_primary, null);
        int inactive = getResources().getColor(R.color.text_secondary, null);

        tabListings.setTextColor(tab == 0 ? active : inactive);
        tabListings.setTypeface(null, tab == 0
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabReviews.setTextColor(tab == 1 ? active : inactive);
        tabReviews.setTypeface(null, tab == 1
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        containerListings.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        containerReviews.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        emptyContent.setVisibility(View.GONE);

        // Проверяем пустое состояние
        if (tab == 0 && containerListings.getChildCount() == 0)
            showEmpty("📋", "Нет активных объявлений");
        if (tab == 1 && containerReviews.getChildCount() == 0)
            showEmpty("⭐", "Отзывов пока нет");
    }

    private void showEmpty(String icon, String text) {
        emptyContent.setVisibility(View.VISIBLE);
        if (tvEmptyIcon != null) tvEmptyIcon.setText(icon);
        if (tvEmptyText != null) tvEmptyText.setText(text);
    }

    // ── Действия ─────────────────────────────────────────────
    private void openChat() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (myUid == null) return;
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("sellerUid", sellerUid);
        i.putExtra("listingId", "");
        i.putExtra("listingTitle", tvSellerName.getText().toString());
        startActivity(i);
    }

    private void callSeller() {
        if (sellerPhone.isEmpty()) {
            Toast.makeText(this, getString(R.string.phone_not_specified), Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + sellerPhone)));
    }

    // ── Блокировка ────────────────────────────────────────────
    private void checkBlocked() {
        if (myUid == null) return;
        mDatabase.child("blocks").child(myUid).child(sellerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        isBlocked = snap.exists();
                        updateBlockButton();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void updateBlockButton() {
        if (tvBlockLabel == null) return;
        if (isBlocked) {
            tvBlockLabel.setText("✅ " + getString(R.string.action_unblock));
            tvBlockLabel.setTextColor(getColor(R.color.text_secondary));
        } else {
            tvBlockLabel.setText("🚫 " + getString(R.string.action_block));
            tvBlockLabel.setTextColor(getColor(R.color.red_error));
        }
    }

    private void toggleBlock() {
        if (myUid == null) return;
        if (isBlocked) {
            mDatabase.child("blocks").child(myUid).child(sellerUid).removeValue()
                    .addOnSuccessListener(a -> {
                        isBlocked = false;
                        updateBlockButton();
                        Toast.makeText(this, getString(R.string.user_unblocked),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.block_user_title))
                    .setMessage(getString(R.string.block_user_message))
                    .setPositiveButton(getString(R.string.action_block), (d, w) ->
                            mDatabase.child("blocks").child(myUid).child(sellerUid)
                                    .setValue(true)
                                    .addOnSuccessListener(a -> {
                                        isBlocked = true;
                                        updateBlockButton();
                                        Toast.makeText(this, getString(R.string.user_blocked),
                                                Toast.LENGTH_SHORT).show();
                                    }))
                    .setNegativeButton(getString(R.string.action_cancel), null)
                    .show();
        }
    }

    // ── Жалоба на пользователя ────────────────────────────────
    private void openReportUserDialog() {
        if (myUid == null) return;
        String[] reasons = {"Мошенничество", "Оскорбительное поведение", "Спам", "Другое"};
        final int[] selected = {0};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        TextView tvLabel = new TextView(this);
        tvLabel.setText("Причина жалобы:");
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(getColor(R.color.text_primary));
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setPadding(0, 0, 0, dp(10));
        layout.addView(tvLabel);

        RadioGroup rg = new RadioGroup(this);
        for (int i = 0; i < reasons.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(reasons[i]);
            rb.setTextSize(14);
            rb.setTextColor(getColor(R.color.text_primary));
            int fi = i;
            rb.setOnCheckedChangeListener((btn, checked) -> { if (checked) selected[0] = fi; });
            if (i == 0) rb.setChecked(true);
            rg.addView(rb);
        }
        layout.addView(rg);

        EditText etMsg = new EditText(this);
        etMsg.setHint("Дополнительные сведения (необязательно)");
        etMsg.setBackgroundResource(R.drawable.bg_input);
        etMsg.setPadding(dp(12), dp(10), dp(12), dp(10));
        etMsg.setMinHeight(dp(80));
        etMsg.setGravity(Gravity.TOP);
        etMsg.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.topMargin = dp(12);
        etMsg.setLayoutParams(etLp);
        layout.addView(etMsg);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.report_user_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.action_send), (d, w) -> {
                    String msg = etMsg.getText().toString().trim();
                    submitUserReport(reasons[selected[0]], msg);
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    private void submitUserReport(String reason, String message) {
        String reportId = mDatabase.child("reports").push().getKey();
        if (reportId == null) return;
        String name = tvSellerName != null ? tvSellerName.getText().toString() : "—";
        Map<String, Object> report = new HashMap<>();
        report.put("reporterUid", myUid);
        report.put("targetUid",   sellerUid);
        report.put("targetName",  name);
        report.put("reason",      reason);
        report.put("type",        "user");
        report.put("createdAt",   System.currentTimeMillis());
        report.put("status",      "open");
        if (!message.isEmpty()) report.put("message", message);
        mDatabase.child("reports").child(reportId).setValue(report)
                .addOnSuccessListener(a -> Toast.makeText(this,
                        "Жалоба отправлена. Мы рассмотрим её в течение 24 часов.",
                        Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Ошибка отправки", Toast.LENGTH_SHORT).show());
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ── Утилиты ───────────────────────────────────────────────
    private String formatPrice(double price) {
        if (price <= 0) return "Договорная";
        return String.format(Locale.getDefault(), "%,.0f сом", price)
                .replace(",", " ");
    }

    private String getStars(double rating) {
        int r = (int) Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < r ? "★" : "☆");
        return sb.toString();
    }

    // ── Модель отзыва ─────────────────────────────────────────
    static class ReviewItem {
        String authorName, text;
        double rating;
        long   createdAt;
    }
}