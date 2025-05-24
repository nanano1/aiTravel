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
import com.example.trave.Adapters.CollapsibleDayAdapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnhancedEditActivity extends AppCompatActivity implements Inputtips.InputtipsListener, PoiSearch.OnPoiSearchListener, CollapsibleDayAdapter.OnDayItemClickListener {

    private static final String TAG = "EnhancedEditActivity";
    private TextView itineraryNameText;
    private TextView itineraryLocationText;
    private Button saveEditBtn;
    private Button addAttractionBtn;
    private Button addDayBtn;
    private RecyclerView attractionsRecyclerView;

    private ArrayList<ItineraryAttraction> itineraryAttractionList = new ArrayList<>();
    private Long itineraryId;
    private CollapsibleDayAdapter collapsibleDayAdapter;
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
    private int selectedDayNumber = 1;

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
        addDayBtn = findViewById(R.id.addDayBtn);
        attractionsRecyclerView = findViewById(R.id.attractionsRecyclerView);
    }

    private void setupRecyclerView() {
        collapsibleDayAdapter = new CollapsibleDayAdapter(itineraryAttractionList, itineraryLocation);
        collapsibleDayAdapter.setOnDayItemClickListener(this);
        collapsibleDayAdapter.setDatabaseHelper(dbHelper);
        collapsibleDayAdapter.setItineraryId(itineraryId);
        attractionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        attractionsRecyclerView.setAdapter(collapsibleDayAdapter);
    }

    private void setupButtonListeners() {
        // 保存按钮点击事件
        saveEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        
        // 添加新的一天按钮点击事件
        addDayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapsibleDayAdapter.addNewDay();
                Toast.makeText(EnhancedEditActivity.this, "已添加新的一天", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddAttractionDialog() {
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_attraction, null);
        builder.setView(dialogView);
        
        // 获取对话框中的视图
        final AutoCompleteTextView nameEditText = dialogView.findViewById(R.id.dialogAttractionNameEditText);
        final EditText dayNumberEditText = dialogView.findViewById(R.id.dialogDayNumberEditText);
        final EditText transportEditText = dialogView.findViewById(R.id.dialogTransportEditText);
        Button cancelButton = dialogView.findViewById(R.id.dialogCancelButton);
        Button addButton = dialogView.findViewById(R.id.dialogAddButton);
        
        // 隐藏游览顺序输入框，因为我们使用拖拽排序
        EditText visitOrderEditText = dialogView.findViewById(R.id.dialogVisitOrderEditText);
        if (visitOrderEditText != null) {
            ViewGroup parent = (ViewGroup) visitOrderEditText.getParent();
            if (parent != null) {
                parent.setVisibility(View.GONE);
            }
        }
        
        // 设置景点搜索的监听器
        this.searchAttractionEditText = nameEditText;
        nameEditText.addTextChangedListener(new TextWatcher() {
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
        nameEditText.setOnItemClickListener((parent, view, position, id) -> {
            selectedPoi = tipList.get(position).getPoiID();
            Log.d(TAG, "Selected POI ID: " + selectedPoi);
            performPoiIdSearch(selectedPoi);
        });
        
        // 默认填入当前最大天数
        int maxDay = 1;
        for (ItineraryAttraction attraction : itineraryAttractionList) {
            if (attraction.getDayNumber() > maxDay) {
                maxDay = attraction.getDayNumber();
            }
        }
        dayNumberEditText.setText(String.valueOf(maxDay));
        selectedDayNumber = maxDay;
        
        // 创建对话框
        final AlertDialog dialog = builder.create();
        
        // 设置按钮点击事件
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取输入的数据
                String name = nameEditText.getText().toString().trim();
                String dayNumberStr = dayNumberEditText.getText().toString().trim();
                String transport = transportEditText.getText().toString().trim();
                
                // 验证输入
                if (name.isEmpty() || dayNumberStr.isEmpty() || transport.isEmpty()) {
                    Toast.makeText(EnhancedEditActivity.this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    int dayNumber = Integer.parseInt(dayNumberStr);
                    selectedDayNumber = dayNumber;
                    
                    // 创建新的景点对象
                    ItineraryAttraction newAttraction;
                    
                    if (selectedPoi != null && !selectedPoi.isEmpty()) {
                        // 如果使用了POI搜索
                        long siteId = dbHelper.addOrGetSite(
                                selectedPoi,
                                attractionName != null ? attractionName : name,
                                latitude,
                                longitude,
                                address,
                                businessArea,
                                tel,
                                website,
                                typeDesc,
                                photos
                        );
                        
                        newAttraction = new ItineraryAttraction(
                                itineraryId,
                                siteId,
                                dayNumber,
                                1, // 默认顺序，会在适配器中更新
                                attractionName != null ? attractionName : name,
                                transport
                        );
                    } else {
                        // 如果直接输入了景点名称
                        newAttraction = new ItineraryAttraction(
                                dayNumber,
                                1, // 默认顺序，会在适配器中更新
                                name,
                                transport
                        );
                        
                        // 设置行程ID
                        newAttraction.setItineraryId(itineraryId);
                        // 设置景点ID，使用默认值1
                        newAttraction.setSiteId(1);
                    }
                    
                    // 添加到适配器
                    collapsibleDayAdapter.addAttraction(newAttraction);
                    
                    // 保存景点到数据库
                    long attractionId = dbHelper.addAttraction(newAttraction);
                    if (attractionId > 0) {
                        newAttraction.setId(attractionId);
                        Log.d(TAG, "新增景点保存成功: " + newAttraction.getAttractionName() + ", ID: " + attractionId);
                        
                        // 更新数据库中的行程天数
                        boolean updated = dbHelper.updateItineraryDaysFromAttractions(itineraryId);
                        Log.d(TAG, "添加景点后更新天数: " + (updated ? "成功" : "失败"));
                    } else {
                        Log.e(TAG, "新增景点保存失败: " + newAttraction.getAttractionName());
                    }
                    
                    // 关闭对话框
                    dialog.dismiss();
                    
                    // 提示用户
                    Toast.makeText(EnhancedEditActivity.this, "景点已添加", Toast.LENGTH_SHORT).show();
                    
                } catch (NumberFormatException e) {
                    Toast.makeText(EnhancedEditActivity.this, "天数必须是数字", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 显示对话框
        dialog.show();
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

    private void saveItinerary() {
        String itineraryName = itineraryNameText.getText().toString();

        if (itineraryName.isEmpty()) {
            Toast.makeText(this, "请输入行程名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取所有景点
        List<ItineraryAttraction> allAttractions = collapsibleDayAdapter.getAllAttractions();
        
        if (allAttractions.isEmpty()) {
            Toast.makeText(this, "请至少添加一个景点", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 首先删除原有的景点数据，避免数据冲突
        dbHelper.deleteAttractionsForItinerary(itineraryId);
        Log.d(TAG, "已删除行程原有景点数据");
        
        // 确保每个景点都有合法的 siteId 并保存到数据库
        for (ItineraryAttraction attraction : allAttractions) {
            if (attraction.getSiteId() <= 0) {
                attraction.setSiteId(1); // 使用默认值1
            }
            
            // 确保景点有正确的行程ID
            attraction.setItineraryId(itineraryId);
            
            // 保存景点到数据库
            long attractionId = dbHelper.addAttraction(attraction);
            if (attractionId > 0) {
                attraction.setId(attractionId);
                Log.d(TAG, "保存景点: " + attraction.getAttractionName() + ", ID: " + attractionId);
            } else {
                Log.e(TAG, "保存景点失败: " + attraction.getAttractionName());
            }
        }
        
        // 现在数据库中的景点数据已经是最新的，可以正确计算天数
        boolean daysUpdated = dbHelper.updateItineraryDaysFromAttractions(itineraryId);
        if (daysUpdated) {
            Log.d(TAG, "行程天数已自动更新");
        } else {
            Log.w(TAG, "行程天数更新失败");
        }
        
        Intent resultIntent = new Intent();
        resultIntent.putExtra("itineraryId", itineraryId);
        Log.d(TAG, "ResultitineraryId " + itineraryId);

        resultIntent.putExtra("itineraryTittle", itineraryName);
        resultIntent.putParcelableArrayListExtra("itineraryAttractions", new ArrayList<>(allAttractions));

        Log.d(TAG, "Returning Itinerary ID: " + itineraryId);
        Log.d(TAG, "Returning Attractions: " + allAttractions.size());

        setResult(RESULT_OK, resultIntent);
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
            
            // 自动填充搜索框
            if (searchAttractionEditText != null) {
                searchAttractionEditText.setText(attractionName);
                searchAttractionEditText.dismissDropDown();
            }
        } else {
            Log.e(TAG, "POI信息获取失败，代码: " + rCode);
        }
    }

    @Override
    public void onExpandCollapse(int position, boolean isExpanded) {
        // 处理折叠/展开事件
        Log.d(TAG, "Day " + position + " " + (isExpanded ? "expanded" : "collapsed"));
    }

    @Override
    public void onAttractionDeleted(ItineraryAttraction attraction) {
        // 处理景点删除事件
        collapsibleDayAdapter.removeAttraction(attraction);
        Toast.makeText(this, "已删除景点: " + attraction.getAttractionName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDayEmpty(int dayNumber) {
        // 处理天数为空的事件
        Log.d(TAG, "Day " + dayNumber + " is now empty");
    }
    
    @Override
    public void onAttractionDayChanged(ItineraryAttraction attraction, int oldDayNumber, int newDayNumber) {
        // 处理景点天数变更事件
        Toast.makeText(this, "景点\"" + attraction.getAttractionName() + "\"已移动到第" + newDayNumber + "天", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 确保每次返回到编辑界面时都会更新天数
        if (itineraryId > 0 && dbHelper != null) {
            boolean updated = dbHelper.updateItineraryDaysFromAttractions(itineraryId);
            Log.d(TAG, "onResume中更新行程天数: " + (updated ? "成功" : "失败"));
        }
    }
} 