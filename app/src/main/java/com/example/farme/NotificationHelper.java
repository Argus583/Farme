package com.example.farme;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Утилиты для Android Notification каналов.
 * Push-уведомления другим пользователям отправляются через Firebase Cloud Functions
 * (functions/index.js) — автоматически при записи нового сообщения в DB.
 */
public class NotificationHelper {

    public static final String CHANNEL_CHAT   = "channel_chat";
    public static final String CHANNEL_SYSTEM = "channel_system";

    /** Вызывать один раз в FarmeApp.onCreate() */
    public static void createChannels(Context ctx) {
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel chat = new NotificationChannel(
                CHANNEL_CHAT,
                ctx.getString(R.string.notif_channel_chat),
                NotificationManager.IMPORTANCE_HIGH);
        chat.setDescription(ctx.getString(R.string.notif_channel_chat_desc));
        chat.enableVibration(true);
        nm.createNotificationChannel(chat);

        NotificationChannel sys = new NotificationChannel(
                CHANNEL_SYSTEM,
                ctx.getString(R.string.notif_channel_system),
                NotificationManager.IMPORTANCE_DEFAULT);
        sys.setDescription(ctx.getString(R.string.notif_channel_system_desc));
        nm.createNotificationChannel(sys);
    }
}
