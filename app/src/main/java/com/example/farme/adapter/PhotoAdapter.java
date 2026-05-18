package com.example.farme.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.farme.R;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private final Context context;
    private final List<Uri> photos = new ArrayList<>();
    private OnPhotoRemoveListener listener;

    public interface OnPhotoRemoveListener {
        void onRemove(int position);
    }

    public PhotoAdapter(Context context, OnPhotoRemoveListener listener) {
        this.context  = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = photos.get(position);
        Glide.with(context).load(uri).centerCrop().into(holder.ivPhoto);
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return photos.size(); }

    public void addPhoto(Uri uri) {
        photos.add(uri);
        notifyItemInserted(photos.size() - 1);
    }

    public void removePhoto(int position) {
        photos.remove(position);
        notifyItemRemoved(position);
    }

    public List<Uri> getPhotos() { return photos; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto   = itemView.findViewById(R.id.ivPhotoPreview);
            btnRemove = itemView.findViewById(R.id.btnRemovePhoto);
        }
    }
}