package com.example.trave.Adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.Collections;
import java.util.List;

public class EnhancedEditAdapter extends RecyclerView.Adapter<EnhancedEditAdapter.EnhancedEditViewHolder> {
    
    private List<ItineraryAttraction> itineraryAttractions;
    private OnStartDragListener dragListener;
    
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }
    
    public EnhancedEditAdapter(List<ItineraryAttraction> itineraryAttractions) {
        this.itineraryAttractions = itineraryAttractions;
    }
    
    public void setOnStartDragListener(OnStartDragListener listener) {
        this.dragListener = listener;
    }

    @NonNull
    @Override
    public EnhancedEditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_enhanced_edit_attraction, parent, false);
        return new EnhancedEditViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EnhancedEditViewHolder holder, int position) {
        ItineraryAttraction itineraryAttraction = itineraryAttractions.get(position);
        holder.attractionNameEditText.setText(itineraryAttraction.getAttractionName());
        holder.transportEditText.setText(itineraryAttraction.getTransport());
        holder.dayNumberEditText.setText(String.valueOf(itineraryAttraction.getDayNumber()));
        
        // 设置访问顺序为位置+1，并隐藏输入框
        itineraryAttraction.setVisitOrder(position + 1);
        holder.visitOrderEditText.setVisibility(View.GONE);
        
        // 正确处理父视图的可见性
        ViewGroup parent = (ViewGroup) holder.visitOrderEditText.getParent();
        if (parent != null) {
            parent.setVisibility(View.GONE);
        }

        // 设置删除按钮点击事件
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    itineraryAttractions.remove(adapterPosition);
                    notifyItemRemoved(adapterPosition);
                    notifyItemRangeChanged(adapterPosition, itineraryAttractions.size());
                    updateVisitOrders(); // 更新删除后的顺序
                }
            }
        });
        
        // 设置拖拽手柄触摸事件
        holder.dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && dragListener != null) {
                    dragListener.onStartDrag(holder);
                    return true;
                }
                return false;
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return itineraryAttractions.size();
    }
    
    // 移动项目的方法
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(itineraryAttractions, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(itineraryAttractions, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        updateVisitOrders(); // 更新移动后的顺序
    }
    
    // 更新所有项目的游览顺序
    private void updateVisitOrders() {
        for (int i = 0; i < itineraryAttractions.size(); i++) {
            ItineraryAttraction attraction = itineraryAttractions.get(i);
            attraction.setVisitOrder(i + 1); // 设置新的顺序，从1开始
        }
        notifyDataSetChanged();
    }
    
    // 获取当前所有景点
    public List<ItineraryAttraction> getAttractions() {
        return itineraryAttractions;
    }
    
    // 添加新景点
    public void addAttraction(ItineraryAttraction attraction) {
        itineraryAttractions.add(attraction);
        attraction.setVisitOrder(itineraryAttractions.size()); // 设置为最后一个
        notifyItemInserted(itineraryAttractions.size() - 1);
    }
    
    // 更新所有景点的数据
    public void updateAttractions() {
        for (int i = 0; i < getItemCount(); i++) {
            View itemView = null; // 这里需要通过RecyclerView获取，在Activity中实现
            if (itemView != null) {
                EnhancedEditViewHolder holder = (EnhancedEditViewHolder) itemView.getTag();
                ItineraryAttraction attraction = itineraryAttractions.get(i);
                
                attraction.setAttractionName(holder.attractionNameEditText.getText().toString());
                attraction.setTransport(holder.transportEditText.getText().toString());
                
                try {
                    attraction.setDayNumber(Integer.parseInt(holder.dayNumberEditText.getText().toString()));
                } catch (NumberFormatException e) {
                    // 使用默认值
                    attraction.setDayNumber(1);
                }
                
                // 设置访问顺序为位置+1
                attraction.setVisitOrder(i + 1);
            }
        }
    }

    public static class EnhancedEditViewHolder extends RecyclerView.ViewHolder {
        public EditText attractionNameEditText;
        public EditText transportEditText;
        public EditText dayNumberEditText;
        public EditText visitOrderEditText;
        public ImageView deleteButton;
        public ImageView dragHandle;

        public EnhancedEditViewHolder(View itemView) {
            super(itemView);
            attractionNameEditText = itemView.findViewById(R.id.attractionNameEditText);
            transportEditText = itemView.findViewById(R.id.transportEditText);
            dayNumberEditText = itemView.findViewById(R.id.dayNumberEditText);
            visitOrderEditText = itemView.findViewById(R.id.visitOrderEditText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            dragHandle = itemView.findViewById(R.id.dragHandle);
            
            // 将ViewHolder设置为itemView的tag，方便后续获取
            itemView.setTag(this);
        }
    }
} 