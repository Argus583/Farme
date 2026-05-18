package com.example.farme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import androidx.viewpager2.widget.ViewPager2;
import com.example.farme.auth.AuthActivity;

public class OnboardingActivity extends BaseActivity {

    private ViewPager2 pager;
    private TextView tvNextLabel, btnSkip;
    private LinearLayout dotsContainer;

    private final String[][] pages = {
            {"🌾", "Farme — Санарип Дыйкан",
                    "Кыргызстандын эң ири ооз\nчарба маркетплейси"},
            {"🐄", "Мал-жандык жана дан эгин",
                    "Скот, жылкы, дан, жашылча,\nмивелер жана техника"},
            {"🛡️", "Текшерилген паспорт",
                    "Ветеринардык сертификат жана\nдыйканчылык паспорт"},
            {"📍", "Жакын жайгашкан жерлер",
                    "Сиздин аймактагы жарнамаларды\nкарта аркылуу табыңыз"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager          = findViewById(R.id.onboardingPager);
        tvNextLabel    = findViewById(R.id.tvNextLabel);
        btnSkip        = findViewById(R.id.btnSkip);
        dotsContainer  = findViewById(R.id.dotsContainer);

        pager.setAdapter(new OnboardingAdapter());
        buildDots(0);

        pager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int pos) {
                        buildDots(pos);
                        tvNextLabel.setText(
                                pos == pages.length - 1
                                        ? "Начать" : "Далее");
                    }
                });

        findViewById(R.id.btnNext).setOnClickListener(v -> {
            int cur = pager.getCurrentItem();
            if (cur < pages.length - 1)
                pager.setCurrentItem(cur + 1);
            else finishOnboarding();
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void finishOnboarding() {
        getSharedPreferences("farme_prefs", MODE_PRIVATE)
                .edit().putBoolean("onboarding_done", true).apply();
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    private void buildDots(int active) {
        dotsContainer.removeAllViews();
        for (int i = 0; i < pages.length; i++) {
            View dot = new View(this);
            int size = i == active ? 28 : 16;
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            dp(size), dp(8));
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundColor(i == active
                    ? 0xFF2D6A4F : 0xFFD1D5DB);
            dotsContainer.addView(dot);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources()
                .getDisplayMetrics().density);
    }

    class OnboardingAdapter extends
            RecyclerView.Adapter<OnboardingAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_onboarding_page, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.emoji.setText(pages[pos][0]);
            h.title.setText(pages[pos][1]);
            h.desc.setText(pages[pos][2]);
        }

        @Override public int getItemCount() { return pages.length; }

        class VH extends RecyclerView.ViewHolder {
            TextView emoji, title, desc;
            VH(View v) {
                super(v);
                emoji = v.findViewById(R.id.tvOnboardEmoji);
                title = v.findViewById(R.id.tvOnboardTitle);
                desc  = v.findViewById(R.id.tvOnboardDesc);
            }
        }
    }
}