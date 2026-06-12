package com.example.farme.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.farme.*;
import com.example.farme.R;
import com.example.farme.adapter.FeaturedListingAdapter;
import com.example.farme.adapter.ListingAdapter;
import com.example.farme.model.Listing;
import com.example.farme.utils.HistoryManager;
import java.util.*;

public class HomeFragment extends Fragment {

    private EditText      etSearch;
    private LinearLayout  emptyState, btnFilter, sectionHistory;
    private RecyclerView  recyclerFeatured, recyclerListings;
    private TextView      tvHeaderAvatar, tvLocation;
    private View          loadingView;

    private FeaturedListingAdapter featuredAdapter;
    private ListingAdapter         listingAdapter;

    private final List<Listing> allListings      = new ArrayList<>();
    private final List<Listing> featuredListings = new ArrayList<>();
    private final Set<String>   favoritesIds     = new HashSet<>();

    private String  filterCategory   = "";   // empty = all
    private String  filterRegion     = "";   // empty = all
    private double  filterPriceMin   = 0;
    private double  filterPriceMax   = Double.MAX_VALUE;
    private String  filterSort       = "new";
    private boolean filterPassport   = false;
    private boolean filterNegotiable = false;
    private boolean filterPhotoOnly    = false;
    private double  filterMinRating    = 0;
    private String  filterSubcategory  = "";

