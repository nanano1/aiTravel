package com.example.trave.Adapters;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;
import java.util.ArrayList;
import java.util.Collections;

public class ItineraryDetailAdapter extends RecyclerView.Adapter<ItineraryDetailAdapter.ViewHolder> {
    private ArrayList<ItineraryAttraction> attractions;
    private OnStartDragListener mDragStartListener;
    private OnItemClickListener mClickListener;
    private boolean isEditMode = false;

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public interface OnItemClickListener {
        void onItemClick(ItineraryAttraction attraction);
    }

    public ItineraryDetailAdapter(ArrayList<ItineraryAttraction> attractions) {
        this.attractions = attractions;
    }

    public void setOnStartDragListener(OnStartDragListener dragStartListener) {
        mDragStartListener = dragStartListener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mClickListener = listener;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
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
            
            // 如果是AI推荐的餐厅，添加特殊标记
            if (type.equals("餐厅") && attraction.isAiRecommended()) {
                SpannableString spannableString = new SpannableString("🧠 AI优化 " + type);
                spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FF8C00")), 
                    0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.tvType.setText(spannableString);
                
                // 设置橙色边框高亮
                if (holder.cardView != null) {
                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
                    // 使用背景色替代边框
                    holder.cardView.setCardElevation(4);
                    
                    // 添加闪烁动画
                    startHighlightAnimation(holder.cardView);
                }
            } else {
                // 恢复默认样式
                if (holder.cardView != null) {
                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
                    holder.cardView.setCardElevation(2);
                }
            }
        } else {
            holder.tvType.setVisibility(View.GONE);
        }
        
        // 编辑模式下显示拖动手柄
        if (isEditMode) {
            holder.dragHandle.setVisibility(View.VISIBLE);
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (mDragStartListener != null) {
                        mDragStartListener.onStartDrag(holder);
                    }
                }
                return false;
            });
        } else {
            holder.dragHandle.setVisibility(View.GONE);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onItemClick(attraction);
            }
        });
    }

    // 高亮动画
    private void startHighlightAnimation(CardView cardView) {
        ValueAnimator animator = ValueAnimator.ofFloat(2f, 6f);
        animator.setDuration(1000);
        animator.setRepeatCount(1);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            cardView.setCardElevation(val);
        });
        animator.start();
    }

    // 获取所有景点数据
    public ArrayList<ItineraryAttraction> getAttractions() {
        return attractions;
    }

    // 移动项目（拖拽排序）
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(attractions, i, i + 1);
                
                // 更新访问顺序
                ItineraryAttraction from = attractions.get(i);
                ItineraryAttraction to = attractions.get(i + 1);
                
                int tempOrder = from.getVisitOrder();
                from.setVisitOrder(to.getVisitOrder());
                to.setVisitOrder(tempOrder);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(attractions, i, i - 1);
                
                // 更新访问顺序
                ItineraryAttraction from = attractions.get(i);
                ItineraryAttraction to = attractions.get(i - 1);
                
                int tempOrder = from.getVisitOrder();
                from.setVisitOrder(to.getVisitOrder());
                to.setVisitOrder(tempOrder);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    // 标记为AI推荐
    public void markAsAiRecommended(long id) {
        for (ItineraryAttraction attraction : attractions) {
            if (attraction.getId() == id) {
                attraction.setAiRecommended(true);
                notifyDataSetChanged();
                break;
            }
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
        CardView cardView;
        ImageView dragHandle;

        ViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvOrder = itemView.findViewById(R.id.tvOrder);
            tvName = itemView.findViewById(R.id.tvName);
            tvType = itemView.findViewById(R.id.tvType);
            cardView = itemView.findViewById(R.id.cardView);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }
} 