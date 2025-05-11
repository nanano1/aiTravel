package com.example.trave.Adapters;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Activities.SiteDetailActivity;
import com.example.trave.Activities.TripDetailActivity;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.List;

public class AttractionAdapter extends RecyclerView.Adapter<AttractionAdapter.AttractionAdapteViewHolder>{
    private List<ItineraryAttraction> itineraryAttractions;
    public AttractionAdapter(List<ItineraryAttraction> itineraryAttractions) {
        this.itineraryAttractions = itineraryAttractions;
    }
    @NonNull
    @Override
    public AttractionAdapteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_item, parent, false);
        return new AttractionAdapteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttractionAdapter.AttractionAdapteViewHolder holder, int position) {
        ItineraryAttraction itineraryAttraction= itineraryAttractions.get(position);
        holder.attractionName.setText(itineraryAttraction.getAttractionName());
        holder.tvTransprot.setText(itineraryAttraction.getTransport());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, SiteDetailActivity.class);
                intent.putExtra("siteId", itineraryAttraction.getSiteId());
                context.startActivity(intent);
            }
        });
    }
    @Override
    public int getItemCount() {
        return itineraryAttractions.size();
    }
    public static class AttractionAdapteViewHolder extends RecyclerView.ViewHolder{
        ImageView ivImage;
        TextView attractionName,tvTransprot;
        public AttractionAdapteViewHolder(View itemView){
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            attractionName = itemView.findViewById(R.id.attractionName);
            tvTransprot = itemView.findViewById(R.id.tvTransprot);
        }
    }
}
