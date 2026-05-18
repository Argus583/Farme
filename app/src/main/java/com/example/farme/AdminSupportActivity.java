package com.example.farme;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.*;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminSupportActivity extends BaseActivity {

    private RecyclerView recycler;
    private ProgressBar  progressBar;
    private LinearLayout emptyState;
    private TextView     tabOpen, tabAnswered, tvTicketCount;

    private TicketAdapter adapter;
    private DatabaseReference mDatabase;

    private final List<Ticket> allTickets      = new ArrayList<>();
    private final List<Ticket> filteredTickets = new ArrayList<>();
    private String currentFilter = "open";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_support);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        recycler      = findViewById(R.id.recyclerTickets);
        progressBar   = findViewById(R.id.progressBar);
        emptyState    = findViewById(R.id.emptyState);
        tabOpen       = findViewById(R.id.tabOpen);
        tabAnswered   = findViewById(R.id.tabAnswered);
        tvTicketCount = findViewById(R.id.tvTicketCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new TicketAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        tabOpen.setOnClickListener(v -> switchTab("open"));
        tabAnswered.setOnClickListener(v -> switchTab("answered"));

        loadTickets();
    }

    private void switchTab(String filter) {
        currentFilter = filter;
        tabOpen.setTextColor(filter.equals("open")
                ? getColor(R.color.green_primary) : getColor(R.color.text_secondary));
        tabOpen.setTypeface(null, filter.equals("open")
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabAnswered.setTextColor(filter.equals("answered")
                ? getColor(R.color.green_primary) : getColor(R.color.text_secondary));
        tabAnswered.setTypeface(null, filter.equals("answered")
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        applyFilter();
    }

    private void loadTickets() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("support")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        allTickets.clear();
                        for (DataSnapshot child : snap.getChildren()) {
                            Ticket t = new Ticket();
                            t.id        = child.getKey();
                            t.uid       = child.child("userId").getValue(String.class);
                            t.userName  = child.child("userName").getValue(String.class);
                            t.topic     = child.child("subject").getValue(String.class);
                            t.message   = child.child("message").getValue(String.class);
                            t.status    = child.child("status").getValue(String.class);
                            for (DataSnapshot r : child.child("replies").getChildren()) {
                                Boolean fa = r.child("fromAdmin").getValue(Boolean.class);
                                String rt  = r.child("text").getValue(String.class);
                                if (rt == null) rt = r.child("message").getValue(String.class);
                                if (Boolean.TRUE.equals(fa) && rt != null) t.reply = rt;
                            }
                            Long ts     = child.child("createdAt").getValue(Long.class);
                            t.createdAt = ts != null ? ts : 0;
                            allTickets.add(t);
                        }
                        allTickets.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                        long openCount = 0;
                        for (Ticket t : allTickets) if ("open".equals(t.status)) openCount++;
                        tvTicketCount.setText(openCount + " открытых");
                        progressBar.setVisibility(View.GONE);
                        applyFilter();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void applyFilter() {
        filteredTickets.clear();
        for (Ticket t : allTickets) {
            if (currentFilter.equals(t.status)) filteredTickets.add(t);
        }
        adapter.notifyDataSetChanged();
        boolean empty = filteredTickets.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            TextView tv = emptyState.findViewWithTag("emptyText");
            if (tv == null) {
                tv = (TextView) emptyState.getChildAt(1);
            }
            if (tv != null)
                tv.setText(currentFilter.equals("open")
                        ? "Нет открытых обращений" : "Нет отвеченных обращений");
        }
    }

    private void openTicketDialog(Ticket ticket) {
        View dialogView = LayoutInflater.from(this).inflate(
                android.R.layout.simple_list_item_2, null);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        TextView tvUser = new TextView(this);
        tvUser.setText("👤 " + (ticket.userName != null ? ticket.userName : "Неизвестно"));
        tvUser.setTextSize(15);
        tvUser.setTextColor(getColor(R.color.text_primary));
        tvUser.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvUser);

        TextView tvTopic = new TextView(this);
        tvTopic.setText("📌 " + (ticket.topic != null ? ticket.topic : "—"));
        tvTopic.setTextSize(13);
        tvTopic.setTextColor(getColor(R.color.green_primary));
        tvTopic.setPadding(0, dp(4), 0, dp(10));
        layout.addView(tvTopic);

        TextView tvDate = new TextView(this);
        tvDate.setText(new SimpleDateFormat("d MMM yyyy, HH:mm", new Locale("ru"))
                .format(new Date(ticket.createdAt)));
        tvDate.setTextSize(12);
        tvDate.setTextColor(getColor(R.color.text_hint));
        tvDate.setPadding(0, 0, 0, dp(10));
        layout.addView(tvDate);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(ticket.message);
        tvMsg.setTextSize(14);
        tvMsg.setTextColor(getColor(R.color.text_secondary));
        tvMsg.setPadding(0, 0, 0, dp(14));
        layout.addView(tvMsg);

        // Поле ответа
        TextView tvReplyLabel = new TextView(this);
        tvReplyLabel.setText("Ваш ответ:");
        tvReplyLabel.setTextSize(13);
        tvReplyLabel.setTextColor(getColor(R.color.text_primary));
        tvReplyLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvReplyLabel.setPadding(0, 0, 0, dp(6));
        layout.addView(tvReplyLabel);

        EditText etReply = new EditText(this);
        etReply.setHint("Напишите ответ пользователю...");
        etReply.setBackgroundResource(R.drawable.bg_input);
        etReply.setPadding(dp(12), dp(10), dp(12), dp(10));
        etReply.setMinHeight(dp(90));
        etReply.setGravity(android.view.Gravity.TOP);
        etReply.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (ticket.reply != null) etReply.setText(ticket.reply);
        layout.addView(etReply);

        new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(getString(R.string.admin_reply_btn), (d, w) -> {
                    String replyText = etReply.getText().toString().trim();
                    if (replyText.isEmpty()) {
                        Toast.makeText(this, getString(R.string.error_enter_message), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendReply(ticket, replyText);
                })
                .setNegativeButton(getString(R.string.btn_close), null)
                .show();
    }

    private void sendReply(Ticket ticket, String replyText) {
        DatabaseReference ticketRef = mDatabase.child("support").child(ticket.id);

        Map<String, Object> newReply = new HashMap<>();
        newReply.put("fromAdmin", true);
        newReply.put("text",      replyText);
        newReply.put("createdAt", System.currentTimeMillis());

        ticketRef.child("replies").push().setValue(newReply)
                .addOnSuccessListener(a -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastReplyAt", System.currentTimeMillis());
                    updates.put("status",      "answered");
                    ticketRef.updateChildren(updates);

                    if (ticket.uid != null) {
                        String notifId = mDatabase.child("notifications")
                                .child(ticket.uid).push().getKey();
                        if (notifId != null) {
                            Map<String, Object> notif = new HashMap<>();
                            notif.put("type",      "support_reply");
                            notif.put("title",     "Ответ поддержки");
                            notif.put("message",   "Мы ответили на ваше обращение: «" + ticket.topic + "»");
                            notif.put("ticketId",  ticket.id);
                            notif.put("read",      false);
                            notif.put("createdAt", System.currentTimeMillis());
                            mDatabase.child("notifications").child(ticket.uid)
                                    .child(notifId).setValue(notif);
                        }
                    }
                    Toast.makeText(AdminSupportActivity.this, getString(R.string.admin_reply_sent), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AdminSupportActivity.this, getString(R.string.error_loading), Toast.LENGTH_SHORT).show());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // ── Модель ──────────────────────────────────────────────
    static class Ticket {
        String id, uid, userName, topic, message, status, reply;
        long   createdAt;
    }

    // ── Адаптер ─────────────────────────────────────────────
    class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CardView card = new CardView(AdminSupportActivity.this);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            card.setLayoutParams(lp);
            card.setRadius(dp(12));
            card.setCardElevation(0);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout inner = new LinearLayout(AdminSupportActivity.this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(dp(16), dp(14), dp(16), dp(14));
            inner.setTag("inner");
            card.addView(inner);
            return new VH(card);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Ticket t = filteredTickets.get(pos);
            LinearLayout inner = (LinearLayout) ((CardView) h.itemView)
                    .getChildAt(0);
            inner.removeAllViews();

            // Шапка: имя + тема
            LinearLayout topRow = new LinearLayout(AdminSupportActivity.this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(AdminSupportActivity.this);
            tvName.setText("👤 " + (t.userName != null ? t.userName : "—"));
            tvName.setTextSize(14);
            tvName.setTextColor(getColor(R.color.text_primary));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDate = new TextView(AdminSupportActivity.this);
            tvDate.setText(new SimpleDateFormat("d MMM, HH:mm", new Locale("ru"))
                    .format(new Date(t.createdAt)));
            tvDate.setTextSize(11);
            tvDate.setTextColor(getColor(R.color.text_hint));
            topRow.addView(tvName);
            topRow.addView(tvDate);
            inner.addView(topRow);

            // Тема
            TextView tvTopic = new TextView(AdminSupportActivity.this);
            tvTopic.setText("📌 " + (t.topic != null ? t.topic : "Общий вопрос"));
            tvTopic.setTextSize(12);
            tvTopic.setTextColor(getColor(R.color.green_primary));
            tvTopic.setPadding(0, dp(3), 0, dp(6));
            inner.addView(tvTopic);

            // Сообщение (сокращённо)
            TextView tvMsg = new TextView(AdminSupportActivity.this);
            tvMsg.setText(t.message);
            tvMsg.setTextSize(13);
            tvMsg.setTextColor(getColor(R.color.text_secondary));
            tvMsg.setMaxLines(2);
            tvMsg.setEllipsize(android.text.TextUtils.TruncateAt.END);
            inner.addView(tvMsg);

            h.itemView.setOnClickListener(v -> openTicketDialog(t));
        }

        @Override public int getItemCount() { return filteredTickets.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }
    }
}
