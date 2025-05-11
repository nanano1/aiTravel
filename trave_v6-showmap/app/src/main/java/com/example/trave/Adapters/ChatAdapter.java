package com.example.trave.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave.Domains.ChatMessage;
import com.example.trave.R;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;
    private static final int TYPE_RECOMMENDATION = 3;

    private List<ChatMessage> messages;
    private OnRecommendationClickListener listener;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public interface OnRecommendationClickListener {
        void onSelectClick(String itemId);
        void onRefreshClick();
        void onDetailsClick(String itemId);
    }

    public void setOnRecommendationClickListener(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_USER:
                return new UserMessageViewHolder(
                        inflater.inflate(R.layout.item_chat_user, parent, false));
            case TYPE_AI:
                return new AIMessageViewHolder(
                        inflater.inflate(R.layout.item_chat_ai, parent, false));
            case TYPE_RECOMMENDATION:
                return new RecommendationViewHolder(
                        inflater.inflate(R.layout.item_recommendation_card, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        switch (holder.getItemViewType()) {
            case TYPE_USER:
                ((UserMessageViewHolder) holder).bind(message);
                break;
            case TYPE_AI:
                ((AIMessageViewHolder) holder).bind(message);
                break;
            case TYPE_RECOMMENDATION:
                ((RecommendationViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isRecommendation()) {
            return TYPE_RECOMMENDATION;
        }
        return message.isFromUser() ? TYPE_USER : TYPE_AI;
    }

    // 用户消息ViewHolder
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());
        }
    }

    // AI消息ViewHolder
    static class AIMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        AIMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());
        }
    }

    // 推荐卡片ViewHolder
    class RecommendationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvDistance;
        private TextView tvReason;
        private Button btnSelect;
        private Button btnRefresh;
        private Button btnDetails;

        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnSelect = itemView.findViewById(R.id.btnSelect);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }

        void bind(ChatMessage message) {
            // 设置推荐卡片数据
            tvTitle.setText(message.getTitle());
            tvDistance.setText(message.getDistance());
            tvReason.setText(message.getReason());

            // 设置按钮点击事件
            btnSelect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSelectClick(message.getItemId());
                }
            });

            btnDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDetailsClick(message.getItemId());
                }
            });
        }
    }
} 