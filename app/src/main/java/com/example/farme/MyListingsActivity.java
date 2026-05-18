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

public class MyListingsActivity extends AppCompatActivity {

    private RecyclerView  recycler;
    private LinearLayout  emptyState;
    private ProgressBar   progressBar;
    private TextView      tabActive, tabPending, tabArchive;
    private View          tabIndicator;
    private ListingAdapter adapter;
    private DatabaseReference mDatabase;
    private String myUid;
    private int currentTab = 0;
    private final List<Listing> allListings = new ArrayList<>();
    private ValueEventListener listingsListener;
    private Query              listingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_listings);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        myUid     = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        loadListings();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        recycler    = findViewById(R.id.recyclerListings);
        emptyState  = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
        tabActive   = findViewById(R.id.tabActive);
        tabPending  = findViewById(R.id.tabPending);
        tabArchive  = findViewById(R.id.tabArchive);
        tabIndicator= findViewById(R.id.tabIndicator);

        View btnCreate = findViewById(R.id.btnCreate);
        if (btnCreate != null)
            btnCreate.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateListingActivity.class)));

        adapter = new ListingAdapter(this,
                new ListingAdapter.OnListingClickListener() {
                    @Override public void onListingClick(Listing l) {
                        Intent i = new Intent(MyListingsActivity.this,
                                ListingDetailActivity.class);
                        i.putExtra("listingId", l.getId());
                        startActivity(i);
                    }
                    @Override public void onChatClick(Listing l) {}
                    @Override public void onFavoriteClick(Listing l, boolean f) {}
                });

        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(this));
            recycler.setAdapter(adapter);
        }

        if (tabActive  != null) tabActive.setOnClickListener(v -> selectTab(0));
        if (tabPending != null) tabPending.setOnClickListener(v -> selectTab(1));
        if (tabArchive != null) tabArchive.setOnClickListener(v -> selectTab(2));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listingsRef != null && listingsListener != null)
            listingsRef.removeEventListener(listingsListener);
    }

    private void loadListings() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        listingsRef      = mDatabase.child("listings").orderByChild("uid").equalTo(myUid);
        listingsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                allListings.clear();
                for (DataSnapshot child : snap.getChildren()) {
                    Listing l = child.getValue(Listing.class);
                    if (l != null) { l.setId(child.getKey()); allListings.add(l); }
                }
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                filterAndShow();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        };
        listingsRef.addValueEventListener(listingsListener);
    }

    private void selectTab(int tab) {
        currentTab = tab;
        int active   = getColor(R.color.green_primary);
        int inactive = getColor(R.color.text_secondary);
        if (tabActive  != null) tabActive.setTextColor(tab==0 ? active : inactive);
        if (tabPending != null) tabPending.setTextColor(tab==1 ? active : inactive);
        if (tabArchive != null) tabArchive.setTextColor(tab==2 ? active : inactive);
        filterAndShow();
    }

    private void filterAndShow() {
        List<Listing> filtered = new ArrayList<>();
        for (Listing l : allListings) {
            switch (currentTab) {
                case 0: // Активные
                    if (l.isActive() && !l.isRejected()) filtered.add(l); break;
                case 1: // На модерации
                    Boolean pend = l.isPending();
                    if (Boolean.TRUE.equals(pend) && !l.isRejected()) filtered.add(l); break;
                case 2: // Архив
                    Boolean p = l.isPending();
                    boolean archived = !l.isActive()
                            && (p == null || !p) && !l.isRejected();
                    if (l.isRejected() || archived) filtered.add(l); break;
            }
        }
        filtered.sort((a,b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        adapter.setListings(filtered);
        boolean empty = filtered.isEmpty();
        if (recycler    != null) recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyState  != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}