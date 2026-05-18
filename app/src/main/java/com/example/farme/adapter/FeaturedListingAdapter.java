package com.example.farme.adapter;

import android.content.Context;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.example.farme.R;
import com.example.farme.model.Listing;
import com.example.farme.utils.Validator;
import java.util.*;

public class FeaturedListingAdapter
        extends RecyclerView.Adapter<FeaturedListingAdapter.VH> {

    public interface OnClick { void onClick(Listing l); }

    private final Context ctx;
    private final OnClick listener;
    private List<Listing> items = new ArrayList<>();

    public FeaturedListingAdapter(Context ctx, OnClick listener) {
        this.ctx = ctx;
        this.listener = listener;
    }

    public void setListings(List<Listing> list) {
        this.items = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View v = LayoutInflater.from(ctx).inflate(
                R.layout.item_listing_featured, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Listing l = items.get(pos);

        if (h.tvTitle  != null) h.tvTitle.setText(l.getTitle());
        if (h.tvPrice  != null) h.tvPrice.setText(
                Validator.formatPrice(l.getPrice()));
        if (h.tvRegion != null && l.getRegion() != null)
            h.tvRegion.setText("📍 " + l.getRegion());

        // Паспорт
        if (h.badgeVerified != null)
            h.badgeVerified.setVisibility(
                    l.hasPassport() ? View.VISIBLE : View.GONE);

        // Рейтинг
        if (h.ratingBadge != null)
            h.ratingBadge.setVisibility(View.VISIBLE);

        // Фото
        if (h.ivPhoto != null && l.hasPhotos()) {
            try {
                String raw = l.getFirstPhoto();
                String data = raw.contains(",")
                        ? raw.substring(raw.indexOf(",") + 1) : raw;
                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                Glide.with(ctx).load(bytes)
                        .centerCrop()
                        .placeholder(android.R.color.darker_gray)
                        .into(h.ivPhoto);
            } catch (Exception ignored) {}
        }

        h.itemView.setOnClickListener(v -> listener.onClick(l));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView    ivPhoto;
        TextView     tvTitle, tvPrice, tvRegion, tvRating, badgeVerified;
        LinearLayout ratingBadge;

        VH(View v) {
            super(v);
            ivPhoto      = v.findViewById(R.id.ivFeaturedPhoto);
            tvTitle      = v.findViewById(R.id.tvFeaturedTitle);
            tvPrice      = v.findViewById(R.id.tvFeaturedPrice);
            tvRegion     = v.findViewById(R.id.tvFeaturedRegion);
            tvRating     = v.findViewById(R.id.tvFeaturedRating);
            badgeVerified= v.findViewById(R.id.badgeVerified);
            ratingBadge  = v.findViewById(R.id.ratingBadge);
        }
    }
}