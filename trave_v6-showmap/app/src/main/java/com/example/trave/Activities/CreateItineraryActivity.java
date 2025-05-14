package com.example.trave.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.Photo;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import androidx.appcompat.app.AppCompatActivity;


import com.amap.api.services.core.ServiceSettings;

import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.Sites;
import com.example.trave.R;
import com.google.gson.Gson;


import java.util.ArrayList;
import java.util.List;

public class CreateItineraryActivity extends AppCompatActivity implements Inputtips.InputtipsListener, PoiSearch.OnPoiSearchListener {
    private DatabaseHelper dbHelper;
    private EditText itineraryNameEditText;
    private EditText itineraryLocationEditText;
    private EditText dayNumberEditText;
    private EditText visitOrderEditText;
    private EditText transportEditText;
    private Button addButton;
    private Button saveButton;
    private ListView poiSuggestionsList;
    private ArrayAdapter<String> suggestionsAdapter;

    private ArrayList<ItineraryAttraction> itineraryAttractionList = new ArrayList<>();
    private long newItinerary_id; // This should be dynamically managed, e.g., from a database
    private String city ; // 将目的地城市设置为你想要限制的城市
    private AutoCompleteTextView attractionNameEditText;
    private AutoCompleteTextView test;
    private ArrayList<String> suggestions = new ArrayList<String>();
    private ArrayList<Tip> tipListed = new ArrayList<>();
    private Inputtips inputTips;
    private String selectedPoi;
    private double latitude;
    private String rating;
    private double longitude;
    private String address;
    private String businessArea;
    private String tel;
    private String website;
    private String typeDesc;
    private String attractionName;
    private String photos;
    private PoiSearch poiSearch;
    private Photo photo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_itinerary);

        dbHelper = new DatabaseHelper(this);
        itineraryNameEditText = findViewById(R.id.itineraryNameEditText);
        itineraryLocationEditText=findViewById(R.id.itineraryLocationEditText);
        dayNumberEditText = findViewById(R.id.dayNumberEditText);

        visitOrderEditText = findViewById(R.id.visitOrderEditText);
        attractionNameEditText = findViewById(R.id.attractionNameEditText);
        transportEditText = findViewById(R.id.transportEditText);
        addButton = findViewById(R.id.addButton);
        saveButton = findViewById(R.id.saveButton);

        // 添加 TextChangedListener 以监听用户输入
        attractionNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    city=itineraryLocationEditText.getText().toString();
                    InputtipsQuery inputquery = new InputtipsQuery(s.toString(), city);
                    inputquery.setCityLimit(true);

                    inputTips = new Inputtips(CreateItineraryActivity.this, inputquery);
                    inputTips.setInputtipsListener(CreateItineraryActivity.this);
                    inputTips.requestInputtipsAsyn();
                } else {
                    suggestions.clear();
                    suggestionsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        attractionNameEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPoi = tipListed.get(position).getPoiID();
                Log.d("CreateItineraryActivity", "selectedPoiID: " + selectedPoi.toString());
                performPoiIdSearch(selectedPoi);
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItineraryAttraction();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveItinerary();
            }
        });

    }

    private void showToast(String message) {
        // 这里可以使用 Toast 显示提示信息，为了简洁示例中省略了导入 Toast 的代码
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void performPoiIdSearch(String poiId) {
        Context context = getApplicationContext();
        ServiceSettings.updatePrivacyShow(context,true,true);
        ServiceSettings.updatePrivacyAgree(context,true);
        try {
            poiSearch = new PoiSearch(context,null);
            poiSearch.setOnPoiSearchListener(this);
            poiSearch.searchPOIIdAsyn(poiId);
        } catch (AMapException e) {
            Log.e("CreateItineraryActivity", "Error in creating PoiSearch object: " + e.getMessage());
            e.printStackTrace();
        }
        Log.d("CreateItineraryActivity", "Latitude: " + latitude);
        Log.d("CreateItineraryActivity", "Longitude: " + longitude);
        Log.d("CreateItineraryActivity", "Address: " + address);
        Log.d("CreateItineraryActivity", "Photos: " + photos);

    }
    @Override
    public void onPoiSearched(PoiResult poiResult, int rCode) {
        // 这里不处理 PoiResult 的搜索回调
    }

    @Override
        public void onPoiItemSearched(PoiItem item, int rCode) {
        if (rCode == 1000 && item!= null) {
            item.getTitle();
            rating=item.getPoiExtension().getmRating();
            latitude=item.getLatLonPoint().getLatitude();
            longitude=item.getLatLonPoint().getLongitude();
            businessArea=item.getBusinessArea();
            address=item.getSnippet();
            tel=item.getTel();
            typeDesc=item.getTypeDes();
            attractionName=item.getTitle();
            website=item.getWebsite();
            List<Photo> photosList = item.getPhotos();;
            Gson gson = new Gson();
            photos= gson.toJson(photosList);

        }
        else {
            Log.d("CreateItineraryActivity", "rcode: " + rCode);

        }
    }
    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        if (rCode == 1000) {
            suggestions.clear();
            tipListed.clear();
            for (Tip tip : tipList) {
                suggestions.add(tip.getName());
                tipListed.add(tip);
            }
            Log.d("CreateItineraryActivity", "suggestions 值: " + suggestions.toString());
            Log.d("CreateItineraryActivity", "tipListed 值: " + tipListed.toString());

            suggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
            attractionNameEditText.setAdapter(suggestionsAdapter);
            attractionNameEditText.showDropDown();
            Log.d("CreateItineraryActivity", "Adapter count: " + suggestionsAdapter.getCount());

        }
    }
    private void addItineraryAttraction() {
        try {
            // 验证输入
            if (dayNumberEditText.getText().toString().isEmpty() ||
                visitOrderEditText.getText().toString().isEmpty() ||
                attractionNameEditText.getText().toString().isEmpty() ||
                transportEditText.getText().toString().isEmpty()) {
                Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                return;
            }

            int dayNumber = Integer.parseInt(dayNumberEditText.getText().toString());
            int visitOrder = Integer.parseInt(visitOrderEditText.getText().toString());
            attractionName = attractionNameEditText.getText().toString();
            String transport = transportEditText.getText().toString();

            // 添加日志
            Log.d("CreateItineraryActivity", "添加景点 - POI ID: " + selectedPoi);
            Log.d("CreateItineraryActivity", "景点名称: " + attractionName);
            Log.d("CreateItineraryActivity", "类型描述: " + typeDesc);

            // 保存或获取景点
            long siteId = dbHelper.addOrGetSite(selectedPoi, attractionName, latitude, longitude, address,
                    rating, tel, website, typeDesc, photos);
            Log.d("CreateItineraryActivity", "获取到的 siteId: " + siteId);

            if (siteId == -1) {
                Toast.makeText(this, "添加景点失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建ItineraryAttraction对象
            ItineraryAttraction itineraryAttraction = new ItineraryAttraction(siteId, dayNumber, visitOrder, attractionName, transport);
            
            // 获取景点类型
            Sites site = dbHelper.getSiteBySiteId(siteId);
            if (site != null) {
                String type = dbHelper.determineAttractionType(site.getTypeDesc());
                itineraryAttraction.setType(type);
                Log.d("CreateItineraryActivity", "设置景点类型: " + type);
            }

            itineraryAttractionList.add(itineraryAttraction);
            Log.d("CreateItineraryActivity", "添加到列表的景点: " + itineraryAttraction.toString());

            // 清空输入框
            dayNumberEditText.setText("");
            visitOrderEditText.setText("");
            attractionNameEditText.setText("");
            transportEditText.setText("");

            Toast.makeText(this, "景点已添加", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("CreateItineraryActivity", "添加景点时出错: " + e.getMessage());
            Toast.makeText(this, "添加景点失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveItinerary() {
        try {
            // 验证输入
            String itineraryName = itineraryNameEditText.getText().toString();
            String itineraryLocation = itineraryLocationEditText.getText().toString();

            if (itineraryName.isEmpty() || itineraryLocation.isEmpty()) {
                Toast.makeText(this, "请输入行程名称和目的地", Toast.LENGTH_SHORT).show();
                return;
            }

            if (itineraryAttractionList.isEmpty()) {
                Toast.makeText(this, "请至少添加一个景点", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取用户ID
            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            long userId = prefs.getLong("user_id", -1);
            if (userId == -1) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建新行程单
            int status = 0; // 默认为草稿状态
            Itinerary newItinerary = new Itinerary(itineraryName, itineraryLocation, "pic3", userId, status);
            
            // 保存行程单并获取ID
            newItinerary_id = dbHelper.addItinerary(newItinerary);
            Log.d("CreateItineraryActivity", "创建的行程单ID: " + newItinerary_id);

            if (newItinerary_id == -1) {
                Toast.makeText(this, "创建行程单失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存所有景点
            int days = 0;
            for (ItineraryAttraction attraction : itineraryAttractionList) {
                // 设置行程单ID
                attraction.setItineraryId(newItinerary_id);
                Log.d("CreateItineraryActivity", "保存景点 - 行程单ID: " + attraction.getItineraryId() + ", 景点ID: " + attraction.getSiteId());

                // 保存景点
                long attractionId = dbHelper.addAttraction(attraction);
                if (attractionId == -1) {
                    Log.e("CreateItineraryActivity", "保存景点失败: " + attraction.getAttractionName());
                }

                // 更新天数
                if (attraction.getDayNumber() > days) {
                    days = attraction.getDayNumber();
                }
            }

            // 更新行程单天数
            newItinerary.setDays(days);
            if (!dbHelper.updateItinerary(newItinerary)) {
                Log.e("CreateItineraryActivity", "更新行程单天数失败");
            }

            // 返回结果
            Intent resultIntent = new Intent();
            resultIntent.putExtra("itineraryId", newItinerary_id);
            resultIntent.putExtra("itineraryLocation", itineraryLocation);
            resultIntent.putExtra("itineraryName", itineraryName);
            resultIntent.putExtra("days", days);
            resultIntent.putParcelableArrayListExtra("itineraryAttractions", itineraryAttractionList);
            setResult(RESULT_OK, resultIntent);

            Toast.makeText(this, "行程单保存成功", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            Log.e("CreateItineraryActivity", "保存行程单时出错: " + e.getMessage());
            Toast.makeText(this, "保存行程单失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
