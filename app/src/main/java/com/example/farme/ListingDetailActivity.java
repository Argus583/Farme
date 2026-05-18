package com.example.farme;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.farme.adapter.PhotoPagerAdapter;
import com.example.farme.model.Listing;
import com.example.farme.utils.HistoryManager;
import com.example.farme.utils.Validator;
import java.util.*;

public class ListingDetailActivity extends BaseActivity {

    // ── Views ─────────────────────────────────────────────
    private ViewPager2   vpPhotos;
    private TextView     tvTitle, tvPrice, tvNegotiable, tvCategory;
    private TextView     tvRegion, tvDescription, tvCreatedAt;
    private TextView     tvSellerName, tvSellerRegion, tvSellerRating;
    private TextView     tvSellerAvatar;
    private TextView     tvSoldBadge;
    private LinearLayout sellerCard;
    private LinearLayout btnCall, btnMessage;
    private LinearLayout btnMarkSold;
    private LinearLayout btnLeaveReview;
    private LinearLayout reviewDoneRow;
    private TextView     btnBack, btnShare, btnFavorite, btnReport;
    private LinearLayout passportCard;
    private TextView     tvPassportSpecies, tvPassportBreed, tvPassportAge;
    private TextView     tvPassportSex, tvPassportCount, tvPassportVetNo;
    private TextView     tvPassportVetDate, tvPassportStatus;
    private LinearLayout vaccineContainer;
    private ProgressBar  progressBar;
    private LinearLayout mapSection;
    private ScrollView   contentScroll;
    private TextView     tvDotsIndicator;

    // ── Данные ────────────────────────────────────────────
    private DatabaseReference mDatabase;
    private String            listingId, myUid;
    private Listing           currentListing;
    private boolean           isFavorite = false;

    // ═════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        listingId = getIntent().getStringExtra("listingId");
        if (listingId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        HistoryManager.addToHistory(this, listingId);

        initViews();
        loadListing();
        checkFavorite();
    }

