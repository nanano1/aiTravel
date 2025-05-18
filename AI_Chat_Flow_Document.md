# AI聊天功能调用流程文档

## 1. 功能概述

AI聊天功能是本应用的核心特色，允许用户通过自然语言与AI助手交互，获取行程规划建议、景点和餐厅推荐等。整个功能由前端的`AIChatActivity`和后端的Flask服务共同实现，通过REST API进行通信。

## 2. 数据流程图

```
用户输入 → AIChatActivity → AIService.getAIResponse() → 后端API(/chat) → 处理回复
    ↓                                                          ↓
显示聊天记录 ← 更新UI ← 解析响应 ← 接收JSON响应 ← AI生成回复
    ↓
处理结构化数据(如推荐) ← 检测data_type → 显示推荐卡片 → 用户选择 → 更新行程
```

## 3. 前端实现 (Android)

### 3.1 主要组件

- **AIChatActivity.java**: 聊天界面UI和交互逻辑
- **AIService.java**: 与后端API通信的服务类
- **RestaurantRecommendService.java**: 处理餐厅推荐的专用服务

### 3.2 关键方法

#### 3.2.1 消息发送流程

1. **发送用户消息**:
```java
private void sendMessage(final String message) {
    addMessage(message, true);  // 添加用户消息到UI
    messageInput.setText("");   // 清空输入框
    
    // 在后台线程执行网络请求
    executorService.execute(() -> {
        try {
            // 隐藏之前的推荐
            mainHandler.post(() -> {
                recommendationsRecyclerView.setVisibility(View.GONE);
                // 清除当前适配器的数据
                if (recommendationsAdapter != null) {
                    recommendationsAdapter.updateRecommendations(new ArrayList<>());
                }
                // 显示处理中提示
                Toast.makeText(AIChatActivity.this, "正在处理您的请求...", Toast.LENGTH_SHORT).show();
            });

            // 调用AI服务获取响应
            AIService.AIResponseData responseData = aiService.getAIResponse(message, itineraryId, dbHelper);
            
            // 在主线程更新UI
            mainHandler.post(() -> {
                // 添加AI文本响应
                addMessage(responseData.getCleanText(), false);
                
                // 处理结构化数据
                if (responseData.hasStructuredData()) {
                    processStructuredData(responseData);
                }
            });
        } catch (Exception e) {
            // 错误处理
            mainHandler.post(() -> {
                Log.e(TAG, "发生错误", e);
                Toast.makeText(AIChatActivity.this, "错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
                addMessage("抱歉，发生错误: " + e.getMessage(), false);
            });
        }
    });
}
```

2. **AIService.getAIResponse 方法**:
```java
public AIResponseData getAIResponse(String message, long itineraryId, DatabaseHelper dbHelper) throws Exception {
    // 获取行程数据
    Itinerary itinerary = dbHelper.getItineraryById(itineraryId);
    ArrayList<ItineraryAttraction> attractions = dbHelper.getItineraryAttractions(itineraryId);
    
    // 构建请求体
    JSONObject requestBody = new JSONObject();
    requestBody.put("message", message);
    
    // 添加行程信息到请求
    JSONObject itineraryData = new JSONObject();
    itineraryData.put("itinerary_id", itineraryId);
    itineraryData.put("title", itinerary.getTittle());
    itineraryData.put("location", itinerary.getLocation());
    itineraryData.put("days", itinerary.getDays());
    
    // 添加行程中的景点信息
    JSONArray attractionsArray = new JSONArray();
    for (ItineraryAttraction attraction : attractions) {
        JSONObject attractionObj = new JSONObject();
        // 设置景点信息...
        attractionsArray.put(attractionObj);
    }
    
    itineraryData.put("attractions", attractionsArray);
    requestBody.put("itinerary_data", itineraryData);
    
    // 发送请求并获取响应
    String response = sendRequest(requestBody);
    
    // 处理响应
    AIResponseData responseData = processAIResponse(response);
    return responseData;
}
```

#### 3.2.2 结构化数据处理

```java
private void processStructuredData(AIService.AIResponseData responseData) {
    try {
        String dataType = responseData.getDataType();
        JSONObject structuredData = responseData.getStructuredData();
        
        if ("restaurant_recommendations".equals(dataType)) {
            handleRestaurantRecommendations(structuredData);
        } else if ("poi_recommendations".equals(dataType)) {
            handlePOIRecommendations(structuredData);
        } else if ("itinerary_update".equals(dataType)) {
            handleItineraryUpdate(structuredData);
        }
    } catch (Exception e) {
        Log.e(TAG, "处理结构化数据失败", e);
    }
}
```

## 4. 后端实现 (Flask)

### 4.1 API端点

#### 4.1.1 `/chat` API

