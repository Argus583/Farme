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
import java.util.*;

public class ListingAdapter
        extends RecyclerView.Adapter<ListingAdapter.VH> {

    public interface OnListingClickListener {
        void onListingClick(Listing listing);
        void onChatClick(Listing listing);
        void onFavoriteClick(Listing listing, boolean isFavorite);
    }

    private List<Listing>          items    = new ArrayList<>();
    private final Set<String>      favorites= new HashSet<>();
    private final Context          ctx;
    private final OnListingClickListener listener;

    public ListingAdapter(Context ctx, OnListingClickListener l) {
        this.ctx = ctx;
        this.listener = l;
    }

    public void setListings(List<Listing> list) {
        this.items = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public void setFavorites(Set<String> favIds) {
        this.favorites.clear();
        this.favorites.addAll(favIds);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View v = LayoutInflater.from(ctx).inflate(
                R.layout.item_listing, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Listing l = items.get(pos);

        // Заголовок
        if (h.tvTitle != null)
            h.tvTitle.setText(l.getTitle());

        // Цена
        if (h.tvPrice != null)
            h.tvPrice.setText(l.getPriceFormatted());

        // Регион
        if (h.tvRegion != null)
            h.tvRegion.setText(l.getRegion() != null
                    ? l.getRegion() : "");

        // Категория
        if (h.tvCategory != null) {
            h.tvCategory.setText(
                    l.getCategoryEmoji() + " " + l.getCategory());
            h.tvCategory.setVisibility(
                    l.getCategory() != null ? View.VISIBLE : View.GONE);
        }

        // Паспорт
        if (h.tvPassportIcon != null)
            h.tvPassportIcon.setVisibility(
                    l.hasPassport() ? View.VISIBLE : View.GONE);
        if (h.badgePassport != null)
            h.badgePassport.setVisibility(
                    l.hasPassport() ? View.VISIBLE : View.GONE);

        // Фото
        loadPhoto(h, l);

        // Избранное
        boolean isFav = favorites.contains(l.getId());
        if (h.btnFavorite != null) {
            h.btnFavorite.setText(isFav ? "❤️" : "🤍");
            h.btnFavorite.setOnClickListener(v -> {
                boolean nowFav = !favorites.contains(l.getId());
                if (nowFav) favorites.add(l.getId());
                else        favorites.remove(l.getId());
                h.btnFavorite.setText(nowFav ? "❤️" : "🤍");
                if (listener != null)
                    listener.onFavoriteClick(l, nowFav);
            });
        }

        // Клик на карточку
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onListingClick(l);
        });
    }

    private void loadPhoto(VH h, Listing l) {
        if (h.ivPhoto == null) return;
        String photo = l.getFirstPhoto();
        if (photo == null) {
            h.ivPhoto.setImageResource(android.R.color.darker_gray);
            return;
        }
        try {
            String data = photo.contains(",")
                    ? photo.substring(photo.indexOf(",") + 1) : photo;
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            Glide.with(ctx).load(bytes)
                    .centerCrop()
                    .placeholder(android.R.color.darker_gray)
                    .into(h.ivPhoto);
        } catch (Exception e) {
            h.ivPhoto.setImageResource(android.R.color.darker_gray);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView  tvTitle, tvPrice, tvRegion, tvRating;
        TextView  tvCategory, tvPassportIcon, badgePassport;
        TextView  btnFavorite;

        VH(View v) {
            super(v);
            ivPhoto       = v.findViewById(R.id.ivListingPhoto);
            tvTitle       = v.findViewById(R.id.tvTitle);
            tvPrice       = v.findViewById(R.id.tvPrice);
            tvRegion      = v.findViewById(R.id.tvRegion);
            tvRating      = v.findViewById(R.id.tvRating);
            tvCategory    = v.findViewById(R.id.tvCategory);
            tvPassportIcon= v.findViewById(R.id.tvPassportIcon);
            badgePassport = v.findViewById(R.id.badgePassport);
            btnFavorite   = v.findViewById(R.id.btnFavorite);
        }
    }
}