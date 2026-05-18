package com.example.farme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.farme.fragments.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navHome, navMap,
            navChats, navProfile;
    private android.widget.FrameLayout fabCreate;
    private TextView     badgeChats;
    private int          activeTab = 0;

    private DatabaseReference mDatabase;
    private final Map<String, Long>               unreadCounts    = new HashMap<>();
    private final Map<String, ValueEventListener> unreadListeners = new HashMap<>();
    private ValueEventListener chatIdsListener;
    private DatabaseReference  chatIdsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        initNav();
        loadFragment(new HomeFragment(), false);
        setActiveTab(0);
        listenUnreadChats();
    }

    private void initNav() {
        navHome   = findViewById(R.id.navHome);
        navMap    = findViewById(R.id.navFavorites); // ID navFavorites = Map
        fabCreate = findViewById(R.id.fabCreate);
        navChats  = findViewById(R.id.navChats);
        navProfile= findViewById(R.id.navProfile);
        badgeChats= findViewById(R.id.badgeChats);

        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment(), false);
            setActiveTab(0);
        });
        navChats.setOnClickListener(v -> {
            loadFragment(new ChatsFragment(), false);
            setActiveTab(1);
        });
        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this,
                        CreateListingActivity.class)));
        navMap.setOnClickListener(v -> {
            loadFragment(new MapFragment(), false);
            setActiveTab(3);
        });
        navProfile.setOnClickListener(v -> {
            loadFragment(new ProfileFragment(), false);
            setActiveTab(4);
        });
    }

    public void loadFragment(Fragment fragment, boolean addToBack) {
        var tx = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment);
        if (addToBack) tx.addToBackStack(null);
        tx.commit();
    }

    public void setActiveTab(int tab) {
        activeTab = tab;
        int active   = getColor(R.color.green_primary);
        int inactive = getColor(R.color.nav_inactive);

        if (navHome    != null) setTabColor(navHome,    tab == 0 ? active : inactive);
        if (navChats   != null) setTabColor(navChats,   tab == 1 ? active : inactive);
        if (navMap     != null) setTabColor(navMap,     tab == 3 ? active : inactive);
        if (navProfile != null) setTabColor(navProfile, tab == 4 ? active : inactive);

        // FAB кнопка "+" видна только на Главной
        if (fabCreate != null) {
            fabCreate.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setTabColor(LinearLayout nav, int color) {
        for (int i = 0; i < nav.getChildCount(); i++) {
            View child = nav.getChildAt(i);
            if (child instanceof TextView)
                ((TextView) child).setTextColor(color);
        }
    }

    public void openProfile() {
        loadFragment(new ProfileFragment(), false);
        setActiveTab(4);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatIdsRef != null && chatIdsListener != null)
            chatIdsRef.removeEventListener(chatIdsListener);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            for (Map.Entry<String, ValueEventListener> e : unreadListeners.entrySet())
                mDatabase.child("chats").child(e.getKey())
                        .child("unread").child(uid).removeEventListener(e.getValue());
        }
        unreadListeners.clear();
    }

    // ── Бейдж непрочитанных сообщений ────────────────
    private void listenUnreadChats() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatIdsRef      = mDatabase.child("users").child(uid).child("chatIds");
        chatIdsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        Set<String> newIds = new HashSet<>();
                        for (DataSnapshot ch : snap.getChildren())
                            newIds.add(ch.getKey());

                        // Убираем слушатели чатов, которых больше нет
                        for (String old : new ArrayList<>(unreadListeners.keySet())) {
                            if (!newIds.contains(old)) {
                                mDatabase.child("chats").child(old)
                                        .child("unread").child(uid)
                                        .removeEventListener(unreadListeners.remove(old));
                                unreadCounts.remove(old);
                            }
                        }

                        if (newIds.isEmpty()) { updateChatBadge(0); return; }

                        // Вешаем слушатель на unread/{uid} каждого чата
                        for (String chatId : newIds) {
                            if (unreadListeners.containsKey(chatId)) continue;
                            ValueEventListener l = new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot s) {
                                    Long val = s.getValue(Long.class);
                                    unreadCounts.put(chatId, val != null ? val : 0L);
                                    long total = 0;
                                    for (long v : unreadCounts.values()) total += v;
                                    updateChatBadge(total);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            };
                            unreadListeners.put(chatId, l);
                            mDatabase.child("chats").child(chatId)
                                    .child("unread").child(uid)
                                    .addValueEventListener(l);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                };
        chatIdsRef.addValueEventListener(chatIdsListener);
    }

    private void updateChatBadge(long count) {
        if (badgeChats == null) return;
        if (count > 0) {
            badgeChats.setText(count > 99 ? "99+" : String.valueOf(count));
            badgeChats.setVisibility(View.VISIBLE);
        } else {
            badgeChats.setVisibility(View.GONE);
        }
    }
}