```python
@app.route('/chat', methods=['POST'])
def chat():
    try:
        data = request.get_json()
        message = data.get('message')
        itinerary_data = data.get('itinerary_data')
        
        # 调用AI处理函数
        response = process_message(message, itinerary_data)
        
        # 检查response是否是字典类型（结构化响应）
        if isinstance(response, dict):
            return jsonify({
                'success': True,
                'response': response.get('message', '处理完成'),
                'data': response.get('data'),
                'data_type': response.get('data_type')
            })
        else:
            # 处理字符串响应
            return jsonify({
                'success': True,
                'response': response,
                'data': None,
                'data_type': None
            })
            
    except Exception as e:
        logger.error(f"处理请求时出错: {str(e)}", exc_info=True)
        return jsonify({'error': str(e)}), 500
```

### 4.2 响应格式

#### 4.2.1 普通文本响应

```json
{
  "success": true,
  "response": "这是AI的文本回复",
  "data": null,
  "data_type": null
}
```

#### 4.2.2 结构化响应（餐厅推荐示例）

```json
{
  "success": true,
  "response": "我为您找到了几家不错的餐厅，您可以选择其中一家添加到行程中。",
  "data": {
    "day_info": {
      "day": 2,
      "order": 1
    },
    "recommendations": [
      {
        "uid": "d6960855f9573e9d744ff15b",
        "name": "示例餐厅",
        "label": "美食;快餐店",
        "latitude": 30.548506,
        "longitude": 114.364165,
        "address": "示例地址",
        "telephone": "(027)12345678",
        "overall_rating": "4.5",
        "reason": "这家餐厅靠近您的下一个景点，评分很高"
      }
    ]
  },
  "data_type": "restaurant_recommendations"
}
```

## 5. 推荐数据格式

### 5.1 数据类型

系统支持的结构化数据类型包括：

- **restaurant_recommendations**: 餐厅推荐
- **poi_recommendations**: 景点推荐
- **itinerary_update**: 行程更新建议

### 5.2 数据结构

每种类型的数据结构如下：

#### 5.2.1 餐厅推荐

```json
{
  "data_type": "restaurant_recommendations",
  "day_info": {
    "day": 1,
    "order": 2
  },
  "recommendations": [
    {
      "uid": "餐厅ID",
      "name": "餐厅名称",
      "label": "餐厅类型",
      "latitude": 纬度,
      "longitude": 经度,
      "address": "地址",
      "telephone": "电话",
      "overall_rating": "评分",
      "reason": "推荐理由"
    }
  ]
}
```

#### 5.2.2 景点推荐

```json
{
  "data_type": "poi_recommendations",
  "day_info": {
    "day": 1,
    "order": 3
  },
  "recommendations": [
    {
      "uid": "景点ID",
      "name": "景点名称",
      "label": "景点类型",
      "latitude": 纬度,
      "longitude": 经度,
      "address": "地址",
      "overall_rating": "评分",
      "reason": "推荐理由"
    }
  ]
}
```

## 6. 使用示例

### 6.1 用户对话示例

- 用户: "帮我找一家今天晚餐的餐厅"
- AI: "好的，根据您的行程和位置，我为您推荐以下几家餐厅..." (同时显示餐厅推荐卡片)
- 用户: (点击其中一家餐厅的"选择"按钮)
- AI: "已将[餐厅名称]添加到您的行程中"

### 6.2 代码调用示例

```java
// 在AIChatActivity中
// 1. 用户点击发送按钮
sendButton.setOnClickListener(v -> {
    String message = messageInput.getText().toString().trim();
    if (!message.isEmpty()) {
        sendMessage(message);
    }
});

// 2. 用户选择推荐的餐厅
@Override
public void onSelectClick(RecommendedRestaurant restaurant) {
    selectRestaurant(restaurant);
}

private void selectRestaurant(RecommendedRestaurant restaurant) {
    try {
        // 构建JSON数据
        JSONObject confirmData = new JSONObject();
        confirmData.put("itinerary_id", itineraryId);
        
        // 从AI响应中获取day_info
        JSONObject dayInfo = structuredData.getJSONObject("day_info");
        confirmData.put("day_info", dayInfo);
        
        // 添加餐厅信息
        confirmData.put("restaurant", restaurant.toJson());
        
        // 调用服务确认选择
        boolean success = restaurantRecommendService.confirmRestaurantSelection(confirmData, this);
        
        if (success) {
            // 处理成功
            recommendationsRecyclerView.setVisibility(View.GONE);
            addMessage("已将 " + restaurant.getName() + " 添加到您的行程中。", false);
            hasUnsavedChanges = true;
            loadItineraryData();  // 刷新行程显示
        } else {
            // 处理失败
            addMessage("添加餐厅失败，请重试。", false);
        }
    } catch (Exception e) {
        Log.e(TAG, "选择餐厅时出错", e);
        addMessage("发生错误: " + e.getMessage(), false);
    }
}
```

## 7. 注意事项与常见问题

1. **网络延迟处理**：确保添加适当的加载指示器和错误处理
2. **API版本兼容**：根据API版本的不同，可能需要额外的兼容逻辑
3. **UI状态管理**：确保在获取新推荐时清除旧推荐的UI显示
4. **错误恢复**：当AI服务暂时不可用时，提供友好的提示信息
5. **数据持久化**：用户与AI的对话历史目前不会持久化存储，这可能是未来的改进点 