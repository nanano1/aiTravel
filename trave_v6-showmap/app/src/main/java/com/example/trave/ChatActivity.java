package com.example.trave;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.example.trave.Adapters.ChatAdapter;
import com.example.trave.Adapters.RestaurantRecommendationAdapter;
import com.example.trave.Domains.ChatMessage;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.Services.AIService;
import com.example.trave.Services.RestaurantRecommendService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements RestaurantRecommendationAdapter.OnRecommendationClickListener {
    private RecyclerView chatRecyclerView;
    private RecyclerView recommendationsRecyclerView;
    private ChatAdapter chatAdapter;
    private RestaurantRecommendationAdapter restaurantAdapter;
    private EditText messageInput;
    private Button btnSend;
    private Button btnSave;
    private AIService aiService;
    private RestaurantRecommendService restaurantService;
    private DatabaseHelper dbHelper;
    private long currentItineraryId;
    private List<ChatMessage> chatMessages;
    private Itinerary currentItinerary;
    private boolean hasUnsavedChanges = false;
    
    // 当前餐厅推荐的上下文
    private int currentDayNumber = 1;
    private String currentMealType = "午餐";
    private List<RecommendedRestaurant> currentRecommendations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 初始化变量
        currentRecommendations = new ArrayList<>();
        dbHelper = new DatabaseHelper(this);
        aiService = new AIService();
        restaurantService = new RestaurantRecommendService();

        // 获取Intent中的行程ID
        currentItineraryId = getIntent().getLongExtra("itineraryId", -1);
        if (currentItineraryId == -1) {
            Toast.makeText(this, "无效的行程ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图
        setupViews();
        setupRecyclerViews();
        setupButtons();
        loadItineraryData();
    }

    private void setupViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        recommendationsRecyclerView = findViewById(R.id.recommendationsRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        btnSend = findViewById(R.id.sendButton);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupRecyclerViews() {
        // 设置聊天列表
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // 设置推荐列表
        restaurantAdapter = new RestaurantRecommendationAdapter(currentRecommendations, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recommendationsRecyclerView.setLayoutManager(layoutManager);
        recommendationsRecyclerView.setAdapter(restaurantAdapter);

        // 添加SnapHelper使卡片对齐
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recommendationsRecyclerView);

        // 设置推荐卡片点击事件
        restaurantAdapter.setOnRecommendationClickListener(this);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> handleSave());
    }

    private void loadItineraryData() {
        // 从数据库加载行程数据
        currentItinerary = dbHelper.getItineraryById(currentItineraryId);
        if (currentItinerary != null) {
            // 可以在这里添加必要的初始化
        }
    }

    private void handleSave() {
        if (currentItinerary != null && hasUnsavedChanges) {
            dbHelper.updateItinerary(currentItinerary);
            Toast.makeText(this, "行程已保存", Toast.LENGTH_SHORT).show();
            hasUnsavedChanges = false;
        }
    }

    private void handleRecommendationSelection(String itemId) {
        // 查找选中的餐厅
        for (RecommendedRestaurant restaurant : currentRecommendations) {
            if (restaurant.getId().equals(itemId)) {
                selectRestaurant(restaurant);
                break;
            }
        }
    }

    private void requestNewRecommendations() {
        refreshRestaurantRecommendations();
    }

    private void showAttractionDetails(String itemId) {
        // 查找选中的餐厅
        for (RecommendedRestaurant restaurant : currentRecommendations) {
            if (restaurant.getId().equals(itemId)) {
                showRestaurantDetails(restaurant);
                break;
            }
        }
    }
    
    /**
     * 处理用户输入消息
     */
    public void onSendButtonClick(View view) {
        TextView inputText = findViewById(R.id.messageInput);
        String message = inputText.getText().toString().trim();
        
        if (!message.isEmpty()) {
            // 添加用户消息
            addMessage(message, true);
            inputText.setText("");

            // 其他AI处理
            processAiResponse(message);

        }
    }
    
    /**
     * 处理AI响应
     */
    private void processAiResponse(String userMessage) {
        // 显示加载中消息
        addMessage("正在思考中...", false);
        
        // 异步获取AI响应
        new ProcessAiResponseTask().execute(userMessage);
    }
    
    /**
     * 添加消息到聊天列表
     */
    private void addMessage(String message, boolean isFromUser) {
        ChatMessage chatMessage = new ChatMessage(message, isFromUser);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }
    
    /**
     * 显示餐厅推荐列表
     */
    private void displayRestaurantRecommendations(List<RecommendedRestaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) {
            addMessage("抱歉，没有找到符合条件的餐厅。", false);
            recommendationsRecyclerView.setVisibility(View.GONE);
            return;
        }
        
        // 更新当前推荐
        currentRecommendations.clear();
        currentRecommendations.addAll(restaurants);
        
        // 更新适配器
        restaurantAdapter.updateRecommendations(currentRecommendations);
        
        // 显示餐厅推荐列表
        recommendationsRecyclerView.setVisibility(View.VISIBLE);
        
        // 添加消息
        StringBuilder sb = new StringBuilder();
        sb.append("为您找到了").append(restaurants.size()).append("家餐厅:\n");
        
        for (RecommendedRestaurant restaurant : restaurants) {
            sb.append("• ").append(restaurant.getName())
              .append(" (").append(restaurant.getCuisineType()).append(")\n");
        }
        sb.append("\n请在下方滑动卡片查看详情，或选择一家您喜欢的餐厅。");
        
        addMessage(sb.toString(), false);
    }
    
    /**
     * 选择一家餐厅
     */
    private void selectRestaurant(RecommendedRestaurant restaurant) {
        // 异步确认餐厅选择
        new SelectRestaurantTask().execute(restaurant);
    }
    
    /**
     * 刷新餐厅推荐
     */
    private void refreshRestaurantRecommendations() {
        // 显示加载中消息
        addMessage("正在为您寻找更多餐厅选择...", false);
        
        // 异步获取新的餐厅推荐
        new RefreshRestaurantRecommendationsTask().execute();
    }
    
    /**
     * 显示餐厅详情
     */
    private void showRestaurantDetails(RecommendedRestaurant restaurant) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_restaurant_details);
        
        // 设置餐厅信息
        TextView tvName = dialog.findViewById(R.id.tvRestaurantName);
        TextView tvCuisine = dialog.findViewById(R.id.tvCuisine);
        TextView tvPrice = dialog.findViewById(R.id.tvPrice);
        TextView tvAddress = dialog.findViewById(R.id.tvAddress);
        TextView tvReason = dialog.findViewById(R.id.tvReason);

        tvName.setText(restaurant.getName());
        tvCuisine.setText(restaurant.getCuisineType());
        tvPrice.setText(getPriceLevelString(restaurant.getPriceLevel()));
        tvAddress.setText(restaurant.getAddress());
        tvReason.setText(restaurant.getReason());
        
        // 设置按钮点击事件
        Button btnSelect = dialog.findViewById(R.id.btnSelect);
        Button btnCancel = dialog.findViewById(R.id.btnClose);
        
        btnSelect.setOnClickListener(v -> {
            selectRestaurant(restaurant);
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // 显示对话框
        dialog.show();
    }
    
    /**
     * 根据价格级别返回价格标识
     */
    private String getPriceLevelString(double priceLevel) {
        int level = (int) Math.round(priceLevel);
        switch (level) {
            case 1:
                return "¥ (经济实惠)";
            case 2:
                return "¥¥ (中等价位)";
            case 3:
                return "¥¥¥ (高档价位)";
            case 4:
                return "¥¥¥¥ (奢华价位)";
            default:
                return "¥ (经济实惠)";
        }
    }
    
    /**
     * 返回按钮处理
     */
    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            // 有未保存的更改，询问用户是否保存
            new AlertDialog.Builder(this)
                    .setTitle("保存更改")
                    .setMessage("您有未保存的更改，是否保存？")
                    .setPositiveButton("保存", (dialog, which) -> {
                        handleSave();
                        finish();
                    })
                    .setNegativeButton("不保存", (dialog, which) -> finish())
                    .setNeutralButton("取消", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (aiService != null) {
            try {
                aiService.clearSession(currentItineraryId);
            } catch (Exception e) {
                Log.e("ChatActivity", "清理会话失败", e);
            }
        }
    }
    
    /**
     * 异步获取餐厅推荐
     */
    private class GetRestaurantRecommendationsTask extends AsyncTask<Object, Void, List<RecommendedRestaurant>> {
        @Override
        protected List<RecommendedRestaurant> doInBackground(Object... params) {
            long itineraryId = (long) params[0];
            int dayNumber = (int) params[1];
            String mealType = (String) params[2];
            
            try {
                return restaurantService.getRecommendations(itineraryId, dayNumber, mealType);
            } catch (Exception e) {
                Log.e("ChatActivity", "获取餐厅推荐失败", e);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<RecommendedRestaurant> restaurants) {
            // 移除"正在查找"消息
            chatMessages.remove(chatMessages.size() - 1);
            chatAdapter.notifyItemRemoved(chatMessages.size());
            
            // 显示餐厅推荐
            displayRestaurantRecommendations(restaurants);
        }
    }
    
    /**
     * 异步处理AI响应
     */
    private class ProcessAiResponseTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String userMessage = params[0];
            
            try {
                // 获取AI响应
                return aiService.getAIResponse(userMessage, currentItineraryId, dbHelper).getCleanText();
            } catch (Exception e) {
                Log.e("ChatActivity", "获取AI响应失败", e);
                return "抱歉，我暂时无法回答您的问题。(" + e.getMessage() + ")";
            }
        }
        
        @Override
        protected void onPostExecute(String response) {
            // 移除"正在思考"消息
            chatMessages.remove(chatMessages.size() - 1);
            chatAdapter.notifyItemRemoved(chatMessages.size());
            
            // 添加AI响应消息
            addMessage(response, false);
            
            // 检查是否有结构化数据
            if (aiService.hasStructuredDataOfType("restaurant_recommendations")) {
                // 显示餐厅推荐列表
                List<RecommendedRestaurant> recommendations = aiService.getRestaurantRecommendations();
                if (recommendations != null && !recommendations.isEmpty()) {
                    // 更新当前推荐
                    currentRecommendations.clear();
                    currentRecommendations.addAll(recommendations);
                    
                    // 更新适配器
                    restaurantAdapter.updateRecommendations(currentRecommendations);
                    
                    // 显示餐厅推荐列表
                    recommendationsRecyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                // 隐藏推荐列表
                recommendationsRecyclerView.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * 异步选择餐厅
     */
    private class SelectRestaurantTask extends AsyncTask<RecommendedRestaurant, Void, Boolean> {
        private RecommendedRestaurant selectedRestaurant;
        private String errorMessage = "";

        @Override
        protected Boolean doInBackground(RecommendedRestaurant... params) {
            selectedRestaurant = params[0];

            try {
                // 确认餐厅选择
                JSONObject confirmationData = new JSONObject();
                confirmationData.put("itinerary_id", currentItineraryId);
                confirmationData.put("day", currentDayNumber);
                confirmationData.put("meal_type", currentMealType);
                confirmationData.put("restaurant_id", selectedRestaurant.getId());
                confirmationData.put("restaurant_name", selectedRestaurant.getName());

                return true;
            } catch (Exception e) {
                Log.e("ChatActivity", "确认餐厅选择失败", e);
                errorMessage = e.getMessage();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // 隐藏推荐列表
                recommendationsRecyclerView.setVisibility(View.GONE);
                
                // 添加确认消息
                addMessage("好的，已为您选择了「" + selectedRestaurant.getName() + "」，已添加到您的行程中。", false);
                
                // 标记有未保存的更改
                hasUnsavedChanges = true;
            } else {
                // 显示错误消息
                Toast.makeText(ChatActivity.this, "选择餐厅失败: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 异步刷新餐厅推荐
     */
    private class RefreshRestaurantRecommendationsTask extends AsyncTask<Void, Void, List<RecommendedRestaurant>> {
        private String errorMessage = "";
        
        @Override
        protected List<RecommendedRestaurant> doInBackground(Void... params) {
            try {
                // 获取新的餐厅推荐
                return restaurantService.refreshRecommendations(currentItineraryId, currentDayNumber, currentMealType);
            } catch (Exception e) {
                Log.e("ChatActivity", "刷新餐厅推荐失败", e);
                errorMessage = e.getMessage();
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<RecommendedRestaurant> restaurants) {
            // 移除"正在查找"消息
            chatMessages.remove(chatMessages.size() - 1);
            chatAdapter.notifyItemRemoved(chatMessages.size());
            
            if (restaurants != null && !restaurants.isEmpty()) {
                // 显示新的餐厅推荐
                displayRestaurantRecommendations(restaurants);
            } else {
                // 显示错误消息
                addMessage("抱歉，无法找到更多餐厅推荐。" + (errorMessage.isEmpty() ? "" : "(" + errorMessage + ")"), false);
            }
        }
    }
    
    /**
     * 异步确认AI推荐
     */
    private class ConfirmAIRecommendationTask extends AsyncTask<Object, Void, Boolean> {
        private RecommendedRestaurant selectedRestaurant;
        private String errorMessage = "";
        
        @Override
        protected Boolean doInBackground(Object... params) {
            selectedRestaurant = (RecommendedRestaurant) params[0];
            long itineraryId = (long) params[1];
            int dayNumber = (int) params[2];
            String mealType = (String) params[3];
            
            try {
                // 构建确认数据
                JSONObject confirmData = new JSONObject();
                confirmData.put("itinerary_id", itineraryId);
                confirmData.put("day", dayNumber);
                confirmData.put("meal_type", mealType);
                confirmData.put("restaurant", new JSONObject()
                        .put("id", selectedRestaurant.getId())
                        .put("name", selectedRestaurant.getName())
                        .put("address", selectedRestaurant.getAddress())
                        .put("cuisine", selectedRestaurant.getCuisineType())
                        .put("price", selectedRestaurant.getPriceLevel())
                        .put("latitude", selectedRestaurant.getLatitude())
                        .put("longitude", selectedRestaurant.getLongitude())
                );
                
                // 调用API确认选择
                return restaurantService.confirmRecommendation(confirmData);
            } catch (Exception e) {
                Log.e("ChatActivity", "确认AI推荐失败", e);
                errorMessage = e.getMessage();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                // 显示错误消息
                Toast.makeText(ChatActivity.this, "确认选择失败: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSelectClick(RecommendedRestaurant restaurant) {
        selectRestaurant(restaurant);
    }

    @Override
    public void onRefreshClick() {
        refreshRestaurantRecommendations();
    }

    @Override
    public void onDetailsClick(RecommendedRestaurant restaurant) {
        showRestaurantDetails(restaurant);
    }
} 