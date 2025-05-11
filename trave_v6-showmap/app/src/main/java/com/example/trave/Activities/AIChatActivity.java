package com.example.trave.Activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave.Adapters.ChatAdapter;
import com.example.trave.Adapters.ItineraryDetailAdapter;
import com.example.trave.Adapters.RestaurantRecommendationAdapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.ChatMessage;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.R;
import com.example.trave.Services.AIService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIChatActivity extends AppCompatActivity implements RestaurantRecommendationAdapter.OnRecommendationClickListener {
    private static final String TAG = "AIChatActivity";
    private static final String JSON_DATA_PATTERN = "<!--JSON_DATA:(.+?)-->";
    private RecyclerView chatRecyclerView;
    private RecyclerView recommendationsRecyclerView;
    private RecyclerView itineraryRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private ChatAdapter chatAdapter;
    private RestaurantRecommendationAdapter recommendationsAdapter;
    private ItineraryDetailAdapter itineraryDetailAdapter;
    private ArrayList<ChatMessage> chatMessages;
    private List<ItineraryAttraction> itineraryAttractions;
    private AIService aiService;
    private ExecutorService executorService;
    private Handler mainHandler;
    private long itineraryId;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_new);
        Log.d(TAG, "AIChatActivity 被创建");

        // 初始化变量
        itineraryId = getIntent().getLongExtra("itineraryId", -1);
        chatMessages = new ArrayList<>();
        itineraryAttractions = new ArrayList<>();
        aiService = new AIService();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        dbHelper = new DatabaseHelper(this);

        initializeViews();
        setupRecyclerViews();
        setupClickListeners();

        addMessage("您好！我是您的AI旅行助手。我可以帮您优化行程，推荐景点和餐厅，或者回答旅行相关的问题。请问有什么可以帮您的吗？", false);
        loadItineraryData();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        recommendationsRecyclerView = findViewById(R.id.recommendationsRecyclerView);
        itineraryRecyclerView = findViewById(R.id.itineraryRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
    }

    private void setupRecyclerViews() {
        // 设置聊天列表
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // 设置推荐列表
        recommendationsAdapter = new RestaurantRecommendationAdapter(new ArrayList<>(), this);
        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recommendationsRecyclerView.setAdapter(recommendationsAdapter);

        // 设置行程列表
        itineraryDetailAdapter = new ItineraryDetailAdapter(new ArrayList<>());
        itineraryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        itineraryRecyclerView.setAdapter(itineraryDetailAdapter);
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
        });
    }

    private void loadItineraryData() {
        executorService.execute(() -> {
            try {
                ArrayList<ItineraryAttraction> attractions = dbHelper.getItineraryAttractions(itineraryId);
                mainHandler.post(() -> {
                    itineraryAttractions = attractions;
                    itineraryDetailAdapter.updateAttractions(attractions);
                });
            } catch (Exception e) {
                Log.e(TAG, "加载行程数据失败", e);
                mainHandler.post(() -> 
                    Toast.makeText(this, "加载行程数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void sendMessage(final String message) {
        addMessage(message, true);
        messageInput.setText("");

        executorService.execute(() -> {
            try {
                mainHandler.post(() -> {
                    recommendationsRecyclerView.setVisibility(View.GONE);
                    Toast.makeText(AIChatActivity.this, "正在处理您的请求...", Toast.LENGTH_SHORT).show();
                });

                AIService.AIResponseData responseData = aiService.getAIResponse(message, itineraryId, dbHelper);
                
                mainHandler.post(() -> {
                    // 添加AI文本响应
                    addMessage(responseData.getCleanText(), false);
                    
                    // 处理结构化数据
                    if (responseData.hasStructuredData()) {
                        processStructuredData(responseData);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Log.e(TAG, "发生错误", e);
                    Toast.makeText(AIChatActivity.this, "错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    addMessage("抱歉，发生错误: " + e.getMessage(), false);
                });
            }
        });
    }

    private void processStructuredData(AIService.AIResponseData responseData) {
        try {
            String dataType = responseData.getDataType();
            JSONObject structuredData = responseData.getStructuredData();

            if ("restaurant_recommendations".equals(dataType)) {
                handleRestaurantRecommendations(structuredData);
            } else if ("itinerary_update".equals(dataType)) {
                handleItineraryUpdate(structuredData);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理结构化数据失败", e);
        }
    }

    private void handleRestaurantRecommendations(JSONObject data) throws JSONException {
        JSONArray recommendations = data.getJSONArray("recommendations");
        List<RecommendedRestaurant> items = new ArrayList<>();
        
        for (int i = 0; i < recommendations.length(); i++) {
            JSONObject rec = recommendations.getJSONObject(i);
            items.add(new RecommendedRestaurant(rec));
        }

        recommendationsRecyclerView.setVisibility(View.VISIBLE);
        recommendationsAdapter.updateRecommendations(items);
    }

    private void handleItineraryUpdate(JSONObject data) {
        loadItineraryData();  // 重新加载行程数据
    }

    @Override
    public void onSelectClick(RecommendedRestaurant restaurant) {
        executorService.execute(() -> {
            try {
                boolean success = aiService.updateItineraryWithRecommendation(
                    itineraryId, 
                    restaurant.getOriginalData(),
                    dbHelper
                );

                mainHandler.post(() -> {
                    if (success) {
                        Toast.makeText(this, "已更新行程", Toast.LENGTH_SHORT).show();
                        loadItineraryData();
                        recommendationsRecyclerView.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "更新行程失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> 
                    Toast.makeText(this, "更新行程失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onRefreshClick() {
        // 重新获取推荐
        sendMessage("请重新推荐其他餐厅");
    }

    @Override
    public void onDetailsClick(RecommendedRestaurant restaurant) {
        // 显示餐厅详细信息
        String details = String.format(
            "餐厅：%s\n类型：%s\n价格：¥%.0f/人\n地址：%s\n%s",
            restaurant.getName(),
            restaurant.getCuisineType(),
            restaurant.getPriceLevel(),
            restaurant.getAddress(),
            restaurant.getReason()
        );
        
        addMessage(details, false);
    }

    private void addMessage(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
} 