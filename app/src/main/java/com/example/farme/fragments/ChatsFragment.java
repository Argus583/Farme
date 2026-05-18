package com.example.farme.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.farme.*;
import com.example.farme.R;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatsFragment extends Fragment {

    private RecyclerView recycler;
    private LinearLayout emptyState;
    private ProgressBar  progressBar;
    private EditText     etSearch;

    private ChatsAdapter adapter;
    private DatabaseReference mDatabase;
    private String myUid;

    private final List<ChatItem> allItems = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_chats, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        myUid     = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        recycler    = view.findViewById(R.id.recyclerChats);
        emptyState  = view.findViewById(R.id.emptyState);
        progressBar = view.findViewById(R.id.progressBar);
        etSearch    = view.findViewById(R.id.etChatSearch);

        adapter = new ChatsAdapter();
        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            recycler.setAdapter(adapter);
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    applySearch(s.toString().trim());
                }
            });
        }

        loadChats();
    }

    private void applySearch(String query) {
        if (query.isEmpty()) {
            adapter.setItems(new ArrayList<>(allItems));
            showEmpty(allItems.isEmpty());
            return;
        }
        String lower = query.toLowerCase();
        List<ChatItem> filtered = new ArrayList<>();
        for (ChatItem item : allItems) {
            boolean matchName = item.partnerName != null
                    && item.partnerName.toLowerCase().contains(lower);
            boolean matchTitle = item.listingTitle != null
                    && item.listingTitle.toLowerCase().contains(lower);
            if (matchName || matchTitle) filtered.add(item);
        }
        adapter.setItems(filtered);
        showEmpty(filtered.isEmpty());
    }

    private void loadChats() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("users").child(myUid).child("chatIds")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        List<String> chatIds = new ArrayList<>();
                        for (DataSnapshot child : snap.getChildren())
                            chatIds.add(child.getKey());

                        if (chatIds.isEmpty()) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            allItems.clear();
                            adapter.setItems(allItems);
                            showEmpty(true);
                            return;
                        }
                        loadChatDetails(chatIds);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadChatDetails(List<String> chatIds) {
        List<ChatItem> items = new ArrayList<>();
        final int[] pending = {chatIds.size()};

        for (String chatId : chatIds) {
            mDatabase.child("chats").child(chatId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snap) {
                            if (!isAdded()) return;
                            ChatItem item = new ChatItem();
                            item.chatId      = chatId;
                            item.lastMessage = snap.child("lastMessage").getValue(String.class);
                            item.updatedAt   = snap.child("updatedAt").getValue(Long.class);

                            for (DataSnapshot p : snap.child("participants").getChildren()) {
                                if (!myUid.equals(p.getKey())) {
                                    item.otherUid = p.getKey();
                                    break;
                                }
                            }
                            item.listingId = snap.child("listingId").getValue(String.class);
                            Long unread = snap.child("unread").child(myUid).getValue(Long.class);
                            item.unreadCount = unread != null ? unread : 0;

                            if (item.otherUid != null)
                                loadPartnerName(item, items, pending);
                            else
                                completeChats(items, pending);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {
                            completeChats(items, pending);
                        }
                    });
        }
    }

    private void loadPartnerName(ChatItem item, List<ChatItem> items, int[] pending) {
        mDatabase.child("users").child(item.otherUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        item.partnerName   = snap.child("name").getValue(String.class);
                        item.partnerRegion = snap.child("region").getValue(String.class);
                        if (item.listingId != null && !item.listingId.isEmpty()) {
                            mDatabase.child("listings").child(item.listingId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override public void onDataChange(@NonNull DataSnapshot ls) {
                                            item.listingTitle = ls.child("title").getValue(String.class);
                                            // Первое фото
                                            for (DataSnapshot ph : ls.child("photos").getChildren()) {
                                                item.listingPhoto = ph.getValue(String.class);
                                                break;
                                            }
                                            items.add(item);
                                            completeChats(items, pending);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) {
                                            items.add(item);
                                            completeChats(items, pending);
                                        }
                                    });
                        } else {
                            items.add(item);
                            completeChats(items, pending);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        completeChats(items, pending);
                    }
                });
    }

    private void completeChats(List<ChatItem> items, int[] pending) {
        pending[0]--;
        if (pending[0] > 0) return;
        if (!isAdded()) return;
        items.sort((a, b) -> {
            long ta = a.updatedAt != null ? a.updatedAt : 0;
            long tb = b.updatedAt != null ? b.updatedAt : 0;
            return Long.compare(tb, ta);
        });
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        allItems.clear();
        allItems.addAll(items);
        String query = etSearch != null ? etSearch.getText().toString().trim() : "";
        applySearch(query);
    }

    private void showEmpty(boolean show) {
        if (emptyState != null)
            emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recycler != null)
            recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // ── Модель чата ───────────────────────────────────
    static class ChatItem {
        String chatId, otherUid, partnerName, partnerRegion,
                lastMessage, listingId, listingTitle, listingPhoto;
        Long   updatedAt;
        long   unreadCount;
    }

    // ── Адаптер ───────────────────────────────────────
    class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.VH> {
        List<ChatItem> items = new ArrayList<>();

        void setItems(List<ChatItem> list) {
            this.items = list;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_chat, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            ChatItem item = items.get(pos);

            h.tvName.setText(item.partnerName != null ? item.partnerName : getString(R.string.unknown));
            h.tvLastMsg.setText(item.lastMessage != null ? item.lastMessage : getString(R.string.no_messages));

            if (item.updatedAt != null)
                h.tvTime.setText(new SimpleDateFormat("HH:mm",
                        Locale.getDefault()).format(new Date(item.updatedAt)));

            if (item.partnerName != null && !item.partnerName.isEmpty())
                h.tvAvatar.setText(String.valueOf(item.partnerName.charAt(0)).toUpperCase());

            // Название товара
            if (item.listingTitle != null && !item.listingTitle.isEmpty()) {
                h.tvListingTitle.setVisibility(View.VISIBLE);
                h.tvListingTitle.setText(item.listingTitle);
            } else {
                h.tvListingTitle.setVisibility(View.GONE);
            }

            // Бейдж непрочитанных
            if (item.unreadCount > 0) {
                h.tvUnreadBadge.setVisibility(View.VISIBLE);
                h.tvUnreadBadge.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
            } else {
                h.tvUnreadBadge.setVisibility(View.GONE);
            }

            // Превью фото товара
            if (item.listingPhoto != null && !item.listingPhoto.isEmpty()) {
                try {
                    String raw = item.listingPhoto;
                    String d   = raw.contains(",")
                            ? raw.substring(raw.indexOf(",") + 1) : raw;
                    byte[] bytes = Base64.decode(d, Base64.DEFAULT);
                    Glide.with(requireContext()).asBitmap().load(bytes)
                            .centerCrop().into(h.ivListingPhoto);
                    h.cardListingPhoto.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    h.cardListingPhoto.setVisibility(View.GONE);
                }
            } else {
                h.cardListingPhoto.setVisibility(View.GONE);
            }

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), ChatActivity.class);
                i.putExtra("sellerUid",   item.otherUid);
                i.putExtra("listingId",   item.listingId   != null ? item.listingId   : "");
                i.putExtra("listingTitle", item.listingTitle != null ? item.listingTitle : "");
                startActivity(i);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvLastMsg, tvTime, tvAvatar, tvListingTitle, tvUnreadBadge;
            ImageView ivListingPhoto;
            CardView  cardListingPhoto;

            VH(View v) {
                super(v);
                tvAvatar        = v.findViewById(R.id.tvChatAvatar);
                tvName          = v.findViewById(R.id.tvChatName);
                tvLastMsg       = v.findViewById(R.id.tvLastMessage);
                tvTime          = v.findViewById(R.id.tvChatTime);
                tvListingTitle  = v.findViewById(R.id.tvListingTitle);
                tvUnreadBadge   = v.findViewById(R.id.tvUnreadBadge);
                ivListingPhoto  = v.findViewById(R.id.ivListingPhoto);
                cardListingPhoto = v.findViewById(R.id.cardListingPhoto);
            }
        }
    }
}
