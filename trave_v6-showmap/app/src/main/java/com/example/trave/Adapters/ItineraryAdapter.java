package com.example.trave.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.example.trave.Activities.TripDetailMainActivity;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.ArrayList;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {
    private ArrayList<Itinerary> items;
    private Context context;
    private DatabaseHelper dbHelper;
    long Userid;

    public ItineraryAdapter(Context context, long UserId) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
        this.items = dbHelper.getAllPublishItineraries();
        this.Userid = UserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.itinerary_overview, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d("ItineraryAdapter", "Days: " + items.get(position).getLocation());
        holder.tittleText.setText(items.get(position).getTittle());
        holder.locationTxt.setText(items.get(position).getLocation());

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
                Toast.makeText(context, "准备跳转到 TripDetailMainActivity", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, TripDetailMainActivity.class);
                intent.putExtra("itineraryId", itinerary.getId());
                intent.putExtra("days", itinerary.getDays());
                intent.putExtra("Tittle", itinerary.getTittle());
                intent.putExtra("itineraryLocation", itinerary.getLocation());
                Log.i("ItemClick", "点击了Item，准备跳转！");
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void refreshData() {
        items.clear();
        items.addAll(dbHelper.getAllPublishItineraries());
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        Button editButton;
        TextView tittleText, locationTxt, daysTxt, itid;
        ImageView pic;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tittleText = itemView.findViewById(R.id.tittleTxt);
            locationTxt = itemView.findViewById(R.id.locationTxt);
            pic = itemView.findViewById(R.id.picImg);
            daysTxt = itemView.findViewById(R.id.daysTxt);
        }
    }
}
