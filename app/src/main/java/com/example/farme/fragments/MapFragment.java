package com.example.farme.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import com.example.farme.ListingDetailActivity;
import com.example.farme.R;
import com.example.farme.adapter.ListingAdapter;
import com.example.farme.model.Listing;

import java.util.*;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private RecyclerView recyclerNearby;
    private TextView tvNearbyTitle;
    private ListingAdapter nearbyAdapter;

    private DatabaseReference mDatabase;
    private final List<Listing> allListings = new ArrayList<>();
    private final List<Marker> allMarkers   = new ArrayList<>();
    private String activeCategory = "";   // empty = all

    // Чипы
    private TextView mapChipAll, mapChipSkot, mapChipZerno,
            mapChipOvoshi, mapChipFrukty, mapChipPtitsa;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_map, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initChips(view);
        initNearbyList(view);
        initMap();
        loadListings();
    }

    // ── Карта ────────────────────────────────────────────────
    private void initMap() {
        SupportMapFragment mapFrag = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.mapContainer, mapFrag).commit();
        mapFrag.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Центр Кыргызстана
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(41.2044, 74.7661), 7f));

        // Клик на маркер → открываем объявление
        googleMap.setOnMarkerClickListener(marker -> {
            String listingId = (String) marker.getTag();
            if (listingId != null) {
                Intent i = new Intent(requireContext(),
                        ListingDetailActivity.class);
                i.putExtra("listingId", listingId);
                startActivity(i);
            }
            return true;
        });

        // Добавляем маркеры если данные уже загружены
        if (!allListings.isEmpty()) addMarkersToMap();
    }

    // ── Загрузка данных ───────────────────────────────────────
    private void loadListings() {
        mDatabase.child("listings").orderByChild("active").equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allListings.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Listing l = child.getValue(Listing.class);
                            if (l != null && l.getLatitude() != 0) {
                                l.setId(child.getKey());
                                allListings.add(l);
                            }
                        }
                        filterAndShow();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Маркеры ───────────────────────────────────────────────
    private void addMarkersToMap() {
        if (googleMap == null) return;
        // Очищаем старые маркеры
        for (Marker m : allMarkers) m.remove();
        allMarkers.clear();

        List<Listing> filtered = getFiltered();
        for (Listing l : filtered) {
            LatLng pos = new LatLng(l.getLatitude(), l.getLongitude());
            String priceLabel = l.getPrice() > 0
                    ? formatPrice(l.getPrice()) + " с" : "Дог.";

            // Цветная метка с ценой
            boolean isLivestock = "Скот".equals(l.getCategory());
            BitmapDescriptor icon = makePriceBitmap(priceLabel, isLivestock);

            MarkerOptions opts = new MarkerOptions()
                    .position(pos)
                    .title(l.getTitle())
                    .icon(icon);

            Marker marker = googleMap.addMarker(opts);
            if (marker != null) {
                marker.setTag(l.getId());
                allMarkers.add(marker);
            }
        }

        // Обновляем нижний список
        updateNearbyList(filtered);
    }

    // ── Кастомный маркер с ценой ─────────────────────────────
    private BitmapDescriptor makePriceBitmap(String price, boolean isLivestock) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(28f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float textW = paint.measureText(price);
        int w = (int) textW + 28;
        int h = 52;

        Bitmap bmp = Bitmap.createBitmap(w, h + 14, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Пузырёк
        paint.setColor(isLivestock ? Color.parseColor("#E74C3C")
                : Color.parseColor("#2D6A4F"));
        RectF rect = new RectF(0, 0, w, h);
        canvas.drawRoundRect(rect, 12, 12, paint);

        // Хвостик
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(w / 2f - 8, h);
        path.lineTo(w / 2f + 8, h);
        path.lineTo(w / 2f, h + 14);
        path.close();
        canvas.drawPath(path, paint);

        // Текст
        paint.setColor(Color.WHITE);
        paint.setTextSize(26f);
        float x = (w - paint.measureText(price)) / 2f;
        float y = h / 2f - (paint.descent() + paint.ascent()) / 2f;
        canvas.drawText(price, x, y, paint);

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    // ── Чипы ─────────────────────────────────────────────────
    private void initChips(View view) {
        mapChipAll    = view.findViewById(R.id.mapChipAll);
        mapChipSkot   = view.findViewById(R.id.mapChipSkot);
        mapChipZerno  = view.findViewById(R.id.mapChipZerno);
        mapChipOvoshi = view.findViewById(R.id.mapChipOvoshi);
        mapChipFrukty = view.findViewById(R.id.mapChipFrukty);
        mapChipPtitsa = view.findViewById(R.id.mapChipPtitsa);

        TextView[] chips = {mapChipAll, mapChipSkot, mapChipZerno,
                mapChipOvoshi, mapChipFrukty, mapChipPtitsa};
        // "" = all; others are Russian DB keys stored in Firebase
        String[]   cats  = {"", "Скот", "Зерно", "Овощи", "Фрукты", "Птица"};

        for (int i = 0; i < chips.length; i++) {
            final String cat = cats[i];
            if (chips[i] == null) continue;
            chips[i].setOnClickListener(v -> {
                activeCategory = cat;
                updateChips(chips, cats);
                filterAndShow();
            });
        }
    }

    private void updateChips(TextView[] chips, String[] cats) {
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean active = cats[i].equals(activeCategory);
            chips[i].setBackgroundColor(active ? 0xFF2D6A4F : 0xFFF2F5F2);
            chips[i].setTextColor(active ? 0xFFFFFFFF : 0xFF2D6A4F);
        }
    }

    private void filterAndShow() {
        addMarkersToMap();
    }

    private List<Listing> getFiltered() {
        if (activeCategory.isEmpty()) return new ArrayList<>(allListings);
        List<Listing> result = new ArrayList<>();
        for (Listing l : allListings)
            if (activeCategory.equals(l.getCategory())) result.add(l);
        return result;
    }

    // ── Нижний список ────────────────────────────────────────
    private void initNearbyList(View view) {
        recyclerNearby = view.findViewById(R.id.recyclerMapListings);
        tvNearbyTitle  = view.findViewById(R.id.tvMapNearbyTitle);

        recyclerNearby.setLayoutManager(
                new LinearLayoutManager(requireContext(),
                        LinearLayoutManager.HORIZONTAL, false));

        nearbyAdapter = new ListingAdapter(requireContext(),
                new ListingAdapter.OnListingClickListener() {
                    @Override public void onListingClick(Listing l) {
                        Intent i = new Intent(requireContext(),
                                ListingDetailActivity.class);
                        i.putExtra("listingId", l.getId());
                        startActivity(i);
                    }
                    @Override public void onChatClick(Listing l) {}
                    @Override public void onFavoriteClick(Listing l, boolean fav) {}
                });
        recyclerNearby.setAdapter(nearbyAdapter);

        // Кнопка "Все →"
        View btnAll = view.findViewById(R.id.btnMapShowAll);
        if (btnAll != null) btnAll.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.farme.MainActivity)
                ((com.example.farme.MainActivity) getActivity()).setActiveTab(0);
        });
    }

    private void updateNearbyList(List<Listing> listings) {
        if (tvNearbyTitle != null)
            tvNearbyTitle.setText(getString(R.string.nearby_count_format, listings.size()));
        nearbyAdapter.setListings(listings);
    }

    // ── Утилиты ──────────────────────────────────────────────
    private String formatPrice(double price) {
        if (price >= 1000)
            return String.format(Locale.getDefault(), "%.0f", price / 1000) + "к";
        return String.valueOf((int) price);
    }
}