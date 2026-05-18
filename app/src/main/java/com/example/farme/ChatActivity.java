package com.example.farme;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.database.ServerValue;
import com.example.farme.adapter.MessageAdapter;
import com.example.farme.model.Message;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private TextView     tvChatName, tvChatSubtitle, tvChatAvatar;
    private TextView     btnBackChat;
    private RecyclerView recyclerMessages;
    private EditText     etMessage;
    private LinearLayout btnSend;

    private String chatId, myUid, sellerUid,
            listingId, listingTitle, sellerPhone = "";

    private DatabaseReference  mDatabase;
    private MessageAdapter     messageAdapter;
    private final List<Message> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish(); return;
        }
        mDatabase    = FirebaseDatabase.getInstance().getReference();
        myUid        = FirebaseAuth.getInstance().getCurrentUser().getUid();
        sellerUid    = getIntent().getStringExtra("sellerUid");
        listingId    = getIntent().getStringExtra("listingId");
        listingTitle = getIntent().getStringExtra("listingTitle");

        if (sellerUid == null) { finish(); return; }

        if (listingId != null && !listingId.isEmpty()) {
            String u1 = myUid.compareTo(sellerUid) < 0 ? myUid : sellerUid;
            String u2 = myUid.compareTo(sellerUid) < 0 ? sellerUid : myUid;
            chatId = listingId + "_" + u1 + "_" + u2;
        } else {
            chatId = myUid.compareTo(sellerUid) < 0
                    ? myUid + "_" + sellerUid
                    : sellerUid + "_" + myUid;
        }

        initViews();
        loadSellerInfo();
        setupChat();
        listenMessages();
        checkBlockStatus();
    }

    private void initViews() {
        tvChatName     = findViewById(R.id.tvChatName);
        tvChatSubtitle = findViewById(R.id.tvChatSubtitle);
        tvChatAvatar   = findViewById(R.id.tvChatAvatar);
        btnBackChat    = findViewById(R.id.btnBackChat);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage      = findViewById(R.id.etMessage);
        btnSend        = findViewById(R.id.btnSend);

        if (btnBackChat != null)
            btnBackChat.setOnClickListener(v -> finish());
        if (btnSend != null)
            btnSend.setOnClickListener(v -> sendMessage());

        // Шапка → профиль продавца
        View chatHeader = findViewById(R.id.chatHeader);
        if (chatHeader != null)
            chatHeader.setOnClickListener(v -> {
                Intent i = new Intent(this, SellerProfileActivity.class);
                i.putExtra("sellerUid", sellerUid);
                startActivity(i);
            });

        // Заголовок объявления
        if (tvChatSubtitle != null && listingTitle != null)
            tvChatSubtitle.setText(listingTitle);
    }

    private void loadSellerInfo() {
        mDatabase.child("users").child(sellerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s) {
                        String name  = s.child("name").getValue(String.class);
                        String phone = s.child("phone").getValue(String.class);
                        if (phone != null) sellerPhone = phone;
                        if (name != null) {
                            if (tvChatName != null) tvChatName.setText(name);
                            if (tvChatAvatar != null)
                                tvChatAvatar.setText(
                                        String.valueOf(name.charAt(0)).toUpperCase());
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void setupChat() {
        // Метаданные чата
        Map<String, Object> meta = new HashMap<>();
        meta.put("participants/" + myUid,     true);
        meta.put("participants/" + sellerUid, true);
        if (listingId != null && !listingId.isEmpty())
            meta.put("listingId", listingId);
        meta.put("updatedAt", System.currentTimeMillis());
        mDatabase.child("chats").child(chatId).updateChildren(meta);

        // Сбрасываем счётчик непрочитанных для текущего пользователя
        mDatabase.child("chats").child(chatId)
                .child("unread").child(myUid).setValue(0);

        // chatIds в профилях
        mDatabase.child("users").child(myUid)
                .child("chatIds").child(chatId).setValue(true);
        mDatabase.child("users").child(sellerUid)
                .child("chatIds").child(chatId).setValue(true);

        // RecyclerView
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(lm);
        messageAdapter = new MessageAdapter(this, messages, myUid);
        recyclerMessages.setAdapter(messageAdapter);
        attachSwipeToDelete();
    }

    private void listenMessages() {
        mDatabase.child("chats").child(chatId).child("messages")
                .addChildEventListener(new ChildEventListener() {
                    @Override public void onChildAdded(
                            @NonNull DataSnapshot snap, String prev) {
                        Message m = snap.getValue(Message.class);
                        if (m != null) {
                            m.setId(snap.getKey());
                            messages.add(m);
                            messageAdapter.notifyItemInserted(messages.size()-1);
                            recyclerMessages.smoothScrollToPosition(messages.size()-1);
                        }
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void sendMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");

        String msgKey = mDatabase.child("chats").child(chatId)
                .child("messages").push().getKey();
        if (msgKey == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId",  myUid);
        msg.put("text",      text);
        msg.put("createdAt", System.currentTimeMillis());
        msg.put("type",      "text");
        mDatabase.child("chats").child(chatId)
                .child("messages").child(msgKey).setValue(msg);

        Map<String, Object> upd = new HashMap<>();
        upd.put("lastMessage",           text);
        upd.put("lastSenderId",          myUid);
        upd.put("updatedAt",             System.currentTimeMillis());
        upd.put("unread/" + sellerUid,   ServerValue.increment(1));
        mDatabase.child("chats").child(chatId).updateChildren(upd);
    }

    private void attachSwipeToDelete() {
        Paint paint = new Paint();
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView rv,
                    @NonNull RecyclerView.ViewHolder a, @NonNull RecyclerView.ViewHolder b) { return false; }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                int pos = vh.getAdapterPosition();
                if (pos < 0 || pos >= messages.size()) return 0;
                // Только свои сообщения
                return myUid.equals(messages.get(pos).getSenderId()) ? ItemTouchHelper.LEFT : 0;
            }

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
                c.drawText("Удалить", item.getRight() - 120,
                        item.getTop() + item.getHeight() / 2f + 13f, paint);
                super.onChildDraw(c, rv, vh, dX, dY, state, active);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                if (pos < 0 || pos >= messages.size()) return;
                Message m = messages.get(pos);
                if (m.getId() != null)
                    mDatabase.child("chats").child(chatId)
                            .child("messages").child(m.getId()).removeValue();
                messages.remove(pos);
                messageAdapter.notifyItemRemoved(pos);
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(recyclerMessages);
    }

    private void checkBlockStatus() {
        // Проверяем: продавец заблокировал меня?
        mDatabase.child("blocks").child(sellerUid).child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            disableChat("Вы не можете писать этому пользователю");
                            return;
                        }
                        // Проверяем: я заблокировал продавца?
                        mDatabase.child("blocks").child(myUid).child(sellerUid)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override public void onDataChange(@NonNull DataSnapshot s) {
                                        if (s.exists()) disableChat("Вы заблокировали этого пользователя");
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                                });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void disableChat(String reason) {
        if (etMessage != null) {
            etMessage.setEnabled(false);
            etMessage.setHint(reason);
            etMessage.setHintTextColor(0xFFFF4444);
        }
        if (btnSend != null) {
            btnSend.setAlpha(0.3f);
            btnSend.setClickable(false);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        messages.clear();
    }
}