    private DatabaseReference mDatabase;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_home, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initViews(view);
        setupFeaturedRecycler();
        setupListingsRecycler();
        setupSearch();
        setupCategoryChips(view);
        setupFilterButton();
        loadUserInfo();
        loadFavorites();
        loadListings();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!allListings.isEmpty()) loadFeatured();
    }

    private void initViews(View v) {
        etSearch       = v.findViewById(R.id.etSearch);
        emptyState     = v.findViewById(R.id.emptyState);
        btnFilter      = v.findViewById(R.id.btnFilter);
        recyclerFeatured = v.findViewById(R.id.recyclerFeatured);
        recyclerListings = v.findViewById(R.id.recyclerListings);
        tvHeaderAvatar = v.findViewById(R.id.tvHeaderAvatar);
        tvLocation     = v.findViewById(R.id.tvLocation);
        sectionHistory = v.findViewById(R.id.sectionHistory);

        // Уведомления
        FrameLayout btnNotif = v.findViewById(R.id.btnNotifications);
        if (btnNotif != null)
            btnNotif.setOnClickListener(x -> startActivity(
                    new Intent(requireContext(), NotificationsActivity.class)));

        // Аватар → профиль
        if (tvHeaderAvatar != null)
            tvHeaderAvatar.setOnClickListener(x -> {
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).openProfile();
            });

        // Очистить историю
        TextView btnClear = v.findViewById(R.id.btnSeeAllFavorites);
        if (btnClear != null)
            btnClear.setOnClickListener(x -> {
                HistoryManager.clearHistory(requireContext());
                featuredListings.clear();
                if (featuredAdapter != null)
                    featuredAdapter.setListings(featuredListings);
                if (sectionHistory != null)
                    sectionHistory.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        getString(R.string.history_cleared), Toast.LENGTH_SHORT).show();
            });
    }

    private void setupFeaturedRecycler() {
        featuredAdapter = new FeaturedListingAdapter(requireContext(),
                l -> {
                    Intent i = new Intent(requireContext(),
                            ListingDetailActivity.class);
                    i.putExtra("listingId", l.getId());
                    startActivity(i);
                });
        if (recyclerFeatured != null) {
            recyclerFeatured.setLayoutManager(new LinearLayoutManager(
                    requireContext(), LinearLayoutManager.HORIZONTAL, false));
            recyclerFeatured.setAdapter(featuredAdapter);
        }
    }

    private void setupListingsRecycler() {
        listingAdapter = new ListingAdapter(requireContext(),
                new ListingAdapter.OnListingClickListener() {
                    @Override public void onListingClick(Listing l) {
                        Intent i = new Intent(requireContext(),
                                ListingDetailActivity.class);
                        i.putExtra("listingId", l.getId());
                        startActivity(i);
                    }
                    @Override public void onChatClick(Listing l) {}
                    @Override public void onFavoriteClick(Listing l, boolean fav) {
                        saveFavorite(l.getId(), fav);
                    }
                });
        if (recyclerListings != null) {
            recyclerListings.setLayoutManager(
                    new LinearLayoutManager(requireContext()));
            recyclerListings.setAdapter(listingAdapter);
            recyclerListings.setNestedScrollingEnabled(false);
        }
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                applyFilters(s.toString().trim());
            }
        });
    }

    private void setupCategoryChips(View view) {
        int[] ids  = { R.id.chipLivestock, R.id.chipGrain, R.id.chipTech,
                R.id.chipFeed, R.id.chipServices, R.id.chipPoultry };
        String[] cats = { "Скот","Зерно","Техника","Корма","Услуги","Птица" };

        for (int i = 0; i < ids.length; i++) {
            LinearLayout chip = view.findViewById(ids[i]);
            if (chip == null) continue;
            final String cat = cats[i];
            chip.setOnClickListener(v -> {
                filterCategory = cat.equals(filterCategory) ? "" : cat;
                updateChipStyles(view, ids, cats);
                applyFilters(etSearch != null
                        ? etSearch.getText().toString().trim() : "");
            });
        }
    }

    private void updateChipStyles(View view, int[] ids, String[] cats) {
        for (int i = 0; i < ids.length; i++) {
            LinearLayout chip = view.findViewById(ids[i]);
            if (chip == null) continue;
            boolean active = cats[i].equals(filterCategory);
            chip.setBackgroundResource(active
                    ? R.drawable.bg_chip_selected
                    : R.drawable.bg_chip_category);
            // Обновляем текст дочернего TextView
            for (int j = 0; j < chip.getChildCount(); j++) {
                View child = chip.getChildAt(j);
                if (child instanceof TextView)
                    ((TextView) child).setTextColor(
                            active ? 0xFFFFFFFF : 0xFF374151);
            }
        }
    }

    private void setupFilterButton() {
        if (btnFilter == null) return;
        btnFilter.setOnClickListener(v -> {
            FilterBottomSheet sheet = new FilterBottomSheet();
            sheet.setInitialParams(getCurrentParams());
            sheet.setListener(p -> {
                filterCategory   = p.category;
                filterRegion     = p.region;
                filterPriceMin   = p.priceMin;
                filterPriceMax   = p.priceMax;
                filterSort       = p.sortBy;
                filterPassport   = p.passportOnly;
                filterNegotiable = p.negotiableOnly;
                filterPhotoOnly   = p.photoOnly;
                filterMinRating   = p.minSellerRating;
                filterSubcategory = p.subcategory;
                applyFilters(etSearch != null
                        ? etSearch.getText().toString().trim() : "");
                boolean hasFilter = !p.category.isEmpty()
                        || !p.subcategory.isEmpty()
                        || !p.region.isEmpty()
                        || p.priceMin > 0 || p.priceMax < Double.MAX_VALUE
                        || !"new".equals(p.sortBy)
                        || p.passportOnly || p.negotiableOnly
                        || p.photoOnly || p.minSellerRating > 0;
                int activeCount = 0;
                if (!p.category.isEmpty()) activeCount++;
                if (!p.subcategory.isEmpty()) activeCount++;
                if (!p.region.isEmpty()) activeCount++;
                if (p.priceMin > 0 || p.priceMax < Double.MAX_VALUE) activeCount++;
                if (!"new".equals(p.sortBy)) activeCount++;
                if (p.passportOnly) activeCount++;
                if (p.negotiableOnly) activeCount++;
                if (p.photoOnly) activeCount++;
                if (p.minSellerRating > 0) activeCount++;
                updateFilterBadge(hasFilter, activeCount);
            });
            sheet.show(getParentFragmentManager(), "filter");
        });
    }

    private FilterBottomSheet.FilterParams getCurrentParams() {
        FilterBottomSheet.FilterParams p = new FilterBottomSheet.FilterParams();
        p.category       = filterCategory;
        p.subcategory    = filterSubcategory;
        p.region         = filterRegion;
        p.priceMin       = filterPriceMin;
        p.priceMax       = filterPriceMax;
        p.sortBy         = filterSort;
        p.passportOnly   = filterPassport;
        p.negotiableOnly = filterNegotiable;
        p.photoOnly      = filterPhotoOnly;
        p.minSellerRating = filterMinRating;
        return p;
    }

    private void updateFilterBadge(boolean hasFilter, int count) {
        if (btnFilter == null) return;
        btnFilter.setBackgroundResource(hasFilter
                ? R.drawable.bg_chip_selected
                : R.drawable.bg_filter_btn);
        for (int i = 0; i < btnFilter.getChildCount(); i++) {
            View child = btnFilter.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (tv.getText() != null && !tv.getText().toString().equals("⚙️")) {
                    tv.setText(hasFilter
                            ? getString(R.string.filter_active_count, count)
                            : getString(R.string.action_filter));
                    tv.setTextColor(hasFilter ? 0xFFFFFFFF : getResources().getColor(R.color.text_secondary, null));
                    break;
                }
            }
        }
    }

    private void loadUserInfo() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        String name   = snap.child("name").getValue(String.class);
                        String region = snap.child("region").getValue(String.class);
                        if (name != null && tvHeaderAvatar != null) {
                            String[] parts = name.trim().split("\\s+");
                            String init = parts.length >= 2
                                    ? String.valueOf(parts[0].charAt(0)).toUpperCase()
                                    + String.valueOf(parts[1].charAt(0)).toUpperCase()
                                    : String.valueOf(name.charAt(0)).toUpperCase();
                            tvHeaderAvatar.setText(init);
                        }
                        if (region != null && tvLocation != null)
                            tvLocation.setText(region);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadFavorites() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase.child("favorites").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        favoritesIds.clear();
                        for (DataSnapshot child : snap.getChildren())
                            favoritesIds.add(child.getKey());
                        if (listingAdapter != null)
                            listingAdapter.setFavorites(favoritesIds);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadListings() {
        mDatabase.child("listings")
                .orderByChild("active").equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        allListings.clear();
                        for (DataSnapshot child : snap.getChildren()) {
                            Listing l = child.getValue(Listing.class);
                            if (l != null) {
                                l.setId(child.getKey());
                                allListings.add(l);
                            }
                        }
                        applyFilters(etSearch != null
                                ? etSearch.getText().toString().trim() : "");
                        loadFeatured();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (isAdded())
                            Toast.makeText(requireContext(),
                                    getString(R.string.error_loading), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFeatured() {
        if (!isAdded()) return;
        List<String> historyIds = HistoryManager.getHistory(requireContext());
        featuredListings.clear();

        if (historyIds.isEmpty()) {
            if (sectionHistory != null)
                sectionHistory.setVisibility(View.GONE);
            return;
        }

        Map<String, Listing> map = new HashMap<>();
        for (Listing l : allListings)
            if (l.getId() != null) map.put(l.getId(), l);

        for (String id : historyIds) {
            Listing l = map.get(id);
            if (l != null) {
                boolean matchCategory = filterCategory.isEmpty()
                        || filterCategory.equals(l.getCategory());
                if (matchCategory) featuredListings.add(l);
            }
        }

        if (featuredListings.isEmpty()) {
            if (sectionHistory != null)
                sectionHistory.setVisibility(View.GONE);
        } else {
            if (sectionHistory != null)
                sectionHistory.setVisibility(View.VISIBLE);
            if (featuredAdapter != null)
                featuredAdapter.setListings(featuredListings);
        }
    }

    private void applyFilters(String query) {
        List<Listing> filtered = new ArrayList<>();
        for (Listing l : allListings) {
            boolean matchQ = query.isEmpty()
                    || (l.getTitle() != null && l.getTitle().toLowerCase()
                    .contains(query.toLowerCase()))
                    || (l.getDescription() != null && l.getDescription()
                    .toLowerCase().contains(query.toLowerCase()));
            boolean matchC = filterCategory.isEmpty()
                    || filterCategory.equals(l.getCategory());
            boolean matchR = filterRegion.isEmpty()
                    || filterRegion.equals(l.getRegion());
            boolean matchP = l.getPrice() >= filterPriceMin
                    && l.getPrice() <= filterPriceMax;
            boolean matchPass = !filterPassport || l.hasPassport();
            boolean matchNeg  = !filterNegotiable || l.isNegotiable();
            boolean matchPhoto  = !filterPhotoOnly || l.hasPhotos();
            boolean matchRating = filterMinRating <= 0 || l.getSellerRating() >= filterMinRating;
            boolean matchSubcat = filterSubcategory.isEmpty()
                    || filterSubcategory.equals(l.getSubcategory());
            if (matchQ && matchC && matchSubcat && matchR && matchP && matchPass && matchNeg && matchPhoto && matchRating)
                filtered.add(l);
        }
        switch (filterSort) {
            case "price_asc":
                filtered.sort((a,b) -> Double.compare(a.getPrice(),b.getPrice())); break;
            case "price_desc":
                filtered.sort((a,b) -> Double.compare(b.getPrice(),a.getPrice())); break;
            case "old":
                filtered.sort((a,b) -> Long.compare(a.getCreatedAt(),b.getCreatedAt())); break;
            default:
                filtered.sort((a,b) -> Long.compare(b.getCreatedAt(),a.getCreatedAt()));
        }
        if (listingAdapter != null) {
            listingAdapter.setFavorites(favoritesIds);
            listingAdapter.setListings(filtered);
        }
        boolean empty = filtered.isEmpty();
        if (recyclerListings != null)
            recyclerListings.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyState != null)
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        loadFeatured();
    }

    private void saveFavorite(String listingId, boolean isFav) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (isFav) mDatabase.child("favorites").child(uid).child(listingId).setValue(true);
        else       mDatabase.child("favorites").child(uid).child(listingId).removeValue();
    }
}