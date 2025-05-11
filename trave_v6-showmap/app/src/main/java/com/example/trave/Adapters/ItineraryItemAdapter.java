package com.example.trave.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.Collections;
import java.util.List;

public class ItineraryItemAdapter extends RecyclerView.Adapter<ItineraryItemAdapter.ItemViewHolder> {
    private Context context;
    private List<ItineraryAttraction> attractions;
    private OnItemActionListener listener;
    private OnStartDragListener dragListener;

    public interface OnItemActionListener {
        void onEdit(ItineraryAttraction attraction);
        void onDelete(ItineraryAttraction attraction);
        void onShowReason(ItineraryAttraction attraction);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public ItineraryItemAdapter(Context context, List<ItineraryAttraction> attractions,
                              OnItemActionListener listener, OnStartDragListener dragListener) {
        this.context = context;
        this.attractions = attractions;
        this.listener = listener;
        this.dragListener = dragListener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_itinerary_attraction, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItineraryAttraction attraction = attractions.get(position);
        
        holder.tvAttractionName.setText(attraction.getAttractionName());
        holder.tvVisitOrder.setText(String.format("%d.", attraction.getVisitOrder()));
        
        // 设置交通方式图标
        setTransportIcon(holder.ivTransport, attraction.getTransport());

        // 设置按钮点击事件
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(attraction);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(attraction);
            }
        });

        // 设置拖拽手柄的触摸事件
        holder.ivDragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dragListener.onStartDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return attractions.size();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(attractions, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(attractions, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        
        // 更新访问顺序
        for (int i = 0; i < attractions.size(); i++) {
            attractions.get(i).setVisitOrder(i + 1);
        }
    }

    private void setTransportIcon(ImageView imageView, String transport) {
        // 根据交通方式设置对应的图标
        int iconRes;
        switch (transport.toLowerCase()) {
            case "walking":
                iconRes = android.R.drawable.ic_menu_directions;
                break;
            case "driving":
                iconRes = android.R.drawable.ic_menu_directions;
                break;
            case "transit":
                iconRes = android.R.drawable.ic_menu_directions;
                break;
            default:
                iconRes = android.R.drawable.ic_menu_directions;
        }
        imageView.setImageResource(iconRes);
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvAttractionName;
        TextView tvVisitOrder;
        ImageView ivTransport;
        TextView tvAiOptimized;
        ImageButton btnEdit;
        ImageButton btnDelete;
        ImageView ivDragHandle;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAttractionName = itemView.findViewById(R.id.tvAttractionName);
            tvVisitOrder = itemView.findViewById(R.id.tvVisitOrder);
            ivTransport = itemView.findViewById(R.id.ivTransport);
            tvAiOptimized = itemView.findViewById(R.id.tvAiOptimized);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
        }
    }
} 