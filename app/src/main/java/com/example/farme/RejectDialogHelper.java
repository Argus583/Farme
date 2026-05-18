package com.example.farme;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.example.farme.model.Listing;

import java.util.HashMap;
import java.util.Map;

public class RejectDialogHelper {

    public interface OnRejectConfirmed {
        void onConfirmed(String reason);
    }

    private final Context context;
    private String selectedReason = "";

    public RejectDialogHelper(Context context) {
        this.context = context;
    }

    public void show(Listing listing, OnRejectConfirmed callback) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_reject);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int)(context.getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView reasonInvalidDocs = dialog.findViewById(R.id.reasonInvalidDocs);
        TextView reasonFakeInfo    = dialog.findViewById(R.id.reasonFakeInfo);
        TextView reasonNoVetCert   = dialog.findViewById(R.id.reasonNoVetCert);
        TextView reasonOther       = dialog.findViewById(R.id.reasonOther);
        EditText etCustomReason    = dialog.findViewById(R.id.etCustomReason);
        Button btnCancel           = dialog.findViewById(R.id.btnCancelReject);
        Button btnConfirm          = dialog.findViewById(R.id.btnConfirmReject);

        TextView[] reasons = {reasonInvalidDocs, reasonFakeInfo, reasonNoVetCert, reasonOther};
        String[] texts = {
                "Недействительные документы",
                "Недостоверная информация",
                "Нет ветеринарного свидетельства",
                "Другая причина"
        };

        // Выбор причины
        for (int i = 0; i < reasons.length; i++) {
            final int idx = i;
            reasons[i].setOnClickListener(v -> {
                // Сброс всех
                for (TextView r : reasons) {
                    r.setBackgroundResource(R.drawable.bg_chip_default);
                    r.setTextColor(context.getResources().getColor(R.color.text_primary, null));
                }
                // Выделяем выбранный
                reasons[idx].setBackgroundResource(R.drawable.bg_chip_selected);
                reasons[idx].setTextColor(0xFFFFFFFF);
                selectedReason = texts[idx];

                // Показываем поле если "Другая"
                etCustomReason.setVisibility(idx == 3 ? View.VISIBLE : View.GONE);
            });
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String reason = selectedReason;
            if (reason.equals("Другая причина")) {
                reason = etCustomReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    etCustomReason.setError("Укажите причину");
                    return;
                }
            }
            if (reason.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.error_select_reason), Toast.LENGTH_SHORT).show();
                return;
            }

            rejectListing(listing, reason);
            callback.onConfirmed(reason);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void rejectListing(Listing listing, String reason) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // 1. Скрываем объявление из ленты
        db.child("listings").child(listing.getId()).child("active").setValue(false);

        // 2. Помечаем как отклонённое и убираем из модерации
        db.child("listings").child(listing.getId()).child("rejected").setValue(true);
        db.child("listings").child(listing.getId()).child("pending").setValue(false);
        db.child("listings").child(listing.getId()).child("rejectReason").setValue(reason);

        // 3. Отправляем уведомление продавцу
        Map<String, Object> notification = new HashMap<>();
        notification.put("type",      "rejected");
        notification.put("title",     "Объявление отклонено");
        notification.put("message",   "Ваше объявление \"" + listing.getTitle()
                + "\" было отклонено. Причина: " + reason);
        notification.put("listingId", listing.getId());
        notification.put("createdAt", System.currentTimeMillis());
        notification.put("read",      false);

        db.child("notifications").child(listing.getUid()).push().setValue(notification);

        Toast.makeText(context,
                "❌ Объявление скрыто. Продавец уведомлён.", Toast.LENGTH_SHORT).show();
    }
}