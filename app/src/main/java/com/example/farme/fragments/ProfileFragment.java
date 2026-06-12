package com.example.farme.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.example.farme.AdminSupportActivity;
import com.example.farme.EditProfileActivity;
import com.example.farme.FavoritesActivity;
import com.example.farme.MyListingsActivity;
import com.example.farme.NotificationsActivity;
import com.example.farme.R;
import com.example.farme.ReviewsActivity;
import com.example.farme.SettingsActivity;
import com.example.farme.SupportActivity;
import com.example.farme.auth.AuthActivity;

import java.util.Locale;

/**
 * Простой профиль фермера/покупателя.
 * Аватар, имя, регион, рейтинг, статистика, меню действий.
 */
public class ProfileFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView tvAvatarInitials, tvUserName, tvRegion, tvRating, tvReviewsCount;
    private TextView statListings, statSold, statReviews;
    private TextView btnSettings;
    private View btnEditProfile, btnLogout;
    private View rowMyListings, rowFavorites, rowReviews, rowNotifications;
    private View rowSettings, rowSupport, rowAbout, rowAdminPanel, dividerAdminPanel;
    private SwipeRefreshLayout swipeRefresh;

    private FirebaseAuth      mAuth;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() == null) {
            goToAuth();
            return;
        }
        currentUid = mAuth.getCurrentUser().getUid();

        bindViews(view);
        setupMenuRows();
        setupClickListeners();
        loadUserData();
        loadStatistics();

        swipeRefresh.setOnRefreshListener(() -> {
            loadUserData();
            loadStatistics();
        });
        swipeRefresh.setColorSchemeColors(0xFF2D6A4F);
    }

    private void bindViews(View v) {
        imgAvatar         = v.findViewById(R.id.imgAvatar);
        tvAvatarInitials  = v.findViewById(R.id.tvAvatarInitials);
        tvUserName        = v.findViewById(R.id.tvUserName);
        tvRegion          = v.findViewById(R.id.tvRegion);
        tvRating          = v.findViewById(R.id.tvRating);
        tvReviewsCount    = v.findViewById(R.id.tvReviewsCount);
        statListings      = v.findViewById(R.id.statListings);
        statSold          = v.findViewById(R.id.statSold);
        statReviews       = v.findViewById(R.id.statReviews);
        btnSettings       = v.findViewById(R.id.btnSettings);
        btnEditProfile    = v.findViewById(R.id.btnEditProfile);
        btnLogout         = v.findViewById(R.id.btnLogout);
        rowMyListings     = v.findViewById(R.id.rowMyListings);
        rowFavorites      = v.findViewById(R.id.rowFavorites);
        rowReviews        = v.findViewById(R.id.rowReviews);
        rowNotifications  = v.findViewById(R.id.rowNotifications);
        rowSettings        = v.findViewById(R.id.rowSettings);
        rowSupport         = v.findViewById(R.id.rowSupport);
        rowAbout           = v.findViewById(R.id.rowAbout);
        rowAdminPanel      = v.findViewById(R.id.rowAdminPanel);
        dividerAdminPanel  = v.findViewById(R.id.dividerAdminPanel);
        swipeRefresh       = v.findViewById(R.id.swipeRefresh);
    }

    private void setupMenuRows() {
        configRow(rowMyListings, "📋", getString(R.string.menu_my_listings), null);
        configRow(rowFavorites, "❤️", getString(R.string.menu_favorites), null);
        configRow(rowReviews, "⭐", getString(R.string.menu_reviews_about_me), null);
        configRow(rowNotifications, "🔔", getString(R.string.menu_notifications), null);
        configRow(rowSettings, "⚙", getString(R.string.menu_settings), null);
        configRow(rowSupport, "💬", getString(R.string.menu_support), null);
        configRow(rowAbout, "ℹ", getString(R.string.menu_about), null);
        configRow(rowAdminPanel, "🛡", getString(R.string.menu_admin_panel), getString(R.string.menu_admin_panel_subtitle));
    }

    private void configRow(View row, String icon, String title, String subtitle) {
        if (row == null) return;
        TextView ic = row.findViewById(R.id.menuIcon);
        TextView tt = row.findViewById(R.id.menuTitle);
        TextView st = row.findViewById(R.id.menuSubtitle);
        if (ic != null) ic.setText(icon);
        if (tt != null) tt.setText(title);
        if (st != null) {
            if (subtitle != null) {
                st.setText(subtitle);
                st.setVisibility(View.VISIBLE);
            } else {
                st.setVisibility(View.GONE);
            }
        }
    }

    private void setMenuBadge(View row, int count) {
        if (row == null) return;
        TextView badge = row.findViewById(R.id.menuBadge);
        if (badge != null) {
            if (count > 0) {
                badge.setText(count > 99 ? "99+" : String.valueOf(count));
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }
        }
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> startActivity(
                new Intent(getActivity(), EditProfileActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(
                new Intent(getActivity(), SettingsActivity.class)));

        rowMyListings.setOnClickListener(v -> startActivity(
                new Intent(getActivity(), MyListingsActivity.class)));
        rowFavorites.setOnClickListener(v -> startActivity(
                new Intent(getActivity(), FavoritesActivity.class)));
        rowReviews.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ReviewsActivity.class);
            i.putExtra("sellerUid", currentUid);
            startActivity(i);
        });
        rowNotifications.setOnClickListener(v -> startActivity(
                new Intent(getActivity(), NotificationsActivity.class)));
        rowSettings.setOnClickListener(v -> startActivity(
                new Intent(getActivity(), SettingsActivity.class)));

        rowSupport.setOnClickListener(v -> startActivity(
                new Intent(getActivity(), SupportActivity.class)));
        rowAbout.setOnClickListener(v -> showAboutDialog());
        if (rowAdminPanel != null)
            rowAdminPanel.setOnClickListener(v -> startActivity(
                    new Intent(getActivity(), AdminSupportActivity.class)));

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadUserData() {
        mDatabase.child("users").child(currentUid).get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (!snap.exists()) return;

                    String firstName = snap.child("firstName").getValue(String.class);
                    String lastName  = snap.child("lastName").getValue(String.class);
                    String name      = snap.child("name").getValue(String.class);
                    String region    = snap.child("region").getValue(String.class);
                    String avatar    = snap.child("avatar").getValue(String.class);
                    Double rating    = snap.child("rating").getValue(Double.class);
                    Long reviewsCount = snap.child("reviewsCount").getValue(Long.class);

                    String displayName = name != null ? name :
                            (firstName != null ? firstName : "") +
                                    (lastName != null ? " " + lastName : "");
                    tvUserName.setText(displayName.trim().isEmpty()
                            ? getString(R.string.user_no_name) : displayName);

                    tvRegion.setText("📍 " + (region != null ? region : "—"));

                    if (avatar != null && !avatar.isEmpty()) {
                        if (avatar.startsWith("data:") || avatar.length() > 200) {
                            try {
                                String base64 = avatar.contains(",")
                                        ? avatar.split(",")[1] : avatar;
                                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                                tvAvatarInitials.setVisibility(View.GONE);
                                imgAvatar.setVisibility(View.VISIBLE);
                                Glide.with(requireActivity())
                                        .load(bytes).circleCrop().into(imgAvatar);
                            } catch (Exception e) {
                                showInitials(displayName);
                            }
                        } else {
                            tvAvatarInitials.setVisibility(View.GONE);
                            imgAvatar.setVisibility(View.VISIBLE);
                            Glide.with(requireActivity()).load(avatar).circleCrop().into(imgAvatar);
                        }
                    } else {
                        showInitials(displayName);
                    }

                    double r = rating != null ? rating : 0;
                    tvRating.setText(String.format(Locale.US, "★ %.1f", r));
                    long rc = reviewsCount != null ? reviewsCount : 0;
                    tvReviewsCount.setText(getString(R.string.reviews_count_format, rc));
                    statReviews.setText(String.valueOf(rc));

                    // Показываем кнопку панели администратора только для админов
                    String role = snap.child("role").getValue(String.class);
                    boolean isAdmin = "admin".equals(role);
                    if (rowAdminPanel != null)
                        rowAdminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    if (dividerAdminPanel != null)
                        dividerAdminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
    }

    private void showInitials(String name) {
        String init = "?";
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2)
                init = (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
            else
                init = String.valueOf(parts[0].charAt(0)).toUpperCase();
        }
        tvAvatarInitials.setText(init);
        tvAvatarInitials.setVisibility(View.VISIBLE);
        imgAvatar.setVisibility(View.GONE);
    }

    private void loadStatistics() {
        mDatabase.child("listings").orderByChild("uid").equalTo(currentUid)
                .get().addOnSuccessListener(snap -> {
                    int total = 0, active = 0, sold = 0;
                    for (DataSnapshot child : snap.getChildren()) {
                        total++;
                        Boolean a = child.child("active").getValue(Boolean.class);
                        Boolean s = child.child("sold").getValue(Boolean.class);
                        if (Boolean.TRUE.equals(a)) active++;
                        if (Boolean.TRUE.equals(s)) sold++;
                    }
                    statListings.setText(String.valueOf(total));
                    statSold.setText(String.valueOf(sold));
                    setMenuBadge(rowMyListings, active);
                });

        mDatabase.child("notifications").child(currentUid)
                .orderByChild("read").equalTo(false).get()
                .addOnSuccessListener(snap ->
                        setMenuBadge(rowNotifications, (int) snap.getChildrenCount()));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(getString(R.string.action_logout), (d, w) -> {
                    mAuth.signOut();
                    goToAuth();
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.about_title))
                .setMessage(getString(R.string.about_message))
                .setPositiveButton("OK", null)
                .show();
    }

    private void goToAuth() {
        Intent i = new Intent(getActivity(), AuthActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        if (getActivity() != null) getActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            loadUserData();
            loadStatistics();
        }
    }
}