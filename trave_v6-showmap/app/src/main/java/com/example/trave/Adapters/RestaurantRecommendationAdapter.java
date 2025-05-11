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

import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.R;

import java.util.List;

public class RestaurantRecommendationAdapter extends RecyclerView.Adapter<RestaurantRecommendationAdapter.RecommendationViewHolder> {
    private List<RecommendedRestaurant> restaurants;
    private OnRecommendationClickListener listener;
    private Context context;

    public RestaurantRecommendationAdapter(List<RecommendedRestaurant> restaurants, OnRecommendationClickListener listener) {
        this.restaurants = restaurants;
        this.listener = listener;
    }

    public interface OnRecommendationClickListener {
        void onSelectClick(RecommendedRestaurant restaurant);
        void onRefreshClick();
        void onDetailsClick(RecommendedRestaurant restaurant);
    }

    public void setOnRecommendationClickListener(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    public void updateRecommendations(List<RecommendedRestaurant> newRestaurants) {
        this.restaurants = newRestaurants;
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
        RecommendedRestaurant restaurant = restaurants.get(position);
        
        // 设置餐厅信息
        holder.tvTitle.setText(restaurant.getName());
        holder.ratingBar.setRating((float) restaurant.getRating());
        
        // 设置距离和价格信息
        String priceInfo = String.format("人均: ¥%.0f", restaurant.getPriceLevel());
        if (restaurant.getDistance() != null && !restaurant.getDistance().isEmpty()) {
            holder.tvDistance.setText(String.format("%s · %s", restaurant.getDistance(), priceInfo));
        } else {
            holder.tvDistance.setText(priceInfo);
        }
        
        // 设置推荐理由
        String reason = restaurant.getReason();
        if (reason != null && !reason.isEmpty()) {
            // 如果有推荐理由，显示推荐理由和菜系
            String cuisineType = restaurant.getCuisineType();
            holder.tvReason.setText(String.format("%s\n%s", cuisineType, reason));
        } else {
            // 如果没有推荐理由，只显示菜系
            holder.tvReason.setText(restaurant.getCuisineType());
        }
        
        // 设置按钮点击事件
        holder.btnSelect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelectClick(restaurant);
            }
        });

        
        holder.btnDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDetailsClick(restaurant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurants != null ? restaurants.size() : 0;
    }

    /**
     * 根据价格级别返回价格标识
     */
    private String getPriceLevelString(double priceLevel) {
        int level = (int) Math.round(priceLevel);
        switch (level) {
            case 1:
                return "¥";
            case 2:
                return "¥¥";
            case 3:
                return "¥¥¥";
            case 4:
                return "¥¥¥¥";
            default:
                return "¥";
        }
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        RatingBar ratingBar;
        TextView tvDistance;
        TextView tvReason;
        Button btnSelect;
        Button btnDetails;

        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnSelect = itemView.findViewById(R.id.btnSelect);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
} 