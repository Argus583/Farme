package com.example.farme;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ReviewDialogHelper {

    private final Context context;
    private final String sellerUid;
    private final String listingId;
    private int selectedRating = 0;

    private TextView[] stars;
    private TextView tvRatingLabel;

    private static final String[] RATING_LABELS = {
            "Нажмите на звезду",
            "Очень плохо ★",
            "Плохо ★★",
            "Нормально ★★★",
            "Хорошо ★★★★",
            "Отлично! ★★★★★"
    };

    public ReviewDialogHelper(Context context, String sellerUid, String listingId) {
        this.context   = context;
        this.sellerUid = sellerUid;
        this.listingId = listingId;
    }

    public void show() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (myUid == null) {
            Toast.makeText(context, context.getString(R.string.error_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // Нельзя оставить отзыв самому себе
        if (myUid.equals(sellerUid)) {
            Toast.makeText(context, context.getString(R.string.error_own_review), Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверяем — уже оставлял отзыв?
        FirebaseDatabase.getInstance().getReference()
                .child("reviews").child(sellerUid).child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(context,
                                    context.getString(R.string.error_already_reviewed),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            showDialog(myUid);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        showDialog(myUid);
                    }
                });
    }

    private void showDialog(String myUid) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_review);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int)(context.getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Инициализируем элементы
        stars = new TextView[]{
                dialog.findViewById(R.id.star1),
                dialog.findViewById(R.id.star2),
                dialog.findViewById(R.id.star3),
                dialog.findViewById(R.id.star4),
                dialog.findViewById(R.id.star5)
        };
        tvRatingLabel = dialog.findViewById(R.id.tvRatingLabel);
        EditText etReviewText  = dialog.findViewById(R.id.etReviewText);
        Button btnCancel       = dialog.findViewById(R.id.btnCancelReview);
        Button btnSubmit       = dialog.findViewById(R.id.btnSubmitReview);

        // Клики на звёзды
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> updateStars(rating));
        }

        // Отмена
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Отправить
        btnSubmit.setOnClickListener(v -> {
            if (selectedRating == 0) {
                Toast.makeText(context, context.getString(R.string.error_select_rating), Toast.LENGTH_SHORT).show();
                return;
            }

            String text = etReviewText.getText().toString().trim();
            submitReview(myUid, selectedRating, text, dialog);
        });

        dialog.show();
    }

    private void updateStars(int rating) {
        selectedRating = rating;
        for (int i = 0; i < stars.length; i++) {
            stars[i].setText(i < rating ? "★" : "☆");
        }
        tvRatingLabel.setText(RATING_LABELS[rating]);
        tvRatingLabel.setTextColor(context.getResources()
                .getColor(R.color.green_primary, null));
    }

    private void submitReview(String myUid, int rating, String text, Dialog dialog) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // Получаем имя автора
        db.child("users").child(myUid).child("name")
                .get().addOnSuccessListener(snap -> {
                    String authorName = snap.getValue(String.class);

                    Map<String, Object> review = new HashMap<>();
                    review.put("authorName", authorName != null ? authorName : "Аноним");
                    review.put("authorUid",  myUid);
                    review.put("rating",     (double) rating);
                    review.put("text",       text);
                    review.put("listingId",  listingId);
                    review.put("createdAt",  System.currentTimeMillis());

                    // Сохраняем отзыв — ключ = UID автора (один отзыв на продавца)
                    db.child("reviews").child(sellerUid).child(myUid).setValue(review)
                            .addOnSuccessListener(v -> {
                                // Пересчитываем средний рейтинг продавца
                                updateSellerRating(sellerUid, db);
                                dialog.dismiss();
                                Toast.makeText(context, context.getString(R.string.review_sent), Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context,
                                            context.getString(R.string.error_loading) + ": " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                });
    }

    private void updateSellerRating(String sellerUid, DatabaseReference db) {
        db.child("reviews").child(sellerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (count == 0) return;

                        double total = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Double r = child.child("rating").getValue(Double.class);
                            if (r != null) total += r;
                        }
                        double avg = total / count;

                        // Обновляем рейтинг в профиле продавца
                        db.child("users").child(sellerUid).child("rating").setValue(avg);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }
}