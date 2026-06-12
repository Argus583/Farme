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
import java.util.*;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterApplied {
        void onApply(FilterParams params);
    }

    public static class FilterParams {
        public String  category        = "";
        public String  subcategory     = "";
        public String  region          = "";
        public double  priceMin        = 0;
        public double  priceMax        = Double.MAX_VALUE;
        public String  sortBy          = "new";
        public boolean passportOnly    = false;
        public boolean negotiableOnly  = false;
        public boolean photoOnly       = false;
        public double  minSellerRating = 0;
    }

    public static class Filters extends FilterParams {}

    // ── Subcategories map (Russian DB keys) ──────────────────
    private static final Map<String, String[]> SUBCATEGORIES = new LinkedHashMap<>();
    static {
        SUBCATEGORIES.put("Скот",    new String[]{"КРС (Коровы)", "Овцы", "Козы", "Лошади", "Верблюды", "Свиньи"});
        SUBCATEGORIES.put("Птица",   new String[]{"Куры", "Утки", "Гуси", "Индейки", "Перепела", "Страусы"});
        SUBCATEGORIES.put("Зерно",   new String[]{"Пшеница", "Ячмень", "Кукуруза", "Овёс", "Рис", "Просо", "Гречиха"});
        SUBCATEGORIES.put("Овощи",   new String[]{"Картофель", "Морковь", "Лук", "Чеснок", "Капуста", "Помидоры", "Огурцы", "Перец", "Свёкла", "Тыква", "Редька"});
        SUBCATEGORIES.put("Фрукты",  new String[]{"Яблоки", "Груши", "Абрикосы", "Персики", "Сливы", "Виноград", "Вишня", "Черешня", "Грецкий орех", "Дыня", "Арбуз"});
        SUBCATEGORIES.put("Молоко",  new String[]{"Коровье молоко", "Козье молоко", "Кобылье молоко (Кымыз)", "Сметана", "Творог", "Сыр", "Масло", "Курут"});
        SUBCATEGORIES.put("Корма",   new String[]{"Сено", "Силос", "Солома", "Комбикорм", "Отруби", "Жмых", "Зернофураж", "Травяная мука"});
        SUBCATEGORIES.put("Техника", new String[]{"Тракторы", "Комбайны", "Сеялки", "Культиваторы", "Плуги", "Косилки", "Поливное оборудование", "Грузовики", "Мини-тракторы", "Запчасти"});
        SUBCATEGORIES.put("Услуги",  new String[]{"Вспашка", "Посев", "Уборка урожая", "Полив", "Ветеринар", "Перевозка скота", "Аренда техники", "Стрижка овец", "Ковка лошадей"});
    }

    private OnFilterApplied listener;
    private FilterParams params = new FilterParams();

    // Чипы категорий
    private TextView chipCatAll, chipCatSkot, chipCatZerno, chipCatOvoshi,
            chipCatFrukty, chipCatMoloko, chipCatPtitsa, chipCatKorma,
            chipCatTekhnika, chipCatUslugi;
    // Подкатегория
    private LinearLayout subcatSection;
    private LinearLayout chipGroupSubcat;
    // Чипы регионов
    private TextView chipRegAll, chipRegChuy, chipRegIssyk, chipRegOsh,
            chipRegJalal, chipRegNaryn, chipRegBatken, chipRegTalas,
            chipRegBishkek, chipRegOshCity;
    // Сортировка
    private TextView sortNew, sortOld, sortPriceAsc, sortPriceDesc;
    // Цена
    private EditText etPriceMin, etPriceMax;
    private TextView tvPriceRange;
    // Рейтинг
    private TextView ratingAny, rating3, rating4, rating45;
    // Переключатели
    private SwitchCompat switchPassportOnly, switchNegotiable, switchPhotoOnly;
    // Кнопки
    private LinearLayout btnApply;
    private TextView btnReset;

    public void setListener(OnFilterApplied listener) { this.listener = listener; }

    public void setInitialParams(FilterParams p) { if (p != null) params = p; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);

        chipCatAll      = view.findViewById(R.id.chipCatAll);
        chipCatSkot     = view.findViewById(R.id.chipCatSkot);
        chipCatZerno    = view.findViewById(R.id.chipCatZerno);
        chipCatOvoshi   = view.findViewById(R.id.chipCatOvoshi);
        chipCatFrukty   = view.findViewById(R.id.chipCatFrukty);
        chipCatMoloko   = view.findViewById(R.id.chipCatMoloko);
        chipCatPtitsa   = view.findViewById(R.id.chipCatPtitsa);
        chipCatKorma    = view.findViewById(R.id.chipCatKorma);
        chipCatTekhnika = view.findViewById(R.id.chipCatTekhnika);
        chipCatUslugi   = view.findViewById(R.id.chipCatUslugi);

        subcatSection   = view.findViewById(R.id.subcatSection);
        chipGroupSubcat = view.findViewById(R.id.chipGroupSubcat);

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

        sortNew       = view.findViewById(R.id.sortNew);
        sortOld       = view.findViewById(R.id.sortOld);
        sortPriceAsc  = view.findViewById(R.id.sortPriceAsc);
        sortPriceDesc = view.findViewById(R.id.sortPriceDesc);

        etPriceMin   = view.findViewById(R.id.etPriceMin);
        etPriceMax   = view.findViewById(R.id.etPriceMax);
        tvPriceRange = view.findViewById(R.id.tvPriceRange);

        ratingAny = view.findViewById(R.id.ratingAny);
        rating3   = view.findViewById(R.id.rating3);
        rating4   = view.findViewById(R.id.rating4);
        rating45  = view.findViewById(R.id.rating45);

        switchPassportOnly = view.findViewById(R.id.switchPassportOnly);
        switchNegotiable   = view.findViewById(R.id.switchNegotiable);
        switchPhotoOnly    = view.findViewById(R.id.switchPhotoOnly);

        btnApply = view.findViewById(R.id.btnApplyFilters);
        btnReset = view.findViewById(R.id.btnResetFilters);

        setupCategoryChips();
        setupRegionChips();
        setupSortChips();
        setupPriceFields();
        setupRatingChips();
        setupSwitches();

        // Restore subcategory if category was already set
        if (!params.category.isEmpty()) {
            refreshSubcatSection(params.category);
        }

        btnApply.setOnClickListener(v -> applyAndClose());
        btnReset.setOnClickListener(v -> resetAll());
    }

    // ── Категории ─────────────────────────────────────────────
    private void setupCategoryChips() {
        TextView[] chips = {chipCatAll, chipCatSkot, chipCatZerno, chipCatOvoshi,
                chipCatFrukty, chipCatMoloko, chipCatPtitsa,
                chipCatKorma, chipCatTekhnika, chipCatUslugi};
        String[] keys = {"", "Скот", "Зерно", "Овощи",
                "Фрукты", "Молоко", "Птица",
                "Корма", "Техника", "Услуги"};
        for (int i = 0; i < chips.length; i++) {
            final String key = keys[i];
            if (chips[i] == null) continue;
            chips[i].setOnClickListener(v -> {
                params.category    = key;
                params.subcategory = "";
                updateChips(chips, keys, params.category);
                refreshSubcatSection(key);
            });
        }
        updateChips(chips, keys, params.category);
    }

    private void refreshSubcatSection(String category) {
        if (subcatSection == null || chipGroupSubcat == null) return;
        String[] subs = SUBCATEGORIES.get(category);
        if (subs == null || subs.length == 0) {
            subcatSection.setVisibility(View.GONE);
            return;
        }
        subcatSection.setVisibility(View.VISIBLE);
        chipGroupSubcat.removeAllViews();

        // "Все подкатегории" chip
        TextView all = makeSubcatChip(getString(R.string.chip_cat_all));
        boolean allActive = params.subcategory.isEmpty();
        styleChip(all, allActive);
        all.setOnClickListener(v -> {
            params.subcategory = "";
            refreshSubcatChipStyles();
        });
        chipGroupSubcat.addView(all);

        for (String sub : subs) {
            TextView chip = makeSubcatChip(sub);
            boolean active = sub.equals(params.subcategory);
            styleChip(chip, active);
            chip.setOnClickListener(v -> {
                params.subcategory = sub;
                refreshSubcatChipStyles();
            });
            chipGroupSubcat.addView(chip);
        }
    }

    private void refreshSubcatChipStyles() {
        if (chipGroupSubcat == null) return;
        for (int i = 0; i < chipGroupSubcat.getChildCount(); i++) {
            View child = chipGroupSubcat.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                String text = tv.getText().toString();
                boolean isAll = getString(R.string.chip_cat_all).equals(text);
                boolean active = isAll ? params.subcategory.isEmpty()
                                       : text.equals(params.subcategory);
                styleChip(tv, active);
            }
        }
    }

    private TextView makeSubcatChip(String label) {
        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(36));
        lp.setMarginEnd(dpToPx(8));
        tv.setLayoutParams(lp);
        tv.setText(label);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(dpToPx(14), 0, dpToPx(14), 0);
        tv.setTextSize(12);
        tv.setClickable(true);
        tv.setFocusable(true);
        return tv;
    }

    private void styleChip(TextView tv, boolean active) {
        tv.setBackgroundColor(active ? 0xFF2D6A4F : 0xFFF2F5F2);
        tv.setTextColor(active ? 0xFFFFFFFF : 0xFF2D6A4F);
    }

    // ── Регионы ───────────────────────────────────────────────
    private void setupRegionChips() {
        TextView[] chips = {chipRegAll, chipRegChuy, chipRegIssyk, chipRegOsh,
                chipRegJalal, chipRegNaryn, chipRegBatken,
                chipRegTalas, chipRegBishkek, chipRegOshCity};
        String[] names = {"", "Чуйская область", "Иссык-Кульская область",
                "Ошская область", "Джалал-Абадская область", "Нарынская область",
                "Баткенская область", "Таласская область", "г. Бишкек", "г. Ош"};
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
        TextView[] chips = {sortNew, sortOld, sortPriceAsc, sortPriceDesc};
        String[]   keys  = {"new", "old", "price_asc", "price_desc"};
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

    // ── Рейтинг продавца ─────────────────────────────────────
    private void setupRatingChips() {
        TextView[] chips = {ratingAny, rating3, rating4, rating45};
        double[]   vals  = {0, 3.0, 4.0, 4.5};
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            final double val = vals[i];
            chips[i].setOnClickListener(v -> {
                params.minSellerRating = val;
                updateRatingChips(chips, vals);
            });
        }
        updateRatingChips(chips, vals);
    }

    private void updateRatingChips(TextView[] chips, double[] vals) {
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean active = vals[i] == params.minSellerRating;
            chips[i].setBackgroundColor(active ? 0xFF2D6A4F : 0xFFF2F5F2);
            chips[i].setTextColor(active ? 0xFFFFFFFF : 0xFF2D6A4F);
        }
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
        if (params.priceMin > 0 && etPriceMin != null)
            etPriceMin.setText(String.valueOf((int) params.priceMin));
        if (params.priceMax < Double.MAX_VALUE && etPriceMax != null)
            etPriceMax.setText(String.valueOf((int) params.priceMax));
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
        if (switchPassportOnly != null) switchPassportOnly.setChecked(params.passportOnly);
        if (switchNegotiable   != null) switchNegotiable.setChecked(params.negotiableOnly);
        if (switchPhotoOnly    != null) switchPhotoOnly.setChecked(params.photoOnly);
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
        try {
            String minStr = etPriceMin.getText().toString().trim();
            params.priceMin = minStr.isEmpty() ? 0 : Double.parseDouble(minStr);
        } catch (Exception e) { params.priceMin = 0; }
        try {
            String maxStr = etPriceMax.getText().toString().trim();
            params.priceMax = maxStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxStr);
        } catch (Exception e) { params.priceMax = Double.MAX_VALUE; }

        if (switchPassportOnly != null) params.passportOnly   = switchPassportOnly.isChecked();
        if (switchNegotiable   != null) params.negotiableOnly = switchNegotiable.isChecked();
        if (switchPhotoOnly    != null) params.photoOnly      = switchPhotoOnly.isChecked();

        if (listener != null) listener.onApply(params);
        dismiss();
    }

    // ── Сбросить ──────────────────────────────────────────────
    private void resetAll() {
        params = new FilterParams();
        setupCategoryChips();
        setupRegionChips();
        setupSortChips();
        setupRatingChips();
        if (etPriceMin != null) etPriceMin.setText("");
        if (etPriceMax != null) etPriceMax.setText("");
        if (switchPassportOnly != null) switchPassportOnly.setChecked(false);
        if (switchNegotiable   != null) switchNegotiable.setChecked(false);
        if (switchPhotoOnly    != null) switchPhotoOnly.setChecked(false);
        if (subcatSection      != null) subcatSection.setVisibility(View.GONE);
        if (chipGroupSubcat    != null) chipGroupSubcat.removeAllViews();
        updatePriceLabel();
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
