package com.example.trave.Activities;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Adapters.ItineraryAdapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_CREATE_ITINERARY = 1;
    private RecyclerView.Adapter adapterItinerary; // 适配器变量，用于管理流行和分类的RecyclerView
    private ItineraryAdapter ItineraryAdapter;
    private RecyclerView recyclerViewItinerary; // 流行和分类的RecyclerView变量
    public static ArrayList<Itinerary> items; // 存储行程单的ArrayList
    public static Map<Long, ArrayList<ItineraryAttraction>> itineraryMap = new HashMap<>();
    private static final int REQUEST_CODE_ADD_ITEM = 1; // 请求码，用于添加新项
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // 设置布局文件
        dbHelper = new DatabaseHelper(this);
        items= new ArrayList<>();

        recyclerViewItinerary = findViewById(R.id.view_itinerary);
        recyclerViewItinerary.setLayoutManager(new GridLayoutManager(this, 2));
        ItineraryAdapter = new ItineraryAdapter(this, 0);

        recyclerViewItinerary.setAdapter(ItineraryAdapter);
        refreshItineraries();

        ImageView createItineraryButton = findViewById(R.id.btn_add_item); // 获取创建行程单按钮
        createItineraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NewItineraryCreateActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CREATE_ITINERARY);
            }
        });
        LinearLayout MyRouteButton = findViewById(R.id.me); // 获取创建行程单按钮
        MyRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MyRouteActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CREATE_ITINERARY);
            }
        });
        loadAllItineraries();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回MainActivity时刷新数据
        refreshItineraries();
    }

    private void refreshItineraries() {
        // 从数据库加载所有行程单并更新适配器
        items.clear();
        items.addAll(dbHelper.getAllPublishItineraries());
        ItineraryAdapter.refreshData();
    }

    private void initializeSampleData() {
        // 检查数据库中是否已有行程单
        if (dbHelper.getAllItineraries().size() == 0) {
            // 如果没有行程单，才添加样例数据
            Itinerary itinerary1 = new Itinerary("行程单1", "武汉", "pic1", 2, 1, 1);
            Itinerary itinerary2 = new Itinerary("行程单2", "上海", "pic2", 2, 2, 1);

            long itinerary1_id = dbHelper.addItinerary(itinerary1);
            long itinerary2_id = dbHelper.addItinerary(itinerary2);

            dbHelper.addAttraction(new ItineraryAttraction(itinerary1_id, 1, 1, "第1天景点1", "走路"));
            dbHelper.addAttraction(new ItineraryAttraction(itinerary1_id, 1, 2, "第1天景点2db", "走路"));
            dbHelper.addAttraction(new ItineraryAttraction(itinerary1_id, 2, 1, "第2天景点1db", "走路"));

            dbHelper.addAttraction(new ItineraryAttraction(itinerary2_id, 1, 1, "第1天景点1db", "走路"));
            dbHelper.addAttraction(new ItineraryAttraction(itinerary2_id, 2, 1, "第2天景点1db", "走路"));

            ItineraryAdapter.refreshData();
        }
    }

    private void loadAllItineraries() {
        items.clear();
        ArrayList<Itinerary> userItineraries = dbHelper.getAllItineraries();


        items.addAll(userItineraries);
        ItineraryAdapter.refreshData();  // 调用适配器的更新数据方法
    }


}
