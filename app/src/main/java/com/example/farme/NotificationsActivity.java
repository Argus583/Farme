package com.example.farme;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private LinearLayout emptyState;
    private NotifAdapter adapter;
    private DatabaseReference mDatabase;
    private String myUid;
    private ValueEventListener notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        myUid     = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        recycler   = findViewById(R.id.recyclerNotifications);
        emptyState = findViewById(R.id.emptyState);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnMarkAll = findViewById(R.id.btnMarkAllRead);
        if (btnMarkAll != null)
            btnMarkAll.setOnClickListener(v -> markAllRead());

        adapter = new NotifAdapter();
        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(this));
            recycler.setAdapter(adapter);
            attachSwipeToDelete();
        }
        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null)
            mDatabase.child("notifications").child(myUid)
                    .removeEventListener(notifListener);
    }

    private void attachSwipeToDelete() {
        Paint paint = new Paint();
        String deleteLabel = getString(R.string.action_delete);
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView rv,
                    @NonNull RecyclerView.ViewHolder a, @NonNull RecyclerView.ViewHolder b) { return false; }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                    @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                    int state, boolean active) {
                View item = vh.itemView;
                paint.setColor(0xFFE53935);
                c.drawRect(item.getRight() + dX, item.getTop(), item.getRight(), item.getBottom(), paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(36f);
                paint.setTextAlign(Paint.Align.CENTER);
                c.drawText(deleteLabel, item.getRight() - 120,
                        item.getTop() + item.getHeight() / 2f + 13f, paint);
                super.onChildDraw(c, rv, vh, dX, dY, state, active);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                if (pos < 0 || pos >= adapter.items.size()) return;
                NotifItem n = adapter.items.get(pos);
                mDatabase.child("notifications").child(myUid).child(n.id).removeValue();
                adapter.items.remove(pos);
                adapter.notifyItemRemoved(pos);
                if (adapter.items.isEmpty()) {
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    if (recycler   != null) recycler.setVisibility(View.GONE);
                }
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(recycler);
    }

    private void loadNotifications() {
        notifListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                List<NotifItem> items = new ArrayList<>();
                for (DataSnapshot child : snap.getChildren()) {
                    NotifItem n = new NotifItem();
                    n.id        = child.getKey();
                    n.title     = child.child("title").getValue(String.class);
                    n.message   = child.child("message").getValue(String.class);
                    n.type      = child.child("type").getValue(String.class);
                    n.listingId = child.child("listingId").getValue(String.class);
                    n.ticketId  = child.child("ticketId").getValue(String.class);
                    n.createdAt = child.child("createdAt").getValue(Long.class);
                    Boolean read= child.child("read").getValue(Boolean.class);
                    n.read      = Boolean.TRUE.equals(read);
                    items.add(n);
                }
                Collections.reverse(items);
                adapter.setItems(items);
                boolean empty = items.isEmpty();
                if (emptyState != null)
                    emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (recycler != null)
                    recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        mDatabase.child("notifications").child(myUid)
                .orderByChild("createdAt")
                .addValueEventListener(notifListener);
    }

    private void markAllRead() {
        mDatabase.child("notifications").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        for (DataSnapshot child : snap.getChildren())
                            child.getRef().child("read").setValue(true);
                        Toast.makeText(NotificationsActivity.this,
                                getString(R.string.notif_all_read),
                                Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    static class NotifItem {
        String  id, title, message, type, listingId, ticketId;
        Long    createdAt;
        boolean read;
    }

    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        List<NotifItem> items = new ArrayList<>();
        void setItems(List<NotifItem> l) { items=l; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(NotificationsActivity.this)
                    .inflate(R.layout.item_notification, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            NotifItem n = items.get(pos);
            if (h.tvTitle != null) h.tvTitle.setText(n.title);
            if (h.tvMessage != null) h.tvMessage.setText(n.message);
            if (h.tvTime != null && n.createdAt != null)
                h.tvTime.setText(new SimpleDateFormat("dd.MM HH:mm",
                        Locale.getDefault()).format(new Date(n.createdAt)));
            // Иконка по типу
            if (h.tvIcon != null) {
                String icon = "🔔";
                if      ("listing_approved".equals(n.type)) icon = "✅";
                else if ("listing_rejected".equals(n.type)) icon = "❌";
                else if ("new_message".equals(n.type))      icon = "💬";
                else if ("new_review".equals(n.type))       icon = "⭐";
                else if ("support_reply".equals(n.type))    icon = "🛡";
                h.tvIcon.setText(icon);
            }
            // Фон для непрочитанных
            h.itemView.setBackgroundColor(n.read ? 0xFFFFFFFF : 0xFFEFF8F1);

            h.itemView.setOnClickListener(v -> {
                mDatabase.child("notifications").child(myUid)
                        .child(n.id).child("read").setValue(true);
                if ("support_reply".equals(n.type)) {
                    Intent i;
                    if (n.ticketId != null) {
                        i = new Intent(NotificationsActivity.this, TicketChatActivity.class);
                        i.putExtra("ticketId", n.ticketId);
                    } else {
                        i = new Intent(NotificationsActivity.this, SupportActivity.class);
                    }
                    startActivity(i);
                }
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvTitle, tvMessage, tvTime;
            VH(View v) {
                super(v);
                tvIcon    = v.findViewById(R.id.tvNotifIcon);
                tvTitle   = v.findViewById(R.id.tvNotifTitle);
                tvMessage = v.findViewById(R.id.tvNotifMessage);
                tvTime    = v.findViewById(R.id.tvNotifTime);
            }
        }
    }
}