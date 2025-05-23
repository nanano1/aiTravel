package com.example.trave.Adapters;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.ArrayList;
import java.util.List;

public class TripDetailAdapter extends RecyclerView.Adapter<TripDetailAdapter.TripDetailViewHolder>  {

    private Context context;
    private DatabaseHelper dbHelper;
    private ArrayList<ItineraryAttraction> itineraryAttractionList;
    private Long ItineraryId;
    public TripDetailAdapter(long itineraryId,Context context, ArrayList<ItineraryAttraction> itineraryAttractionList) {
        this.context=context;
        this.ItineraryId=itineraryId;
        dbHelper = new DatabaseHelper(context);
        this.itineraryAttractionList = itineraryAttractionList;
    }

    @NonNull
    @Override
    public TripDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_day, parent, false);
        return new TripDetailViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull TripDetailViewHolder holder, int position) {
        int day = position + 1;
        holder.tvDay.setText("Day: " + day);

        List<ItineraryAttraction> dailyAttractions = new ArrayList<>();
        for (ItineraryAttraction attraction : itineraryAttractionList) {
            if (attraction.getDayNumber() == day) {
                dailyAttractions.add(attraction);
            }
        }
        if (dailyAttractions!= null) {
            Log.d("TripDetailAdapter", "Number of daily attractions for day " + day + ": " + dailyAttractions.size());
        } else {
            Log.e("TripDetailAdapter", "No daily attractions for day " + day + ". dailyAttractions is null.");
        }
        
        // 使用LinearLayoutManager替代GridLayoutManager，并设置为垂直方向
        LinearLayoutManager layoutManager = new LinearLayoutManager(holder.itemView.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        holder.recyclerViewItems.setLayoutManager(layoutManager);
        
        // 启用嵌套滚动
        holder.recyclerViewItems.setNestedScrollingEnabled(true);
        
        AttractionAdapter itemAdapter = new AttractionAdapter(dailyAttractions);
        holder.recyclerViewItems.setAdapter(itemAdapter);
    }

    @Override
    public int getItemCount() {
        int maxDay = 0;
        for (ItineraryAttraction attraction : itineraryAttractionList) {
            if (attraction.getDayNumber() > maxDay) {
                maxDay = attraction.getDayNumber();
            }
        }
        return maxDay;
    }
    public void refreshAttractionData() {
        itineraryAttractionList.clear();
        itineraryAttractionList.addAll(dbHelper.getItineraryAttractions(ItineraryId));
        notifyDataSetChanged();
    }
    public static class TripDetailViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvRoute, tvSummary;
        RecyclerView recyclerViewItems;
        Button editButton;
        public TripDetailViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            recyclerViewItems = itemView.findViewById(R.id.recyclerViewItems);
        }
    }
}
