package com.example.farme;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.farme.model.Listing;

import java.util.HashMap;
import java.util.Map;

public class EditListingActivity extends AppCompatActivity {

    private TextView btnBackEdit, btnSave;
    private EditText etTitle, etDescription, etPrice;
    private Spinner spinnerCategory, spinnerRegion;
    private LinearLayout progressLayout;

    // Паспорт
    private LinearLayout passportSection;
    private EditText etSpecies, etBreed, etAge, etCount, etVetCertNo, etVetDate;
    private Spinner spinnerSex;

    private DatabaseReference mDatabase;
    private String listingId;
    private Listing currentListing;

    private static final String[] CATEGORIES = {
            "Скот", "Зерно", "Овощи", "Фрукты", "Молоко", "Птица"
    };

    private static final String[] REGIONS = {
            "Чуйская область", "Иссык-Кульская область", "Ошская область",
            "Джалал-Абадская область", "Нарынская область",
            "Баткенская область", "Таласская область", "г. Бишкек", "г. Ош"
    };

    private static final String[] SEX = { "Самец", "Самка" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_listing);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        listingId = getIntent().getStringExtra("listingId");

        if (listingId == null) { finish(); return; }

        initViews();
        setupSpinners();
        loadListing();
    }

    private void initViews() {
        btnBackEdit   = findViewById(R.id.btnBackEditListing);
        btnSave       = findViewById(R.id.btnSaveListing);
        etTitle       = findViewById(R.id.etEditTitle);
        etDescription = findViewById(R.id.etEditDescription);
        etPrice       = findViewById(R.id.etEditPrice);
        spinnerCategory = findViewById(R.id.spinnerEditCategory);
        spinnerRegion   = findViewById(R.id.spinnerEditRegion);
        passportSection = findViewById(R.id.passportSection);

        etSpecies   = findViewById(R.id.etEditSpecies);
        etBreed     = findViewById(R.id.etEditBreed);
        etAge       = findViewById(R.id.etEditAge);
        etCount     = findViewById(R.id.etEditCount);
        etVetCertNo = findViewById(R.id.etEditVetCertNo);
        etVetDate   = findViewById(R.id.etEditVetDate);
        spinnerSex  = findViewById(R.id.spinnerEditSex);

        btnBackEdit.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveListing());
    }

    private void setupSpinners() {
        setSpinner(spinnerCategory, CATEGORIES);
        setSpinner(spinnerRegion,   REGIONS);
        setSpinner(spinnerSex,      SEX);

        // Показываем/скрываем паспорт при смене категории
        if (spinnerCategory != null) {
            spinnerCategory.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> p,
                                                   View v, int pos, long id) {
                            boolean isLivestock = "Скот".equals(CATEGORIES[pos]);
                            if (passportSection != null)
                                passportSection.setVisibility(isLivestock ? View.VISIBLE : View.GONE);
                        }
                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> p) {}
                    });
        }
    }

    private void setSpinner(Spinner s, String[] items) {
        if (s == null) return;
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
    }

    private void loadListing() {
        mDatabase.child("listings").child(listingId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentListing = snapshot.getValue(Listing.class);
                        if (currentListing == null) { finish(); return; }
                        currentListing.setId(listingId);

                        // Проверяем что это объявление текущего пользователя
                        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                        if (!currentListing.getUid().equals(myUid)) {
                            Toast.makeText(EditListingActivity.this,
                                    getString(R.string.error_access_denied), Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        fillFields(currentListing);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { finish(); }
                });
    }

    private void fillFields(Listing l) {
        if (etTitle != null) etTitle.setText(l.getTitle());
        if (etDescription != null) etDescription.setText(l.getDescription());
        if (etPrice != null && l.getPrice() > 0)
            etPrice.setText(String.valueOf((int) l.getPrice()));

        // Категория
        if (spinnerCategory != null && l.getCategory() != null) {
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equals(l.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        // Регион
        if (spinnerRegion != null && l.getRegion() != null) {
            for (int i = 0; i < REGIONS.length; i++) {
                if (REGIONS[i].equals(l.getRegion())) {
                    spinnerRegion.setSelection(i);
                    break;
                }
            }
        }

        // Паспорт
        Listing.Passport p = l.getPassport();
        if (p != null && passportSection != null) {
            passportSection.setVisibility(View.VISIBLE);
            if (etSpecies   != null) etSpecies.setText(p.getSpecies());
            if (etBreed     != null) etBreed.setText(p.getBreed());
            if (etAge       != null) etAge.setText(String.valueOf(p.getAge()));
            if (etCount     != null) etCount.setText(String.valueOf(p.getCount()));
            if (etVetCertNo != null) etVetCertNo.setText(p.getVetCertNo());
            if (etVetDate   != null) etVetDate.setText(p.getVetDate());

            if (spinnerSex != null && p.getSex() != null) {
                for (int i = 0; i < SEX.length; i++) {
                    if (SEX[i].equals(p.getSex())) {
                        spinnerSex.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    private void saveListing() {
        if (etTitle == null || etTitle.getText().toString().trim().isEmpty()) {
            if (etTitle != null) etTitle.setError(getString(R.string.error_enter_title));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title",       etTitle.getText().toString().trim());
        updates.put("description", etDescription != null
                ? etDescription.getText().toString().trim() : "");
        updates.put("region",      spinnerRegion != null
                ? spinnerRegion.getSelectedItem().toString() : "");
        updates.put("category",    spinnerCategory != null
                ? spinnerCategory.getSelectedItem().toString() : "");

        String priceStr = etPrice != null ? etPrice.getText().toString().trim() : "0";
        try { updates.put("price", Double.parseDouble(priceStr)); }
        catch (Exception e) { updates.put("price", 0.0); }

        // После редактирования снова на модерацию
        updates.put("active",  false);
        updates.put("pending", true);
        updates.put("rejected", false);

        // Паспорт
        String category = spinnerCategory != null
                ? spinnerCategory.getSelectedItem().toString() : "";
        if ("Скот".equals(category)) {
            Map<String, Object> passport = new HashMap<>();
            passport.put("species",   etSpecies   != null ? etSpecies.getText().toString().trim() : "");
            passport.put("breed",     etBreed     != null ? etBreed.getText().toString().trim() : "");
            passport.put("sex",       spinnerSex  != null ? spinnerSex.getSelectedItem().toString() : "");
            passport.put("vetCertNo", etVetCertNo != null ? etVetCertNo.getText().toString().trim() : "");
            passport.put("vetDate",   etVetDate   != null ? etVetDate.getText().toString().trim() : "");
            try { passport.put("age",   Integer.parseInt(etAge   != null ? etAge.getText().toString() : "0")); }
            catch (Exception e) { passport.put("age", 0); }
            try { passport.put("count", Integer.parseInt(etCount != null ? etCount.getText().toString() : "0")); }
            catch (Exception e) { passport.put("count", 0); }
            passport.put("verified", false); // сбрасываем верификацию
            updates.put("passport", passport);
        }

        mDatabase.child("listings").child(listingId).updateChildren(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this,
                            getString(R.string.listing_updated),
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_loading) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}