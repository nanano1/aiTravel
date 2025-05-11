package com.example.trave.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;
import java.util.ArrayList;

public class ItineraryDetailAdapter extends RecyclerView.Adapter<ItineraryDetailAdapter.ViewHolder> {
    private ArrayList<ItineraryAttraction> attractions;

    public ItineraryDetailAdapter(ArrayList<ItineraryAttraction> attractions) {
        this.attractions = attractions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItineraryAttraction attraction = attractions.get(position);
        holder.tvDay.setText("第" + attraction.getDayNumber() + "天");
        holder.tvOrder.setText("第" + attraction.getVisitOrder() + "站");
        holder.tvName.setText(attraction.getAttractionName());
        
        // 设置类型标签
        String type = attraction.getType();
        if (type != null && !type.isEmpty()) {
            holder.tvType.setVisibility(View.VISIBLE);
            holder.tvType.setText(type);
        } else {
            holder.tvType.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return attractions.size();
    }

    public void updateAttractions(ArrayList<ItineraryAttraction> newAttractions) {
        this.attractions = newAttractions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        TextView tvOrder;
        TextView tvName;
        TextView tvType;

        ViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvOrder = itemView.findViewById(R.id.tvOrder);
            tvName = itemView.findViewById(R.id.tvName);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }
} 