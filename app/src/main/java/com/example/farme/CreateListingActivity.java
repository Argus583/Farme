package com.example.farme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.io.*;
import java.util.*;

public class CreateListingActivity extends AppCompatActivity {

    // ── Шапка ────────────────────────────────────────────
    private TextView     btnBack, tvStepTitle, tvStepCounter;
    private View         stepBar1, stepBar2, stepBar3;
    private LinearLayout step1, step2, step3;

    // ── Шаг 1: Фото ──────────────────────────────────────
    private GridLayout   gridPhotos;
    private LinearLayout btnAddPhotos;
    private TextView     tvPhotoCount;

    // ── Шаг 2: Категории ─────────────────────────────────
    private LinearLayout catBtnLivestock, catBtnPoultry, catBtnGrain,
            catBtnVeggies,  catBtnFruits,  catBtnMilk,
            catBtnFeed,     catBtnTech,    catBtnServices;

    private LinearLayout groupSubcategory;
    private TextView     tvSubcategoryLabel;
    private LinearLayout rowSubcategory;

    // Блоки полей по категориям
    private LinearLayout blockLivestock, blockPoultry, blockWeightGoods,
            blockMilk, blockTech, blockServices;

    // Скот
    private EditText     etBreedInfo, etQuantity, etAgeValue, etWeightInfo, etColor;
    private Spinner      spinnerGender, spinnerAgeUnit;
    private LinearLayout groupSingleAnimal, groupMultipleAnimals;
    private EditText     etMaleCount, etFemaleCount;
    private TextView     tvGenderCountError;
    private LinearLayout btnVaccYes, btnVaccNo;
    private TextView     tvVaccYesCheck, tvVaccNoCheck;
    private EditText     etChipNumber;
    private TextView     tvArashanHint, tvChipRequired, tvChipError;
    private LinearLayout groupChipPassport;   // скрывается при qty > 1
    private LinearLayout groupMultipleHint;   // подсказка при qty > 1

    // Птица
    private EditText     etPoultryCount, etPoultryAge;

    // Весовые товары (Зерно/Овощи/Фрукты/Корма)
    private EditText etGoodsWeight, etGoodsSort, etHarvestYear;

    // Молоко
    private EditText etMilkVolume, etMilkFat;
    private Spinner  spinnerMilkFreq;

    // Техника
    private EditText etTechBrand, etTechYear;
    private Spinner  spinnerTechCondition;

    // Услуги
    private EditText etServiceExp;

    // Общие поля
    private EditText     etTitle, etDescription, etPrice;
    private TextView     tvDescCounter;
    private TextView     btnCurrencyKGS, btnCurrencyUSD;
    private CheckBox     cbNegotiable;
    private Spinner      spinnerRegion, spinnerCategory;
    private LinearLayout btnDetectLocation;
    private TextView     tvLocationStatus;
    private ProgressBar  pbLocation;
    private FrameLayout  locationMapContainer;

    // ── Шаг 3 ────────────────────────────────────────────
    private LinearLayout passportHeader, confirmHeader;
    private EditText     etVetCertNo, etVetDate;
    private TextView     tvPreviewTitle, tvPreviewCategory, tvPreviewPrice,
            tvPreviewRegion, tvPreviewPhotoCount, tvPreviewAnimalInfo;

    // ── Кнопка «Подробнее» ───────────────────────────────
    private LinearLayout btnToggleDetails, groupDetails;
    private TextView     tvDetailsArrow;
    private boolean      detailsExpanded = false;

    // ── Нижние кнопки ────────────────────────────────────
    private LinearLayout btnPrev, btnNext;
    private TextView     tvNextLabel;
    private ProgressBar  pbSubmit;

    // ── Состояние ────────────────────────────────────────
    private DatabaseReference         mDatabase;
    private FusedLocationProviderClient fusedLocation;
    private String  myUid;
    private final List<String> base64Photos = new ArrayList<>();
    private int     currentStep      = 1;
    private double  selectedLat      = 0, selectedLng = 0;
    private boolean hasVaccinations  = true;
    private String  currency         = "KGS";
    private String  selectedCategory = "";       // Скот / Птица / Зерно …
    private String  selectedSubcat   = "";       // КРС / Курица / Пшеница …

    private static final int LOCATION_REQ = 1001;

    // ── Константы подкатегорий ────────────────────────────
    private static final Map<String, String[]> SUBCATEGORIES = new LinkedHashMap<>();
    static {
        SUBCATEGORIES.put("Скот",    new String[]{
                "КРС (Коровы)", "Овцы", "Козы", "Лошади", "Верблюды", "Свиньи"});
        SUBCATEGORIES.put("Птица",   new String[]{
                "Куры", "Утки", "Гуси", "Индейки", "Перепела", "Страусы"});
        SUBCATEGORIES.put("Зерно",   new String[]{
                "Пшеница", "Ячмень", "Кукуруза", "Овёс", "Рис", "Просо", "Гречиха"});
        SUBCATEGORIES.put("Овощи",   new String[]{
                "Картофель", "Морковь", "Лук", "Чеснок", "Капуста",
                "Помидоры", "Огурцы", "Перец", "Свёкла", "Тыква", "Редька"});
        SUBCATEGORIES.put("Фрукты",  new String[]{
                "Яблоки", "Груши", "Абрикосы", "Персики", "Сливы",
                "Виноград", "Вишня", "Черешня", "Грецкий орех", "Дыня", "Арбуз"});
        SUBCATEGORIES.put("Молоко",  new String[]{
                "Коровье молоко", "Козье молоко", "Кобылье молоко (Кымыз)",
                "Сметана", "Творог", "Сыр", "Масло", "Курут"});
        SUBCATEGORIES.put("Корма",   new String[]{
                "Сено", "Силос", "Солома", "Комбикорм", "Отруби",
                "Жмых", "Зернофураж", "Травяная мука"});
        SUBCATEGORIES.put("Техника", new String[]{
                "Тракторы", "Комбайны", "Сеялки", "Культиваторы",
                "Плуги", "Косилки", "Поливное оборудование",
                "Грузовики", "Мини-тракторы", "Запчасти"});
        SUBCATEGORIES.put("Услуги",  new String[]{
                "Вспашка", "Посев", "Уборка урожая", "Полив",
                "Ветеринар", "Перевозка скота", "Аренда техники",
                "Стрижка овец", "Ковка лошадей"});
    }

