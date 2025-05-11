package com.example.trave.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Adapters.TripDetailAdapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.Sites;
import com.example.trave.R;

import java.util.ArrayList;

public class TripDetailMainActivity extends AppCompatActivity {
    private static final String TAG = "TripDetailMainActivity";
    private static final int EDIT_ITINERARY_REQUEST_CODE = 2;
    public static final int REQUEST_CODE_EDIT = 2;
    private RecyclerView recyclerViewItinerary;
    private TextView textViewtest;
    private TripDetailAdapter tripDetailAdapter;
    private DatabaseHelper dbHelper;
    private long itineraryId;
    private Button aiButton;
    private Button saveButton;

    ArrayList<Sites> SitesList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail_main);
        Log.d(TAG, "onCreate: Activity 创建");
        
        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 获取传入的参数
        itineraryId = getIntent().getLongExtra("itineraryId", -1);
        Log.d(TAG, "获取到的 itineraryId: " + itineraryId);
        
        if (itineraryId == -1) {
            Log.d(TAG, "未获取到有效的 itineraryId");
            Toast.makeText(this, "行程ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化 RecyclerView
        initRecyclerView();

        // 初始化按钮
        initButtons();
    }

    private void initRecyclerView() {
        Log.d(TAG, "初始化 RecyclerView...");
        recyclerViewItinerary = findViewById(R.id.recyclerViewItinerary);
        
        if (recyclerViewItinerary == null) {
            Log.d(TAG, "RecyclerView 未找到！");
            return;
        }

        recyclerViewItinerary.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        ArrayList<ItineraryAttraction> itineraryAttractionList = dbHelper.getItineraryAttractions(itineraryId);
        Log.d(TAG, "获取到的景点数量: " + (itineraryAttractionList != null ? itineraryAttractionList.size() : 0));

        tripDetailAdapter = new TripDetailAdapter(itineraryId, this, itineraryAttractionList);
        recyclerViewItinerary.setAdapter(tripDetailAdapter);
    }

    private void initButtons() {
        Log.d(TAG, "初始化按钮...");

        // 初始化 AI 优化按钮
        aiButton = findViewById(R.id.btnAi);
        if (aiButton == null) {
            Log.e(TAG, "AI优化按钮未找到！");
            return;
        }
        Log.d(TAG, "AI优化按钮已找到");

        aiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "AI优化按钮被点击");
                handleAiButtonClick();
            }
        });

        // 初始化保存按钮
        saveButton = findViewById(R.id.btnSave);
        if (saveButton == null) {
            Log.e(TAG, "保存按钮未找到！");
            return;
        }
        Log.d(TAG, "保存按钮已找到");

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "保存按钮被点击");
                handleSaveButtonClick();
            }
        });
    }

    private void handleAiButtonClick() {
        try {
            Log.i(TAG, "准备启动 AIChatActivity，传递 itineraryId: " + itineraryId);
            Intent intent = new Intent(TripDetailMainActivity.this, AIChatActivity.class);
            intent.putExtra("itineraryId", itineraryId);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "启动 AIChatActivity 失败", e);
            Toast.makeText(this, "启动AI优化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSaveButtonClick() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
            long userId = sharedPreferences.getLong("user_id", -1);
            Log.i(TAG, "当前用户ID: " + userId);
            
            if (userId == -1) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.copyItineraryToCurrentUser(itineraryId, userId);
            Toast.makeText(this, "行程单已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "保存行程失败", e);
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_ITINERARY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Long itineraryId = data.getLongExtra("itineraryId", -1);

            ArrayList<ItineraryAttraction> updatedAttractions = data.getParcelableArrayListExtra("itineraryAttractions");
            if (itineraryId != -1 && updatedAttractions != null) {
                dbHelper.deleteAttractionsForItinerary(itineraryId);
               for (ItineraryAttraction attraction : updatedAttractions) {
                    dbHelper.addAttraction(attraction);
               }
                tripDetailAdapter.refreshAttractionData();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: Activity 恢复");
    }

}