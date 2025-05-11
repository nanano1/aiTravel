package com.example.trave.Services;

import android.util.Log;

import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.Domains.Itinerary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RestaurantRecommendService {
    private static final String TAG = "RestaurantRecommendService";
    private static final String API_URL = "http://10.0.2.2:5002/recommend_restaurants"; // Flask API端点

    /**
     * 获取餐厅推荐列表
     * @param itineraryId 行程ID
     * @param dayNumber 天数
     * @param mealType 餐食类型（早餐、午餐、晚餐）
     * @return 推荐餐厅列表
     */
    public List<RecommendedRestaurant> getRecommendedRestaurants(long itineraryId, int dayNumber, String mealType) {
        List<RecommendedRestaurant> restaurants = new ArrayList<>();
        
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("itinerary_id", itineraryId);
            requestBody.put("day_number", dayNumber);
            requestBody.put("meal_type", mealType);
            
            // 发送请求
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000); // 10秒超时
            conn.setReadTimeout(10000);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // 读取响应
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "响应代码: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    // 解析响应JSON
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray recommendationsArray = jsonResponse.getJSONArray("recommendations");
                    
                    for (int i = 0; i < recommendationsArray.length(); i++) {
                        JSONObject restaurantJson = recommendationsArray.getJSONObject(i);
                        
                        RecommendedRestaurant restaurant = new RecommendedRestaurant(
                            restaurantJson.getString("id"),
                            restaurantJson.getString("name"),
                            restaurantJson.getDouble("rating"),
                            restaurantJson.getString("distance"),
                            restaurantJson.getString("reason"),
                            restaurantJson.getString("cuisine_type"),
                            restaurantJson.getString("image_url"),
                            restaurantJson.getString("address"),
                            restaurantJson.getDouble("price_level"),
                            restaurantJson.optDouble("latitude", 0),
                            restaurantJson.optDouble("longitude", 0)
                        );
                        
                        restaurants.add(restaurant);
                    }
                }
            } else {
                Log.e(TAG, "服务器返回错误: " + responseCode);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "获取餐厅推荐时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        return restaurants;
    }
    
    /**
     * 获取餐厅推荐
     * @param itineraryId 行程ID
     * @param dayNumber 天数
     * @param mealType 餐食类型
     * @return 推荐餐厅列表
     */
    public List<RecommendedRestaurant> getRecommendations(long itineraryId, int dayNumber, String mealType) throws Exception {
        List<RecommendedRestaurant> restaurants = new ArrayList<>();
        
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("itinerary_id", itineraryId);
        requestBody.put("day_number", dayNumber);
        requestBody.put("meal_type", mealType);
        
        // 发送请求
        URL url = new URL("http://10.0.2.2:5002/recommend_restaurants");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000); // 增加超时时间
        conn.setReadTimeout(15000);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        Log.d(TAG, "获取餐厅推荐响应代码: " + responseCode);
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("获取餐厅推荐失败: HTTP " + responseCode);
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            
            // 解析响应JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            if (!jsonResponse.optBoolean("success", false)) {
                throw new Exception("获取餐厅推荐失败: " + jsonResponse.optString("error", "未知错误"));
            }
            
            JSONArray recommendationsArray = jsonResponse.getJSONArray("recommendations");
            
            for (int i = 0; i < recommendationsArray.length(); i++) {
                JSONObject restaurantJson = recommendationsArray.getJSONObject(i);
                
                // 提取坐标，如果有的话
                double latitude = 0, longitude = 0;
                if (restaurantJson.has("coordinates")) {
                    try {
                        JSONArray coords = restaurantJson.getJSONArray("coordinates");
                        if (coords.length() >= 2) {
                            latitude = coords.getDouble(0);
                            longitude = coords.getDouble(1);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析坐标时出错: " + e.getMessage());
                    }
                }
                
                RecommendedRestaurant restaurant = new RecommendedRestaurant(
                    restaurantJson.optString("uid", "id_" + i),
                    restaurantJson.optString("name", "未知餐厅"),
                    restaurantJson.optDouble("rating", 4.5),
                    restaurantJson.optString("distance", "1km"),
                    restaurantJson.optString("reason", "AI推荐"),
                    restaurantJson.optString("label", "未知菜系"),
                    restaurantJson.optString("image_url", ""),
                    restaurantJson.optString("address", "未知地址"),
                    restaurantJson.optDouble("price", 2),
                    latitude,
                    longitude
                );
                
                restaurants.add(restaurant);
            }
        }
        
        return restaurants;
    }
    
    /**
     * 选择推荐的餐厅，更新行程
     * @param itineraryId 行程ID
     * @param dayNumber 天数
     * @param mealType 餐食类型
     * @param restaurantId 选择的餐厅ID
     * @return 是否更新成功
     */
    public boolean selectRestaurant(long itineraryId, int dayNumber, String mealType, String restaurantId) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("itinerary_id", itineraryId);
            requestBody.put("day_number", dayNumber);
            requestBody.put("meal_type", mealType);
            requestBody.put("restaurant_id", restaurantId);
            
            // 发送请求
            URL url = new URL("http://10.0.2.2:5002/select_restaurant");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // 读取响应
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "选择餐厅响应代码: " + responseCode);
            
            return responseCode == HttpURLConnection.HTTP_OK;
            
        } catch (Exception e) {
            Log.e(TAG, "选择餐厅时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取新的推荐
     * @param itineraryId 行程ID
     * @param dayNumber 天数
     * @param mealType 餐食类型
     * @return 新的推荐餐厅列表
     */
    public List<RecommendedRestaurant> refreshRecommendations(long itineraryId, int dayNumber, String mealType) throws Exception {
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("itinerary_id", itineraryId);
        requestBody.put("day_number", dayNumber);
        requestBody.put("meal_type", mealType);
        
        // 发送请求
        URL url = new URL("http://10.0.2.2:5002/refresh_recommendations");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        Log.d(TAG, "刷新推荐响应代码: " + responseCode);
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("刷新餐厅推荐失败: HTTP " + responseCode);
        }
        
        List<RecommendedRestaurant> restaurants = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            
            // 解析响应JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            if (!jsonResponse.optBoolean("success", false)) {
                throw new Exception("刷新餐厅推荐失败: " + jsonResponse.optString("error", "未知错误"));
            }
            
            JSONArray recommendationsArray = jsonResponse.getJSONArray("recommendations");
            
            for (int i = 0; i < recommendationsArray.length(); i++) {
                JSONObject restaurantJson = recommendationsArray.getJSONObject(i);
                
                // 提取坐标，如果有的话
                double latitude = 0, longitude = 0;
                if (restaurantJson.has("coordinates")) {
                    try {
                        JSONArray coords = restaurantJson.getJSONArray("coordinates");
                        if (coords.length() >= 2) {
                            latitude = coords.getDouble(0);
                            longitude = coords.getDouble(1);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析坐标时出错: " + e.getMessage());
                    }
                }
                
                RecommendedRestaurant restaurant = new RecommendedRestaurant(
                    restaurantJson.optString("uid", "id_" + i),
                    restaurantJson.optString("name", "未知餐厅"),
                    restaurantJson.optDouble("rating", 4.5),
                    restaurantJson.optString("distance", "1km"),
                    restaurantJson.optString("reason", "AI推荐"),
                    restaurantJson.optString("label", "未知菜系"),
                    restaurantJson.optString("image_url", ""),
                    restaurantJson.optString("address", "未知地址"),
                    restaurantJson.optDouble("price", 2),
                    latitude,
                    longitude
                );
                
                restaurants.add(restaurant);
            }
        }
        
        return restaurants;
    }

    /**
     * 确认使用AI推荐的餐厅，将其更新到行程中
     * @param confirmData 包含确认餐厅数据的JSON对象
     * @return 是否更新成功
     */
    public boolean confirmRecommendation(JSONObject confirmData) throws Exception {
        // 发送请求
        URL url = new URL("http://10.0.2.2:5002/confirm_recommendation");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        Log.d(TAG, "确认推荐请求体: " + confirmData.toString());
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = confirmData.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        Log.d(TAG, "确认餐厅推荐响应代码: " + responseCode);
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("确认餐厅选择失败: HTTP " + responseCode);
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            boolean success = jsonResponse.optBoolean("success", false);
            
            if (!success) {
                String errorMsg = jsonResponse.optString("error", "未知错误");
                throw new Exception("确认餐厅选择失败: " + errorMsg);
            }
            
            return true;
        }
    }
} 