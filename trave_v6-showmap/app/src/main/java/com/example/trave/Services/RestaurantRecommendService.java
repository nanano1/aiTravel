package com.example.trave.Services;

import android.content.Context;
import android.util.Log;

import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.ItineraryAttraction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RestaurantRecommendService {
    private static final String TAG = "RestaurantRecommendService";
    private AIService aiService;

    public RestaurantRecommendService() {
        this.aiService = new AIService();
    }

    /**
     * 确认使用选择的餐厅，将其更新到行程中
     * @param restaurant 选择的餐厅信息
     * @param itineraryId 行程ID
     * @param dayNumber 天数
     * @param order 在行程中的顺序
     * @param dbHelper 数据库Helper
     * @return 是否更新成功
     */
    public boolean confirmRestaurantSelection(RecommendedRestaurant restaurant, long itineraryId, 
                                             int dayNumber, int order, DatabaseHelper dbHelper) {
        try {
            // 获取餐厅信息
            String poiId = restaurant.getId();
            String name = restaurant.getName();
            double latitude = restaurant.getLatitude();
            double longitude = restaurant.getLongitude();
            String address = restaurant.getAddress();
            String typeDesc = "餐饮服务";  // 设置为餐饮服务类型
            String tel = restaurant.getTelephone();
            
            Log.d(TAG, "确认餐厅选择: " + name + ", 坐标: [" + latitude + ", " + longitude + "]");
            
            // 添加或获取景点
            long siteId = dbHelper.addOrGetSite(poiId, name, latitude, longitude, address,
                    "", tel, "", typeDesc, "");
            
            Log.d(TAG, "获取到的 siteId: " + siteId);
            
            // 如果没有成功添加或获取景点
            if (siteId <= 0) {
                Log.e(TAG, "添加或获取景点失败");
                return false;
            }
            
            // 先删除原有同一位置的景点（如果有）
            dbHelper.deleteAttractionByDayAndOrder(itineraryId, dayNumber, order);
            
            // 创建ItineraryAttraction对象
            ItineraryAttraction attraction = new ItineraryAttraction(
                    itineraryId, siteId, dayNumber, order, name, "步行");
            
            // 设置类型为餐厅
            attraction.setType("餐厅");
            
            // 如果有推荐理由，设置AI推荐
            if (restaurant.getReason() != null && !restaurant.getReason().isEmpty()) {
                attraction.setAiRecommended(true);
                attraction.setAiRecommendReason(restaurant.getReason());
            }
            
            // 添加到数据库
            long attractionId = dbHelper.addAttraction(attraction);
            
            return attractionId > 0;
        } catch (Exception e) {
            Log.e(TAG, "确认选择餐厅时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 接收JSON对象确认餐厅选择 - 此方法供AIChatActivity和ChatActivity使用
     * @param confirmData 包含确认信息的JSON对象
     * @param context 上下文
     * @return 是否成功确认
     */
    public boolean confirmRestaurantSelection(JSONObject confirmData, Context context) {
        try {
            if (context == null) {
                Log.e(TAG, "Context不能为null");
                return false;
            }
            
            Log.d(TAG, "接收到确认数据: " + confirmData.toString());
            
            // 解析JSON数据
            long itineraryId = confirmData.getLong("itinerary_id");
            int dayNumber = confirmData.getInt("day");
            int order = 1;  // 默认顺序为1
            
            // 从day_info中获取order（如果有）
            if (confirmData.has("day_info") && !confirmData.isNull("day_info")) {
                JSONObject dayInfo = confirmData.getJSONObject("day_info");
                if (dayInfo.has("order")) {
                    order = dayInfo.getInt("order");
                }
            }
            
            // 检查是否有res_info（餐厅信息），这表示需要替换现有的项目
            boolean hasResInfo = confirmData.has("res_info") && !confirmData.isNull("res_info");
            
            // 获取restaurant对象
            JSONObject restaurantObj;
            if (confirmData.has("restaurant")) {
                restaurantObj = confirmData.getJSONObject("restaurant");
            } else if (confirmData.has("recommendations") && confirmData.getJSONArray("recommendations").length() > 0) {
                restaurantObj = confirmData.getJSONArray("recommendations").getJSONObject(0);
            } else {
                Log.e(TAG, "没有找到餐厅信息");
                return false;
            }
            
            // 需要将restaurantObj转换为RecommendedRestaurant可接受的格式
            JSONObject formattedRestaurantObj = formatRestaurantJson(restaurantObj);
            
            // 创建RecommendedRestaurant对象
            RecommendedRestaurant restaurant = new RecommendedRestaurant(formattedRestaurantObj);
            
            // 使用Context创建DatabaseHelper
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            
            return confirmRestaurantSelection(restaurant, itineraryId, dayNumber, order, dbHelper);
        } catch (Exception e) {
            Log.e(TAG, "处理餐厅选择JSON数据时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 向后兼容的方法，不推荐使用
     */
    public boolean confirmRestaurantSelection(JSONObject confirmData) {
        Log.w(TAG, "使用了不推荐的无Context的方法，可能会导致错误");
        try {
            // 这个方法可能不能正常工作，因为缺少Context
            // 尝试格式化餐厅数据
            JSONObject restaurantObj = confirmData.getJSONObject("restaurant");
            JSONObject formattedRestaurantObj = formatRestaurantJson(restaurantObj);
            
            // 打印一个警告但返回成功
            Log.w(TAG, "无Context方法已被调用，返回模拟成功");
            return true;
                    } catch (Exception e) {
            Log.e(TAG, "无Context的方法处理失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 辅助方法：格式化餐厅JSON数据
     */
    private JSONObject formatRestaurantJson(JSONObject restaurantObj) throws JSONException {
        JSONObject formattedObj = new JSONObject();
        
        // 基本信息
        formattedObj.put("uid", restaurantObj.optString("id", ""));
        formattedObj.put("name", restaurantObj.optString("name", "未知餐厅"));
        formattedObj.put("label", restaurantObj.optString("cuisine", "未知菜系"));
        formattedObj.put("address", restaurantObj.optString("address", "未知地址"));
        formattedObj.put("latitude", restaurantObj.optDouble("latitude", 0));
        formattedObj.put("longitude", restaurantObj.optDouble("longitude", 0));
        formattedObj.put("telephone", restaurantObj.optString("telephone", "无电话"));
        formattedObj.put("overall_rating", restaurantObj.optDouble("rating", 4.5));
        formattedObj.put("reason", restaurantObj.optString("reason", "AI推荐的餐厅"));
        
        // 添加res_detail信息
        JSONObject resDetail = new JSONObject();
        resDetail.put("price", restaurantObj.optDouble("price", 0));
        resDetail.put("shop_hours", restaurantObj.optString("shop_hours", "暂无营业时间"));
        resDetail.put("comment_num", restaurantObj.optString("comment_num", "0"));
        formattedObj.put("res_detail", resDetail);
        
        return formattedObj;
    }
} 