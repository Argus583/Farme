package com.example.farme;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SupportActivity extends BaseActivity {

    private LinearLayout faqContainer, ticketsContainer, btnSendTicket, topicChips;
    private EditText     etMessage;
    private TextView     tvTicketsHeader;

    private DatabaseReference mDatabase;
    private String myUid, myName;
    private String selectedTopic = "Общий вопрос";
    private ValueEventListener ticketsListener;
    private Query              ticketsQuery;

    private String[] faqQuestions;
    private String[] faqAnswers;
    private String[] topics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        myUid     = FirebaseAuth.getInstance().getCurrentUser() != null
                  ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        faqQuestions = getResources().getStringArray(R.array.faq_questions);
        faqAnswers   = getResources().getStringArray(R.array.faq_answers);
        topics       = getResources().getStringArray(R.array.support_topics);
        selectedTopic = topics[0];

        faqContainer    = findViewById(R.id.faqContainer);
        ticketsContainer = findViewById(R.id.ticketsContainer);
        btnSendTicket   = findViewById(R.id.btnSendTicket);
        etMessage       = findViewById(R.id.etSupportMessage);
        tvTicketsHeader = findViewById(R.id.tvTicketsHeader);
        topicChips      = findViewById(R.id.topicChips);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        buildFaq();
        buildTopicChips();
        btnSendTicket.setOnClickListener(v -> sendTicket());

        if (myUid != null) loadMyName();
        if (myUid != null) loadMyTickets();
    }

    private void buildFaq() {
        for (int i = 0; i < faqQuestions.length; i++) {
            String question = faqQuestions[i];
            String answer   = i < faqAnswers.length ? faqAnswers[i] : "";

            CardView card = new CardView(this);
            CardView.LayoutParams lp = new CardView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            card.setLayoutParams(lp);
            card.setRadius(dp(12));
            card.setCardElevation(0);
            card.setCardBackgroundColor(getColor(R.color.bg_card));

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(dp(16), dp(14), dp(16), dp(14));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvQ = new TextView(this);
            tvQ.setText(question);
            tvQ.setTextSize(14);
            tvQ.setTextColor(getColor(R.color.text_primary));
            tvQ.setTypeface(null, android.graphics.Typeface.BOLD);
            tvQ.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView arrow = new TextView(this);
            arrow.setText("▼");
            arrow.setTextSize(12);
            arrow.setTextColor(getColor(R.color.green_primary));
            arrow.setPadding(dp(8), 0, 0, 0);

            row.addView(tvQ);
            row.addView(arrow);

            TextView tvA = new TextView(this);
            tvA.setText(answer);
            tvA.setTextSize(13);
            tvA.setTextColor(getColor(R.color.text_secondary));
            tvA.setPadding(0, dp(10), 0, 0);
            tvA.setVisibility(View.GONE);

            inner.addView(row);
            inner.addView(tvA);
            card.addView(inner);

            row.setOnClickListener(v -> {
                boolean expanded = tvA.getVisibility() == View.VISIBLE;
                tvA.setVisibility(expanded ? View.GONE : View.VISIBLE);
                arrow.setText(expanded ? "▼" : "▲");
            });
            row.setClickable(true);
            row.setFocusable(true);

            faqContainer.addView(card);
        }
    }

    private void buildTopicChips() {
        for (String topic : topics) {
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setText(topic);
            chip.setTextSize(12);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            setChipStyle(chip, topic.equals(selectedTopic));

            chip.setOnClickListener(v -> {
                selectedTopic = topic;
                for (int i = 0; i < topicChips.getChildCount(); i++) {
                    View c = topicChips.getChildAt(i);
                    if (c instanceof TextView)
                        setChipStyle((TextView) c, ((TextView) c).getText().toString().equals(selectedTopic));
                }
            });
            topicChips.addView(chip);
        }
    }

    private void setChipStyle(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_chip_selected : R.drawable.bg_chip_category);
        chip.setTextColor(active ? Color.WHITE : getColor(R.color.text_primary));
    }

    private void loadMyName() {
        mDatabase.child("users").child(myUid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s) {
                        myName = s.getValue(String.class);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void sendTicket() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_message), Toast.LENGTH_SHORT).show();
            return;
        }
        if (myUid == null) return;

        String ticketId = mDatabase.child("support").push().getKey();
        if (ticketId == null) return;

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("userId",    myUid);
        ticket.put("userName",  myName != null ? myName : "Пользователь");
        ticket.put("subject",   selectedTopic);
        ticket.put("message",   text);
        ticket.put("createdAt", System.currentTimeMillis());
        ticket.put("status",    "open");

        mDatabase.child("support").child(ticketId)
                .setValue(ticket)
                .addOnSuccessListener(a -> {
                    etMessage.setText("");
                    Toast.makeText(this,
                            getString(R.string.ticket_sent),
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_send), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ticketsQuery != null && ticketsListener != null)
            ticketsQuery.removeEventListener(ticketsListener);
    }

    private void loadMyTickets() {
        if (myUid == null) return;
        ticketsQuery    = mDatabase.child("support").orderByChild("userId").equalTo(myUid);
        ticketsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                ticketsContainer.removeAllViews();
                if (!snap.exists() || snap.getChildrenCount() == 0) {
                    tvTicketsHeader.setVisibility(View.GONE);
                    return;
                }
                tvTicketsHeader.setVisibility(View.VISIBLE);

                List<DataSnapshot> list = new ArrayList<>();
                for (DataSnapshot child : snap.getChildren()) list.add(child);
                list.sort((a, b) -> {
                    Long ta = a.child("createdAt").getValue(Long.class);
                    Long tb = b.child("createdAt").getValue(Long.class);
                    return Long.compare(tb != null ? tb : 0, ta != null ? ta : 0);
                });

                for (DataSnapshot child : list) {
                    addTicketCard(child);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        ticketsQuery.addValueEventListener(ticketsListener);
    }

    private void addTicketCard(DataSnapshot snap) {
        String ticketId = snap.getKey();
        String topic    = snap.child("subject").getValue(String.class);
        String message  = snap.child("message").getValue(String.class);
        String status   = snap.child("status").getValue(String.class);
        Long   created  = snap.child("createdAt").getValue(Long.class);

        String reply = null;
        for (DataSnapshot r : snap.child("replies").getChildren()) {
            Boolean fromAdmin = r.child("fromAdmin").getValue(Boolean.class);
            String rText = r.child("text").getValue(String.class);
            if (rText == null) rText = r.child("message").getValue(String.class);
            if (Boolean.TRUE.equals(fromAdmin) && rText != null) reply = rText;
        }

        CardView card = new CardView(this);
        CardView.LayoutParams lp = new CardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        card.setRadius(dp(12));
        card.setCardElevation(0);
        card.setCardBackgroundColor(getColor(R.color.bg_card));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(14), dp(16), dp(14));

        // Тема + статус
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvTopic = new TextView(this);
        tvTopic.setText("📌 " + (topic != null ? topic : getString(R.string.ticket_title)));
        tvTopic.setTextSize(14);
        tvTopic.setTextColor(getColor(R.color.text_primary));
        tvTopic.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTopic.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvStatus = new TextView(this);
        boolean answered = "answered".equals(status);
        tvStatus.setText(answered ? getString(R.string.ticket_status_answered) : getString(R.string.ticket_status_pending));
        tvStatus.setTextSize(11);
        tvStatus.setTextColor(answered ? 0xFFFFFFFF : 0xFFFF9800);
        tvStatus.setPadding(dp(8), dp(3), dp(8), dp(3));
        tvStatus.setBackgroundResource(answered
                ? R.drawable.bg_chip_selected : R.drawable.bg_chip_category);

        topRow.addView(tvTopic);
        topRow.addView(tvStatus);
        inner.addView(topRow);

        // Дата
        if (created != null) {
            TextView tvDate = new TextView(this);
            tvDate.setText(new SimpleDateFormat("d MMM, HH:mm", new Locale("ru"))
                    .format(new Date(created)));
            tvDate.setTextSize(12);
            tvDate.setTextColor(getColor(R.color.text_hint));
            tvDate.setPadding(0, dp(4), 0, dp(8));
            inner.addView(tvDate);
        }

        // Моё сообщение
        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(13);
        tvMsg.setTextColor(getColor(R.color.text_secondary));
        inner.addView(tvMsg);

        // Ответ администратора
        if (reply != null && !reply.isEmpty()) {
            View divider = new View(this);
            divider.setBackgroundColor(getColor(R.color.bg_screen));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dlp.topMargin = dp(12);
            dlp.bottomMargin = dp(12);
            divider.setLayoutParams(dlp);
            inner.addView(divider);

            LinearLayout replyBox = new LinearLayout(this);
            replyBox.setOrientation(LinearLayout.VERTICAL);
            replyBox.setBackgroundResource(R.drawable.bg_chip_category);
            replyBox.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView tvAdminLabel = new TextView(this);
            tvAdminLabel.setText(getString(R.string.support_admin_reply));
            tvAdminLabel.setTextSize(12);
            tvAdminLabel.setTextColor(getColor(R.color.green_primary));
            tvAdminLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAdminLabel.setPadding(0, 0, 0, dp(6));

            TextView tvReply = new TextView(this);
            tvReply.setText(reply);
            tvReply.setTextSize(13);
            tvReply.setTextColor(getColor(R.color.text_primary));

            replyBox.addView(tvAdminLabel);
            replyBox.addView(tvReply);
            inner.addView(replyBox);
        }

        card.addView(inner);
        // Открыть чат обращения по тапу
        if (ticketId != null) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> {
                Intent i = new Intent(this, TicketChatActivity.class);
                i.putExtra("ticketId", ticketId);
                startActivity(i);
            });
            addSwipeToDelete(card, ticketId);
        }
        ticketsContainer.addView(card);
    }

    private void addSwipeToDelete(View card, String ticketId) {
        final float[] startX   = {0};
        final float[] startY   = {0};
        final boolean[] swiping = {false};

        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startX[0]   = event.getRawX();
                    startY[0]   = event.getRawY();
                    swiping[0]  = false;
                    return false;

                case android.view.MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - startX[0];
                    float dy = event.getRawY() - startY[0];
                    if (!swiping[0] && dx < -dp(10) && Math.abs(dx) > Math.abs(dy)) {
                        swiping[0] = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (swiping[0]) {
                        v.setTranslationX(Math.min(0, dx));
                        v.setAlpha(Math.max(0.3f, 1f + dx / (v.getWidth() * 0.6f)));
                        return true;
                    }
                    return false;

                case android.view.MotionEvent.ACTION_UP:
                    if (swiping[0]) {
                        swiping[0] = false;
                        if (v.getTranslationX() < -v.getWidth() * 0.4f) {
                            v.animate().translationX(-v.getWidth()).alpha(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> removeTicketCard(v, ticketId))
                                    .start();
                        } else {
                            v.animate().translationX(0f).alpha(1f).setDuration(200).start();
                        }
                        return true;
                    }
                    return false;

                case android.view.MotionEvent.ACTION_CANCEL:
                    if (swiping[0]) {
                        swiping[0] = false;
                        v.animate().translationX(0f).alpha(1f).setDuration(200).start();
                        return true;
                    }
                    return false;
            }
            return false;
        });
    }

    private void removeTicketCard(View card, String ticketId) {
        mDatabase.child("support").child(ticketId).removeValue();
        ViewGroup parent = (ViewGroup) card.getParent();
        if (parent != null) parent.removeView(card);
        if (ticketsContainer.getChildCount() == 0)
            tvTicketsHeader.setVisibility(View.GONE);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
