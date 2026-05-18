package com.example.farme.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.farme.ChatActivity;
import com.example.farme.ListingDetailActivity;
import com.example.farme.R;
import com.example.farme.adapter.ListingAdapter;
import com.example.farme.model.Listing;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerFavorites;
    private LinearLayout emptyFavorites;
    private TextView tvFavCount, btnClearAll;
    private ListingAdapter adapter;

    private DatabaseReference mDatabase;
    private String myUid;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        myUid     = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews(view);
        setupRecyclerView();
        loadFavorites();
    }

    // ─── ИНИЦИАЛИЗАЦИЯ ────────────────────────────────────────
    private void initViews(View view) {
        recyclerFavorites = view.findViewById(R.id.recyclerFavorites);
        emptyFavorites    = view.findViewById(R.id.emptyFavorites);
        tvFavCount        = view.findViewById(R.id.tvFavCount);
        btnClearAll       = view.findViewById(R.id.btnClearAll);

        // Очистить все избранные
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> confirmClearAll());
        }

        // Кнопка "Смотреть объявления" на пустом экране
        View btnGoHome = view.findViewById(R.id.btnGoToHome);
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                // Переключаемся на HomeFragment
                if (getActivity() instanceof com.example.farme.MainActivity) {
                    ((com.example.farme.MainActivity) getActivity())
                            .loadFragment(new HomeFragment(), false);
                    ((com.example.farme.MainActivity) getActivity())
                            .setActiveTab(0);
                }
            });
        }
    }

    // ─── RECYCLERVIEW ─────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new ListingAdapter(requireContext(), new ListingAdapter.OnListingClickListener() {
            @Override
            public void onListingClick(Listing listing) {
                Intent intent = new Intent(requireContext(), ListingDetailActivity.class);
                intent.putExtra("listingId", listing.getId());
                startActivity(intent);
            }

            @Override
            public void onChatClick(Listing listing) {
                String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (myUid.equals(listing.getUid())) {
                    Toast.makeText(requireContext(),
                            "Это ваше объявление", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("sellerUid",    listing.getUid());
                intent.putExtra("listingId",    listing.getId());
                intent.putExtra("listingTitle", listing.getTitle());
                startActivity(intent);
            }

            @Override
            public void onFavoriteClick(Listing listing, boolean isFavorite) {
                // Удаляем из избранного при нажатии ❤
                if (!isFavorite) {
                    removeFavorite(listing.getId());
                }
            }
        });

        recyclerFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerFavorites.setAdapter(adapter);
    }

    // ─── ЗАГРУЗКА ИЗБРАННОГО ──────────────────────────────────
    private void loadFavorites() {
        mDatabase.child("favorites").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> favIds = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            favIds.add(child.getKey());
                        }

                        // Обновляем счётчик
                        updateCounter(favIds.size());

                        if (favIds.isEmpty()) {
                            adapter.setListings(new ArrayList<>());
                            showEmpty(true);
                            return;
                        }

                        // Загружаем каждое объявление
                        loadListingsByIds(favIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(),
                                "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadListingsByIds(List<String> ids) {
        List<Listing> listings = new ArrayList<>();
        final int[] loaded = {0};

        for (String id : ids) {
            mDatabase.child("listings").child(id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Listing listing = snapshot.getValue(Listing.class);
                            if (listing != null) {
                                listing.setId(snapshot.getKey());
                                listings.add(listing);
                            }
                            loaded[0]++;
                            if (loaded[0] == ids.size()) {
                                // Сортируем по дате
                                listings.sort((a, b) ->
                                        Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                                adapter.setListings(listings);
                                showEmpty(listings.isEmpty());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loaded[0]++;
                            if (loaded[0] == ids.size()) {
                                adapter.setListings(listings);
                                showEmpty(listings.isEmpty());
                            }
                        }
                    });
        }
    }

    // ─── УДАЛЕНИЕ ИЗ ИЗБРАННОГО ───────────────────────────────
    private void removeFavorite(String listingId) {
        mDatabase.child("favorites").child(myUid).child(listingId)
                .removeValue()
                .addOnSuccessListener(v ->
                        Toast.makeText(requireContext(),
                                "Удалено из избранного", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─── ОЧИСТИТЬ ВСЁ ─────────────────────────────────────────
    private void confirmClearAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.clear_favorites_title))
                .setMessage(getString(R.string.clear_favorites_message))
                .setPositiveButton(getString(R.string.action_delete), (dialog, which) -> {
                    mDatabase.child("favorites").child(myUid).removeValue()
                            .addOnSuccessListener(v ->
                                    Toast.makeText(requireContext(),
                                            getString(R.string.empty_favorites), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show();
    }

    // ─── ВСПОМОГАТЕЛЬНЫЕ ──────────────────────────────────────
    private void updateCounter(int count) {
        if (tvFavCount != null) {
            tvFavCount.setText(count + " объявлений сохранено");
        }
    }

    private void showEmpty(boolean empty) {
        if (recyclerFavorites == null || emptyFavorites == null) return;
        recyclerFavorites.setVisibility(empty ? View.GONE  : View.VISIBLE);
        emptyFavorites.setVisibility(   empty ? View.VISIBLE : View.GONE);
    }
}