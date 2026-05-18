package com.example.farme;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class BlocklistActivity extends BaseActivity {

    private LinearLayout listContainer, emptyState;
    private ProgressBar  progressBar;
    private DatabaseReference mDatabase;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocklist);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        myUid     = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        listContainer = findViewById(R.id.listContainer);
        emptyState    = findViewById(R.id.emptyState);
        progressBar   = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        loadBlocklist();
    }

    private void loadBlocklist() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("blocks").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        listContainer.removeAllViews();
                        if (!snap.exists() || snap.getChildrenCount() == 0) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            showEmpty(true);
                            return;
                        }
                        showEmpty(false);
                        List<String> uids = new ArrayList<>();
                        for (DataSnapshot child : snap.getChildren()) uids.add(child.getKey());
                        final int[] pending = {uids.size()};
                        for (String uid : uids) {
                            mDatabase.child("users").child(uid).child("name")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override public void onDataChange(@NonNull DataSnapshot ns) {
                                            String name = ns.getValue(String.class);
                                            addCard(uid, name != null ? name : getString(R.string.unknown));
                                            if (--pending[0] == 0 && progressBar != null)
                                                progressBar.setVisibility(View.GONE);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) {
                                            if (--pending[0] == 0 && progressBar != null)
                                                progressBar.setVisibility(View.GONE);
                                        }
                                    });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void addCard(String uid, String name) {
        CardView card = new CardView(this);
        CardView.LayoutParams lp = new CardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        card.setRadius(dp(12));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(16), dp(14), dp(16), dp(14));

        // Avatar circle
        TextView tvAvatar = new TextView(this);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        avLp.setMarginEnd(dp(12));
        tvAvatar.setLayoutParams(avLp);
        tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        tvAvatar.setTextSize(16);
        tvAvatar.setTextColor(Color.WHITE);
        tvAvatar.setGravity(Gravity.CENTER);
        tvAvatar.setBackgroundResource(R.drawable.bg_avatar_green_circle);

        // Name
        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(name);
        tvName.setTextSize(15);
        tvName.setTextColor(getColor(R.color.text_primary));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        // Unblock button
        TextView btnUnblock = new TextView(this);
        btnUnblock.setText(getString(R.string.action_unblock));
        btnUnblock.setTextSize(13);
        btnUnblock.setTextColor(getColor(R.color.green_primary));
        btnUnblock.setBackgroundResource(R.drawable.bg_chip_category);
        btnUnblock.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnUnblock.setClickable(true);
        btnUnblock.setFocusable(true);
        btnUnblock.setOnClickListener(v ->
                mDatabase.child("blocks").child(myUid).child(uid).removeValue()
                        .addOnSuccessListener(a -> {
                            listContainer.removeView(card);
                            if (listContainer.getChildCount() == 0) showEmpty(true);
                            Toast.makeText(this, getString(R.string.user_unblocked), Toast.LENGTH_SHORT).show();
                        }));

        inner.addView(tvAvatar);
        inner.addView(tvName);
        inner.addView(btnUnblock);
        card.addView(inner);
        listContainer.addView(card);
    }

    private void showEmpty(boolean show) {
        if (emptyState != null)    emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (listContainer != null) listContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
