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
    private static final int EDIT_ITINERARY_REQUEST_CODE = 2;
    public static final int REQUEST_CODE_EDIT = 2;
    private RecyclerView recyclerViewItinerary;
    private TextView textViewtest;
    private TripDetailAdapter tripDetailAdapter;
    private DatabaseHelper dbHelper;

    ArrayList<Sites> SitesList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        dbHelper = new DatabaseHelper(this);

        recyclerViewItinerary = findViewById(R.id.recyclerViewItinerary);
        recyclerViewItinerary.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false));

        long itineraryId = getIntent().getLongExtra("itineraryId", 0);
        ArrayList<ItineraryAttraction> itineraryAttractionList = dbHelper.getItineraryAttractions(itineraryId);

        String tittle=getIntent().getStringExtra("Tittle");
        String itineraryLocation=getIntent().getStringExtra("itineraryLocation");

        tripDetailAdapter=new TripDetailAdapter(itineraryId,this,itineraryAttractionList);
        recyclerViewItinerary.setAdapter(tripDetailAdapter);


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
                Intent intent = new Intent(context, detailEditActivity.class);
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

                    // 更新按钮文本为“已发布”
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