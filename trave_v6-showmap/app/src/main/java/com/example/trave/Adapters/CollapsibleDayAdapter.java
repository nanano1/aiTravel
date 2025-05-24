package com.example.trave.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;
import com.example.trave.DatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CollapsibleDayAdapter extends RecyclerView.Adapter<CollapsibleDayAdapter.DayViewHolder> {

    private List<Integer> dayNumbers; // 天数列表
    private Map<Integer, List<ItineraryAttraction>> attractionsByDay; // 按天分组的景点
    private OnDayItemClickListener listener;
    private boolean isEditMode = true;
    private String itineraryLocation; // 行程地点，用于POI搜索
    private DatabaseHelper dbHelper;
    private long itineraryId;
    
    // 存储每个天数对应的RecyclerView
    private Map<Integer, RecyclerView> dayRecyclerViews = new HashMap<>();
    // 存储每个天数对应的ItemTouchHelper
    private Map<Integer, ItemTouchHelper> dayItemTouchHelpers = new HashMap<>();
    
    public interface OnDayItemClickListener {
        void onExpandCollapse(int position, boolean isExpanded);
        void onAttractionDeleted(ItineraryAttraction attraction);
        void onDayEmpty(int dayNumber);
        void onAttractionDayChanged(ItineraryAttraction attraction, int oldDayNumber, int newDayNumber);
    }
    
    public CollapsibleDayAdapter(List<ItineraryAttraction> attractions, String itineraryLocation) {
        this.itineraryLocation = itineraryLocation;
        this.dayNumbers = new ArrayList<>();
        this.attractionsByDay = new HashMap<>();
        if (attractions != null && !attractions.isEmpty()) {
            this.itineraryId = attractions.get(0).getItineraryId();
            groupAttractionsByDay(attractions);
        }
    }
    
    public void setDatabaseHelper(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    
    public void setItineraryId(long itineraryId) {
        this.itineraryId = itineraryId;
    }
    
    private void groupAttractionsByDay(List<ItineraryAttraction> attractions) {
        attractionsByDay.clear();
        
        // 按天分组景点
        for (ItineraryAttraction attraction : attractions) {
            int day = attraction.getDayNumber();
            if (!attractionsByDay.containsKey(day)) {
                attractionsByDay.put(day, new ArrayList<>());
            }
            attractionsByDay.get(day).add(attraction);
        }
        
        // 确保每天的景点按照访问顺序排序
        for (List<ItineraryAttraction> dayAttractions : attractionsByDay.values()) {
            Collections.sort(dayAttractions, (a1, a2) -> Integer.compare(a1.getVisitOrder(), a2.getVisitOrder()));
        }
        
        // 更新天数列表
        dayNumbers = new ArrayList<>(attractionsByDay.keySet());
        Collections.sort(dayNumbers);
    }
    
    public void setOnDayItemClickListener(OnDayItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collapsible_day, parent, false);
        return new DayViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int dayNumber = dayNumbers.get(position);
        List<ItineraryAttraction> dayAttractions = attractionsByDay.get(dayNumber);
        
        holder.tvDay.setText("第" + dayNumber + "天");
        
        // 设置展开/折叠图标
        boolean isExpanded = holder.attractionsContainer.getVisibility() == View.VISIBLE;
        holder.expandCollapseIcon.setImageResource(
                isExpanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
        
        // 设置点击展开/折叠
        holder.dayHeaderLayout.setOnClickListener(v -> {
            boolean shouldExpand = holder.attractionsContainer.getVisibility() != View.VISIBLE;
            holder.attractionsContainer.setVisibility(shouldExpand ? View.VISIBLE : View.GONE);
            holder.expandCollapseIcon.setImageResource(
                    shouldExpand ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
            
            if (listener != null) {
                listener.onExpandCollapse(position, shouldExpand);
            }
        });
        
        // 设置景点列表
        AttractionAdapter attractionAdapter = new AttractionAdapter(dayAttractions, dayNumber);
        holder.recyclerViewAttractions.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerViewAttractions.setAdapter(attractionAdapter);
        
        // 保存RecyclerView引用
        dayRecyclerViews.put(dayNumber, holder.recyclerViewAttractions);
        
        // 为每个天数的RecyclerView设置独立的ItemTouchHelper
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new AttractionTouchHelperCallback(dayNumber));
        itemTouchHelper.attachToRecyclerView(holder.recyclerViewAttractions);
        dayItemTouchHelpers.put(dayNumber, itemTouchHelper);
        
        // 显示空提示或景点列表
        if (dayAttractions == null || dayAttractions.isEmpty()) {
            holder.emptyDayText.setVisibility(View.VISIBLE);
            holder.recyclerViewAttractions.setVisibility(View.GONE);
        } else {
            holder.emptyDayText.setVisibility(View.GONE);
            holder.recyclerViewAttractions.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public int getItemCount() {
        return dayNumbers.size();
    }
    
    // 添加新景点
    public void addAttraction(ItineraryAttraction attraction) {
        int day = attraction.getDayNumber();
        
        if (!attractionsByDay.containsKey(day)) {
            attractionsByDay.put(day, new ArrayList<>());
            dayNumbers.add(day);
            Collections.sort(dayNumbers);
            
            // 移除自动天数更新，由调用方在保存景点到数据库后负责更新
            // if (dbHelper != null && itineraryId > 0) {
            //     dbHelper.updateItineraryDaysFromAttractions(itineraryId);
            // }
            
            notifyDataSetChanged();
        } else {
            List<ItineraryAttraction> dayAttractions = attractionsByDay.get(day);
            // 设置为该天最后一个景点的顺序+1
            if (!dayAttractions.isEmpty()) {
                int maxOrder = 0;
                for (ItineraryAttraction att : dayAttractions) {
                    if (att.getVisitOrder() > maxOrder) {
                        maxOrder = att.getVisitOrder();
                    }
                }
                attraction.setVisitOrder(maxOrder + 1);
            } else {
                attraction.setVisitOrder(1);
            }
            
            dayAttractions.add(attraction);
            int position = dayNumbers.indexOf(day);
            notifyItemChanged(position);
        }
    }
    
    // 删除景点
    public void removeAttraction(ItineraryAttraction attraction) {
        int day = attraction.getDayNumber();
        
        if (attractionsByDay.containsKey(day)) {
            List<ItineraryAttraction> dayAttractions = attractionsByDay.get(day);
            int indexToRemove = -1;
            
            for (int i = 0; i < dayAttractions.size(); i++) {
                if (dayAttractions.get(i).getId() == attraction.getId()) {
                    indexToRemove = i;
                    break;
                }
            }
            
            if (indexToRemove != -1) {
                dayAttractions.remove(indexToRemove);
                
                // 重新排序剩余景点
                for (int i = 0; i < dayAttractions.size(); i++) {
                    dayAttractions.get(i).setVisitOrder(i + 1);
                }
                
                // 如果该天没有景点了，通知监听器
                if (dayAttractions.isEmpty() && listener != null) {
                    listener.onDayEmpty(day);
                }
                
                int position = dayNumbers.indexOf(day);
                notifyItemChanged(position);
            }
        }
    }
    
    // 移动景点到不同天数
    public void moveAttractionToDay(ItineraryAttraction attraction, int newDayNumber) {
        int oldDayNumber = attraction.getDayNumber();
        
        // 如果天数没有变化，不做处理
        if (oldDayNumber == newDayNumber) {
            return;
        }
        
        // 从原天数中移除
        if (attractionsByDay.containsKey(oldDayNumber)) {
            List<ItineraryAttraction> oldDayAttractions = attractionsByDay.get(oldDayNumber);
            oldDayAttractions.removeIf(a -> a.getId() == attraction.getId());
            
            // 重新排序原天数的景点
            for (int i = 0; i < oldDayAttractions.size(); i++) {
                oldDayAttractions.get(i).setVisitOrder(i + 1);
            }
            
            // 如果原天数没有景点了，通知监听器
            if (oldDayAttractions.isEmpty() && listener != null) {
                listener.onDayEmpty(oldDayNumber);
            }
        }
        
        // 添加到新天数
        if (!attractionsByDay.containsKey(newDayNumber)) {
            attractionsByDay.put(newDayNumber, new ArrayList<>());
            dayNumbers.add(newDayNumber);
            Collections.sort(dayNumbers);
        }
        
        List<ItineraryAttraction> newDayAttractions = attractionsByDay.get(newDayNumber);
        attraction.setDayNumber(newDayNumber);
        attraction.setVisitOrder(newDayAttractions.size() + 1);
        newDayAttractions.add(attraction);
        
        // 通知监听器
        if (listener != null) {
            listener.onAttractionDayChanged(attraction, oldDayNumber, newDayNumber);
        }
        
        // 移除自动天数更新，由保存时统一处理
        // if (dbHelper != null && itineraryId > 0) {
        //     dbHelper.updateItineraryDaysFromAttractions(itineraryId);
        // }
        
        // 刷新界面
        notifyDataSetChanged();
    }
    
    // 获取所有景点的列表（按天和顺序排序）
    public List<ItineraryAttraction> getAllAttractions() {
        List<ItineraryAttraction> allAttractions = new ArrayList<>();
        
        for (int day : dayNumbers) {
            List<ItineraryAttraction> dayAttractions = attractionsByDay.get(day);
            if (dayAttractions != null) {
                allAttractions.addAll(dayAttractions);
            }
        }
        
        return allAttractions;
    }
    
    // 更新景点数据
    public void updateAttractions(List<ItineraryAttraction> attractions) {
        groupAttractionsByDay(attractions);
        notifyDataSetChanged();
    }
    
    // 添加新的一天
    public void addNewDay() {
        int newDayNumber = 1;
        if (!dayNumbers.isEmpty()) {
            newDayNumber = Collections.max(dayNumbers) + 1;
        }
        
        attractionsByDay.put(newDayNumber, new ArrayList<>());
        dayNumbers.add(newDayNumber);
        Collections.sort(dayNumbers);
        
        // 移除自动天数更新，只有在保存景点到数据库时才更新天数
        // if (dbHelper != null && itineraryId > 0) {
        //     dbHelper.updateItineraryDaysFromAttractions(itineraryId);
        // }
        
        notifyDataSetChanged();
    }
    
    // 开始拖拽景点
    public void startDrag(RecyclerView.ViewHolder viewHolder, int dayNumber) {
        if (dayItemTouchHelpers.containsKey(dayNumber)) {
            ItemTouchHelper itemTouchHelper = dayItemTouchHelpers.get(dayNumber);
            if (itemTouchHelper != null) {
                itemTouchHelper.startDrag(viewHolder);
            }
        }
    }
    
    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        ImageView expandCollapseIcon;
        LinearLayout dayHeaderLayout;
        LinearLayout attractionsContainer;
        RecyclerView recyclerViewAttractions;
        TextView emptyDayText;
        
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            expandCollapseIcon = itemView.findViewById(R.id.expandCollapseIcon);
            dayHeaderLayout = itemView.findViewById(R.id.dayHeaderLayout);
            attractionsContainer = itemView.findViewById(R.id.attractionsContainer);
            recyclerViewAttractions = itemView.findViewById(R.id.recyclerViewAttractions);
            emptyDayText = itemView.findViewById(R.id.emptyDayText);
        }
    }
    
    // 景点拖拽回调
    private class AttractionTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
        private int dayNumber;
        
        public AttractionTouchHelperCallback(int dayNumber) {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            this.dayNumber = dayNumber;
        }
        
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            
            List<ItineraryAttraction> dayAttractions = attractionsByDay.get(dayNumber);
            if (dayAttractions != null && fromPosition < dayAttractions.size() && toPosition < dayAttractions.size()) {
                // 交换位置
                Collections.swap(dayAttractions, fromPosition, toPosition);
                
                // 更新访问顺序
                for (int i = 0; i < dayAttractions.size(); i++) {
                    dayAttractions.get(i).setVisitOrder(i + 1);
                }
                
                // 通知适配器
                RecyclerView.Adapter adapter = recyclerView.getAdapter();
                if (adapter != null) {
                    adapter.notifyItemMoved(fromPosition, toPosition);
                }
                return true;
            }
            return false;
        }
        
        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // 不实现滑动删除
        }
        
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                // 开始拖拽
                if (viewHolder instanceof AttractionAdapter.AttractionViewHolder) {
                    viewHolder.itemView.setAlpha(0.7f);
                }
            } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                // 结束拖拽
                if (viewHolder != null) {
                    viewHolder.itemView.setAlpha(1.0f);
                }
            }
        }
        
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
    }
    
    // 内部适配器，用于显示每天的景点
    private class AttractionAdapter extends RecyclerView.Adapter<AttractionAdapter.AttractionViewHolder> {
        
        private List<ItineraryAttraction> attractions;
        private int dayNumber;
        private List<String> suggestions = new ArrayList<>();
        private List<Tip> tipList = new ArrayList<>();
        
        public AttractionAdapter(List<ItineraryAttraction> attractions, int dayNumber) {
            this.attractions = attractions;
            this.dayNumber = dayNumber;
        }
        
        @NonNull
        @Override
        public AttractionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_enhanced_edit_attraction, parent, false);
            return new AttractionViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull AttractionViewHolder holder, int position) {
            ItineraryAttraction attraction = attractions.get(position);
            
            // 设置景点名称
            holder.attractionNameEditText.setText(attraction.getAttractionName());
            setupAttractionNameAutoComplete(holder.attractionNameEditText, holder.itemView.getContext());
            holder.attractionNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    attraction.setAttractionName(s.toString());
                }
            });
            
            // 设置天数
            holder.dayNumberEditText.setText(String.valueOf(attraction.getDayNumber()));
            holder.dayNumberEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int newDayNumber = Integer.parseInt(s.toString());
                        if (newDayNumber > 0 && newDayNumber != attraction.getDayNumber()) {
                            // 移动景点到新的天数
                            moveAttractionToDay(attraction, newDayNumber);
                        }
                    } catch (NumberFormatException e) {
                        // 输入不是有效的数字，恢复原值
                        holder.dayNumberEditText.setText(String.valueOf(attraction.getDayNumber()));
                    }
                }
            });
            
            // 设置交通方式
            holder.transportEditText.setText(attraction.getTransport());
            holder.transportEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    attraction.setTransport(s.toString());
                }
            });
            
            // 设置拖动手柄
            if (isEditMode) {
                holder.dragHandle.setVisibility(View.VISIBLE);
                holder.dragHandle.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startDrag(holder, dayNumber);
                    }
                    return false;
                });
            } else {
                holder.dragHandle.setVisibility(View.GONE);
            }
            
            // 设置删除按钮
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAttractionDeleted(attraction);
                }
            });
        }
        
        private void setupAttractionNameAutoComplete(AutoCompleteTextView autoCompleteTextView, Context context) {
            autoCompleteTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0 && itineraryLocation != null && !itineraryLocation.isEmpty()) {
                        InputtipsQuery inputquery = new InputtipsQuery(s.toString(), itineraryLocation);
                        inputquery.setCityLimit(true);
                        Inputtips inputTips = new Inputtips(context, inputquery);
                        inputTips.setInputtipsListener((tips, rCode) -> {
                            if (rCode == 1000) {
                                suggestions.clear();
                                tipList.clear();
                                
                                for (Tip tip : tips) {
                                    suggestions.add(tip.getName());
                                    tipList.add(tip);
                                }
                                
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        context,
                                        android.R.layout.simple_dropdown_item_1line,
                                        suggestions
                                );
                                
                                autoCompleteTextView.setAdapter(adapter);
                                
                                if (suggestions.size() > 0) {
                                    autoCompleteTextView.showDropDown();
                                }
                            }
                        });
                        inputTips.requestInputtipsAsyn();
                    }
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        
        @Override
        public int getItemCount() {
            return attractions == null ? 0 : attractions.size();
        }
        
        class AttractionViewHolder extends RecyclerView.ViewHolder {
            AutoCompleteTextView attractionNameEditText;
            EditText dayNumberEditText;
            EditText transportEditText;
            ImageView dragHandle;
            ImageView deleteButton;
            
            public AttractionViewHolder(@NonNull View itemView) {
                super(itemView);
                attractionNameEditText = itemView.findViewById(R.id.attractionNameEditText);
                dayNumberEditText = itemView.findViewById(R.id.dayNumberEditText);
                transportEditText = itemView.findViewById(R.id.transportEditText);
                dragHandle = itemView.findViewById(R.id.dragHandle);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
} 