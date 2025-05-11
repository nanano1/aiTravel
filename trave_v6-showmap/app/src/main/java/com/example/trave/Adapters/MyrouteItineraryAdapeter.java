package com.example.trave.Adapters;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.example.trave.Activities.TripDetailActivity;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.R;

import java.util.ArrayList;

public class MyrouteItineraryAdapeter  extends RecyclerView.Adapter<MyrouteItineraryAdapeter.ViewHolder> {
    private ArrayList<Itinerary> items;
    private Context context;
    private DatabaseHelper dbHelper;
    long Userid;
    public MyrouteItineraryAdapeter(Context context,ArrayList<Itinerary> items,long UserId) {
        this.context=context;
        dbHelper = new DatabaseHelper(context);
        this.items = dbHelper.getUserItineraries(Userid);
        this.Userid=UserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.itinerary_overview, parent, false);
        return new ViewHolder(inflate);

    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tittleText.setText(items.get(position).getTittle());
        holder.locationTxt.setText(items.get(position).getLocation());
        holder.daysTxt.setText(items.get(position).getDays()+"天");
        Itinerary itinerary = items.get(position);
                int drawableResId = holder.itemView.getResources().getIdentifier(items.get(position).getPic(),
                "drawable", holder.itemView.getContext().getPackageName());
        Glide.with(holder.itemView.getContext())
                .load(drawableResId)
                .transform(new CenterCrop(), new GranularRoundedCorners(40, 40, 40, 40))
                .into(holder.pic);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, TripDetailActivity.class);
                intent.putExtra("itineraryId", itinerary.getId());
                intent.putExtra("days", itinerary.getDays());
                intent.putExtra("Tittle", itinerary.getTittle());
                intent.putExtra("itineraryLocation", itinerary.getLocation());
                context.startActivity(intent);
            }
        });

    }
    public void updateData() {
        this.items.clear();
        this.items.addAll(dbHelper.getUserItineraries(Userid));

        notifyDataSetChanged();  // 刷新RecyclerView
    }


    @Override
    public int getItemCount() {
        Log.d("ItineraryAdapter", "Item count: " + items.size());
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        Button editButton;
        TextView tittleText, locationTxt, daysTxt,itid;
        ImageView pic;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tittleText = itemView.findViewById(R.id.tittleTxt);
            locationTxt = itemView.findViewById(R.id.locationTxt);
            pic = itemView.findViewById(R.id.picImg);
            daysTxt=itemView.findViewById(R.id.daysTxt);
        }
    }
}
