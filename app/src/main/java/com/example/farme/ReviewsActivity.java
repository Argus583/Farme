package com.example.farme;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReviewsActivity extends BaseActivity {

    private RecyclerView  recycler;
    private LinearLayout  emptyState;
    private TextView      tvRatingSummary, tvReviewCount;
    private ReviewAdapter adapter;
    private DatabaseReference mDatabase;
    private String sellerUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        sellerUid = getIntent().getStringExtra("sellerUid");
        if (sellerUid == null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null)
                sellerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            else { finish(); return; }
        }
        mDatabase = FirebaseDatabase.getInstance().getReference();

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        recycler        = findViewById(R.id.recyclerReviews);
        emptyState      = findViewById(R.id.emptyState);
        tvRatingSummary = findViewById(R.id.tvRatingSummary);
        tvReviewCount   = findViewById(R.id.tvReviewCount);

        adapter = new ReviewAdapter();
        if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(this));
            recycler.setAdapter(adapter);
        }
        loadReviews();
    }

    private void loadReviews() {
        mDatabase.child("reviews").child(sellerUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        List<ReviewItem> items = new ArrayList<>();
                        double total = 0;
                        for (DataSnapshot child : snap.getChildren()) {
                            ReviewItem r = new ReviewItem();
                            r.authorName = child.child("authorName").getValue(String.class);
                            r.text       = child.child("text").getValue(String.class);
                            Double rat   = child.child("rating").getValue(Double.class);
                            r.rating     = rat != null ? rat : 0;
                            Long ca      = child.child("createdAt").getValue(Long.class);
                            r.createdAt  = ca != null ? ca : 0;
                            items.add(r);
                            total += r.rating;
                        }
                        items.sort((a,b) -> Long.compare(b.createdAt, a.createdAt));
                        adapter.setItems(items);

                        boolean empty = items.isEmpty();
                        if (emptyState != null)
                            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                        if (recycler != null)
                            recycler.setVisibility(empty ? View.GONE : View.VISIBLE);

                        if (!empty) {
                            double avg = total / items.size();
                            if (tvRatingSummary != null)
                                tvRatingSummary.setText(
                                        String.format(Locale.getDefault(), "%.1f", avg));
                            if (tvReviewCount != null)
                                tvReviewCount.setText(items.size() + " отзывов");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    static class ReviewItem {
        String authorName, text;
        double rating;
        long   createdAt;
    }

    class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {
        List<ReviewItem> items = new ArrayList<>();
        void setItems(List<ReviewItem> l) { items=l; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(ReviewsActivity.this)
                    .inflate(R.layout.item_review, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            ReviewItem r = items.get(pos);
            if (h.tvAuthor != null)
                h.tvAuthor.setText(r.authorName != null ? r.authorName : getString(R.string.anonymous));
            if (h.tvText != null)
                h.tvText.setText(r.text != null ? r.text : "");
            if (h.tvRating != null)
                h.tvRating.setText(getStars(r.rating));
            if (h.tvDate != null && r.createdAt > 0)
                h.tvDate.setText(new SimpleDateFormat("dd.MM.yyyy",
                        Locale.getDefault()).format(new Date(r.createdAt)));
            if (h.tvInitial != null && r.authorName != null && !r.authorName.isEmpty())
                h.tvInitial.setText(
                        String.valueOf(r.authorName.charAt(0)).toUpperCase());
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvInitial, tvAuthor, tvText, tvRating, tvDate;
            VH(View v) {
                super(v);
                tvInitial = v.findViewById(R.id.tvReviewInitial);
                tvAuthor  = v.findViewById(R.id.tvReviewAuthor);
                tvText    = v.findViewById(R.id.tvReviewText);
                tvRating  = v.findViewById(R.id.tvReviewRating);
                tvDate    = v.findViewById(R.id.tvReviewDate);
            }
        }
    }

    private String getStars(double rating) {
        int r = (int) Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<5;i++) sb.append(i<r ? "★" : "☆");
        return sb.toString();
    }
}
