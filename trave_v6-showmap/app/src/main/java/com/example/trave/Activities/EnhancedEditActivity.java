package com.example.trave.Activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.Photo;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.example.trave.Adapters.EnhancedEditAdapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class EnhancedEditActivity extends AppCompatActivity implements EnhancedEditAdapter.OnStartDragListener, Inputtips.InputtipsListener, PoiSearch.OnPoiSearchListener {

    private static final String TAG = "EnhancedEditActivity";
    private TextView itineraryNameText;
    private TextView itineraryLocationText;
    private Button saveEditBtn;
    private Button addAttractionBtn;
    private RecyclerView attractionsRecyclerView;

    private ArrayList<ItineraryAttraction> itineraryAttractionList = new ArrayList<>();
    private Long itineraryId;
    private EnhancedEditAdapter enhancedEditAdapter;
    private ItemTouchHelper itemTouchHelper;
    private DatabaseHelper dbHelper;
    
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
    private Dialog attractionDialog;
    private String itineraryLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enhanced_edit);
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 初始化视图
        initViews();
        
        // 获取传入的数据
        Intent intent = getIntent();
        itineraryId = intent.getLongExtra("itineraryId", 0);
        Log.d(TAG, "itineraryId " + itineraryId);
        String itineraryTitle = intent.getStringExtra("itineraryTittle");
        itineraryLocation = intent.getStringExtra("itineraryLocation");
        itineraryNameText.setText(itineraryTitle);
        itineraryLocationText.setText(itineraryLocation);
        itineraryAttractionList = intent.getParcelableArrayListExtra("itineraryAttractions");

        // 设置RecyclerView和适配器
        setupRecyclerView();
        
        // 设置按钮点击事件
        setupButtonListeners();
    }

    private void initViews() {
        itineraryNameText = findViewById(R.id.itineraryNameText);
        itineraryLocationText = findViewById(R.id.itineraryLocationText);
        saveEditBtn = findViewById(R.id.saveEditBtn);
        addAttractionBtn = findViewById(R.id.addAttractionBtn);
        attractionsRecyclerView = findViewById(R.id.attractionsRecyclerView);
    }

    private void setupRecyclerView() {
        enhancedEditAdapter = new EnhancedEditAdapter(itineraryAttractionList);
        enhancedEditAdapter.setOnStartDragListener(this);
        attractionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        attractionsRecyclerView.setAdapter(enhancedEditAdapter);
        
        // 设置ItemTouchHelper实现拖拽功能
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
                int fromPosition = source.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                enhancedEditAdapter.moveItem(fromPosition, toPosition);
                return true;
            }
            
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // 不实现滑动删除功能
            }
        };
        
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(attractionsRecyclerView);
    }

    private void setupButtonListeners() {
        // 保存按钮点击事件
        saveEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAttractions();
                saveItinerary();
            }
        });
        
        // 添加景点按钮点击事件
        addAttractionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddAttractionDialog();
            }
        });
    }

    private void showAddAttractionDialog() {
        // 创建对话框
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
        
        // 设置景点搜索的监听器
        searchAttractionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    String city = itineraryLocation;
                    InputtipsQuery inputquery = new InputtipsQuery(s.toString(), city);
                    inputquery.setCityLimit(true);
                    Inputtips inputTips = new Inputtips(EnhancedEditActivity.this, inputquery);
                    inputTips.setInputtipsListener(EnhancedEditActivity.this);
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
            if (selectedPoi != null && !selectedPoi.isEmpty() && attractionName != null && !attractionName.isEmpty()) {
                addNewAttraction();
                attractionDialog.dismiss();
            } else {
                Toast.makeText(EnhancedEditActivity.this, "请选择一个景点", Toast.LENGTH_SHORT).show();
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
     * 添加新景点
     */
    private void addNewAttraction() {
        // 获取最大天数
        int maxDay = 1;
        for (ItineraryAttraction attraction : itineraryAttractionList) {
            if (attraction.getDayNumber() > maxDay) {
                maxDay = attraction.getDayNumber();
            }
        }
        
        // 创建新的景点对象
        ItineraryAttraction newAttraction = new ItineraryAttraction(
                itineraryId,
                0, // 临时siteId，后面会更新
                maxDay,
                itineraryAttractionList.size() + 1,
                attractionName,
                "步行"
        );
        
        // 保存或获取景点基本信息
        long siteId = dbHelper.addOrGetSite(
                selectedPoi,
                attractionName,
                latitude,
                longitude,
                address,
                businessArea,
                tel,
                website,
                typeDesc,
                photos
        );
        
        // 更新siteId
        newAttraction.setSiteId(siteId);
        
        // 添加到列表
        enhancedEditAdapter.addAttraction(newAttraction);
        
        // 提示用户
        Toast.makeText(this, "景点已添加: " + attractionName, Toast.LENGTH_SHORT).show();
        
        // 重置选择
        selectedPoi = null;
        attractionName = null;
    }

    private void updateAttractions() {
        // 遍历RecyclerView中的所有项，更新数据
        for (int i = 0; i < attractionsRecyclerView.getChildCount(); i++) {
            View itemView = attractionsRecyclerView.getChildAt(i);
            EnhancedEditAdapter.EnhancedEditViewHolder holder = 
                    (EnhancedEditAdapter.EnhancedEditViewHolder) attractionsRecyclerView.getChildViewHolder(itemView);
            
            if (i < itineraryAttractionList.size()) {
                ItineraryAttraction attraction = itineraryAttractionList.get(i);
                
                attraction.setAttractionName(holder.attractionNameEditText.getText().toString());
                attraction.setTransport(holder.transportEditText.getText().toString());
                
                try {
                    attraction.setDayNumber(Integer.parseInt(holder.dayNumberEditText.getText().toString()));
                } catch (NumberFormatException e) {
                    attraction.setDayNumber(1);
                }
                
                // 设置访问顺序为位置+1
                attraction.setVisitOrder(i + 1);
            }
        }
    }

    private void saveItinerary() {
        String itineraryName = itineraryNameText.getText().toString();

        if (itineraryName.isEmpty() || itineraryAttractionList.isEmpty()) {
            Toast.makeText(this, "请输入行程名称并至少添加一个景点", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保每个景点都有合法的 siteId
        for (ItineraryAttraction attraction : itineraryAttractionList) {
            if (attraction.getSiteId() <= 0) {
                // 如果没有 siteId，设置一个默认值
                attraction.setSiteId(1); // 使用默认值1，实际应用中可能需要不同的处理方式
            }
        }
        
        Intent resultIntent = new Intent();
        resultIntent.putExtra("itineraryId", itineraryId);
        Log.d(TAG, "ResultitineraryId " + itineraryId);

        resultIntent.putExtra("itineraryTittle", itineraryName);
        resultIntent.putParcelableArrayListExtra("itineraryAttractions", itineraryAttractionList);

        Log.d(TAG, "Returning Itinerary ID: " + itineraryId);
        Log.d(TAG, "Returning Attractions: " + itineraryAttractionList.size());

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
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
} 