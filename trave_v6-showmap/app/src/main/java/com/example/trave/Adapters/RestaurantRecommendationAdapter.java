package com.example.trave.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Activities.RestaurantDetailActivity;
import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.R;

import java.util.List;

public class RestaurantRecommendationAdapter extends RecyclerView.Adapter<RestaurantRecommendationAdapter.RecommendationViewHolder> {
    private List<RecommendedRestaurant> restaurants;
    private OnRecommendationClickListener listener;
    private Context context;
    private long itineraryId;
    private int dayNumber;
    private String mealType;

    public RestaurantRecommendationAdapter(List<RecommendedRestaurant> restaurants, OnRecommendationClickListener listener) {
        this.restaurants = restaurants;
        this.listener = listener;
    }
    
    public void setItineraryInfo(long itineraryId, int dayNumber, String mealType) {
        this.itineraryId = itineraryId;
        this.dayNumber = dayNumber;
        this.mealType = mealType;
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
        
        // 设置价格信息
        String priceInfo = String.format("人均: ¥%.0f", restaurant.getPriceLevel());
        holder.tvDistance.setText(priceInfo);
        
        // 设置推荐理由和菜系
        String reason = restaurant.getReason();
        String cuisineType = restaurant.getCuisineType();
        if (reason != null && !reason.isEmpty()) {
            holder.tvReason.setText(String.format("%s\n%s", cuisineType, reason));
        } else {
            holder.tvReason.setText(cuisineType);
        }
        
        // 设置整个卡片的点击事件
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDetailActivity(restaurant);
            }
        });
        
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
        
        holder.btnRefresh.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRefreshClick();
            }
        });
    }
    
    private void openDetailActivity(RecommendedRestaurant restaurant) {
        Intent intent = new Intent(context, RestaurantDetailActivity.class);
        intent.putExtra("restaurant_json", restaurant.getOriginalData().toString());
        intent.putExtra("itinerary_id", itineraryId);
        intent.putExtra("day_number", dayNumber);
        intent.putExtra("meal_type", mealType);
        context.startActivity(intent);
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
        CardView cardView;
        TextView tvTitle;
        RatingBar ratingBar;
        TextView tvDistance;
        TextView tvReason;
        Button btnSelect;
        Button btnDetails;
        Button btnRefresh;

        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
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