package com.example.farme.adapter;

import android.content.Context;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import com.example.farme.R;
import com.example.farme.model.Message;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private static final int TYPE_MINE  = 1;
    private static final int TYPE_OTHER = 2;

    private final Context       ctx;
    private final List<Message> messages;
    private final String        myUid;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(Context ctx, List<Message> messages, String myUid) {
        this.ctx      = ctx;
        this.messages = messages;
        this.myUid    = myUid;
    }

    @Override
    public int getItemViewType(int pos) {
        return myUid.equals(messages.get(pos).getSenderId())
                ? TYPE_MINE : TYPE_OTHER;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        int layout = type == TYPE_MINE
                ? R.layout.item_message_mine
                : R.layout.item_message_other;
        View v = LayoutInflater.from(ctx).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Message m = messages.get(pos);
        if (h.tvText != null)
            h.tvText.setText(m.getText());
        if (h.tvTime != null && m.getCreatedAt() > 0)
            h.tvTime.setText(sdf.format(new Date(m.getCreatedAt())));
    }

    @Override public int getItemCount() { return messages.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        VH(View v) {
            super(v);
            tvText = v.findViewById(R.id.tvMessageText);
            tvTime = v.findViewById(R.id.tvMessageTime);
        }
    }
}