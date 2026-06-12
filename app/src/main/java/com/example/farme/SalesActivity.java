package com.example.farme;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.farme.adapter.ListingAdapter;
import com.example.farme.model.Listing;

import java.util.ArrayList;
import java.util.List;

public class SalesActivity extends BaseActivity {

    private RecyclerView recyclerSales;
    private LinearLayout emptySales;
    private TextView tvBack, tvTitle;
    private ListingAdapter adapter;
    private DatabaseReference mDatabase;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        mDatabase  = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        loadSales();
    }

    private void initViews() {
        recyclerSales = findViewById(R.id.recyclerSales);
        emptySales    = findViewById(R.id.emptySales);
        tvBack        = findViewById(R.id.btnBackSales);
        tvTitle       = findViewById(R.id.tvSalesTitle);

        if (tvBack != null) tvBack.setOnClickListener(v -> finish());

        adapter = new ListingAdapter(this, new ListingAdapter.OnListingClickListener() {
            @Override public void onListingClick(Listing listing) {
                android.content.Intent i = new android.content.Intent(
                        SalesActivity.this, ListingDetailActivity.class);
                i.putExtra("listingId", listing.getId());
                startActivity(i);
            }
            @Override public void onChatClick(Listing listing) {}
            @Override public void onFavoriteClick(Listing listing, boolean isFav) {}
        });

        recyclerSales.setLayoutManager(new LinearLayoutManager(this));
        recyclerSales.setAdapter(adapter);
    }

    private void loadSales() {
        // Продажи — объявления помеченные как sold
        mDatabase.child("sales").child(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> soldIds = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            soldIds.add(child.getKey());
                        }

                        if (tvTitle != null)
                            tvTitle.setText(getString(R.string.sales_count_format, soldIds.size()));

                        if (soldIds.isEmpty()) {
                            recyclerSales.setVisibility(View.GONE);
                            if (emptySales != null) emptySales.setVisibility(View.VISIBLE);
                            return;
                        }

                        // Загружаем каждое проданное объявление
                        List<Listing> sales = new ArrayList<>();
                        final int[] loaded = {0};
                        for (String id : soldIds) {
                            mDatabase.child("listings").child(id)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snap) {
                                            Listing l = snap.getValue(Listing.class);
                                            if (l != null) { l.setId(snap.getKey()); sales.add(l); }
                                            loaded[0]++;
                                            if (loaded[0] == soldIds.size()) {
                                                adapter.setListings(sales);
                                                recyclerSales.setVisibility(View.VISIBLE);
                                                if (emptySales != null)
                                                    emptySales.setVisibility(View.GONE);
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError e) { loaded[0]++; }
                                    });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SalesActivity.this,
                                "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}