    private static final String[] REGIONS = {
            "Чуйская область", "Иссык-Кульская область", "Ошская область",
            "Джалал-Абадская область", "Нарынская область", "Баткенская область",
            "Таласская область", "г. Бишкек", "г. Ош"
    };
    private static final String[] GENDERS   = {"Самец", "Самка"};
    private static final String[] AGE_UNITS = {"лет", "месяцев"};
    private static final String[] MILK_FREQ = {
            "Разово", "Ежедневно", "Еженедельно", "Ежемесячно"};
    private static final String[] TECH_COND = {
            "Отличное", "Хорошее", "Удовлетворительное", "На запчасти"};
    private static final String[] CATEGORIES_COMPAT = {
            "Скот","Птица","Зерно","Овощи","Фрукты","Молоко","Корма","Техника","Услуги"};

    // ── Выбор фото ───────────────────────────────────────
    private final ActivityResultLauncher<String[]> pickPhotos =
            registerForActivityResult(
                    new ActivityResultContracts.OpenMultipleDocuments(),
                    uris -> { if (uris != null && !uris.isEmpty()) processPhotos(uris); });

    // ═════════════════════════════════════════════════════
    // onCreate
    // ═════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_listing);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        myUid        = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase    = FirebaseDatabase.getInstance().getReference();
        fusedLocation= LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupSpinners();
        setupListeners();
        setupCategoryButtons();

