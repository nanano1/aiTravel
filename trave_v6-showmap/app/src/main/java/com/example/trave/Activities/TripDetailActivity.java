package com.example.trave.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Adapters.TripDetailAdapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.Sites;
import com.example.trave.R;

import java.util.ArrayList;
import java.util.List;

public class TripDetailActivity extends AppCompatActivity {
    private static final String TAG = "TripDetailActivity";
    private static final int EDIT_ITINERARY_REQUEST_CODE = 2;
    public static final int REQUEST_CODE_EDIT = 2;
    private RecyclerView recyclerViewItinerary;
    private TextView textViewtest;
    private TripDetailAdapter tripDetailAdapter;
    private DatabaseHelper dbHelper;
    private Button aiButton; // 新增AI优化按钮

    ArrayList<Sites> SitesList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        dbHelper = new DatabaseHelper(this);

        recyclerViewItinerary = findViewById(R.id.recyclerViewItinerary);
        
        // 设置水平布局管理器，并启用嵌套滚动
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerViewItinerary.setLayoutManager(layoutManager);
        recyclerViewItinerary.setNestedScrollingEnabled(true);

        long itineraryId = getIntent().getLongExtra("itineraryId", 0);
        ArrayList<ItineraryAttraction> itineraryAttractionList = dbHelper.getItineraryAttractions(itineraryId);

        String tittle=getIntent().getStringExtra("Tittle");
        String itineraryLocation=getIntent().getStringExtra("itineraryLocation");

        tripDetailAdapter=new TripDetailAdapter(itineraryId,this,itineraryAttractionList);
        recyclerViewItinerary.setAdapter(tripDetailAdapter);

        // 初始化AI优化按钮
        aiButton = findViewById(R.id.aiBtn);
        if (aiButton != null) {
            aiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleAiButtonClick(itineraryId);
                }
            });
        } else {
            Log.e(TAG, "AI优化按钮未找到！请确认布局文件中已添加此按钮");
        }

        Button deleteButton = findViewById(R.id.deleteBtn);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示确认对话框以确认删除操作
                new AlertDialog.Builder(TripDetailActivity.this)
                        .setTitle("删除行程单")
                        .setMessage("确定要删除这个行程单吗？删除后将无法恢复。")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // 调用删除行程单的方法
                                boolean isDeleted = dbHelper.deleteItinerary(itineraryId);

                                if (isDeleted) {
                                    Toast.makeText(TripDetailActivity.this, "行程单已删除", Toast.LENGTH_SHORT).show();
                                    finish(); // 结束当前活动并返回上一个界面
                                } else {
                                    Toast.makeText(TripDetailActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        Button editButton=findViewById(R.id.btnEditItinerary);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EnhancedEditActivity.class); // 使用新的增强编辑界面
                intent.putExtra("attractilistSize",String.valueOf(itineraryAttractionList.size()));
                intent.putExtra("itineraryTittle", tittle);
                intent.putExtra("itineraryId", itineraryId);
                intent.putExtra("itineraryLocation", itineraryLocation);
                intent.putParcelableArrayListExtra("itineraryAttractions", itineraryAttractionList);
                startActivityForResult(intent,EDIT_ITINERARY_REQUEST_CODE);
            }
        });
        Button releaseButton=findViewById(R.id.release);

        // 获取行程单的发布状态
        int itineraryStatus = dbHelper.getItineraryById(itineraryId).getStatus();
        Log.d("TripDetailAdapter", "itineraryStatus " +  dbHelper.getItineraryById(itineraryId).getLocation());

        // 根据发布状态设置按钮文本和功能
        if (itineraryStatus == 0) {  // 未发布
            releaseButton.setText("未发布");

            // 设置点击事件，发布行程单并更改按钮文本
            releaseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 发布行程单
                    dbHelper.publishItinerary(itineraryId);

                    // 更新按钮文本为"已发布"
                    releaseButton.setText("已发布");

                    // 提示用户已发布
                    Toast.makeText(TripDetailActivity.this, "行程单已发布", Toast.LENGTH_SHORT).show();
                }
            });
        } else {  // 已发布
            releaseButton.setText("已发布");

            // 你可以选择禁用按钮，防止重复点击
            releaseButton.setEnabled(false);
        }
    }

    // 处理AI优化按钮点击事件
    private void handleAiButtonClick(long itineraryId) {
        try {
            Log.i(TAG, "准备启动 AIChatActivity，传递 itineraryId: " + itineraryId);
            Intent intent = new Intent(TripDetailActivity.this, AIChatActivity.class);
            intent.putExtra("itineraryId", itineraryId);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "启动 AIChatActivity 失败", e);
            Toast.makeText(this, "启动AI优化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    }

}