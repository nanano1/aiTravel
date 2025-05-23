package com.example.trave.Activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.Photo;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewItineraryCreateActivity extends AppCompatActivity implements Inputtips.InputtipsListener, PoiSearch.OnPoiSearchListener {
    private static final String TAG = "NewItineraryCreate";
    
    // UI元素
    private EditText itineraryNameEditText;
    private EditText itineraryLocationEditText;
    private Button addDayButton;
    private LinearLayout daysContainer;
    private Button saveItineraryButton;
    
    // 数据
    private DatabaseHelper dbHelper;
    private ArrayList<DayData> daysList = new ArrayList<>();
    private int currentDayCount = 0;
    
    // POI搜索相关
    private AutoCompleteTextView searchAttractionEditText;
    private ArrayList<String> suggestions = new ArrayList<>();
    private ArrayList<Tip> tipList = new ArrayList<>();
    private String selectedPoi;
    private PoiSearch poiSearch;
    private String attractionName;
    private double latitude;
    private double longitude;
    private String address;
    private String businessArea;
    private String tel;
    private String website;
    private String typeDesc;
    private String photos;
    
    // 当前正在编辑的日期和景点
    private int currentEditingDay = -1;
    private Dialog attractionDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_itinerary_create);
        
        dbHelper = new DatabaseHelper(this);
        
        // 初始化UI元素
        itineraryNameEditText = findViewById(R.id.itineraryNameEditText);
        itineraryLocationEditText = findViewById(R.id.itineraryLocationEditText);
        addDayButton = findViewById(R.id.addDayButton);
        daysContainer = findViewById(R.id.daysContainer);
        saveItineraryButton = findViewById(R.id.saveItineraryButton);
        
        // 设置监听器
        addDayButton.setOnClickListener(v -> addDay());
        saveItineraryButton.setOnClickListener(v -> saveItinerary());
    }
    
    /**
     * 添加新的天数
     */
    private void addDay() {
        currentDayCount++;
        
        // 创建新的日期数据对象
        DayData dayData = new DayData(currentDayCount, new ArrayList<>());
        daysList.add(dayData);
        
        // 从布局文件中加载日期视图
        View dayView = getLayoutInflater().inflate(R.layout.item_itinerary_day_new, null);
        
        // 设置日期标题
        TextView dayTitle = dayView.findViewById(R.id.dayTitle);
        dayTitle.setText("Day " + currentDayCount);
        
        // 设置添加景点按钮的点击事件
        Button addAttractionButton = dayView.findViewById(R.id.addAttractionButton);
        int dayNumber = currentDayCount; // 捕获当前的日期编号
        addAttractionButton.setOnClickListener(v -> showAddAttractionDialog(dayNumber - 1)); // 索引从0开始
        
        // 为该天设置标签
        dayView.setTag(currentDayCount - 1); // 标记视图，便于后续查找
        
        // 将日期视图添加到容器中
        daysContainer.addView(dayView);
    }
    
    /**
     * 显示添加景点的对话框
     */
    private void showAddAttractionDialog(int dayIndex) {
        currentEditingDay = dayIndex;
        
        attractionDialog = new Dialog(this);
        attractionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        attractionDialog.setContentView(R.layout.dialog_add_attraction_search);
        
        // 初始化对话框中的UI元素
        searchAttractionEditText = attractionDialog.findViewById(R.id.searchAttractionEditText);
        Button cancelButton = attractionDialog.findViewById(R.id.cancelButton);
        Button confirmButton = attractionDialog.findViewById(R.id.confirmButton);
        
        // 确保searchAttractionEditText不为null
        if (searchAttractionEditText == null) {
            Log.e(TAG, "searchAttractionEditText is null! Dialog layout may be incorrect.");
            Toast.makeText(this, "初始化对话框失败", Toast.LENGTH_SHORT).show();
            attractionDialog.dismiss();
            return;
        }
        
        // 保存到类成员变量
        this.searchAttractionEditText = searchAttractionEditText;
        
        // 设置景点搜索的监听器
        searchAttractionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    String city = itineraryLocationEditText.getText().toString();
                    InputtipsQuery inputquery = new InputtipsQuery(s.toString(), city);
                    inputquery.setCityLimit(true);
                    Inputtips inputTips = new Inputtips(NewItineraryCreateActivity.this, inputquery);
                    inputTips.setInputtipsListener(NewItineraryCreateActivity.this);
                    inputTips.requestInputtipsAsyn();
                } else {
                    suggestions.clear();
                    if (searchAttractionEditText.getAdapter() != null) {
                        ((ArrayAdapter<?>) searchAttractionEditText.getAdapter()).notifyDataSetChanged();
                    }
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // 设置景点选择监听器
        searchAttractionEditText.setOnItemClickListener((parent, view, position, id) -> {
            selectedPoi = tipList.get(position).getPoiID();
            Log.d(TAG, "Selected POI ID: " + selectedPoi);
            performPoiIdSearch(selectedPoi);
        });
        
        // 设置按钮点击事件
        cancelButton.setOnClickListener(v -> attractionDialog.dismiss());
        
        confirmButton.setOnClickListener(v -> {
            if (selectedPoi != null && !selectedPoi.isEmpty()) {
                addAttractionToDay(currentEditingDay);
                attractionDialog.dismiss();
            } else {
                Toast.makeText(NewItineraryCreateActivity.this, "请选择一个景点", Toast.LENGTH_SHORT).show();
            }
        });
        
        attractionDialog.show();
    }
    
    /**
     * 进行POI ID搜索
     */
    private void performPoiIdSearch(String poiId) {
        Context context = getApplicationContext();
        ServiceSettings.updatePrivacyShow(context, true, true);
        ServiceSettings.updatePrivacyAgree(context, true);
        
        try {
            poiSearch = new PoiSearch(context, null);
            poiSearch.setOnPoiSearchListener(this);
            poiSearch.searchPOIIdAsyn(poiId);
        } catch (AMapException e) {
            Log.e(TAG, "POI搜索错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 将景点添加到指定的日期
     */
    private void addAttractionToDay(int dayIndex) {
        DayData dayData = daysList.get(dayIndex);
        
        // 获取该日景点列表的下一个序号
        int visitOrder = dayData.attractions.size() + 1;
        
        // 创建新的景点对象
        AttractionData attractionData = new AttractionData(
                selectedPoi,
                attractionName,
                "上午", // 默认时段
                "", // 默认无交通方式
                "", // 默认无备注
                latitude,
                longitude,
                address,
                businessArea,
                typeDesc,
                tel,
                website,
                photos
        );
        
        // 将景点添加到日期数据中
        dayData.attractions.add(attractionData);
        
        // 在UI中添加景点卡片
        addAttractionCardToDay(dayIndex, attractionData, visitOrder);
        
        // 重置选择
        selectedPoi = null;
        attractionName = null;
    }
    
    /**
     * 在UI中为指定日期添加景点卡片
     */
    private void addAttractionCardToDay(int dayIndex, AttractionData attractionData, int visitOrder) {
        // 查找对应的日期容器
        View dayView = daysContainer.findViewWithTag(dayIndex);
        if (dayView == null) {
            Log.e(TAG, "找不到对应的日期视图: " + dayIndex);
            return;
        }
        
        LinearLayout attractionsContainer = dayView.findViewById(R.id.attractionsContainer);
        
        // 加载景点卡片视图
        View attractionCard = getLayoutInflater().inflate(R.layout.item_attraction_card, null);
        
        // 设置景点卡片数据
        TextView attractionNameText = attractionCard.findViewById(R.id.attractionName);
        TextView attractionTimeText = attractionCard.findViewById(R.id.attractionTime);
        ImageView expandButton = attractionCard.findViewById(R.id.expandButton);
        LinearLayout expandedView = attractionCard.findViewById(R.id.expandedView);
        
        // 设置基本信息
        attractionNameText.setText(attractionData.name);
        attractionTimeText.setText(attractionData.timeSlot);
        
        // 设置详细信息区域
        RadioGroup timeRadioGroup = attractionCard.findViewById(R.id.timeRadioGroup);
        EditText transportEditText = attractionCard.findViewById(R.id.transportEditText);
        EditText notesEditText = attractionCard.findViewById(R.id.notesEditText);
        
        // 设置交通方式和备注
        transportEditText.setText(attractionData.transport);
        notesEditText.setText(attractionData.notes);
        
        // 根据时段选择正确的单选按钮
        if (attractionData.timeSlot.equals("上午")) {
            ((RadioButton) timeRadioGroup.findViewById(R.id.morningRadio)).setChecked(true);
        } else if (attractionData.timeSlot.equals("下午")) {
            ((RadioButton) timeRadioGroup.findViewById(R.id.afternoonRadio)).setChecked(true);
        } else if (attractionData.timeSlot.equals("晚上")) {
            ((RadioButton) timeRadioGroup.findViewById(R.id.eveningRadio)).setChecked(true);
        }
        
        // 设置时段选择监听器
        timeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.morningRadio) {
                attractionData.timeSlot = "上午";
            } else if (checkedId == R.id.afternoonRadio) {
                attractionData.timeSlot = "下午";
            } else if (checkedId == R.id.eveningRadio) {
                attractionData.timeSlot = "晚上";
            }
            attractionTimeText.setText(attractionData.timeSlot);
        });
        
        // 设置交通方式变化监听器
        transportEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                attractionData.transport = s.toString();
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // 设置备注变化监听器
        notesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                attractionData.notes = s.toString();
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // 设置展开/收起按钮点击事件
        expandButton.setOnClickListener(v -> {
            if (expandedView.getVisibility() == View.VISIBLE) {
                expandedView.setVisibility(View.GONE);
                expandButton.setImageResource(android.R.drawable.arrow_down_float);
            } else {
                expandedView.setVisibility(View.VISIBLE);
                expandButton.setImageResource(android.R.drawable.arrow_up_float);
            }
        });
        
        // 将景点卡片添加到容器中
        attractionsContainer.addView(attractionCard);
    }
    
    /**
     * 保存整个行程
     */
    private void saveItinerary() {
        String itineraryName = itineraryNameEditText.getText().toString().trim();
        String itineraryLocation = itineraryLocationEditText.getText().toString().trim();
        
        // 验证输入
        if (itineraryName.isEmpty()) {
            Toast.makeText(this, "请输入行程标题", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (itineraryLocation.isEmpty()) {
            Toast.makeText(this, "请输入目的地", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (daysList.isEmpty()) {
            Toast.makeText(this, "请至少添加一天行程", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否有景点
        boolean hasAttractions = false;
        for (DayData day : daysList) {
            if (!day.attractions.isEmpty()) {
                hasAttractions = true;
                break;
            }
        }
        
        if (!hasAttractions) {
            Toast.makeText(this, "请至少添加一个景点", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取当前用户ID
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        long userId = prefs.getLong("user_id", -1);
        
        if (userId == -1) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建行程对象
        Itinerary newItinerary = new Itinerary(itineraryName, itineraryLocation, "pic3", userId, 0);
        
        // 保存行程到数据库
        long itineraryId = dbHelper.addItinerary(newItinerary);
        
        // 转换并保存每个景点
        ArrayList<ItineraryAttraction> itineraryAttractions = new ArrayList<>();
        
        for (int dayIndex = 0; dayIndex < daysList.size(); dayIndex++) {
            DayData dayData = daysList.get(dayIndex);
            int dayNumber = dayIndex + 1;
            
            for (int orderIndex = 0; orderIndex < dayData.attractions.size(); orderIndex++) {
                AttractionData attraction = dayData.attractions.get(orderIndex);
                int visitOrder = orderIndex + 1;
                
                // 保存或获取景点基本信息
                long siteId = dbHelper.addOrGetSite(
                        attraction.poiId,
                        attraction.name,
                        attraction.latitude,
                        attraction.longitude,
                        attraction.address,
                        attraction.businessArea,
                        attraction.tel,
                        attraction.website,
                        attraction.typeDesc,
                        attraction.photos
                );
                
                // 创建行程景点对象
                ItineraryAttraction itineraryAttraction = new ItineraryAttraction(
                        siteId,
                        dayNumber,
                        visitOrder,
                        attraction.name,
                        attraction.transport
                );
                
                itineraryAttraction.setItineraryId(itineraryId);
                
                // 保存到数据库
                dbHelper.addAttraction(itineraryAttraction);
                
                // 添加到列表用于返回
                itineraryAttractions.add(itineraryAttraction);
            }
        }
        
        // 更新行程的总天数
        newItinerary.setId(itineraryId);
        newItinerary.setDays(daysList.size());

        dbHelper.updateItinerary(newItinerary);
        
        // 创建返回结果
        Intent resultIntent = new Intent();
        resultIntent.putExtra("itineraryId", itineraryId);
        resultIntent.putExtra("itineraryLocation", itineraryLocation);
        resultIntent.putExtra("itineraryName", itineraryName);
        resultIntent.putExtra("days", daysList.size());
        resultIntent.putParcelableArrayListExtra("itineraryAttractions", itineraryAttractions);
        setResult(RESULT_OK, resultIntent);
        
        Toast.makeText(this, "行程创建成功", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public void onGetInputtips(List<Tip> tips, int rCode) {
        if (rCode == 1000) {
            suggestions.clear();
            tipList.clear();
            
            for (Tip tip : tips) {
                suggestions.add(tip.getName());
                tipList.add(tip);
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    suggestions
            );
            
            searchAttractionEditText.setAdapter(adapter);
            
            if (suggestions.size() > 0) {
                searchAttractionEditText.showDropDown();
            }
        }
    }
    
    @Override
    public void onPoiSearched(PoiResult poiResult, int rCode) {
        // 不处理普通POI搜索结果
    }
    
    @Override
    public void onPoiItemSearched(PoiItem item, int rCode) {
        if (rCode == 1000 && item != null) {
            // 保存搜索到的POI信息
            attractionName = item.getTitle();
            latitude = item.getLatLonPoint().getLatitude();
            longitude = item.getLatLonPoint().getLongitude();
            address = item.getSnippet();
            businessArea = item.getBusinessArea();
            tel = item.getTel();
            website = item.getWebsite();
            typeDesc = item.getTypeDes();
            
            List<Photo> photosList = item.getPhotos();
            Gson gson = new Gson();
            photos = gson.toJson(photosList);
            
            Log.d(TAG, "POI信息获取成功: " + attractionName);
        } else {
            Log.e(TAG, "POI信息获取失败，代码: " + rCode);
        }
    }
    
    /**
     * 日期数据类
     */
    private static class DayData {
        int dayNumber;
        ArrayList<AttractionData> attractions;
        
        DayData(int dayNumber, ArrayList<AttractionData> attractions) {
            this.dayNumber = dayNumber;
            this.attractions = attractions;
        }
    }
    
    /**
     * 景点数据类
     */
    private static class AttractionData {
        String poiId;
        String name;
        String timeSlot;
        String transport;
        String notes;
        double latitude;
        double longitude;
        String address;
        String businessArea;
        String typeDesc;
        String tel;
        String website;
        String photos;
        
        AttractionData(String poiId, String name, String timeSlot, String transport, String notes,
                      double latitude, double longitude, String address, String businessArea,
                      String typeDesc, String tel, String website, String photos) {
            this.poiId = poiId;
            this.name = name;
            this.timeSlot = timeSlot;
            this.transport = transport;
            this.notes = notes;
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.businessArea = businessArea;
            this.typeDesc = typeDesc;
            this.tel = tel;
            this.website = website;
            this.photos = photos;
        }
    }
} 