package com.example.farme;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Firebase Cloud Messaging сервис.
 *
 * Отвечает за:
 *  - Сохранение FCM-токена в Firebase DB при его обновлении
 *  - Показ push-уведомления когда приложение на переднем плане
 *    (в фоне FCM показывает уведомление автоматически из notification-payload)
 */
public class FarmeFcmService extends FirebaseMessagingService {

    // ── Токен обновился ───────────────────────────────────────────────
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveToken(token);
    }

    /**
     * Сохранить FCM-токен в Firebase DB под текущим пользователем.
     * Вызывается: при обновлении токена (onNewToken) и после входа.
     */
    public static void saveToken(String token) {
        if (token == null || token.isEmpty()) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("fcmToken")
                .setValue(token);
    }

    // ── Сообщение получено (app на переднем плане) ────────────────────
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();

        // Достаём текст: сначала из data-payload, затем из notification-payload
        String title = "";
        String body  = "";
        if (message.getNotification() != null) {
            String t = message.getNotification().getTitle();
            String b = message.getNotification().getBody();
            if (t != null) title = t;
            if (b != null) body  = b;
        }
        if (data.containsKey("title")) title = data.get("title");
        if (data.containsKey("body"))  body  = data.get("body");

        // Доп. данные
        String type      = data.get("type");       // "chat" | "support" | "listing"
        String chatId    = data.get("chatId");
        String senderUid = data.get("senderUid");
        String sellerUid = data.get("sellerUid");
        String otherUid  = senderUid != null ? senderUid : sellerUid;

        showNotification(title, body, type, chatId, otherUid);
    }

    // ── Показ локального уведомления ──────────────────────────────────
    private void showNotification(String title, String body, String type,
                                   String chatId, String otherUid) {
        // Интент — куда перейти при тапе на уведомление
        Intent intent;
        if ("chat".equals(type) && chatId != null) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chatId",    chatId);
            intent.putExtra("sellerUid", otherUid);
        } else if ("support".equals(type)) {
            intent = new Intent(this, SupportActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pending = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channel = "chat".equals(type)
                ? NotificationHelper.CHANNEL_CHAT
                : NotificationHelper.CHANNEL_SYSTEM;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.mipmap.farme_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending);

        NotificationManager nm = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            // Используем время как id, чтобы не заменять старые уведомления
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
