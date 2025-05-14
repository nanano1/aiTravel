package com.example.trave.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Domains.RecommendedPOI;
import com.example.trave.R;

import java.util.List;

public class POIRecommendationAdapter extends RecyclerView.Adapter<POIRecommendationAdapter.RecommendationViewHolder> {
    private List<RecommendedPOI> pois;
    private OnRecommendationClickListener listener;
    private Context context;

    public POIRecommendationAdapter(List<RecommendedPOI> pois, OnRecommendationClickListener listener) {
        this.pois = pois;
        this.listener = listener;
    }

    public interface OnRecommendationClickListener {
        void onSelectClick(RecommendedPOI poi);
        void onRefreshClick();
        void onDetailsClick(RecommendedPOI poi);
    }

    public void setOnRecommendationClickListener(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    public void updateRecommendations(List<RecommendedPOI> newPOIs) {
        this.pois = newPOIs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_recommendation_card, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        RecommendedPOI poi = pois.get(position);
        
        // 设置景点信息
        holder.tvTitle.setText(poi.getName());
        holder.ratingBar.setRating((float) poi.getRating());
        
        // 设置距离信息
        String distanceInfo = "";
        if (poi.getDistance() != null && !poi.getDistance().isEmpty()) {
            distanceInfo = String.format("距离: %s", poi.getDistance());
        }
        
        // 设置类型信息
        String typeInfo = poi.getSimpleType();
        if (!distanceInfo.isEmpty() && !typeInfo.isEmpty()) {
            holder.tvDistance.setText(String.format("%s · %s", distanceInfo, typeInfo));
        } else if (!distanceInfo.isEmpty()) {
            holder.tvDistance.setText(distanceInfo);
        } else if (!typeInfo.isEmpty()) {
            holder.tvDistance.setText(typeInfo);
        }
        
        // 设置推荐理由
        String reason = poi.getRecommendationReason();
        if (reason != null && !reason.isEmpty()) {
            holder.tvReason.setText(reason);
        } else {
            holder.tvReason.setText(poi.getSimpleType());
        }
        
        // 设置按钮点击事件
        holder.btnSelect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelectClick(poi);
            }
        });
        
        holder.btnDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDetailsClick(poi);
            }
        });
        
        holder.btnRefresh.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRefreshClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return pois != null ? pois.size() : 0;
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        RatingBar ratingBar;
        TextView tvDistance;
        TextView tvReason;
        Button btnSelect;
        Button btnDetails;
        Button btnRefresh;

        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnSelect = itemView.findViewById(R.id.btnSelect);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            btnRefresh = itemView.findViewById(R.id.btnRefresh);
        }
    }
} 