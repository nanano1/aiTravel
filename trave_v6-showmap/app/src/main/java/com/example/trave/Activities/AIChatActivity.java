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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave.Adapters.ChatAdapter;
import com.example.trave.Adapters.ChatItineraryAdapter;
import com.example.trave.Adapters.ItineraryDetailAdapter;
import com.example.trave.Adapters.POIRecommendationAdapter;
import com.example.trave.Adapters.RestaurantRecommendationAdapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.ChatMessage;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.RecommendedPOI;
import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.R;
import com.example.trave.Services.AIService;
import com.example.trave.Services.RestaurantRecommendService;
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


public class AIChatActivity extends AppCompatActivity implements 
        RestaurantRecommendationAdapter.OnRecommendationClickListener,
        POIRecommendationAdapter.OnRecommendationClickListener,
        ItineraryDetailAdapter.OnItemClickListener {
        
    private static final String TAG = "AIChatActivity";
    private static final String JSON_DATA_PATTERN = "<!--JSON_DATA:(.+?)-->";
    private RecyclerView chatRecyclerView;
    private RecyclerView recommendationsRecyclerView;
    private RecyclerView itineraryRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private Button saveButton;
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
    private ItemTouchHelper itemTouchHelper;
    private boolean hasChanges = false;
    private POIRecommendationAdapter poiRecommendationsAdapter;

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
        setupItemTouchHelper();

        addMessage("您好！我是您的AI旅行助手。我可以帮您优化行程，推荐景点和餐厅，或者回答旅行相关的问题。请问有什么可以帮您的吗？", false);
        loadItineraryData();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        recommendationsRecyclerView = findViewById(R.id.recommendationsRecyclerView);
        itineraryRecyclerView = findViewById(R.id.itineraryRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupRecyclerViews() {
        // 设置聊天列表
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // 设置水平布局管理器，用于横向滚动推荐卡片
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        recommendationsRecyclerView.setLayoutManager(horizontalLayoutManager);

        // 初始化两个推荐适配器
        recommendationsAdapter = new RestaurantRecommendationAdapter(new ArrayList<>(), this);
        poiRecommendationsAdapter = new POIRecommendationAdapter(new ArrayList<>(), this);
        
        // 默认使用餐厅推荐适配器
        recommendationsRecyclerView.setAdapter(recommendationsAdapter);

        // 设置行程列表
        itineraryDetailAdapter = new ItineraryDetailAdapter(new ArrayList<>());
        itineraryDetailAdapter.setOnItemClickListener(this);
        itineraryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        itineraryRecyclerView.setAdapter(itineraryDetailAdapter);
        
        Log.d(TAG, "所有RecyclerView和适配器已设置完成");
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            }
        });
        
        saveButton.setOnClickListener(v -> {
            saveOptimizedItinerary();
        });
    }
    
    private void setupItemTouchHelper() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, 
                                 RecyclerView.ViewHolder target) {
                if (source.getItemViewType() != target.getItemViewType()) {
                    return false;
                }
                
                // 移动项目
                int fromPosition = source.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                itineraryDetailAdapter.moveItem(fromPosition, toPosition);
                hasChanges = true;
                return true;
            }
            
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // 我们不实现滑动删除
            }
            
            @Override
            public boolean isLongPressDragEnabled() {
                // 禁用长按拖动，只通过手柄拖动
                return false;
            }
            
            @Override
            public boolean isItemViewSwipeEnabled() {
                // 禁用滑动
                return false;
            }
            
            @Override
            public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                // 只在编辑模式下允许拖动
                if (itineraryDetailAdapter.isEditMode()) {
                    return super.getDragDirs(recyclerView, viewHolder);
                }
                return 0;
            }
        };
        
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(itineraryRecyclerView);
        
        // 设置拖拽监听器
        itineraryDetailAdapter.setOnStartDragListener(viewHolder -> 
            itemTouchHelper.startDrag(viewHolder));
    }

    private void loadItineraryData() {
        executorService.execute(() -> {
            try {
                ArrayList<ItineraryAttraction> attractions = dbHelper.getItineraryAttractions(itineraryId);
                mainHandler.post(() -> {
                    itineraryAttractions = attractions;
                    itineraryDetailAdapter.updateAttractions(new ArrayList<>(attractions));
                });
            } catch (Exception e) {
                Log.e(TAG, "加载行程数据失败", e);
                mainHandler.post(() -> 
                    Toast.makeText(this, "加载行程数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    
    private void saveItineraryChanges() {
        executorService.execute(() -> {
            try {
                ArrayList<ItineraryAttraction> attractions = itineraryDetailAdapter.getAttractions();
                
                // 更新数据库中的顺序
                for (ItineraryAttraction attraction : attractions) {
                    dbHelper.updateAttractionOrder(
                        attraction.getId(), 
                        attraction.getDayNumber(), 
                        attraction.getVisitOrder()
                    );
                }
                
                hasChanges = false;
                
                mainHandler.post(() -> 
                    Toast.makeText(this, "行程顺序已更新", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                Log.e(TAG, "保存行程更改失败", e);
                mainHandler.post(() -> 
                    Toast.makeText(this, "保存行程更改失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
                    // 清除当前适配器的数据，避免显示旧数据
                    if (recommendationsAdapter != null) {
                        recommendationsAdapter.updateRecommendations(new ArrayList<>());
                    }
                    if (poiRecommendationsAdapter != null) {
                        poiRecommendationsAdapter.updateRecommendations(new ArrayList<>());
                    }
                    Toast.makeText(AIChatActivity.this, "正在处理您的请求...", Toast.LENGTH_SHORT).show();
                });

                AIService.AIResponseData responseData = aiService.getAIResponse(message, itineraryId, dbHelper);
                
                mainHandler.post(() -> {
                    // 添加AI文本响应
                    addMessage(responseData.getCleanText(), false);
                    
                    // 处理结构化数据
                    if (responseData.hasStructuredData()) {
                        processAIResponse(responseData);
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

    private void processAIResponse(AIService.AIResponseData responseData) {
        // 添加AI消息到聊天列表
        addMessage(responseData.getCleanText(), false);
        
        // 检查是否有结构化数据
        if (responseData.hasStructuredData()) {
            String dataType = responseData.getDataType();
            JSONObject structuredData = responseData.getStructuredData();
            Log.d(TAG, "收到结构化数据，类型: " + dataType);
            
            try {
                switch (dataType) {
                    case "restaurant_recommendations":
                        handleRestaurantRecommendations(structuredData);
                        break;
                    case "poi_recommendations":
                        handlePOIRecommendations(structuredData);
                        break;
                    case "poi_replace":
                        handlePOIReplacement(structuredData);
                        break;
                    case "itinerary_update":
                        handleItineraryUpdate(structuredData);
                        break;
                    case "optimized_itinerary":
                        handleOptimizedItinerary(structuredData);
                        break;
                    default:
                        Log.d(TAG, "未知的数据类型: " + dataType);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "处理结构化数据时出错", e);
            }
        }
    }

    private void handleRestaurantRecommendations(JSONObject data) throws JSONException {
        JSONArray recommendations = data.getJSONArray("recommendations");
        List<RecommendedRestaurant> items = new ArrayList<>();
        
        for (int i = 0; i < recommendations.length(); i++) {
            JSONObject rec = recommendations.getJSONObject(i);
            items.add(new RecommendedRestaurant(rec));
        }

        // 确保使用餐厅推荐适配器
        recommendationsRecyclerView.setAdapter(recommendationsAdapter);
        recommendationsRecyclerView.setVisibility(View.VISIBLE);
        recommendationsAdapter.updateRecommendations(items);
        
        Log.d(TAG, "已更新餐厅推荐，数量: " + items.size());
    }

    private void handlePOIRecommendations(JSONObject data) throws JSONException {
        JSONArray recommendations = data.getJSONArray("recommendations");
        List<RecommendedPOI> items = new ArrayList<>();
        
        for (int i = 0; i < recommendations.length(); i++) {
            JSONObject rec = recommendations.getJSONObject(i);
            if (i == 0) {
                // 为第一个POI添加日和序号，确保能正确更新行程
                rec.put("day", 1);  // 默认添加到第1天
                rec.put("order", 1); // 默认作为第1个景点
            }
            items.add(new RecommendedPOI(rec));
        }

        // 使用新的POI推荐对话框替代列表显示
        POIRecommendationDialog dialog = new POIRecommendationDialog(this, items, new POIRecommendationDialog.POIRecommendationListener() {
            @Override
            public void onPOIsSelected(List<RecommendedPOI> selectedPOIs) {
                // 处理用户选择的多个POI
                processPOISelections(selectedPOIs);
            }

            @Override
            public void onRefreshRequest() {
                // 处理"换一批"请求
                String refreshMessage = "请告诉我您的更多偏好，例如：\n"
                        + "- 想去更文艺的地方\n"
                        + "- 有适合小孩子的景点吗\n"
                        + "- 想去人少一点的地方\n"
                        + "- 想找评分高于4.5的景点\n\n"
                        + "这样我可以为您提供更符合需求的推荐。";
                addMessage(refreshMessage, false);
            }
        });

        dialog.show();
        
        Log.d(TAG, "已显示POI推荐对话框，数量: " + items.size());
    }

    // 添加处理多个POI选择的新方法
    private void processPOISelections(List<RecommendedPOI> selectedPOIs) {
        if (selectedPOIs == null || selectedPOIs.isEmpty()) {
            Log.w(TAG, "未选择任何POI");
            return;
        }
        
        executorService.execute(() -> {
            boolean allSuccess = true;
            StringBuilder resultMessage = new StringBuilder("已添加以下景点到您的行程：\n");
            
            for (RecommendedPOI poi : selectedPOIs) {
                try {
                    // 获取POI的原始JSON数据
                    JSONObject poiData = poi.getOriginalData();
                    if (poiData != null) {
                        // 确保有day和order字段
                        int day = poiData.optInt("day", 1);
                        int order = poiData.optInt("order", 1);
                        Log.d(TAG, "处理POI - " + poi.getName() + ", day: " + day + ", order: " + order);
                        
                        // 更新到行程中
                        boolean success = aiService.updateItineraryWithPOIRecommendation(
                            itineraryId, 
                            poiData,
                            dbHelper
                        );
                        
                        if (success) {
                            resultMessage.append("• ").append(poi.getName()).append("\n");
                        } else {
                            allSuccess = false;
                            Log.e(TAG, "添加POI失败: " + poi.getName());
                        }
                    }
                } catch (Exception e) {
                    allSuccess = false;
                    Log.e(TAG, "处理POI选择时出错: " + e.getMessage(), e);
                }
            }
            
            final boolean finalSuccess = allSuccess;
            mainHandler.post(() -> {
                // 重新加载行程数据
                loadItineraryData();
                
                // 显示结果消息
                if (finalSuccess) {
                    addMessage(resultMessage.toString(), false);
                } else {
                    addMessage("部分景点添加失败，请重试。", false);
                }
                StringBuilder selectedNames = new StringBuilder();
                for (RecommendedPOI poi : selectedPOIs) {
                    // 获取POI的原始JSON数据
                    JSONObject poiData = poi.getOriginalData();
                    String name = poiData.optString("name");
                    // 将名称添加到字符串构建器中
                    if (selectedNames.length() > 0) {
                        selectedNames.append(", "); // 添加逗号分隔
                    }
                    selectedNames.append(name);
                }
                // 向AI发送确认消息
                sendMessage("我已选择了" +selectedNames.toString());
            });
        });
    }

    private void handleItineraryUpdate(JSONObject data) {
        loadItineraryData();  // 重新加载行程数据
    }

    // 处理优化后的行程
    private void handleOptimizedItinerary(JSONObject data) {
        try {
            JSONObject itineraryData = data.getJSONObject("itinerary");
            Log.d(TAG, "收到优化行程数据: " + itineraryData.toString());
            
            // 从JSON提取行程景点数据
            ArrayList<ItineraryAttraction> optimizedAttractions = new ArrayList<>();
            JSONArray attractions = itineraryData.getJSONArray("attractions");
            
            for (int i = 0; i < attractions.length(); i++) {
                JSONObject attraction = attractions.getJSONObject(i);
                
                String name = attraction.getString("name");
                int day = attraction.getInt("day");
                int order = attraction.getInt("order");
                String type = attraction.optString("type", "景点");
                String transport = attraction.optString("transport", "步行");
                String poi_id=attraction.getString("poi_id");
                double latitude=attraction.getDouble("latitude");
                double longitude=attraction.getDouble("longitude");
                String address=attraction.getString("address");
                String tel=attraction.getString("tel");
                String type_desc=attraction.getString("type_desc");
                Long uid=dbHelper.addOrGetSite(poi_id,name,latitude,longitude,address,"",tel,"",type_desc,"");
                                // 创建新的ItineraryAttraction对象
                ItineraryAttraction item = new ItineraryAttraction(
                    itineraryId,
                    uid, // 景点ID可能需要从其他地方获取
                    day,
                    order,
                    name,
                    transport,
                    type
                );
                
                // 如果有推荐理由，设置为AI推荐
                if (attraction.has("reason") && !attraction.optString("reason").isEmpty()) {
                    item.setAiRecommended(true);
                    item.setAiRecommendReason(attraction.optString("reason"));
                }
                
                optimizedAttractions.add(item);
            }
            
            // 使用现有适配器更新数据
            itineraryDetailAdapter.updateAttractions(optimizedAttractions);
            
            // 显示保存按钮
            saveButton.setVisibility(View.VISIBLE);
            
        } catch (JSONException e) {
            Log.e(TAG, "解析优化行程数据出错", e);
        }
    }
    
    // 保存优化后的行程
    private void saveOptimizedItinerary() {
        executorService.execute(() -> {
            final boolean success = aiService.saveOptimizedItinerary(itineraryId, dbHelper);
            
            mainHandler.post(() -> {
                if (success) {
                    Toast.makeText(this, "行程保存成功！", Toast.LENGTH_SHORT).show();
                    // 重新加载行程数据
                    loadItineraryData();
                    // 通知AI用户已保存行程
                    sendMessage("我已保存了优化后的行程");
                } else {
                    Toast.makeText(this, "保存行程失败，请重试", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onSelectClick(RecommendedRestaurant restaurant) {
        executorService.execute(() -> {
            try {
                // 保存原始行程内容
                String originalName = "";
                
                // 获取日期和顺序
                int day = 0;
                int order = 0;
                
                // 从restaurant对象的原始数据中获取day和order
                if (restaurant.getOriginalData() != null) {
                    day = restaurant.getOriginalData().optInt("day", 0);
                    order = restaurant.getOriginalData().optInt("order", 0);
                    
                    // 根据日子和序号获取被替换的景点名称
                    for (ItineraryAttraction attraction : itineraryAttractions) {
                        if (attraction.getDayNumber() == day && attraction.getVisitOrder() == order) {
                            originalName = attraction.getAttractionName();
                            break;
                        }
                    }
                }
                
                // 使用RestaurantRecommendService确认选择
                RestaurantRecommendService service = new RestaurantRecommendService();
                boolean success = service.confirmRestaurantSelection(
                    restaurant, 
                    itineraryId, 
                    day, 
                    order, 
                    dbHelper
                );

                final String finalOriginalName = originalName;
                mainHandler.post(() -> {
                    if (success) {
                        // 添加成功消息
                        String successMessage;
                        if (!finalOriginalName.isEmpty()) {
                            successMessage = String.format("已成功替换",
                                    finalOriginalName, restaurant.getName());
                        } else {
                            successMessage = String.format("已添加成功到您的行程 ✓",
                                    restaurant.getName());
                        }
                        addMessage(successMessage, false);
                        
                        // 隐藏推荐列表
                        recommendationsRecyclerView.setVisibility(View.GONE);
                        
                        // 重新加载行程数据
                        loadItineraryData();
                        
                        // 标记有变更
                        hasChanges = true;
                    } else {
                        // 添加失败消息
                        addMessage("无法更新行程，请重试。", false);
                    }
                });
                
                executorService.execute(() -> {
                    try {
                        // 延迟一下等待数据库刷新
                        Thread.sleep(500);
                        
                        // 获取新添加的餐厅景点ID
                        ArrayList<ItineraryAttraction> updatedAttractions = 
                            dbHelper.getItineraryAttractions(itineraryId);
                        
                        
                    } catch (Exception e) {
                        Log.e(TAG, "保存AI推荐记录失败", e);
                    }
                });
                
                recommendationsRecyclerView.setVisibility(View.GONE);
            } catch (Exception e) {
                mainHandler.post(() -> {
                    // 添加错误消息
                    addMessage("更新行程时出错: " + e.getMessage(), false);
                });
            }
        });
    }

    @Override
    public void onRefreshClick() {
        RecyclerView.Adapter<?> currentAdapter = recommendationsRecyclerView.getAdapter();
        
        // 记录当前适配器类型
        Log.d(TAG, "刷新按钮点击，当前适配器类型: " + 
              (currentAdapter instanceof RestaurantRecommendationAdapter ? "餐厅推荐" : 
               currentAdapter instanceof POIRecommendationAdapter ? "POI推荐" : "未知"));
        
        // 根据当前适配器类型提供不同的消息
        if (currentAdapter instanceof RestaurantRecommendationAdapter) {
            // 重新获取餐厅推荐
            String refreshMessage = "请告诉我您的更多偏好，例如：\n"
                    + "- 您的预算区间\n"
                    + "- 您想吃的类型\n"
                    + "- 您想去的景点评分\n\n"
                    + "这样我可以为您提供更符合需求的推荐。";
            addMessage(refreshMessage, false);
            recommendationsRecyclerView.setVisibility(View.GONE);
        } else if (currentAdapter instanceof POIRecommendationAdapter) {
            // 对于POI推荐，直接显示提示消息，不调用后端
            String refreshMessage = "请告诉我您的更多偏好，例如：\n"
                    + "- 想去更文艺的地方\n"
                    + "- 有适合小孩子的景点吗\n"
                    + "- 想去人少一点的地方\n"
                    + "- 想找评分高于4.5的景点\n\n"
                    + "这样我可以为您提供更符合需求的推荐。";
            addMessage(refreshMessage, false);
            
            // 隐藏推荐列表
            recommendationsRecyclerView.setVisibility(View.GONE);
        } else {
            // 默认处理
            Log.w(TAG, "当前没有活跃的推荐适配器");
            sendMessage("请告诉我您想要什么样的推荐");
        }
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
    
    @Override
    public void onItemClick(ItineraryAttraction attraction) {
        // 处理行程项目点击
        {
            String reasonMessage = attraction.getType();
            addMessage(reasonMessage, false);
        }
    }

    @Override
    public void onSelectClick(RecommendedPOI poi) {
        executorService.execute(() -> {
            try {
                // 保存原始行程内容
                String originalName = "";
                
                // 获取POI的原始JSON数据
                JSONObject poiData = poi.getOriginalData();
                if (poiData != null) {
                    // 确保有day和order字段
                    int day = poiData.optInt("day", 1);
                    int order = poiData.optInt("order", 1);
                    Log.d(TAG, "选择POI - day: " + day + ", order: " + order);
                    
                    for (ItineraryAttraction attraction : itineraryAttractions) {
                        if (attraction.getDayNumber() == day && attraction.getVisitOrder() == order) {
                            originalName = attraction.getAttractionName();
                            break;
                        }
                    }
                    
                    boolean success = aiService.updateItineraryWithPOIRecommendation(
                        itineraryId, 
                        poiData,
                        dbHelper
                    );

                    final String finalOriginalName = originalName;
                    
                    mainHandler.post(() -> {
                        if (success) {
                            // 重新加载行程数据
                            loadItineraryData();
                            
                            // 添加一条AI消息，确认修改
                            String aiMessage = String.format(
                                "已将 Day %d 的「%s」修改为「%s」",
                                day,
                                finalOriginalName.isEmpty() ? "原项目" : finalOriginalName,
                                poi.getName()
                            );
                            addMessage(aiMessage, false);
                            
                            // 保存推荐理由到数据库
                            executorService.execute(() -> {
                                try {
                                    // 延迟一下等待数据库刷新
                                    Thread.sleep(500);
                                    // 获取新添加的景点ID
                                    ArrayList<ItineraryAttraction> updatedAttractions = 
                                        dbHelper.getItineraryAttractions(itineraryId);
                                } catch (Exception e) {
                                    Log.e(TAG, "保存AI推荐记录失败", e);
                                }
                            });
                            
                            // 向后端发送确认选择的消息
                            sendMessage("我选择了景点：" + poi.getName());
                            
                            recommendationsRecyclerView.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(this, "更新行程失败: 请检查景点信息是否完整", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mainHandler.post(() -> 
                        Toast.makeText(this, "更新行程失败: 缺少景点数据", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Log.e(TAG, "更新行程失败", e);
                    Toast.makeText(this, "更新行程失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    public void onDetailsClick(RecommendedPOI poi) {
        // 显示景点详细信息
        StringBuilder details = new StringBuilder();
        details.append(String.format("景点：%s\n", poi.getName()));
        
        // 添加类型
        details.append(String.format("类型：%s\n", poi.getSimpleType()));
        
        // 添加评分
        details.append(String.format("评分：%.1f\n", poi.getRating()));
        
        // 添加距离
        if (poi.getDistance() != null && !poi.getDistance().isEmpty()) {
            details.append(String.format("距离：%s米\n", poi.getDistance()));
        }
        
        // 添加地址
        if (poi.getAddress() != null && !poi.getAddress().isEmpty()) {
            details.append(String.format("地址：%s\n", poi.getAddress()));
        }
        
        // 添加推荐理由
        if (poi.getRecommendationReason() != null && !poi.getRecommendationReason().isEmpty()) {
            details.append(String.format("\n推荐理由：%s", poi.getRecommendationReason()));
        }
        
        addMessage(details.toString(), false);
    }

    private void addMessage(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void handlePOIReplacement(JSONObject data) throws JSONException {
        // 获取推荐列表
        JSONArray recommendations = data.getJSONArray("recommendations");
        List<RecommendedPOI> items = new ArrayList<>();
        
        // 获取替换POI的信息
        JSONObject replaceInfo = data.getJSONObject("replace_poi_info");
        JSONObject targetPoi = replaceInfo.getJSONObject("target_poi");
        
        // 预先提取目标POI的名称，避免在匿名类中处理异常
        final String targetPoiName = targetPoi.getString("name");
        
        // 处理推荐数据
        for (int i = 0; i < recommendations.length(); i++) {
            JSONObject rec = recommendations.getJSONObject(i);
            // 确保每个推荐都包含day和order信息(与被替换景点相同)
            if (!rec.has("day") || !rec.has("order")) {
                int day = targetPoi.getInt("day");
                int order = targetPoi.getInt("order");
                rec.put("day", day);
                rec.put("order", order);
            }
            items.add(new RecommendedPOI(rec));
        }

        // 使用POI替换对话框
        POIReplaceDialog dialog = new POIReplaceDialog(this, items, new POIReplaceDialog.POIReplaceListener() {
            @Override
            public void onPOISelected(RecommendedPOI selectedPOI) {
                // 处理用户选择的替换POI
                processReplacementSelection(selectedPOI, targetPoiName);
            }

            @Override
            public void onRefreshRequest() {
                // 处理"换一批"请求
                String refreshMessage = "请告诉我您的更多偏好，例如：\n"
                        + "- 想去更文艺的地方\n"
                        + "- 有适合小孩子的景点吗\n"
                        + "- 想去人少一点的地方\n"
                        + "- 想找评分高于4.5的景点\n\n"
                        + "这样我可以为您提供更符合需求的推荐。";
                addMessage(refreshMessage, false);
            }
        });

        dialog.show();
        
        Log.d(TAG, "已显示POI替换对话框，数量: " + items.size());
    }

    private void processReplacementSelection(RecommendedPOI selectedPOI, String originalName) {
        executorService.execute(() -> {
            try {
                // 获取POI的原始JSON数据
                JSONObject poiData = selectedPOI.getOriginalData();
                if (poiData != null) {
                    // 获取day和order字段
                    int day = poiData.optInt("day", 1);
                    int order = poiData.optInt("order", 1);
                    Log.d(TAG, "替换POI - 将替换day: " + day + ", order: " + order + "的景点");
                    
                    // 更新到行程中
                    boolean success = aiService.updateItineraryWithPOIRecommendation(
                        itineraryId, 
                        poiData,
                        dbHelper
                    );

                    mainHandler.post(() -> {
                        if (success) {
                            // 重新加载行程数据
                            loadItineraryData();
                            
                            // 添加一条AI消息，确认替换
                            String aiMessage = String.format(
                                "已将第%d天的「%s」替换为「%s」✓",
                                day,
                                originalName,
                                selectedPOI.getName()
                            );
                            addMessage(aiMessage, false);
                            
                            // 向AI发送确认选择的消息
                            sendMessage("我已选择替换为" + selectedPOI.getName());
                        } else {
                            Toast.makeText(this, "替换景点失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mainHandler.post(() -> 
                        Toast.makeText(this, "替换景点失败: 缺少景点数据", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Log.e(TAG, "替换景点失败", e);
                    Toast.makeText(this, "替换景点失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 页面返回时重新加载行程数据，确保显示最新的数据
        loadItineraryData();
    }
} 