package com.example.farme.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

/**
 * Хранит историю просмотренных объявлений локально (SharedPreferences).
 * Максимум 20 записей. Последний просмотренный — первый в списке.
 */
public class HistoryManager {

    private static final String KEY_IDS   = "viewed_ids";
    private static final int    MAX_ITEMS = 20;

    private static String prefName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "guest";
        return "farme_history_" + uid;
    }

    // Добавить объявление в историю
    public static void addToHistory(Context ctx, String listingId) {
        if (listingId == null || listingId.isEmpty()) return;
        SharedPreferences prefs = ctx.getSharedPreferences(
                prefName(), Context.MODE_PRIVATE);
        List<String> ids = getHistory(ctx);

        // Убираем дубликат если уже есть
        ids.remove(listingId);
        // Добавляем в начало
        ids.add(0, listingId);
        // Обрезаем до MAX_ITEMS
        if (ids.size() > MAX_ITEMS)
            ids = ids.subList(0, MAX_ITEMS);

        JSONArray arr = new JSONArray();
        for (String id : ids) arr.put(id);
        prefs.edit().putString(KEY_IDS, arr.toString()).apply();
    }

    // Получить историю (список ID)
    public static List<String> getHistory(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(
                prefName(), Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_IDS, "[]");
        List<String> ids = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++)
                ids.add(arr.getString(i));
        } catch (JSONException ignored) {}
        return ids;
    }

    // Очистить историю
    public static void clearHistory(Context ctx) {
        ctx.getSharedPreferences(prefName(), Context.MODE_PRIVATE)
                .edit().remove(KEY_IDS).apply();
    }
}