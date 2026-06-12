package com.example.farme;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    private static final int MAX_CONTENT_WIDTH_DP = 500;

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences("farme_settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "ru");
        super.attachBaseContext(LocaleHelper.wrap(base, lang));
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyTabletLayout();
    }

    private void applyTabletLayout() {
        int swDp = getResources().getConfiguration().smallestScreenWidthDp;
        if (swDp < 600) return;

        float density = getResources().getDisplayMetrics().density;
        int maxPx = (int) (MAX_CONTENT_WIDTH_DP * density);

        ViewGroup content = getWindow().getDecorView().findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;

        View child = content.getChildAt(0);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                maxPx, FrameLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        child.setLayoutParams(params);
    }
}
