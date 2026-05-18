package com.example.farme;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.farme.adapter.ListingAdapter;
import com.example.farme.model.Listing;
import java.util.*;

public class FavoritesActivity extends BaseActivity {

    private RecyclerView  recycler;
    private LinearLayout  emptyState;
    private ProgressBar   progressBar;
    private ListingAdapter adapter;
    private DatabaseReference mDatabase;
    private String myUid;
    private final Set<String> favIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        myUid     = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        recycler    = findViewById(R.id.recyclerFavorites);
        emptyState  = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);

        adapter = new ListingAdapter(this, new ListingAdapter.OnListingClickListener() {
            @Override public void onListingClick(Listing l) {
                startActivity(new Intent(FavoritesActivity.this,
                        ListingDetailActivity.class)
                        .putExtra("listingId", l.getId()));
            }
            @Override public void onChatClick(Listing l) {}
            @Override public void onFavoriteClick(Listing l, boolean fav) {
                if (!fav) {
                    mDatabase.child("favorites").child(myUid)
                            .child(l.getId()).removeValue();
                    favIds.remove(l.getId());
                }
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        loadFavorites();
    }

    private void loadFavorites() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("favorites").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        favIds.clear();
                        for (DataSnapshot child : snap.getChildren())
                            favIds.add(child.getKey());
                        if (favIds.isEmpty()) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            showEmpty(true);
                            return;
                        }
                        loadListings();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadListings() {
        mDatabase.child("listings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        List<Listing> items = new ArrayList<>();
                        for (DataSnapshot child : snap.getChildren()) {
                            if (!favIds.contains(child.getKey())) continue;
                            Listing l = child.getValue(Listing.class);
                            if (l != null) { l.setId(child.getKey()); items.add(l); }
                        }
                        items.sort((a,b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        adapter.setFavorites(favIds);
                        adapter.setListings(items);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        showEmpty(items.isEmpty());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void showEmpty(boolean show) {
        if (emptyState != null)
            emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recycler != null)
            recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}