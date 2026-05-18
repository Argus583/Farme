package com.example.farme.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.farme.R;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterApplied {
        void onApply(FilterParams params);
    }

    // Параметры фильтра — публичный статический класс
    public static class FilterParams {
        public String  category      = "Все";
        public String  region        = "Все регионы";
        public double  priceMin      = 0;
        public double  priceMax      = Double.MAX_VALUE;
        public String  sortBy        = "new";
        public boolean passportOnly  = false;
        public boolean negotiableOnly = false;
    }

    // Псевдоним для обратной совместимости
    public static class Filters extends FilterParams {}

    private OnFilterApplied listener;
    private FilterParams params = new FilterParams();

    // Чипы категорий
    private TextView chipCatAll, chipCatSkot, chipCatZerno,
            chipCatOvoshi, chipCatFrukty, chipCatMoloko, chipCatPtitsa;
    // Чипы регионов
    private TextView chipRegAll, chipRegChuy, chipRegIssyk, chipRegOsh,
            chipRegJalal, chipRegNaryn, chipRegBatken, chipRegTalas,
            chipRegBishkek, chipRegOshCity;
    // Сортировка
    private TextView sortNew, sortOld, sortPriceAsc, sortPriceDesc;
    // Цена
    private EditText etPriceMin, etPriceMax;
    private TextView tvPriceRange;
    // Переключатели
    private SwitchCompat switchPassportOnly, switchNegotiable;
    // Кнопки
    private LinearLayout btnApply;
    private TextView btnReset;

    public void setListener(OnFilterApplied listener) {
        this.listener = listener;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);

        // Чипы категорий
        chipCatAll    = view.findViewById(R.id.chipCatAll);
        chipCatSkot   = view.findViewById(R.id.chipCatSkot);
        chipCatZerno  = view.findViewById(R.id.chipCatZerno);
        chipCatOvoshi = view.findViewById(R.id.chipCatOvoshi);
        chipCatFrukty = view.findViewById(R.id.chipCatFrukty);
        chipCatMoloko = view.findViewById(R.id.chipCatMoloko);
        chipCatPtitsa = view.findViewById(R.id.chipCatPtitsa);

        // Чипы регионов
        chipRegAll     = view.findViewById(R.id.chipRegAll);
        chipRegChuy    = view.findViewById(R.id.chipRegChuy);
        chipRegIssyk   = view.findViewById(R.id.chipRegIssyk);
        chipRegOsh     = view.findViewById(R.id.chipRegOsh);
        chipRegJalal   = view.findViewById(R.id.chipRegJalal);
        chipRegNaryn   = view.findViewById(R.id.chipRegNaryn);
        chipRegBatken  = view.findViewById(R.id.chipRegBatken);
        chipRegTalas   = view.findViewById(R.id.chipRegTalas);
        chipRegBishkek = view.findViewById(R.id.chipRegBishkek);
        chipRegOshCity = view.findViewById(R.id.chipRegOshCity);

        // Сортировка
        sortNew       = view.findViewById(R.id.sortNew);
        sortOld       = view.findViewById(R.id.sortOld);
        sortPriceAsc  = view.findViewById(R.id.sortPriceAsc);
        sortPriceDesc = view.findViewById(R.id.sortPriceDesc);

        // Цена
        etPriceMin   = view.findViewById(R.id.etPriceMin);
        etPriceMax   = view.findViewById(R.id.etPriceMax);
        tvPriceRange = view.findViewById(R.id.tvPriceRange);

        // Переключатели
        switchPassportOnly = view.findViewById(R.id.switchPassportOnly);
        switchNegotiable   = view.findViewById(R.id.switchNegotiable);

        // Кнопки
        btnApply = view.findViewById(R.id.btnApplyFilters);
        btnReset = view.findViewById(R.id.btnResetFilters);

        setupCategoryChips();
        setupRegionChips();
        setupSortChips();
        setupPriceFields();
        setupSwitches();

        btnApply.setOnClickListener(v -> applyAndClose());
        btnReset.setOnClickListener(v -> resetAll());
    }

    // ── Категории ─────────────────────────────────────────────
    private void setupCategoryChips() {
        TextView[] chips = { chipCatAll, chipCatSkot, chipCatZerno,
                chipCatOvoshi, chipCatFrukty, chipCatMoloko, chipCatPtitsa };
        String[]   names = { "Все", "Скот", "Зерно",
                "Овощи", "Фрукты", "Молоко", "Птица" };
        for (int i = 0; i < chips.length; i++) {
            final String name = names[i];
            if (chips[i] == null) continue;
            chips[i].setOnClickListener(v -> {
                params.category = name;
                updateChips(chips, names, params.category);
            });
        }
        updateChips(chips, names, params.category);
    }

    // ── Регионы ───────────────────────────────────────────────
    private void setupRegionChips() {
        TextView[] chips = { chipRegAll, chipRegChuy, chipRegIssyk, chipRegOsh,
                chipRegJalal, chipRegNaryn, chipRegBatken,
                chipRegTalas, chipRegBishkek, chipRegOshCity };
        String[]   names = { "Все регионы", "Чуйская область",
                "Иссык-Кульская область", "Ошская область",
                "Джалал-Абадская область", "Нарынская область",
                "Баткенская область", "Таласская область",
                "г. Бишкек", "г. Ош" };
        for (int i = 0; i < chips.length; i++) {
            final String name = names[i];
            if (chips[i] == null) continue;
            chips[i].setOnClickListener(v -> {
                params.region = name;
                updateChips(chips, names, params.region);
            });
        }
        updateChips(chips, names, params.region);
    }

    // ── Сортировка ────────────────────────────────────────────
    private void setupSortChips() {
        TextView[] chips = { sortNew, sortOld, sortPriceAsc, sortPriceDesc };
        String[]   keys  = { "new",  "old",  "price_asc",  "price_desc" };
        for (int i = 0; i < chips.length; i++) {
            final String key = keys[i];
            if (chips[i] == null) continue;
            chips[i].setOnClickListener(v -> {
                params.sortBy = key;
                updateChips(chips, keys, params.sortBy);
            });
        }
        updateChips(chips, keys, params.sortBy);
    }

    // ── Цена ─────────────────────────────────────────────────
    private void setupPriceFields() {
        updatePriceLabel();
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) { updatePriceLabel(); }
        };
        if (etPriceMin != null) etPriceMin.addTextChangedListener(watcher);
        if (etPriceMax != null) etPriceMax.addTextChangedListener(watcher);
    }

    private void updatePriceLabel() {
        if (tvPriceRange == null) return;
        String minStr = etPriceMin != null ? etPriceMin.getText().toString().trim() : "";
        String maxStr = etPriceMax != null ? etPriceMax.getText().toString().trim() : "";
        String min = minStr.isEmpty() ? "0" : minStr;
        String max = maxStr.isEmpty() ? "500 000+" : maxStr;
        tvPriceRange.setText(min + " — " + max + " сом");
    }

    // ── Переключатели ─────────────────────────────────────────
    private void setupSwitches() {
        if (switchPassportOnly != null)
            switchPassportOnly.setChecked(params.passportOnly);
        if (switchNegotiable != null)
            switchNegotiable.setChecked(params.negotiableOnly);
    }

    // ── Общий метод обновления чипов ──────────────────────────
    private void updateChips(TextView[] chips, String[] names, String selected) {
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean active = names[i].equals(selected);
            chips[i].setBackgroundColor(active ? 0xFF2D6A4F : 0xFFF2F5F2);
            chips[i].setTextColor(active ? 0xFFFFFFFF : 0xFF2D6A4F);
        }
    }

    // ── Применить ─────────────────────────────────────────────
    private void applyAndClose() {
        // Цена
        try {
            String minStr = etPriceMin.getText().toString().trim();
            params.priceMin = minStr.isEmpty() ? 0 : Double.parseDouble(minStr);
        } catch (Exception e) { params.priceMin = 0; }
        try {
            String maxStr = etPriceMax.getText().toString().trim();
            params.priceMax = maxStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxStr);
        } catch (Exception e) { params.priceMax = Double.MAX_VALUE; }

        // Переключатели
        if (switchPassportOnly != null)
            params.passportOnly   = switchPassportOnly.isChecked();
        if (switchNegotiable != null)
            params.negotiableOnly = switchNegotiable.isChecked();

        if (listener != null) listener.onApply(params);
        dismiss();
    }

    // ── Сбросить ──────────────────────────────────────────────
    private void resetAll() {
        params = new FilterParams();

        setupCategoryChips();
        setupRegionChips();
        setupSortChips();

        if (etPriceMin != null) etPriceMin.setText("");
        if (etPriceMax != null) etPriceMax.setText("");
        if (switchPassportOnly != null) switchPassportOnly.setChecked(false);
        if (switchNegotiable   != null) switchNegotiable.setChecked(false);
        updatePriceLabel();
    }
}