    // ═════════════════════════════════════════════════════
    private void initViews() {
        vpPhotos          = findViewById(R.id.vpPhotos);
        tvTitle           = findViewById(R.id.tvTitle);
        tvPrice           = findViewById(R.id.tvPrice);
        tvNegotiable      = findViewById(R.id.tvNegotiable);
        tvCategory        = findViewById(R.id.tvCategory);
        tvRegion          = findViewById(R.id.tvRegion);
        tvDescription     = findViewById(R.id.tvDescription);
        tvCreatedAt       = findViewById(R.id.tvCreatedAt);
        tvSellerName      = findViewById(R.id.tvSellerName);
        tvSellerRegion    = findViewById(R.id.tvSellerRegion);
        tvSellerRating    = findViewById(R.id.tvSellerRating);
        tvSellerAvatar    = findViewById(R.id.tvSellerAvatar);
        tvSoldBadge       = findViewById(R.id.tvSoldBadge);
        sellerCard        = findViewById(R.id.sellerCard);
        btnCall           = findViewById(R.id.btnCall);
        btnMessage        = findViewById(R.id.btnMessage);
        btnMarkSold       = findViewById(R.id.btnMarkSold);
        btnLeaveReview    = findViewById(R.id.btnLeaveReview);
        reviewDoneRow     = findViewById(R.id.reviewDoneRow);
        btnBack           = findViewById(R.id.btnBack);
        btnShare          = findViewById(R.id.btnShare);
        btnFavorite       = findViewById(R.id.btnFavorite);
        passportCard      = findViewById(R.id.passportCard);
        tvPassportSpecies = findViewById(R.id.tvPassportSpecies);
        tvPassportBreed   = findViewById(R.id.tvPassportBreed);
        tvPassportAge     = findViewById(R.id.tvPassportAge);
        tvPassportSex     = findViewById(R.id.tvPassportSex);
        tvPassportCount   = findViewById(R.id.tvPassportCount);
        tvPassportVetNo   = findViewById(R.id.tvPassportVetNo);
        tvPassportVetDate = findViewById(R.id.tvPassportVetDate);
        tvPassportStatus  = findViewById(R.id.tvPassportStatus);
        vaccineContainer  = findViewById(R.id.vaccineContainer);
        progressBar       = findViewById(R.id.progressBar);
        mapSection        = findViewById(R.id.mapSection);
        contentScroll     = findViewById(R.id.contentScroll);
        tvDotsIndicator   = findViewById(R.id.tvDotsIndicator);

        btnReport = findViewById(R.id.btnReport);

        if (btnBack    != null) btnBack.setOnClickListener(v -> finish());
        if (btnShare   != null) btnShare.setOnClickListener(v -> shareListing());
        if (btnFavorite!= null) btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    // ═════════════════════════════════════════════════════
    private void loadListing() {
        if (progressBar   != null) progressBar.setVisibility(View.VISIBLE);
        if (contentScroll != null) contentScroll.setVisibility(View.GONE);

        mDatabase.child("listings").child(listingId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        currentListing = snap.getValue(Listing.class);
                        if (currentListing == null) { finish(); return; }
                        currentListing.setId(listingId);

                        if (progressBar   != null) progressBar.setVisibility(View.GONE);
                        if (contentScroll != null) contentScroll.setVisibility(View.VISIBLE);

                        bindListing();
                        loadSellerInfo(currentListing.getUid());
                        setupButtons();
                        showMap(currentListing);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        Toast.makeText(ListingDetailActivity.this,
                                getString(R.string.error_loading), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // ═════════════════════════════════════════════════════
    private void bindListing() {
        Listing l = currentListing;

        // Фото
        if (vpPhotos != null && l.hasPhotos()) {
            PhotoPagerAdapter pa = new PhotoPagerAdapter(this, l.getPhotos());
            vpPhotos.setAdapter(pa);
            updateDots(0, l.getPhotos().size());
            vpPhotos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int pos) {
                    updateDots(pos, l.getPhotos().size());
                }
            });
        }

        setText(tvTitle, l.getTitle());

        if (tvPrice != null)
            tvPrice.setText(Validator.formatPrice(l.getPrice()));

        if (tvNegotiable != null)
            tvNegotiable.setVisibility(l.isNegotiable() ? View.VISIBLE : View.GONE);

        if (tvCategory != null)
            tvCategory.setText(l.getCategoryEmoji() + "  " + l.getCategory());

        if (tvRegion != null && l.getRegion() != null)
            tvRegion.setText("📍  " + l.getRegion());

        if (tvDescription != null)
            tvDescription.setText(l.getDescription() != null ? l.getDescription() : "");

        if (tvCreatedAt != null && l.getCreatedAt() > 0) {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
            tvCreatedAt.setText(getString(R.string.added_date_format, sdf.format(new Date(l.getCreatedAt()))));
        }

        // Бейдж «Продано»
        if (tvSoldBadge != null)
            tvSoldBadge.setVisibility(l.isSold() ? View.VISIBLE : View.GONE);

        // Паспорт
        if (l.getPassport() != null && passportCard != null) {
            passportCard.setVisibility(View.VISIBLE);
            Listing.Passport p = l.getPassport();
            setText(tvPassportSpecies, p.getSpecies());
            setText(tvPassportBreed,   p.getBreed());
            setText(tvPassportAge,     p.getAge() > 0 ? p.getAge() + " лет" : "—");
            setText(tvPassportSex,     p.getSex());
            setText(tvPassportCount,   p.getCount() > 0 ? p.getCount() + " гол." : "—");
            setText(tvPassportVetNo,   p.getVetCertNo());
            setText(tvPassportVetDate, p.getVetDate());

            if (tvPassportStatus != null) {
                if (p.isVerified()) {
                    tvPassportStatus.setText(getString(R.string.passport_verified));
                    tvPassportStatus.setTextColor(0xFF2D6A4F);
                } else {
                    tvPassportStatus.setText(getString(R.string.passport_pending));
                    tvPassportStatus.setTextColor(0xFFD97706);
                }
            }

            if (vaccineContainer != null && p.getVaccines() != null) {
                vaccineContainer.removeAllViews();
                for (Listing.Passport.Vaccine v : p.getVaccines()) {
                    TextView tv = new TextView(this);
                    tv.setText("• " + v.getName() + "  —  " + v.getDate());
                    tv.setTextSize(13);
                    tv.setPadding(0, 4, 0, 4);
                    vaccineContainer.addView(tv);
                }
            }
        } else if (passportCard != null) {
            passportCard.setVisibility(View.GONE);
        }
    }

    // ═════════════════════════════════════════════════════
    private void setupButtons() {
        if (currentListing == null) return;
        boolean isOwner = currentListing.getUid() != null
                && currentListing.getUid().equals(myUid);

        if (isOwner) {
            // ── Владелец: редактировать + архив ──────────
            if (btnCall != null) {
                setButtonText(btnCall, "✏️ Редактировать");
                btnCall.setOnClickListener(v -> {
                    Intent i = new Intent(this, EditListingActivity.class);
                    i.putExtra("listingId", listingId);
                    startActivity(i);
                });
            }
            if (btnMessage != null) {
                setButtonText(btnMessage, "🗄️ В архив");
                btnMessage.setBackgroundColor(0xFFE5E7EB);
                if (btnMessage.getChildCount() > 0
                        && btnMessage.getChildAt(0) instanceof TextView)
                    ((TextView) btnMessage.getChildAt(0)).setTextColor(0xFF374151);
                btnMessage.setOnClickListener(v -> moveToArchive());
            }

            // ── Кнопка «Продано» — только для владельца ─
            if (currentListing.isSold()) {
                // Уже продано — бейдж показан в bindListing(), кнопку скрываем
                if (btnMarkSold != null) btnMarkSold.setVisibility(View.GONE);
            } else {
                if (btnMarkSold != null) {
                    btnMarkSold.setVisibility(View.VISIBLE);
                    btnMarkSold.setOnClickListener(v -> confirmMarkSold());
                }
            }

            // Отзыв — владелец не пишет отзыв себе
            if (btnLeaveReview != null) btnLeaveReview.setVisibility(View.GONE);
            if (reviewDoneRow  != null) reviewDoneRow.setVisibility(View.GONE);
            if (btnReport      != null) btnReport.setVisibility(View.GONE);

        } else {
            // ── Покупатель: позвонить + написать ─────────
            if (btnCall    != null) btnCall.setOnClickListener(v -> callSeller());
            if (btnMessage != null) btnMessage.setOnClickListener(v -> openChat());
            if (btnMarkSold!= null) btnMarkSold.setVisibility(View.GONE);
            if (btnReport  != null) {
                btnReport.setVisibility(View.VISIBLE);
                btnReport.setOnClickListener(v -> openReportDialog());
            }

            // Кнопка отзыва — проверяем не оставлял ли уже
            checkReviewStatus(currentListing.getUid());
        }
    }

    // ═════════════════════════════════════════════════════
    // Продано
    // ═════════════════════════════════════════════════════
    private void confirmMarkSold() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_mark_sold_title))
                .setMessage(getString(R.string.confirm_mark_sold_message))
                .setPositiveButton(getString(R.string.btn_mark_sold), (d, w) -> markAsSold())
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    private void markAsSold() {
        Map<String, Object> upd = new HashMap<>();
        upd.put("sold",   true);
        upd.put("active", false);
        mDatabase.child("listings").child(listingId)
                .updateChildren(upd)
                .addOnSuccessListener(x -> {
                    Toast.makeText(this,
                            getString(R.string.marked_as_sold), Toast.LENGTH_SHORT).show();
                    if (tvSoldBadge != null) tvSoldBadge.setVisibility(View.VISIBLE);
                    if (btnMarkSold != null) btnMarkSold.setVisibility(View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_loading),
                                Toast.LENGTH_SHORT).show());
    }

    // ═════════════════════════════════════════════════════
    // Отзыв
    // ═════════════════════════════════════════════════════
    private void checkReviewStatus(String sellerUid) {
        if (myUid == null || sellerUid == null) return;
        // Путь: reviews/{sellerUid}/{myUid}
        mDatabase.child("reviews").child(sellerUid).child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            // Уже оставлен
                            if (btnLeaveReview != null) btnLeaveReview.setVisibility(View.GONE);
                            if (reviewDoneRow  != null) reviewDoneRow.setVisibility(View.VISIBLE);
                        } else {
                            // Ещё не оставлен
                            if (btnLeaveReview != null) {
                                btnLeaveReview.setVisibility(View.VISIBLE);
                                btnLeaveReview.setOnClickListener(v ->
                                        openReviewDialog(sellerUid));
                            }
                            if (reviewDoneRow != null) reviewDoneRow.setVisibility(View.GONE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void openReviewDialog(String sellerUid) {
        Intent i = new Intent(this, ReviewsActivity.class);
        i.putExtra("sellerUid",  sellerUid);
        i.putExtra("listingId",  listingId);
        i.putExtra("openReview", true);
        startActivityForResult(i, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            if (btnLeaveReview != null) btnLeaveReview.setVisibility(View.GONE);
            if (reviewDoneRow  != null) reviewDoneRow.setVisibility(View.VISIBLE);
            Toast.makeText(this, getString(R.string.review_published), Toast.LENGTH_SHORT).show();
        }
    }

    // ═════════════════════════════════════════════════════
    private void moveToArchive() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.action_archive))
                .setMessage(getString(R.string.confirm_archive_message))
                .setPositiveButton(getString(R.string.action_archive), (d, w) -> {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("active",  false);
                    upd.put("pending", false);
                    mDatabase.child("listings").child(listingId)
                            .updateChildren(upd)
                            .addOnSuccessListener(x -> {
                                Toast.makeText(this,
                                        getString(R.string.moved_to_archive),
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    private void callSeller() {
        if (currentListing == null) return;
        mDatabase.child("users").child(currentListing.getUid()).child("phone")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String phone = snap.getValue(String.class);
                        if (phone != null && !phone.isEmpty())
                            startActivity(new Intent(Intent.ACTION_DIAL,
                                    Uri.parse("tel:" + phone)));
                        else
                            Toast.makeText(ListingDetailActivity.this,
                                    getString(R.string.phone_not_specified), Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void openChat() {
        if (currentListing == null || myUid == null) return;
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("sellerUid",    currentListing.getUid());
        i.putExtra("listingId",    listingId);
        i.putExtra("listingTitle", currentListing.getTitle());
        startActivity(i);
    }

    private void shareListing() {
        if (currentListing == null) return;
        String text = currentListing.getTitle() + "\n"
                + Validator.formatPrice(currentListing.getPrice()) + "\n"
                + "📍 " + currentListing.getRegion() + "\n"
                + "Farme — Санарип Дыйкан";
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, getString(R.string.share_dialog_title)));
    }

    private void loadSellerInfo(String sellerUid) {
        mDatabase.child("users").child(sellerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String name   = snap.child("name").getValue(String.class);
                        String region = snap.child("region").getValue(String.class);
                        Double rating = snap.child("rating").getValue(Double.class);

                        if (tvSellerName != null && name != null)
                            tvSellerName.setText(name);
                        if (tvSellerRegion != null && region != null)
                            tvSellerRegion.setText("📍 " + region);
                        if (tvSellerRating != null)
                            tvSellerRating.setText(rating != null && rating > 0
                                    ? String.format(Locale.getDefault(), "★ %.1f", rating)
                                    : "★ —");
                        if (tvSellerAvatar != null && name != null && !name.isEmpty())
                            tvSellerAvatar.setText(
                                    String.valueOf(name.charAt(0)).toUpperCase());

                        if (sellerCard != null && !sellerUid.equals(myUid))
                            sellerCard.setOnClickListener(v -> {
                                Intent i = new Intent(ListingDetailActivity.this,
                                        SellerProfileActivity.class);
                                i.putExtra("sellerUid", sellerUid);
                                startActivity(i);
                            });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void showMap(Listing l) {
        if (l.getLatitude() == 0 && l.getLongitude() == 0) {
            if (mapSection != null) mapSection.setVisibility(View.GONE);
            return;
        }
        if (mapSection != null) mapSection.setVisibility(View.VISIBLE);

        com.google.android.gms.maps.SupportMapFragment mapFrag =
                com.google.android.gms.maps.SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapContainer, mapFrag)
                .commit();

        mapFrag.getMapAsync(map -> {
            com.google.android.gms.maps.model.LatLng pos =
                    new com.google.android.gms.maps.model.LatLng(
                            l.getLatitude(), l.getLongitude());
            map.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .position(pos)
                    .title(l.getTitle())
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory
                            .defaultMarker(com.google.android.gms.maps.model
                                    .BitmapDescriptorFactory.HUE_GREEN)));
            map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory
                    .newLatLngZoom(pos, 13f));
            map.getUiSettings().setScrollGesturesEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(true);
        });
    }

    private void checkFavorite() {
        if (myUid == null || listingId == null) return;
        mDatabase.child("favorites").child(myUid).child(listingId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        isFavorite = snap.exists();
                        if (btnFavorite != null)
                            btnFavorite.setText(isFavorite ? "❤️" : "🤍");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void toggleFavorite() {
        if (myUid == null || listingId == null) return;
        isFavorite = !isFavorite;
        if (btnFavorite != null)
            btnFavorite.setText(isFavorite ? "❤️" : "🤍");
        if (isFavorite)
            mDatabase.child("favorites").child(myUid).child(listingId).setValue(true);
        else
            mDatabase.child("favorites").child(myUid).child(listingId).removeValue();
        Toast.makeText(this,
                isFavorite ? getString(R.string.added_to_favorites) : getString(R.string.removed_from_favorites),
                Toast.LENGTH_SHORT).show();
    }

    // ═════════════════════════════════════════════════════
    // Утилиты
    // ═════════════════════════════════════════════════════
    private void updateDots(int current, int total) {
        if (tvDotsIndicator == null || total <= 1) return;
        tvDotsIndicator.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++)
            sb.append(i == current ? "●" : "○");
        tvDotsIndicator.setText(sb.toString());
    }

    private void setText(TextView tv, String val) {
        if (tv != null) tv.setText(val != null ? val : "—");
    }

    private void setButtonText(LinearLayout btn, String text) {
        if (btn != null && btn.getChildCount() > 0
                && btn.getChildAt(0) instanceof TextView)
            ((TextView) btn.getChildAt(0)).setText(text);
    }

    // ═════════════════════════════════════════════════════
    // Жалоба на объявление
    // ═════════════════════════════════════════════════════
    private void openReportDialog() {
        if (myUid == null) return;
        String[] reasons = {
            "Мошенничество", "Неверная информация",
            "Запрещённый товар", "Повторное объявление", "Другое"
        };
        final int[] selected = {0};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(getString(R.string.report_reason_label));
        tvLabel.setTextSize(14);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setPadding(0, 0, 0, dp(10));
        layout.addView(tvLabel);

        RadioGroup rg = new RadioGroup(this);
        for (int i = 0; i < reasons.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(reasons[i]);
            rb.setTextSize(14);
            int fi = i;
            rb.setOnCheckedChangeListener((btn, checked) -> { if (checked) selected[0] = fi; });
            if (i == 0) rb.setChecked(true);
            rg.addView(rb);
        }
        layout.addView(rg);

        EditText etMsg = new EditText(this);
        etMsg.setHint(getString(R.string.report_details_hint));
        etMsg.setBackgroundResource(R.drawable.bg_input);
        etMsg.setPadding(dp(12), dp(10), dp(12), dp(10));
        etMsg.setMinHeight(dp(80));
        etMsg.setGravity(Gravity.TOP);
        etMsg.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        etMsg.setLayoutParams(lp);
        layout.addView(etMsg);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.report_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.action_send), (d, w) -> {
                    String msg = etMsg.getText().toString().trim();
                    submitReport(reasons[selected[0]], msg);
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    private void submitReport(String reason, String message) {
        if (currentListing == null || myUid == null) return;
        String reportId = mDatabase.child("reports").push().getKey();
        if (reportId == null) return;
        Map<String, Object> report = new HashMap<>();
        report.put("listingId",   listingId);
        report.put("reporterUid", myUid);
        report.put("targetUid",   currentListing.getUid());
        report.put("reason",      reason);
        report.put("type",        "listing");
        report.put("createdAt",   System.currentTimeMillis());
        report.put("status",      "open");
        if (!message.isEmpty()) report.put("message", message);
        mDatabase.child("reports").child(reportId).setValue(report)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, getString(R.string.report_sent),
                            Toast.LENGTH_SHORT).show();
                    if (btnReport != null) {
                        btnReport.setText("✓");
                        btnReport.setTextColor(0xFF52B788);
                        btnReport.setClickable(false);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        getString(R.string.error_send), Toast.LENGTH_SHORT).show());
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}