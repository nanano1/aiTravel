package com.example.trave.Services;

import android.util.Log;

import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.Sites;
import com.example.trave.Domains.RecommendedRestaurant;
import com.example.trave.Domains.RecommendedPOI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AIService {
    private static final String TAG = "AIService";
    private static final String API_URL = "http://10.0.2.2:5002/chat";
    private static final String JSON_DATA_PATTERN = "<!--JSON_DATA:(.+?)-->";
    private JSONObject structuredData;  // 添加结构化数据字段

    public static class AIResponseData {
        private String cleanText;          // 纯文本响应
        private JSONObject structuredData; // JSON数据（如果有）
        private String dataType;           // 数据类型（restaurant_recommendations等）

        public AIResponseData(String cleanText, JSONObject structuredData, String dataType) {
            this.cleanText = cleanText;
            this.structuredData = structuredData;
            this.dataType = dataType;
        }

        public String getCleanText() { return cleanText; }
        public JSONObject getStructuredData() { return structuredData; }
        public String getDataType() { return dataType; }
        public boolean hasStructuredData() { return structuredData != null; }
    }

    public AIResponseData getAIResponse(String message, long itineraryId, DatabaseHelper dbHelper) throws Exception {
        Log.d(TAG, "开始准备请求数据: message=" + message + ", itineraryId=" + itineraryId);
        
        // 获取行程和景点数据
        Itinerary itinerary = dbHelper.getItineraryById(itineraryId);
        
        if (itinerary == null) {
            Log.e(TAG, "获取行程失败：itinerary为null");
            throw new Exception("找不到指定的行程");
        }
        
        // 详细打印行程信息
        Log.d(TAG, "行程详细信息 - " +
              "ID: " + itinerary.getId() + 
              ", 标题: " + itinerary.getTittle() + 
              ", 位置: " + itinerary.getLocation() + 
              ", 天数: " + itinerary.getDays());
        
        ArrayList<ItineraryAttraction> attractions = dbHelper.getItineraryAttractions(itineraryId);
        
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("message", message);
        
        // 构建行程数据
        JSONObject itineraryData = new JSONObject();
        itineraryData.put("itinerary_id", itineraryId);
        itineraryData.put("title", itinerary.getTittle());
        itineraryData.put("location", itinerary.getLocation());
        itineraryData.put("days", itinerary.getDays());
        Log.d(TAG, "行程天数: " + itinerary.getDays());
        
        // 构建景点数组
        JSONArray attractionsArray = new JSONArray();
        for (ItineraryAttraction attraction : attractions) {
            JSONObject attractionObj = new JSONObject();
            attractionObj.put("name", attraction.getAttractionName());
            attractionObj.put("day", attraction.getDayNumber());
            attractionObj.put("order", attraction.getVisitOrder());
            attractionObj.put("transport", attraction.getTransport());
            attractionObj.put("type", attraction.getType());
            
            Sites site = dbHelper.getSiteBySiteId(attraction.getSiteId());
            if (site != null) {
                attractionObj.put("poi_id", site.getPoiId());
                attractionObj.put("latitude", site.getLatitude());
                attractionObj.put("longitude", site.getLongitude());
                attractionObj.put("address", site.getAddress());
                attractionObj.put("type_desc", site.getTypeDesc());
            }
            
            attractionsArray.put(attractionObj);
        }
        
        itineraryData.put("attractions", attractionsArray);
        requestBody.put("itinerary_data", itineraryData);
        Log.d(TAG, "行程数据: " + itineraryData.toString());
        // 发送请求并获取响应
        String response = sendRequest(requestBody);
        
        // 处理响应
        AIResponseData responseData = processAIResponse(response);
        this.structuredData = responseData.getStructuredData();  // 保存结构化数据
        return responseData;
    }

    private String sendRequest(JSONObject requestBody) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(100000);
        conn.setReadTimeout(100000);

        // 发送请求
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // 获取响应
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == HttpURLConnection.HTTP_OK ? conn.getInputStream() : conn.getErrorStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("服务器返回错误: " + response.toString());
        }

        return new JSONObject(response.toString()).getString("response");
    }

    private AIResponseData processAIResponse(String response) {
        String cleanText = response;
        JSONObject structuredData = null;
        String dataType = null;

        // 提取JSON数据
        Pattern pattern = Pattern.compile(JSON_DATA_PATTERN);
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            try {
                String jsonStr = matcher.group(1);
                structuredData = new JSONObject(jsonStr);
                dataType = structuredData.optString("data_type");
                
                // 移除JSON数据部分，获取纯文本
                cleanText = response.replaceAll(JSON_DATA_PATTERN, "").trim();
                
                Log.d(TAG, "成功提取结构化数据: " + structuredData.toString());
            } catch (JSONException e) {
                Log.e(TAG, "解析JSON数据失败: " + e.getMessage());
            }
        }

        return new AIResponseData(cleanText, structuredData, dataType);
    }

    // 处理餐厅推荐的更新
    public boolean updateItineraryWithRecommendation(long itineraryId, JSONObject recommendationData, DatabaseHelper dbHelper) {
        try {
            // 获取要替换的景点信息
            int targetDay = recommendationData.getInt("day");
            int targetOrder = recommendationData.getInt("order");
            
            // 获取新餐厅信息
            String poiId = recommendationData.getString("poi_id");
            String name = recommendationData.getString("name");
            double latitude = recommendationData.getDouble("latitude");
            double longitude = recommendationData.getDouble("longitude");
            String address = recommendationData.getString("address");
            String typeDesc = "餐饮服务";  // 设置为餐饮服务类型
            
            // 添加或获取景点
            long siteId = dbHelper.addOrGetSite(poiId, name, latitude, longitude, address,
                    "", "", "", typeDesc, "");
            
            // 创建新的ItineraryAttraction对象
            ItineraryAttraction newAttraction = new ItineraryAttraction(
                    itineraryId, siteId, targetDay, targetOrder, name, "步行");
            newAttraction.setType("餐厅");  // 设置类型为餐厅
            
            // 更新数据库
            // 首先删除原有的景点
            dbHelper.deleteAttractionByDayAndOrder(itineraryId, targetDay, targetOrder);
            // 添加新的景点
            return dbHelper.addAttraction(newAttraction) > 0;
            
        } catch (Exception e) {
            Log.e(TAG, "更新行程失败: " + e.getMessage());
            return false;
        }
    }

    public void clearSession(long itineraryId) {
        // 清除与特定行程相关的会话状态
        this.structuredData = null;  // 清除结构化数据
    }

    public boolean hasStructuredDataOfType(String dataType) {
        return structuredData != null && 
               structuredData.optString("data_type", "").equals(dataType);
    }

    public List<RecommendedRestaurant> getRestaurantRecommendations() {
        if (structuredData == null || !hasStructuredDataOfType("restaurant_recommendations")) {
            return new ArrayList<>();
        }

        List<RecommendedRestaurant> recommendations = new ArrayList<>();
        try {
            JSONArray recommendationsArray = structuredData.getJSONArray("recommendations");
            for (int i = 0; i < recommendationsArray.length(); i++) {
                JSONObject restaurantJson = recommendationsArray.getJSONObject(i);
                recommendations.add(new RecommendedRestaurant(restaurantJson));
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析餐厅推荐数据失败: " + e.getMessage());
        }
        return recommendations;
    }

    public List<RecommendedPOI> getPOIRecommendations() {
        if (structuredData == null || !hasStructuredDataOfType("poi_recommendations")) {
            return new ArrayList<>();
        }

        List<RecommendedPOI> recommendations = new ArrayList<>();
        try {
            JSONArray recommendationsArray = structuredData.getJSONArray("recommendations");
            for (int i = 0; i < recommendationsArray.length(); i++) {
                JSONObject poiJson = recommendationsArray.getJSONObject(i);
                // 确保每个POI都有day和order字段
                if (!poiJson.has("day")) {
                    poiJson.put("day", 1);
                }
                if (!poiJson.has("order")) {
                    poiJson.put("order", i + 1);
                }
                recommendations.add(new RecommendedPOI(poiJson));
            }
            Log.d(TAG, "成功解析" + recommendations.size() + "个POI推荐");
        } catch (JSONException e) {
            Log.e(TAG, "解析POI推荐数据失败: " + e.getMessage());
        }
        return recommendations;
    }
    
    // 处理POI推荐的更新
    public boolean updateItineraryWithPOIRecommendation(long itineraryId, JSONObject recommendationData, DatabaseHelper dbHelper) {
        try {
            // 获取要替换的景点信息
            int targetDay = recommendationData.getInt("day");
            int targetOrder = recommendationData.getInt("order");

            // 获取新景点信息
            String name = recommendationData.getString("name");

            double latitude = recommendationData.getDouble("lat");
            double longitude = recommendationData.getDouble("lng");


            // 获取其他信息，如果不存在则使用默认值
            String address = recommendationData.optString("address", "未知地址");
            String typeDesc = recommendationData.optString("type", "景点");

            // 以下字段在高德API中可能存在，但在当前数据中可能不存在，使用默认值
            String businessArea = recommendationData.optString("business_area", "");
            String tel = recommendationData.optString("tel", "");
            String website = recommendationData.optString("website", "");
            String photos = recommendationData.optString("photos", "");

            // 打印调试信息
            Log.d(TAG, "更新行程景点 - " +
                    "名称: " + name + ", " +
                    "坐标: [" + latitude + ", " + longitude + "], " +
                    "地址: " + address + ", " +
                    "类型: " + typeDesc);

            // 添加或获取景点
            // 使用name作为poiId，因为POI可能没有明确的ID
            long siteId = dbHelper.addOrGetSite(name, name, latitude, longitude, address,
                    businessArea, tel, website, typeDesc, photos);

            if (siteId <= 0) {
                Log.e(TAG, "添加景点失败: " + name);
                return false;
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, "更新行程景点失败: " + e.getMessage(), e);
            return false;
        }
    }
}