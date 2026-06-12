package com.example.farme;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;

public class TicketChatActivity extends BaseActivity {

    private TextView     tvTicketSubject, tvTicketStatus;
    private ScrollView   scrollChat;
    private LinearLayout chatContainer;
    private EditText     etUserReply;
    private LinearLayout btnSendReply;

    private DatabaseReference  mDatabase;
    private ValueEventListener ticketListener;
    private String ticketId;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_chat);

        ticketId  = getIntent().getStringExtra("ticketId");
        if (ticketId == null) { finish(); return; }

        mDatabase  = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                   ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        tvTicketSubject = findViewById(R.id.tvTicketSubject);
        tvTicketStatus  = findViewById(R.id.tvTicketStatus);
        scrollChat      = findViewById(R.id.scrollChat);
        chatContainer   = findViewById(R.id.chatContainer);
        etUserReply     = findViewById(R.id.etUserReply);
        btnSendReply    = findViewById(R.id.btnSendReply);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendReply.setOnClickListener(v -> sendUserMessage());

        listenTicket();
    }

    private void listenTicket() {
        ticketListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) { finish(); return; }

                String subject   = snap.child("subject").getValue(String.class);
                String message   = snap.child("message").getValue(String.class);
                String status    = snap.child("status").getValue(String.class);
                Long   createdAt = snap.child("createdAt").getValue(Long.class);

                if (tvTicketSubject != null)
                    tvTicketSubject.setText(subject != null ? subject : getString(R.string.ticket_title));

                boolean isClosed = "closed".equals(status);
                if (tvTicketStatus != null) {
                    if (isClosed)
                        tvTicketStatus.setText(getString(R.string.ticket_status_closed));
                    else if ("answered".equals(status))
                        tvTicketStatus.setText("✓ " + getString(R.string.ticket_status_answered));
                    else
                        tvTicketStatus.setText("⏳ " + getString(R.string.ticket_status_pending));
                }

                setInputEnabled(!isClosed);
                buildChat(message, createdAt, snap.child("replies"), isClosed);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        mDatabase.child("support").child(ticketId).addValueEventListener(ticketListener);
    }

    private void setInputEnabled(boolean enabled) {
        if (etUserReply != null) etUserReply.setEnabled(enabled);
        if (btnSendReply != null) {
            btnSendReply.setEnabled(enabled);
            btnSendReply.setAlpha(enabled ? 1f : 0.4f);
        }
    }

    private void buildChat(String message, Long createdAt, DataSnapshot repliesSnap, boolean isClosed) {
        chatContainer.removeAllViews();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM, HH:mm", new Locale("ru"));

        // Моё сообщение — справа
        if (message != null)
            addBubble(message, createdAt, false, sdf);

        // Ответы поддержки — слева
        boolean hasAdminReply = false;
        for (DataSnapshot r : repliesSnap.getChildren()) {
            Boolean fromAdmin = r.child("fromAdmin").getValue(Boolean.class);
            String  text      = r.child("text").getValue(String.class);
            if (text == null) text = r.child("message").getValue(String.class);
            Long    ts        = r.child("createdAt").getValue(Long.class);
            if (text != null) {
                addBubble(text, ts, Boolean.TRUE.equals(fromAdmin), sdf);
                if (Boolean.TRUE.equals(fromAdmin)) hasAdminReply = true;
            }
        }

        if (isClosed) {
            TextView tvClosed = new TextView(this);
            tvClosed.setText(getString(R.string.ticket_closed_by_admin));
            tvClosed.setTextSize(13);
            tvClosed.setTextColor(getColor(R.color.text_hint));
            tvClosed.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(20);
            tvClosed.setLayoutParams(lp);
            chatContainer.addView(tvClosed);
        } else if (!hasAdminReply) {
            TextView tvWait = new TextView(this);
            tvWait.setText(getString(R.string.ticket_waiting_hint));
            tvWait.setTextSize(13);
            tvWait.setTextColor(getColor(R.color.text_hint));
            tvWait.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(20);
            tvWait.setLayoutParams(lp);
            chatContainer.addView(tvWait);
        }

        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private void addBubble(String text, Long timestamp, boolean isAdmin, SimpleDateFormat sdf) {
        // Строка: [пузырёк 75%] + [пробел 25%]  или  [пробел 25%] + [пузырёк 75%]
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(10);
        row.setLayoutParams(rowLp);

        // Колонка с подписью + пузырьком
        LinearLayout bubbleCol = new LinearLayout(this);
        bubbleCol.setOrientation(LinearLayout.VERTICAL);
        bubbleCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 3f));

        // Подпись "🛡 Поддержка" над ответом
        if (isAdmin) {
            TextView tvLabel = new TextView(this);
            tvLabel.setText(getString(R.string.support_admin_reply));
            tvLabel.setTextSize(11);
            tvLabel.setTextColor(getColor(R.color.green_primary));
            tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            llp.bottomMargin = dp(3);
            tvLabel.setLayoutParams(llp);
            bubbleCol.addView(tvLabel);
        }

        // Пузырёк
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        bubble.setBackgroundResource(isAdmin ? R.drawable.bg_msg_other : R.drawable.bg_msg_mine);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextSize(14);
        tvText.setTextColor(getColor(R.color.text_primary));
        bubble.addView(tvText);

        if (timestamp != null) {
            TextView tvTime = new TextView(this);
            tvTime.setText(sdf.format(new Date(timestamp)));
            tvTime.setTextSize(10);
            tvTime.setTextColor(getColor(R.color.text_hint));
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dp(4);
            tvTime.setLayoutParams(tlp);
            tvTime.setGravity(isAdmin ? Gravity.START : Gravity.END);
            bubble.addView(tvTime);
        }

        bubbleCol.addView(bubble);

        // Пробел
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (isAdmin) {
            row.addView(bubbleCol);
            row.addView(spacer);
        } else {
            row.addView(spacer);
            row.addView(bubbleCol);
        }

        chatContainer.addView(row);
    }

    private void sendUserMessage() {
        String text = etUserReply.getText().toString().trim();
        if (text.isEmpty()) return;

        etUserReply.setEnabled(false);
        btnSendReply.setAlpha(0.5f);

        Map<String, Object> reply = new HashMap<>();
        reply.put("fromAdmin", false);
        reply.put("text",      text);
        reply.put("createdAt", System.currentTimeMillis());
        if (currentUid != null) reply.put("userId", currentUid);

        DatabaseReference ticketRef = mDatabase.child("support").child(ticketId);
        ticketRef.child("replies").push().setValue(reply)
                .addOnSuccessListener(a -> {
                    etUserReply.setText("");
                    etUserReply.setEnabled(true);
                    btnSendReply.setAlpha(1f);
                    hideKeyboard();
                    // Возвращаем статус "open" чтобы админ видел новый вопрос
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status",      "open");
                    updates.put("lastReplyAt", System.currentTimeMillis());
                    ticketRef.updateChildren(updates);
                })
                .addOnFailureListener(e -> {
                    etUserReply.setEnabled(true);
                    btnSendReply.setAlpha(1f);
                    Toast.makeText(this, getString(R.string.error_send), Toast.LENGTH_SHORT).show();
                });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null && imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ticketListener != null)
            mDatabase.child("support").child(ticketId).removeEventListener(ticketListener);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
