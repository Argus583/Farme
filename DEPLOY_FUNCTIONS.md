# Деплой Firebase Cloud Functions

## 1. Установить Firebase CLI (один раз)
```bash
npm install -g firebase-tools
```

## 2. Войти в аккаунт Firebase
```bash
firebase login
```

## 3. Установить зависимости функций
```bash
cd functions
npm install
cd ..
```

## 4. Задеплоить функции
```bash
firebase deploy --only functions
```

## 5. Проверить что функции работают
Firebase Console → Functions → список:
- sendChatNotification
- sendSupportNotification  
- sendListingStatusNotification

## Что делают функции
| Функция | Триггер | Кому шлёт |
|---------|---------|-----------|
| sendChatNotification | Новое сообщение в /chats/{id}/messages | Получателю сообщения |
| sendSupportNotification | Ответ admin в тикете | Владельцу тикета |
| sendListingStatusNotification | Статус объявления изменился | Продавцу |

## Бесплатный лимит (Spark plan)
- 125 000 вызовов в месяц — БЕСПЛАТНО
- 40 000 CPU-секунд в месяц — БЕСПЛАТНО
