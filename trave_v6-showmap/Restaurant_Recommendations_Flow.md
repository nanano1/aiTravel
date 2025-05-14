# 餐厅推荐功能调用流程文档

## 1. 数据流程概述

当/chat API返回`restaurant_recommendations`类型的数据时，完整的调用流程如下：

```
用户 → AIChatActivity/ChatActivity → AIService → 解析数据 → 显示推荐 → 用户选择 → RestaurantRecommendService → 更新数据库
```

## 2. 详细流程说明

### 2.1 触发餐厅推荐

当用户在聊天界面（AIChatActivity或ChatActivity）中请求餐厅推荐时，系统会：

1. 用户输入消息（如"推荐餐厅"）
2. 将消息发送到/chat API
3. 服务器处理请求并返回响应数据

### 2.2 接收和解析推荐数据

当服务器返回数据时：

1. `AIService.getAIResponse()`方法接收API响应
2. 响应数据被解析，检测到`data_type`为`restaurant_recommendations`
3. 通过`AIService.getRestaurantRecommendations()`方法从结构化数据中提取餐厅信息
4. 创建`RecommendedRestaurant`对象列表

### 2.3 显示推荐列表

1. 检测到推荐类型为餐厅后，更新`currentRecommendations`列表
2. 设置`RestaurantRecommendationAdapter`显示餐厅卡片
3. 在UI上显示横向滚动的餐厅推荐卡片

### 2.4 用户选择推荐

当用户点击推荐卡片上的"选择"按钮时：

1. 触发`onSelectClick(RecommendedRestaurant)`回调
2. 调用`selectRestaurant(restaurant)`方法处理选择
3. 构建包含餐厅信息的JSON数据
4. 调用`RestaurantRecommendService.confirmRestaurantSelection()`方法

### 2.5 确认选择并更新数据库

`RestaurantRecommendService`接收选择请求后：

1. 格式化JSON数据（转换API和内部数据格式）
2. 创建餐厅的`RecommendedRestaurant`对象
3. 检查日期和位置（day和order）
4. 检查是否有`res_info`字段，判断是替换现有项目还是新增
   - 如果有`res_info`，表示替换现有餐厅
   - 如果没有，表示添加新餐厅
5. 调用`dbHelper.addOrGetSite()`将餐厅添加到站点表
6. 如果需要替换，调用`dbHelper.deleteAttractionByDayAndOrder()`删除现有项目
7. 创建`ItineraryAttraction`对象，设置类型为"餐厅"
8. 设置AI推荐标志和推荐理由
9. 调用`dbHelper.addAttraction()`添加到行程中
10. 返回操作结果

### 2.6 完成更新并通知用户

1. 隐藏推荐列表
2. 向聊天界面添加成功消息
3. 标记行程有未保存的更改
4. 重新加载行程数据（如果在AIChatActivity中）

## 3. 数据结构

### 3.1 餐厅推荐API响应格式

```json
{
  "data_type": "restaurant_recommendations",
  "day_info": {
    "day": 2,
    "order": 1
  },
  "recommendations": [
    {
      "uid": "d6960855f9573e9d744ff15b",
      "name": "武汉大学田园食堂",
      "label": "美食;快餐店",
      "latitude": 30.548506,
      "longitude": 114.364165,
      "address": "珞珈山街道珞狮路武汉大学青年教师公寓",
      "telephone": "(027)68756853",
      "overall_rating": "3.8",
      "res_detail": {
        "comment_num": "3",
        "price": "27.0",
        "shop_hours": "10:30-14:00,16:00-20:30"
      },
      "reason": "校园美食，品种丰富，适合学生"
    }
  ]
}
```

### 3.2 关键判断逻辑

1. 如果`data_type`是`restaurant_recommendations`，则处理餐厅推荐
2. 如果有`res_info`字段，表示要替换现有项目；如果没有，则是新增
3. `day_info`中的day和order字段决定餐厅在行程中的位置

## 4. 注意事项

1. 所有`meal_type`相关代码已移除，使用order来确定餐厅的时间段位置
2. 对于具有context参数的方法和没有context参数的方法，优先使用带context参数的方法
3. 确保RecommendedRestaurant对象正确初始化，特别是坐标和联系信息

## 5. 调试信息

重要日志标签和信息：
- LogTag: "RestaurantRecommendService"
- 主要跟踪点：
  - 接收确认数据
  - 餐厅选择过程
  - 添加/获取站点结果
  - 添加到行程的结果 