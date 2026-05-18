package com.example.farme.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.firebase.database.*;
import com.example.farme.*;
import com.example.farme.R;
import com.example.farme.adapter.ListingAdapter;
import com.example.farme.model.Listing;
import java.util.*;

public class SearchFragment extends Fragment {

    private EditText     etSearch;
    private Spinner      spinnerCategory, spinnerRegion;
    private RecyclerView recycler;
    private LinearLayout emptyState;
    private TextView     tvResultCount;
    private ProgressBar  progressBar;

    private ListingAdapter    adapter;
    private List<Listing>     allListings = new ArrayList<>();
    private DatabaseReference mDatabase;

    private static final String[] CATEGORIES = {
            "Все категории","Скот","Зерно","Овощи",
            "Фрукты","Молоко","Птица","Корма","Техника","Услуги"
    };
    private static final String[] REGIONS = {
            "Все регионы","Чуйская область","Иссык-Кульская область",
            "Ошская область","Джалал-Абадская область","Нарынская область",
            "Баткенская область","Таласская область","г. Бишкек","г. Ош"
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_search, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etSearch       = view.findViewById(R.id.etSearch);
        spinnerCategory= view.findViewById(R.id.spinnerCategory);
        spinnerRegion  = view.findViewById(R.id.spinnerRegion);
        recycler       = view.findViewById(R.id.recyclerListings);
        emptyState     = view.findViewById(R.id.emptyState);
        tvResultCount  = view.findViewById(R.id.tvResultCount);
        progressBar    = view.findViewById(R.id.progressBar);

        setupSpinners();
        setupAdapter();
        setupSearch();
        loadListings();
    }

    private void setupSpinners() {
        if (spinnerCategory != null) {
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, CATEGORIES);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(a);
            spinnerCategory.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int i, long id) {
                            applyFilters(); }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });
        }
        if (spinnerRegion != null) {
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, REGIONS);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRegion.setAdapter(a);
            spinnerRegion.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int i, long id) {
                            applyFilters(); }
                        @Override public void onNothingSelected(AdapterView<?> p) {}
                    });
        }
    }

    private void setupAdapter() {
        adapter = new ListingAdapter(requireContext(),
                new ListingAdapter.OnListingClickListener() {
                    @Override public void onListingClick(Listing l) {
                        Intent i = new Intent(requireContext(),
                                ListingDetailActivity.class);
                        i.putExtra("listingId", l.getId());
                        startActivity(i);
                    }
                    @Override public void onChatClick(Listing l) {}
                    @Override public void onFavoriteClick(Listing l, boolean f) {}
                });
        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            recycler.setAdapter(adapter);
        }
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                applyFilters();
            }
        });
    }

    private void loadListings() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("listings").orderByChild("active").equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        allListings.clear();
                        for (DataSnapshot child : snap.getChildren()) {
                            Listing l = child.getValue(Listing.class);
                            if (l != null) { l.setId(child.getKey()); allListings.add(l); }
                        }
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        applyFilters();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void applyFilters() {
        String query = etSearch != null
                ? etSearch.getText().toString().trim().toLowerCase() : "";
        String cat = spinnerCategory != null && spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString() : "Все категории";
        String reg = spinnerRegion != null && spinnerRegion.getSelectedItem() != null
                ? spinnerRegion.getSelectedItem().toString() : "Все регионы";

        List<Listing> filtered = new ArrayList<>();
        for (Listing l : allListings) {
            boolean mQ = query.isEmpty()
                    || (l.getTitle() != null && l.getTitle().toLowerCase().contains(query))
                    || (l.getDescription() != null && l.getDescription().toLowerCase().contains(query));
            boolean mC = "Все категории".equals(cat) || cat.equals(l.getCategory());
            boolean mR = "Все регионы".equals(reg) || reg.equals(l.getRegion());
            if (mQ && mC && mR) filtered.add(l);
        }

        filtered.sort((a,b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        adapter.setListings(filtered);

        boolean empty = filtered.isEmpty();
        if (emptyState != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recycler != null) recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (tvResultCount != null)
            tvResultCount.setText("Найдено: " + filtered.size());
    }
}