        // Назад — OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (currentStep > 1) showStep(currentStep - 1);
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });

        showStep(1);
    }

    // ═════════════════════════════════════════════════════
    // bindViews
    // ═════════════════════════════════════════════════════
    private void bindViews() {
        btnBack       = findViewById(R.id.btnBack);
        tvStepTitle   = findViewById(R.id.tvStepTitle);
        tvStepCounter = findViewById(R.id.tvStepCounter);
        stepBar1      = findViewById(R.id.stepBar1);
        stepBar2      = findViewById(R.id.stepBar2);
        stepBar3      = findViewById(R.id.stepBar3);
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);

        // Фото
        gridPhotos   = findViewById(R.id.gridPhotos);
        btnAddPhotos = findViewById(R.id.btnAddPhotos);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        // Кнопки категорий
        catBtnLivestock = findViewById(R.id.catBtnLivestock);
        catBtnPoultry   = findViewById(R.id.catBtnPoultry);
        catBtnGrain     = findViewById(R.id.catBtnGrain);
        catBtnVeggies   = findViewById(R.id.catBtnVeggies);
        catBtnFruits    = findViewById(R.id.catBtnFruits);
        catBtnMilk      = findViewById(R.id.catBtnMilk);
        catBtnFeed      = findViewById(R.id.catBtnFeed);
        catBtnTech      = findViewById(R.id.catBtnTech);
        catBtnServices  = findViewById(R.id.catBtnServices);

        // Подкатегории
        groupSubcategory  = findViewById(R.id.groupSubcategory);
        tvSubcategoryLabel= findViewById(R.id.tvSubcategoryLabel);
        rowSubcategory    = findViewById(R.id.rowSubcategory);

        // Блоки
        blockLivestock   = findViewById(R.id.blockLivestock);
        blockPoultry     = findViewById(R.id.blockPoultry);
        blockWeightGoods = findViewById(R.id.blockWeightGoods);
        blockMilk        = findViewById(R.id.blockMilk);
        blockTech        = findViewById(R.id.blockTech);
        blockServices    = findViewById(R.id.blockServices);


        // Скот
        etBreedInfo          = findViewById(R.id.etBreedInfo);
        etQuantity           = findViewById(R.id.etQuantity);
        spinnerGender        = findViewById(R.id.spinnerGender);
        etAgeValue           = findViewById(R.id.etAgeValue);
        spinnerAgeUnit       = findViewById(R.id.spinnerAgeUnit);
        etWeightInfo         = findViewById(R.id.etWeightInfo);
        etColor              = findViewById(R.id.etColor);
        groupSingleAnimal    = findViewById(R.id.groupSingleAnimal);
        groupMultipleAnimals = findViewById(R.id.groupMultipleAnimals);
        etMaleCount          = findViewById(R.id.etMaleCount);
        etFemaleCount        = findViewById(R.id.etFemaleCount);
        tvGenderCountError   = findViewById(R.id.tvGenderCountError);
        btnVaccYes           = findViewById(R.id.btnVaccYes);
        btnVaccNo            = findViewById(R.id.btnVaccNo);
        tvVaccYesCheck       = findViewById(R.id.tvVaccYesCheck);
        tvVaccNoCheck        = findViewById(R.id.tvVaccNoCheck);
        etChipNumber         = findViewById(R.id.etChipNumber);
        tvArashanHint        = findViewById(R.id.tvArashanHint);
        tvChipRequired       = findViewById(R.id.tvChipRequired);
        tvChipError          = findViewById(R.id.tvChipError);
        groupChipPassport    = findViewById(R.id.groupChipPassport);
        groupMultipleHint    = findViewById(R.id.groupMultipleHint);

        // Птица
        etPoultryCount = findViewById(R.id.etPoultryCount);
        etPoultryAge   = findViewById(R.id.etPoultryAge);

        // Весовые
        etGoodsWeight  = findViewById(R.id.etGoodsWeight);
        etGoodsSort    = findViewById(R.id.etGoodsSort);
        etHarvestYear  = findViewById(R.id.etHarvestYear);

        // Молоко
        etMilkVolume   = findViewById(R.id.etMilkVolume);
        etMilkFat      = findViewById(R.id.etMilkFat);
        spinnerMilkFreq= findViewById(R.id.spinnerMilkFreq);

        // Техника
        etTechBrand          = findViewById(R.id.etTechBrand);
        etTechYear           = findViewById(R.id.etTechYear);
        spinnerTechCondition = findViewById(R.id.spinnerTechCondition);

        // Услуги
        etServiceExp = findViewById(R.id.etServiceExp);

        // Общие
        etTitle              = findViewById(R.id.etTitle);
        etDescription        = findViewById(R.id.etDescription);
        tvDescCounter        = findViewById(R.id.tvDescCounter);
        etPrice              = findViewById(R.id.etPrice);
        btnCurrencyKGS       = findViewById(R.id.btnCurrencyKGS);
        btnCurrencyUSD       = findViewById(R.id.btnCurrencyUSD);
        cbNegotiable         = findViewById(R.id.cbNegotiable);
        spinnerRegion        = findViewById(R.id.spinnerRegion);
        spinnerCategory      = findViewById(R.id.spinnerCategory);
        btnDetectLocation    = findViewById(R.id.btnDetectLocation);
        tvLocationStatus     = findViewById(R.id.tvLocationStatus);
        pbLocation           = findViewById(R.id.pbLocation);
        locationMapContainer = findViewById(R.id.locationMapContainer);

        // Шаг 3
        passportHeader     = findViewById(R.id.passportHeader);
        confirmHeader      = findViewById(R.id.confirmHeader);
        etVetCertNo        = findViewById(R.id.etVetCertNo);
        etVetDate          = findViewById(R.id.etVetDate);
        tvPreviewTitle     = findViewById(R.id.tvPreviewTitle);
        tvPreviewCategory  = findViewById(R.id.tvPreviewCategory);
        tvPreviewPrice     = findViewById(R.id.tvPreviewPrice);
        tvPreviewRegion    = findViewById(R.id.tvPreviewRegion);
        tvPreviewPhotoCount= findViewById(R.id.tvPreviewPhotoCount);
        tvPreviewAnimalInfo= findViewById(R.id.tvPreviewAnimalInfo);

        btnPrev     = findViewById(R.id.btnPrev);
        btnNext     = findViewById(R.id.btnNext);
        tvNextLabel = findViewById(R.id.tvNextLabel);
        pbSubmit    = findViewById(R.id.pbSubmit);
    }

    // ═════════════════════════════════════════════════════
    // Спиннеры
    // ═════════════════════════════════════════════════════
    private void setupSpinners() {
        setSpinner(spinnerGender,        GENDERS);
        setSpinner(spinnerAgeUnit,       AGE_UNITS);
        setSpinner(spinnerRegion,        REGIONS);
        setSpinner(spinnerMilkFreq,      MILK_FREQ);
        setSpinner(spinnerTechCondition, TECH_COND);
        setSpinner(spinnerCategory,      CATEGORIES_COMPAT);
    }

    private void setSpinner(Spinner sp, String[] items) {
        if (sp == null) return;
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
    }

    // ═════════════════════════════════════════════════════
    // Кнопки категорий
    // ═════════════════════════════════════════════════════
    private void setupCategoryButtons() {
        Map<LinearLayout, String> catMap = new LinkedHashMap<>();
        catMap.put(catBtnLivestock, "Скот");
        catMap.put(catBtnPoultry,   "Птица");
        catMap.put(catBtnGrain,     "Зерно");
        catMap.put(catBtnVeggies,   "Овощи");
        catMap.put(catBtnFruits,    "Фрукты");
        catMap.put(catBtnMilk,      "Молоко");
        catMap.put(catBtnFeed,      "Корма");
        catMap.put(catBtnTech,      "Техника");
        catMap.put(catBtnServices,  "Услуги");

        for (Map.Entry<LinearLayout, String> e : catMap.entrySet()) {
            final String cat = e.getValue();
            e.getKey().setOnClickListener(v -> selectCategory(cat, catMap));
        }
    }

    private void selectCategory(String cat, Map<LinearLayout, String> catMap) {
        selectedCategory = cat;
        selectedSubcat   = "";

        // Подсветка выбранной кнопки
        int activeColor   = ContextCompat.getColor(this, R.color.green_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.surface_card);
        int inactiveText  = ContextCompat.getColor(this, R.color.text_secondary);
        for (Map.Entry<LinearLayout, String> e : catMap.entrySet()) {
            boolean active = e.getValue().equals(cat);
            e.getKey().setBackgroundColor(active ? activeColor : inactiveColor);
            setCatLabelColor(e.getKey(), active ? 0xFFFFFFFF : inactiveText);
        }

        // Показываем подкатегории
        String[] subs = SUBCATEGORIES.get(cat);
        if (subs != null && subs.length > 0) {
            buildSubcategoryChips(subs);
            if (groupSubcategory != null) groupSubcategory.setVisibility(View.VISIBLE);
        } else {
            if (groupSubcategory != null) groupSubcategory.setVisibility(View.GONE);
        }

        // Показываем нужный блок полей
        showCategoryBlock(cat);

        // Арашан-подсказка только для скота
        if (!"Скот".equals(cat)) {
        }

        // Валюта: Скот-Арашан → USD, остальное → KGS
        setCurrency("KGS");
    }

    private void buildSubcategoryChips(String[] subs) {
        if (rowSubcategory == null) return;
        rowSubcategory.removeAllViews();
        for (String sub : subs) {
            TextView chip = new TextView(this);
            chip.setText(sub);
            chip.setTextSize(12f);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setBackgroundResource(R.drawable.bg_chip_category);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> selectSubcategory(sub));
            rowSubcategory.addView(chip);
        }
    }

    private void selectSubcategory(String sub) {
        selectedSubcat = sub;
        if (rowSubcategory == null) return;
        int activeColor   = ContextCompat.getColor(this, R.color.green_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.surface_card);
        int inactiveText  = ContextCompat.getColor(this, R.color.text_secondary);
        for (int i = 0; i < rowSubcategory.getChildCount(); i++) {
            View v = rowSubcategory.getChildAt(i);
            if (v instanceof TextView) {
                boolean active = ((TextView) v).getText().toString().equals(sub);
                v.setBackgroundColor(active ? activeColor : inactiveColor);
                ((TextView) v).setTextColor(active ? 0xFFFFFFFF : inactiveText);
            }
        }
        // Арашан: если подкатегория содержит "Овц" → предложить USD
        if ("Скот".equals(selectedCategory)) {
            boolean arashan = etBreedInfo != null &&
                    etBreedInfo.getText().toString().toLowerCase().contains("арашан");
            if (arashan) setCurrency("USD");
        }
    }

    private void showCategoryBlock(String cat) {
        // Скрываем все специфичные блоки
        setVisible(blockLivestock,   false);
        setVisible(blockPoultry,     false);
        setVisible(blockWeightGoods, false);
        setVisible(blockMilk,        false);
        setVisible(blockTech,        false);
        setVisible(blockServices,    false);

        // Показываем нужный
        switch (cat) {
            case "Скот":
                setVisible(blockLivestock, true);
                setVaccinations(true);
                updateAnimalGroupVisibility();
                break;
            case "Птица":
                setVisible(blockPoultry, true);
                break;
            case "Зерно": case "Овощи": case "Фрукты": case "Корма":
                setVisible(blockWeightGoods, true);
                break;
            case "Молоко":
                setVisible(blockMilk, true);
                break;
            case "Техника":
                setVisible(blockTech, true);
                break;
            case "Услуги":
                setVisible(blockServices, true);
                break;
        }

        // Автоматически раскрываем детали если категория выбрана
        // (только если уже были раскрыты — иначе не трогаем)
        if (detailsExpanded && groupDetails != null)
            groupDetails.setVisibility(View.VISIBLE);
    }

    // ═════════════════════════════════════════════════════
    // Слушатели
    // ═════════════════════════════════════════════════════
    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (currentStep == 1) finish();
            else showStep(currentStep - 1);
        });
        if (btnPrev != null) btnPrev.setOnClickListener(v -> showStep(currentStep - 1));
        if (btnNext != null) btnNext.setOnClickListener(v -> handleNext());

        if (btnAddPhotos != null)
            btnAddPhotos.setOnClickListener(v -> pickPhotos.launch(new String[]{"image/*"}));

        // Кнопка «Подробнее о товаре»
        if (btnToggleDetails != null) {
            btnToggleDetails.setOnClickListener(v -> {
                detailsExpanded = !detailsExpanded;
                if (groupDetails != null)
                    groupDetails.setVisibility(detailsExpanded ? View.VISIBLE : View.GONE);
                if (tvDetailsArrow != null)
                    tvDetailsArrow.setText(detailsExpanded ? "&#8249;" : "&#8250;");
                // Обновляем текст кнопки
                if (btnToggleDetails.getChildCount() > 0
                        && btnToggleDetails.getChildAt(0) instanceof TextView) {
                    ((TextView) btnToggleDetails.getChildAt(0)).setText(
                            detailsExpanded
                                    ? getString(R.string.btn_hide_details)
                                    : getString(R.string.btn_show_details));
                }
            });
        }

        if (btnDetectLocation != null)
            btnDetectLocation.setOnClickListener(v -> requestLocation());

        // Количество скота → группы полей
        if (etQuantity != null) etQuantity.addTextChangedListener(new SimpleWatcher() {
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                updateAnimalGroupVisibility();
                validateGenderCount();
            }
        });

        // Самцов/самок
        SimpleWatcher gw = new SimpleWatcher() {
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                validateGenderCount();
            }
        };
        if (etMaleCount   != null) etMaleCount.addTextChangedListener(gw);
        if (etFemaleCount != null) etFemaleCount.addTextChangedListener(gw);

        // Порода → Арашан
        if (etBreedInfo != null) etBreedInfo.addTextChangedListener(new SimpleWatcher() {
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                updateArashanMode(s.toString().trim());
            }
        });

        // Чип
        if (etChipNumber != null) etChipNumber.addTextChangedListener(new SimpleWatcher() {
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                validateChip(s.toString().trim());
            }
        });

        // Прививки скот
        if (btnVaccYes != null) btnVaccYes.setOnClickListener(v -> setVaccinations(true));
        if (btnVaccNo  != null) btnVaccNo.setOnClickListener(v -> setVaccinations(false));

        // Валюта
        if (btnCurrencyKGS != null) btnCurrencyKGS.setOnClickListener(v -> setCurrency("KGS"));
        if (btnCurrencyUSD != null) btnCurrencyUSD.setOnClickListener(v -> setCurrency("USD"));

        // Счётчик описания
        if (etDescription != null && tvDescCounter != null)
            etDescription.addTextChangedListener(new SimpleWatcher() {
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    tvDescCounter.setText(s.length() + " / 500");
                }
            });

        // Инициализация
        setVaccinations(true);
        setCurrency("KGS");
    }

    // ═════════════════════════════════════════════════════
    // Управление шагами
    // ═════════════════════════════════════════════════════
    private void showStep(int step) {
        currentStep = step;
        if (step1 != null) step1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        if (step2 != null) step2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        if (step3 != null) step3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        String[] titles = {getString(R.string.step_photos), getString(R.string.step_about), getString(R.string.step_confirmation)};
        if (tvStepTitle  != null) tvStepTitle.setText(titles[step - 1]);
        if (tvStepCounter != null) tvStepCounter.setText(step + " / 3");

        int active   = ContextCompat.getColor(this, R.color.green_primary);
        int inactive = ContextCompat.getColor(this, R.color.divider);
        if (stepBar1 != null) stepBar1.setBackgroundColor(step >= 1 ? active : inactive);
        if (stepBar2 != null) stepBar2.setBackgroundColor(step >= 2 ? active : inactive);
        if (stepBar3 != null) stepBar3.setBackgroundColor(step >= 3 ? active : inactive);

        if (btnPrev     != null) btnPrev.setVisibility(step > 1 ? View.VISIBLE : View.GONE);
        if (tvNextLabel != null) tvNextLabel.setText(step == 3 ? getString(R.string.btn_publish) : getString(R.string.btn_next));

        if (step == 3) {
            updatePreview();
        }
    }

    private void handleNext() {
        if (currentStep == 1) {
            if (base64Photos.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_add_photo),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            showStep(2);
        } else if (currentStep == 2) {
            if (!validateStep2()) return;
            showStep(3);
        } else {
            submit();
        }
    }

    // ═════════════════════════════════════════════════════
    // Динамические поля
    // ═════════════════════════════════════════════════════
    private void updateAnimalGroupVisibility() {
        int qty = getQuantity();
        // qty = 1: показываем пол/возраст/вес/окрас, скрываем самцов/самок
        setVisible(groupSingleAnimal,    qty == 1);
        setVisible(groupMultipleAnimals, qty > 1);

        // qty > 1: скрываем чип и ветпаспорт, показываем подсказку
        setVisible(groupChipPassport,  qty == 1);
        setVisible(groupMultipleHint,  qty > 1);

        // Если скрываем чип — сбрасываем ошибку и Арашан-подсказку
        if (qty > 1) {
            setVisible(tvChipError,    false);
            setVisible(tvChipRequired, false);
            setVisible(tvArashanHint,  false);
        }
    }

    private void updateArashanMode(String breed) {
        boolean is = breed.toLowerCase().contains("арашан");
        setVisible(tvArashanHint,  is);
        setVisible(tvChipRequired, is);
        if (is && "KGS".equals(currency)) setCurrency("USD");
    }

    private boolean validateChip(String chip) {
        if (chip.isEmpty()) {
            setVisible(tvChipError, false);
            return !isArashanBreed();
        }
        boolean ok = chip.matches("\\d{15}");
        setVisible(tvChipError, !ok);
        return ok;
    }

    private boolean validateGenderCount() {
        if (getQuantity() <= 1) {
            setVisible(tvGenderCountError, false);
            return true;
        }
        int qty = getQuantity(), m = parseInt(etMaleCount), f = parseInt(etFemaleCount);
        boolean ok = (m + f == qty);
        setVisible(tvGenderCountError, (m + f > 0 && !ok));
        return ok;
    }

    private void setVaccinations(boolean has) {
        hasVaccinations = has;
        applyToggle(btnVaccYes, tvVaccYesCheck, has);
        applyToggle(btnVaccNo,  tvVaccNoCheck,  !has);
    }

    private void applyToggle(LinearLayout btn, TextView check, boolean active) {
        if (btn == null) return;
        int c = active
                ? ContextCompat.getColor(this, R.color.green_primary)
                : ContextCompat.getColor(this, R.color.surface_card);
        int textColor = active
                ? 0xFFFFFFFF
                : ContextCompat.getColor(this, R.color.text_secondary);
        btn.setBackgroundColor(c);
        if (check != null) check.setVisibility(active ? View.VISIBLE : View.GONE);
        setCatLabelColor(btn, textColor);
    }

    private void setCurrency(String cur) {
        currency = cur;
        boolean kgs = "KGS".equals(cur);
        int ac = ContextCompat.getColor(this, R.color.green_primary);
        int ic = ContextCompat.getColor(this, R.color.surface_card);
        int hint = ContextCompat.getColor(this, R.color.text_hint);
        if (btnCurrencyKGS != null) {
            btnCurrencyKGS.setBackgroundColor(kgs ? ac : ic);
            btnCurrencyKGS.setTextColor(kgs ? 0xFFFFFFFF : hint);
        }
        if (btnCurrencyUSD != null) {
            btnCurrencyUSD.setBackgroundColor(kgs ? ic : ac);
            btnCurrencyUSD.setTextColor(kgs ? hint : 0xFFFFFFFF);
        }
    }

    // ═════════════════════════════════════════════════════
    // Валидация шага 2
    // ═════════════════════════════════════════════════════
    private boolean validateStep2() {
        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_select_category), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedSubcat.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_select_subcategory), Toast.LENGTH_SHORT).show();
            return false;
        }

        // Проверки для скота
        if ("Скот".equals(selectedCategory)) {
            String qStr = etQuantity != null ? etQuantity.getText().toString().trim() : "";
            if (qStr.isEmpty() || Integer.parseInt(qStr) < 1) {
                toast(getString(R.string.error_enter_quantity));
                if (etQuantity != null) etQuantity.requestFocus();
                return false;
            }
            int qty = Integer.parseInt(qStr);
            if (qty > 1 && !validateGenderCount()) {
                toast(getString(R.string.error_gender_count_format, qty));
                return false;
            }
            String chip = etChipNumber != null
                    ? etChipNumber.getText().toString().trim() : "";
            if (getQuantity() == 1) {
                // Чип проверяем только для одной головы
                if (isArashanBreed()) {
                    if (chip.isEmpty()) { toast(getString(R.string.error_arashan_chip_required)); return false; }
                    if (!chip.matches("\\d{15}")) { toast(getString(R.string.error_chip_format)); return false; }
                } else if (!chip.isEmpty() && !chip.matches("\\d{15}")) {
                    toast(getString(R.string.error_chip_format)); return false;
                }
            }
        }

        // Птица
        if ("Птица".equals(selectedCategory)) {
            String cnt = etPoultryCount != null
                    ? etPoultryCount.getText().toString().trim() : "";
            if (cnt.isEmpty() || Integer.parseInt(cnt) < 1) {
                toast(getString(R.string.error_enter_poultry_count)); return false;
            }
        }

        // Весовые
        if (isWeightCategory()) {
            String w = etGoodsWeight != null
                    ? etGoodsWeight.getText().toString().trim() : "";
            if (w.isEmpty()) { toast(getString(R.string.error_enter_weight)); return false; }
        }

        // Молоко
        if ("Молоко".equals(selectedCategory)) {
            String v = etMilkVolume != null
                    ? etMilkVolume.getText().toString().trim() : "";
            if (v.isEmpty()) { toast(getString(R.string.error_enter_milk_volume)); return false; }
        }

        // Заголовок
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        if (title.length() < 3) {
            toast(getString(R.string.error_title_too_short));
            if (etTitle != null) etTitle.requestFocus();
            return false;
        }

        // Цена
        String priceStr = etPrice != null ? etPrice.getText().toString().trim() : "";
        if (!priceStr.isEmpty()) {
            try {
                double p = Double.parseDouble(priceStr.replace(",", "."));
                if (p < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                toast(getString(R.string.error_invalid_price));
                if (etPrice != null) etPrice.requestFocus();
                return false;
            }
        }

        return true;
    }

    // ═════════════════════════════════════════════════════
    // Превью
    // ═════════════════════════════════════════════════════
    private void updatePreview() {
        if (tvPreviewTitle != null && etTitle != null)
            tvPreviewTitle.setText(etTitle.getText().toString().trim());

        if (tvPreviewCategory != null)
            tvPreviewCategory.setText(getCatEmoji(selectedCategory)
                    + "  " + selectedCategory
                    + (selectedSubcat.isEmpty() ? "" : " → " + selectedSubcat));

        if (tvPreviewPrice != null) {
            String p = etPrice != null ? etPrice.getText().toString().trim() : "";
            try {
                double price = p.isEmpty() ? 0 : Double.parseDouble(p.replace(",", "."));
                tvPreviewPrice.setText(price == 0 ? getString(R.string.price_negotiable_label)
                        : formatPrice(price) + " " + ("USD".equals(currency) ? "USD" : "сом"));
            } catch (NumberFormatException e) {
                tvPreviewPrice.setText(getString(R.string.price_negotiable_label));
            }
        }

        if (tvPreviewRegion != null && spinnerRegion != null
                && spinnerRegion.getSelectedItem() != null)
            tvPreviewRegion.setText("📍  " + spinnerRegion.getSelectedItem());

        if (tvPreviewPhotoCount != null)
            tvPreviewPhotoCount.setText("📷  " + base64Photos.size() + " фото");

        // Инфо о животном
        if (tvPreviewAnimalInfo != null) {
            StringBuilder sb = new StringBuilder();
            if ("Скот".equals(selectedCategory)) {
                String breed = etBreedInfo != null
                        ? etBreedInfo.getText().toString().trim() : "";
                if (!breed.isEmpty()) sb.append(breed).append("  •  ");
                sb.append(getQuantity()).append(" гол.");
                if (getQuantity() == 1) {
                    String age = etAgeValue != null
                            ? etAgeValue.getText().toString().trim() : "";
                    if (!age.isEmpty()) {
                        String unit = spinnerAgeUnit != null
                                && spinnerAgeUnit.getSelectedItem() != null
                                ? spinnerAgeUnit.getSelectedItem().toString() : "лет";
                        sb.append("  •  ").append(age).append(" ").append(unit);
                    }
                }
                if (hasVaccinations) sb.append("  •  ✅ привит");
            } else if ("Птица".equals(selectedCategory)) {
                String cnt = etPoultryCount != null
                        ? etPoultryCount.getText().toString().trim() : "";
                if (!cnt.isEmpty()) sb.append(cnt).append(" гол.");
            }
            boolean show = sb.length() > 0;
            tvPreviewAnimalInfo.setText(sb.toString());
            tvPreviewAnimalInfo.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // ═════════════════════════════════════════════════════
    // Геолокация
    // ═════════════════════════════════════════════════════
    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQ);
            return;
        }
        if (pbLocation     != null) pbLocation.setVisibility(View.VISIBLE);
        if (tvLocationStatus != null) tvLocationStatus.setText(getString(R.string.detecting_location));
        try {
            fusedLocation.getLastLocation()
                    .addOnSuccessListener(loc -> {
                        if (pbLocation != null) pbLocation.setVisibility(View.GONE);
                        if (loc != null) {
                            selectedLat = loc.getLatitude();
                            selectedLng = loc.getLongitude();
                            showLocationOnMap();
                        } else {
                            if (tvLocationStatus != null)
                                tvLocationStatus.setText(getString(R.string.error_location_failed));
                            toast(getString(R.string.error_gps_disabled));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pbLocation != null) pbLocation.setVisibility(View.GONE);
                        if (tvLocationStatus != null) tvLocationStatus.setText(getString(R.string.error_gps));
                    });
        } catch (SecurityException e) {
            if (pbLocation != null) pbLocation.setVisibility(View.GONE);
        }
    }

    private void showLocationOnMap() {
        if (tvLocationStatus != null)
            tvLocationStatus.setText(String.format(Locale.getDefault(),
                    "✓ %.4f, %.4f", selectedLat, selectedLng));
        if (locationMapContainer != null)
            locationMapContainer.setVisibility(View.VISIBLE);

        SupportMapFragment mapFrag = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.locationMapContainer, mapFrag).commit();
        mapFrag.getMapAsync(map -> {
            LatLng pos = new LatLng(selectedLat, selectedLng);
            map.addMarker(new MarkerOptions().position(pos)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f));
            map.getUiSettings().setScrollGesturesEnabled(false);
            map.setOnMapClickListener(ll -> {
                selectedLat = ll.latitude; selectedLng = ll.longitude;
                map.clear();
                map.addMarker(new MarkerOptions().position(ll)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                if (tvLocationStatus != null)
                    tvLocationStatus.setText(String.format(Locale.getDefault(),
                            "✓ %.4f, %.4f", selectedLat, selectedLng));
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == LOCATION_REQ && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED)
            requestLocation();
    }

    // ═════════════════════════════════════════════════════
    // Фотографии
    // ═════════════════════════════════════════════════════
    private void processPhotos(List<Uri> uris) {
        if (base64Photos.size() + uris.size() > 5) {
            toast(getString(R.string.error_max_photos)); return;
        }
        new Thread(() -> {
            for (Uri uri : uris) {
                Bitmap original = null, scaled = null;
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is == null) continue;
                    original = BitmapFactory.decodeStream(is);
                    if (original == null) continue;
                    scaled = scaleBitmap(original, 800);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    base64Photos.add("data:image/jpeg;base64,"
                            + android.util.Base64.encodeToString(
                            baos.toByteArray(), android.util.Base64.NO_WRAP));
                } catch (Exception e) {
                    android.util.Log.e("CreateListing", "Photo error", e);
                } finally {
                    if (original != null && !original.isRecycled()) original.recycle();
                    if (scaled   != null && !scaled.isRecycled())   scaled.recycle();
                }
            }
            runOnUiThread(this::updatePhotoGrid);
        }).start();
    }

    private Bitmap scaleBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxSize && h <= maxSize)
            return src.copy(src.getConfig() != null
                    ? src.getConfig() : Bitmap.Config.ARGB_8888, false);
        float scale = maxSize / (float) Math.max(w, h);
        return Bitmap.createScaledBitmap(src,
                Math.round(w * scale), Math.round(h * scale), true);
    }

    private void updatePhotoGrid() {
        if (tvPhotoCount != null) tvPhotoCount.setText(base64Photos.size() + " / 5 фото");
        if (gridPhotos   == null) return;
        gridPhotos.removeAllViews();
        int cellSize = dp(100), margin = dp(4);
        for (int i = 0; i < base64Photos.size(); i++) {
            final int idx = i;
            try {
                String b64  = base64Photos.get(i);
                String data = b64.contains(",") ? b64.substring(b64.indexOf(",") + 1) : b64;
                byte[] bytes= android.util.Base64.decode(data, android.util.Base64.DEFAULT);
                Bitmap bmp  = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                FrameLayout container = new FrameLayout(this);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width  = cellSize; lp.height = cellSize;
                lp.setMargins(margin, margin, margin, margin);
                container.setLayoutParams(lp);

                ImageView iv = new ImageView(this);
                iv.setLayoutParams(new FrameLayout.LayoutParams(cellSize, cellSize));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(bmp);
                container.addView(iv);

                if (i == 0) {
                    TextView badge = new TextView(this);
                    badge.setText(getString(R.string.badge_main_photo)); badge.setTextSize(9);
                    badge.setTextColor(0xFFFFFFFF);
                    badge.setPadding(12, 4, 12, 4);
                    badge.setBackgroundColor(0xFF2D6A4F);
                    FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT);
                    bp.gravity = Gravity.TOP | Gravity.START;
                    bp.setMargins(8, 8, 0, 0);
                    badge.setLayoutParams(bp);
                    container.addView(badge);
                }

                TextView del = new TextView(this);
                del.setText("✕"); del.setTextSize(14);
                del.setTextColor(0xFFFFFFFF); del.setGravity(Gravity.CENTER);
                del.setBackgroundColor(0xCC000000);
                int s = dp(28);
                FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(s, s);
                rp.gravity = Gravity.TOP | Gravity.END;
                rp.setMargins(0, 4, 4, 0);
                del.setLayoutParams(rp);
                del.setClickable(true);
                del.setOnClickListener(v -> { base64Photos.remove(idx); updatePhotoGrid(); });
                container.addView(del);
                gridPhotos.addView(container);
            } catch (Exception ignored) {}
        }
    }

    // ═════════════════════════════════════════════════════
    // Отправка
    // ═════════════════════════════════════════════════════
    private void submit() {
        if (pbSubmit   != null) pbSubmit.setVisibility(View.VISIBLE);
        if (tvNextLabel != null) tvNextLabel.setText(getString(R.string.sending));

        String title  = str(etTitle);
        String desc   = str(etDescription);
        String region = spinnerRegion != null && spinnerRegion.getSelectedItem() != null
                ? spinnerRegion.getSelectedItem().toString() : "";
        double price  = 0;
        try {
            String ps = str(etPrice).replace(",", ".");
            if (!ps.isEmpty()) price = Double.parseDouble(ps);
        } catch (NumberFormatException ignored) {}

        Map<String, Object> listing = new HashMap<>();
        listing.put("uid",          myUid);
        listing.put("category",     selectedCategory);
        listing.put("subcategory",  selectedSubcat);
        listing.put("title",        title);
        listing.put("description",  desc);
        listing.put("price",        price);
        listing.put("currency",     currency);
        listing.put("negotiable",   cbNegotiable != null && cbNegotiable.isChecked());
        listing.put("region",       region);
        listing.put("latitude",     selectedLat);
        listing.put("longitude",    selectedLng);
        listing.put("photos",       base64Photos);
        listing.put("active",       false);
        listing.put("pending",      true);
        listing.put("rejected",     false);
        listing.put("createdAt",    System.currentTimeMillis());

        // Доп. поля по категории
        putCategoryFields(listing);

        mDatabase.child("users").child(myUid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        String name = snap.getValue(String.class);
                        listing.put("sellerName", name != null ? name : "");
                        if ("Скот".equals(selectedCategory))
                            listing.put("passport", buildPassport());

                        mDatabase.child("listings").push().setValue(listing)
                                .addOnSuccessListener(x -> {
                                    if (pbSubmit != null) pbSubmit.setVisibility(View.GONE);
                                    toast(getString(R.string.listing_submitted));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    if (pbSubmit   != null) pbSubmit.setVisibility(View.GONE);
                                    if (tvNextLabel != null) tvNextLabel.setText(getString(R.string.btn_publish));
                                    toast(getString(R.string.error_loading) + ": " + e.getMessage());
                                });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (pbSubmit   != null) pbSubmit.setVisibility(View.GONE);
                        if (tvNextLabel != null) tvNextLabel.setText(getString(R.string.btn_publish));
                        toast(getString(R.string.error_network_short));
                    }
                });
    }

    private void putCategoryFields(Map<String, Object> m) {
        switch (selectedCategory) {
            case "Скот":
                m.put("quantity",        getQuantity());
                m.put("hasVaccinations", hasVaccinations);
                m.put("chipNumber",      str(etChipNumber).isEmpty() ? null : str(etChipNumber));
                m.put("breed",           str(etBreedInfo));
                break;
            case "Птица":
                m.put("quantity",  parseInt(etPoultryCount));
                m.put("ageMonths", parseInt(etPoultryAge));
                break;
            case "Зерно": case "Овощи": case "Фрукты": case "Корма":
                m.put("weightKg",     parseDouble(etGoodsWeight));
                m.put("sort",         str(etGoodsSort));
                m.put("harvestYear",  str(etHarvestYear));
                break;
            case "Молоко":
                m.put("volumeLiters", parseDouble(etMilkVolume));
                m.put("fatPercent",   parseDouble(etMilkFat));
                m.put("frequency",    spinnerMilkFreq != null
                        && spinnerMilkFreq.getSelectedItem() != null
                        ? spinnerMilkFreq.getSelectedItem().toString() : "");
                break;
            case "Техника":
                m.put("brand",     str(etTechBrand));
                m.put("year",      str(etTechYear));
                m.put("condition", spinnerTechCondition != null
                        && spinnerTechCondition.getSelectedItem() != null
                        ? spinnerTechCondition.getSelectedItem().toString() : "");
                break;
            case "Услуги":
                m.put("experience", parseInt(etServiceExp));
                break;
        }
    }

    private Map<String, Object> buildPassport() {
        Map<String, Object> p = new HashMap<>();
        p.put("breed",  str(etBreedInfo));
        int qty = getQuantity();
        p.put("count",  qty);
        if (qty == 1) {
            p.put("sex",  spinnerGender != null && spinnerGender.getSelectedItem() != null
                    ? spinnerGender.getSelectedItem().toString() : "");
            int av = parseInt(etAgeValue);
            String unit = spinnerAgeUnit != null && spinnerAgeUnit.getSelectedItem() != null
                    ? spinnerAgeUnit.getSelectedItem().toString() : "лет";
            p.put("ageYears",  "лет".equals(unit) ? av : 0);
            p.put("ageMonths", "месяцев".equals(unit) ? av : 0);
            p.put("weightKg",  parseDouble(etWeightInfo));
            p.put("color",     str(etColor));
            p.put("maleCount",   1);
            p.put("femaleCount", 0);
        } else {
            p.put("sex",         "Смешанное");
            p.put("maleCount",   parseInt(etMaleCount));
            p.put("femaleCount", parseInt(etFemaleCount));
            p.put("ageYears",  0); p.put("ageMonths", 0);
            p.put("weightKg",  0.0); p.put("color", "");
        }
        // Чип и ветпаспорт — только для одной головы
        if (qty == 1) {
            String chip = str(etChipNumber);
            p.put("chipNumber", chip.isEmpty() ? null : chip);
            String cert = str(etVetCertNo);
            if (!cert.isEmpty() && !cert.startsWith("КГ-")) cert = "КГ-" + cert;
            p.put("vetCertNo", cert.isEmpty() ? null : cert);
            p.put("vetDate",   str(etVetDate));
        } else {
            p.put("chipNumber", null);
            p.put("vetCertNo",  null);
            p.put("vetDate",    "");
        }
        p.put("hasVaccinations", hasVaccinations);
        p.put("verified",   false);
        p.put("rejected",   false);
        return p;
    }

    // ═════════════════════════════════════════════════════
    // Утилиты
    // ═════════════════════════════════════════════════════
    private int getQuantity() {
        try { return Math.max(1, Integer.parseInt(str(etQuantity))); }
        catch (NumberFormatException e) { return 1; }
    }
    private int parseInt(EditText et) {
        try { return Integer.parseInt(str(et)); }
        catch (NumberFormatException e) { return 0; }
    }
    private double parseDouble(EditText et) {
        try { return Double.parseDouble(str(et).replace(",", ".")); }
        catch (NumberFormatException e) { return 0.0; }
    }
    private String str(EditText et) {
        return et != null ? et.getText().toString().trim() : "";
    }
    private boolean isArashanBreed() {
        return str(etBreedInfo).toLowerCase().contains("арашан");
    }
    private boolean isWeightCategory() {
        return "Зерно".equals(selectedCategory) || "Овощи".equals(selectedCategory)
                || "Фрукты".equals(selectedCategory) || "Корма".equals(selectedCategory);
    }
    private void setVisible(View v, boolean show) {
        if (v != null) v.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    private int dp(int val) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val,
                getResources().getDisplayMetrics());
    }
    private void setCatLabelColor(LinearLayout layout, int color) {
        if (layout == null) return;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View c = layout.getChildAt(i);
            if (c instanceof TextView) ((TextView) c).setTextColor(color);
        }
    }
    private String getCatEmoji(String cat) {
        switch (cat) {
            case "Скот":    return "🐄";
            case "Птица":   return "🐔";
            case "Зерно":   return "🌾";
            case "Овощи":   return "🥬";
            case "Фрукты":  return "🍎";
            case "Молоко":  return "🥛";
            case "Корма":   return "🌿";
            case "Техника": return "🚜";
            case "Услуги":  return "🔧";
            default:        return "📦";
        }
    }
    private String formatPrice(double price) {
        if (price >= 1_000_000) return String.format(Locale.getDefault(), "%.1f млн", price / 1_000_000);
        if (price >= 1_000)     return String.format(Locale.getDefault(), "%.0f", price)
                .replaceAll("(\\d)(?=(\\d{3})+$)", "$1 ");
        return String.valueOf((int) price);
    }

    // SimpleTextWatcher
    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) {}
        @Override public abstract void onTextChanged(CharSequence s, int a, int b, int c);
    }
}