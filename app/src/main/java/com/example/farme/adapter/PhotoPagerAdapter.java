package com.example.farme.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import java.util.List;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.VH> {

    private final Context      ctx;
    private final List<String> photos;

    public PhotoPagerAdapter(Context ctx, List<String> photos) {
        this.ctx    = ctx;
        this.photos = photos;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        ImageView iv = new ImageView(ctx);
        iv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new VH(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String raw = photos.get(pos);
        if (raw == null || raw.isEmpty()) return;
        try {
            String data = raw.contains(",")
                    ? raw.substring(raw.indexOf(",") + 1) : raw;
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            Glide.with(ctx)
                    .load(bytes)
                    .centerCrop()
                    .into(h.imageView);
        } catch (Exception e) {
            h.imageView.setImageResource(android.R.color.darker_gray);
        }
    }

    @Override public int getItemCount() { return photos.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;
        VH(ImageView iv) { super(iv); imageView = iv; }
    }
}