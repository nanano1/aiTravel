package com.example.trave.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatItineraryAdapter extends RecyclerView.Adapter<ChatItineraryAdapter.DayViewHolder> {
    private Context context;
    private JSONObject itineraryData;
    private int days;
    private Map<Integer, List<JSONObject>> dayAttractions = new HashMap<>();

    public ChatItineraryAdapter(Context context, JSONObject itineraryData) {
        this.context = context;
        this.itineraryData = itineraryData;
        processItineraryData();
    }

    private void processItineraryData() {
        if (itineraryData == null) {
            days = 0;
            return;
        }

        try {
            days = itineraryData.getInt("days");
            JSONArray attractions = itineraryData.getJSONArray("attractions");

            // 清除旧数据
            dayAttractions.clear();

            // 按天分组景点
            for (int i = 0; i < attractions.length(); i++) {
                JSONObject attraction = attractions.getJSONObject(i);
                int day = attraction.getInt("day");

                if (!dayAttractions.containsKey(day)) {
                    dayAttractions.put(day, new ArrayList<>());
                }
                dayAttractions.get(day).add(attraction);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            days = 0;
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int day = position + 1;
        holder.tvDay.setText("Day " + day);

        List<JSONObject> dailyAttractions = dayAttractions.get(day);
        if (dailyAttractions != null && !dailyAttractions.isEmpty()) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            holder.recyclerViewItems.setLayoutManager(layoutManager);
            
            ChatDailyAttractionAdapter adapter = new ChatDailyAttractionAdapter(dailyAttractions);
            holder.recyclerViewItems.setAdapter(adapter);
        }
    }

    @Override
    public int getItemCount() {
        return days;
    }

    public void updateData(JSONObject newItineraryData) {
        this.itineraryData = newItineraryData;
        processItineraryData();
        notifyDataSetChanged();
    }

    // 视图持有者类
    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        RecyclerView recyclerViewItems;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            recyclerViewItems = itemView.findViewById(R.id.recyclerViewItems);
        }
    }

    // 每日景点适配器
    private class ChatDailyAttractionAdapter extends RecyclerView.Adapter<ChatDailyAttractionAdapter.AttractionViewHolder> {
        private List<JSONObject> attractions;

        public ChatDailyAttractionAdapter(List<JSONObject> attractions) {
            this.attractions = attractions;
        }

        @NonNull
        @Override
        public AttractionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_item, parent, false);
            return new AttractionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AttractionViewHolder holder, int position) {
            try {
                JSONObject attraction = attractions.get(position);
                String name = attraction.getString("name");
                int order = attraction.getInt("order");
                String type = attraction.optString("type", "景点");
                String transport = attraction.optString("transport", "步行");

                holder.attractionName.setText(name);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return attractions.size();
        }

        // 景点视图持有者
        class AttractionViewHolder extends RecyclerView.ViewHolder {
            TextView attractionName;



            public AttractionViewHolder(@NonNull View itemView) {
                super(itemView);
                attractionName = itemView.findViewById(R.id.attractionName);
            }
        }
    }
} 