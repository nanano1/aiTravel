package com.example.trave.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.List;

public class detailEditADapter extends RecyclerView.Adapter<detailEditADapter.detailEditViewHolder> {
    private List<ItineraryAttraction> itineraryAttractions;

    public detailEditADapter(List<ItineraryAttraction> itineraryAttractions) {
        this.itineraryAttractions = itineraryAttractions;
    }

    @NonNull
    @Override
    public detailEditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_attraction, parent, false);
        return new detailEditViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull detailEditViewHolder holder, int position) {
        ItineraryAttraction itineraryAttraction = itineraryAttractions.get(position);
        holder.attractionNameEditText.setText(itineraryAttraction.getAttractionName());
        holder.transportEditText.setText(itineraryAttraction.getTransport());
        holder.dayNumberEditText.setText(String.valueOf(itineraryAttraction.getDayNumber()));
        holder.visitOrderEditText.setText(String.valueOf(itineraryAttraction.getVisitOrder()));

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itineraryAttractions.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, itineraryAttractions.size());
            }
        });
    }
    @Override
    public int getItemCount() {
        return itineraryAttractions.size();
    }

    public static class detailEditViewHolder extends RecyclerView.ViewHolder {
        public EditText attractionNameEditText;
        public EditText transportEditText;
        public EditText dayNumberEditText;
        public EditText visitOrderEditText;
        ImageView deleteButton;

        public detailEditViewHolder(View itemView) {
            super(itemView);
            attractionNameEditText = itemView.findViewById(R.id.attractionNameEditText);
            transportEditText = itemView.findViewById(R.id.transportEditText);
            dayNumberEditText = itemView.findViewById(R.id.dayNumberEditText);
            visitOrderEditText = itemView.findViewById(R.id.visitOrderEditText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
