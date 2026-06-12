const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.database();

// ═══════════════════════════════════════════════════════════════════════
// 1. УВЕДОМЛЕНИЕ О НОВОМ СООБЩЕНИИ В ЧАТЕ
//    Срабатывает автоматически при добавлении нового сообщения
// ═══════════════════════════════════════════════════════════════════════
exports.sendChatNotification = functions.database
  .ref("/chats/{chatId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.val();
    if (!message || !message.senderId) return null;

    const { chatId } = context.params;
    const senderId = message.senderId;
    const text = (message.text || "").trim();
    if (!text) return null;

    // Получаем участников чата
    const chatSnap = await db.ref(`/chats/${chatId}/participants`).once("value");
    const participants = chatSnap.val();
    if (!participants) return null;

    // Получатель = участник, который НЕ отправитель
    const recipientUid = Object.keys(participants).find((uid) => uid !== senderId);
    if (!recipientUid) return null;

    // Получаем FCM-токен получателя и имя отправителя параллельно
    const [tokenSnap, nameSnap] = await Promise.all([
      db.ref(`/users/${recipientUid}/fcmToken`).once("value"),
      db.ref(`/users/${senderId}/name`).once("value"),
    ]);

    const token = tokenSnap.val();
    const senderName = nameSnap.val() || "Farme";
    if (!token) return null;

    const shortText = text.length > 80 ? text.substring(0, 80) + "…" : text;

    return admin.messaging().send({
      token,
      notification: {
        title: senderName,
        body: shortText,
      },
      data: {
        type: "chat",
        chatId: chatId,
        senderUid: senderId,
      },
      android: {
        priority: "high",
        notification: { sound: "default", channelId: "channel_chat" },
      },
      apns: {
        payload: { aps: { sound: "default", badge: 1 } },
      },
    }).catch((err) => console.error("FCM chat error:", err));
  });

// ═══════════════════════════════════════════════════════════════════════
// 2. УВЕДОМЛЕНИЕ ОБ ОТВЕТЕ В СЛУЖБЕ ПОДДЕРЖКИ
//    Срабатывает когда admin пишет ответ в тикет
// ═══════════════════════════════════════════════════════════════════════
exports.sendSupportNotification = functions.database
  .ref("/support_tickets/{ticketId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.val();
    if (!message || !message.senderId) return null;

    const { ticketId } = context.params;

    // Уведомляем только если это ответ от admin
    const isAdmin = message.isAdmin === true;
    if (!isAdmin) return null;

    // Получаем UID владельца тикета
    const ticketSnap = await db.ref(`/support_tickets/${ticketId}/userId`).once("value");
    const userId = ticketSnap.val();
    if (!userId) return null;

    const tokenSnap = await db.ref(`/users/${userId}/fcmToken`).once("value");
    const token = tokenSnap.val();
    if (!token) return null;

    const text = (message.text || "").trim();
    const shortText = text.length > 80 ? text.substring(0, 80) + "…" : text;

    return admin.messaging().send({
      token,
      notification: {
        title: "🛡 Поддержка Farme",
        body: shortText || "Вам ответили в службе поддержки",
      },
      data: {
        type: "support",
        ticketId,
      },
      android: {
        priority: "high",
        notification: { sound: "default", channelId: "channel_system" },
      },
    }).catch((err) => console.error("FCM support error:", err));
  });

// ═══════════════════════════════════════════════════════════════════════
// 3. УВЕДОМЛЕНИЕ О СТАТУСЕ ОБЪЯВЛЕНИЯ (одобрено / отклонено)
// ═══════════════════════════════════════════════════════════════════════
exports.sendListingStatusNotification = functions.database
  .ref("/listings/{listingId}/status")
  .onUpdate(async (change, context) => {
    const newStatus = change.after.val();
    const oldStatus = change.before.val();
    if (newStatus === oldStatus) return null;

    // Реагируем только на "approved" и "rejected"
    if (newStatus !== "approved" && newStatus !== "rejected") return null;

    const { listingId } = context.params;

    // Получаем данные объявления
    const listingSnap = await db.ref(`/listings/${listingId}`).once("value");
    const listing = listingSnap.val();
    if (!listing || !listing.userId) return null;

    const tokenSnap = await db.ref(`/users/${listing.userId}/fcmToken`).once("value");
    const token = tokenSnap.val();
    if (!token) return null;

    const title = listing.title || "Объявление";
    const isApproved = newStatus === "approved";

    return admin.messaging().send({
      token,
      notification: {
        title: isApproved ? "✅ Объявление одобрено" : "❌ Объявление отклонено",
        body: `"${title.substring(0, 50)}" — ${isApproved ? "опубликовано" : "отклонено модератором"}`,
      },
      data: {
        type: "listing",
        listingId,
        status: newStatus,
      },
      android: {
        priority: "normal",
        notification: { sound: "default", channelId: "channel_system" },
      },
    }).catch((err) => console.error("FCM listing error:", err));
  });
