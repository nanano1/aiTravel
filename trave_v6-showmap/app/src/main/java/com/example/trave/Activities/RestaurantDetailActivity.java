package com.example.trave.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.R;
import com.example.trave.Services.RestaurantRecommendService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestaurantDetailActivity extends AppCompatActivity {
    private RecommendedRestaurant restaurant;
    private long itineraryId;
    private int dayNumber;
    private String mealType;
    
    // UI组件
    private TextView restaurantName, ratingText, cuisineTypeText;
    private TextView priceText, shopHoursText, addressText, commentNumText, reasonText;
    private RatingBar ratingBar;
    private ImageView backBtn;
    private Button callButton, selectButton;
    
    private RestaurantRecommendService restaurantService;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);
        
        // 初始化服务和线程池
        restaurantService = new RestaurantRecommendService();
        executorService = Executors.newSingleThreadExecutor();
        
        // 获取Intent中传递的数据
        Intent intent = getIntent();
        if (intent != null) {
            try {
                String restaurantJson = intent.getStringExtra("restaurant_json");
                itineraryId = intent.getLongExtra("itinerary_id", 0);
                dayNumber = intent.getIntExtra("day_number", 0);
                mealType = intent.getStringExtra("meal_type");
                
                if (restaurantJson != null) {
                    JSONObject jsonObject = new JSONObject(restaurantJson);
                    restaurant = new RecommendedRestaurant(jsonObject);
                    initializeViews();
                    populateData();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "数据解析错误", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initializeViews() {
        // 初始化所有UI组件
        restaurantName = findViewById(R.id.restaurantName);
        ratingText = findViewById(R.id.ratingText);
        cuisineTypeText = findViewById(R.id.cuisineTypeText);
        priceText = findViewById(R.id.priceText);
        shopHoursText = findViewById(R.id.shopHoursText);
        addressText = findViewById(R.id.addressText);
        commentNumText = findViewById(R.id.commentNumText);
        reasonText = findViewById(R.id.reasonText);
        ratingBar = findViewById(R.id.ratingBar);
        backBtn = findViewById(R.id.backBtn);
        callButton = findViewById(R.id.callButton);
        selectButton = findViewById(R.id.selectButton);
        
        // 设置返回按钮点击事件
        backBtn.setOnClickListener(v -> finish());
        
        // 设置拨打电话按钮事件
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String telephone = restaurant.getTelephone();
                if (telephone != null && !telephone.equals("无电话") && !telephone.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + telephone));
                    startActivity(intent);
                } else {
                    Toast.makeText(RestaurantDetailActivity.this, "该餐厅暂无电话信息", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 设置选择餐厅按钮事件
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRestaurant();
            }
        });
    }

    private void populateData() {
        // 填充餐厅数据到UI
        restaurantName.setText(restaurant.getName());
        ratingText.setText(String.valueOf(restaurant.getRating()));
        ratingBar.setRating((float) restaurant.getRating());
        cuisineTypeText.setText(restaurant.getCuisineType());
        priceText.setText(String.format("价格: ¥%.0f/人", restaurant.getPriceLevel()));
        shopHoursText.setText("营业时间: " + restaurant.getShopHours());
        addressText.setText("地址: " + restaurant.getAddress());
        commentNumText.setText("评论数: " + restaurant.getCommentNum());
        reasonText.setText(restaurant.getReason());
    }
    
    private void selectRestaurant() {
        // 禁用按钮，防止重复点击
        selectButton.setEnabled(false);
        
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 根据mealType设置不同的order
                    int order = "lunch".equals(mealType) ? 2 : 5; // 午餐是第2个位置，晚餐是第5个位置
                    
                    // 获取数据库Helper
                    DatabaseHelper dbHelper = new DatabaseHelper(RestaurantDetailActivity.this);
                    
                    // 调用服务确认选择餐厅
                    final boolean success = restaurantService.confirmRestaurantSelection(
                            restaurant, itineraryId, dayNumber, order, dbHelper);
                    
                    // 回到主线程显示结果
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                Toast.makeText(RestaurantDetailActivity.this, "已成功选择餐厅", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(RestaurantDetailActivity.this, "选择餐厅失败，请重试", Toast.LENGTH_SHORT).show();
                                selectButton.setEnabled(true);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RestaurantDetailActivity.this, "选择餐厅出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            selectButton.setEnabled(true);
                        }
                    });
                }
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 