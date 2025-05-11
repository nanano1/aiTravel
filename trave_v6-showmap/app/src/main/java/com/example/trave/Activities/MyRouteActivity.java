package com.example.trave.Activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Adapters.ItineraryAdapter;
import com.example.trave.Adapters.MyrouteItineraryAdapeter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyRouteActivity extends AppCompatActivity {
    private Button logoutButton;
    private static final int REQUEST_CODE_CREATE_ITINERARY = 1;
    private static final int REQUEST_CODE_BACK=1;
    private RecyclerView.Adapter adapterItinerary; // 适配器变量，用于管理流行和分类的RecyclerView
    private MyrouteItineraryAdapeter myrouteItineraryAdapeter;
    private RecyclerView recyclerViewItinerary; // 流行和分类的RecyclerView变量
    public static ArrayList<Itinerary> items; // 存储行程单的ArrayList
    public static Map<Long, ArrayList<ItineraryAttraction>> itineraryMap = new HashMap<>();
    private static final int REQUEST_CODE_ADD_ITEM = 1; // 请求码，用于添加新项
    private DatabaseHelper dbHelper;
    private long userId;  // 当前用户的ID
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myroute); // 设置布局文件

        dbHelper = new DatabaseHelper(this);
        items= new ArrayList<>();

        // 获取当前用户ID
        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getLong("user_id", -1);  // 获取用户ID
        recyclerViewItinerary = findViewById(R.id.view_Useritinerary);
        recyclerViewItinerary.setLayoutManager(new GridLayoutManager(this, 2));

        myrouteItineraryAdapeter = new MyrouteItineraryAdapeter(this,items,userId);
        recyclerViewItinerary.setAdapter(myrouteItineraryAdapeter);

        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLoginState(); // 清除登录状态
                Intent intent = new Intent(MyRouteActivity.this, IntroActivity.class);
                startActivity(intent);
                finish(); // 关闭MainActivity
            }
        });

        loadUserItineraries();

        LinearLayout BackMainButton = findViewById(R.id.home); // 获取创建行程单按钮
        BackMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyRouteActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        ImageView createItineraryButton = findViewById(R.id.btn_add_item); // 获取创建行程单按钮
        createItineraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyRouteActivity.this, NewItineraryCreateActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CREATE_ITINERARY);
            }
        });
    }
    private void clearLoginState() {
        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // 清除登录状态
        editor.apply();
    }
    private void loadUserItineraries() {
        items.clear();
        ArrayList<Itinerary> userItineraries = dbHelper.getUserItineraries(userId);

        items.addAll(userItineraries);
        myrouteItineraryAdapeter.updateData();  // 调用适配器的更新数据方法
    }



    @Override
    protected void onResume() {
        super.onResume();
        // 刷新数据
        if (myrouteItineraryAdapeter != null) {
            myrouteItineraryAdapeter.updateData();  // 更新适配器数据
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CREATE_ITINERARY && resultCode == RESULT_OK && data != null) {
            long itineraryId = data.getIntExtra("itineraryId", -1); // 获取新行程单的ID
            ArrayList<ItineraryAttraction> newAttractions = data.getParcelableArrayListExtra("itineraryAttractions");
            Itinerary newItinerary = dbHelper.getItineraryById(itineraryId);
            items.add(newItinerary);
            itineraryMap.put(itineraryId, newAttractions);
            myrouteItineraryAdapeter.updateData();
        }
    }

}
