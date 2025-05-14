package com.example.trave.Domains;

import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Log;

/**
 * 推荐景点(POI)模型类
 */
public class RecommendedPOI {
    private static final String TAG = "RecommendedPOI";
    private String name;
    private String type;
    private double rating;
    private double utilityScore;
    private String recommendationReason;
    private double[] coordinates;
    private String address;
    private String imageUrl;
    private String distance;
    private JSONObject originalData; // 保存原始JSON数据

    public RecommendedPOI(JSONObject jsonObject) {
        try {
            this.originalData = jsonObject;
            
            // 基本信息
            this.name = jsonObject.optString("name", "未知景点");
            this.type = jsonObject.optString("type", "未知类型");
            this.rating = jsonObject.optDouble("rating", 4.0);
            this.utilityScore = jsonObject.optDouble("utility_score", 0.0);
            this.recommendationReason = jsonObject.optString("recommendation_reason", "");
            if (this.recommendationReason.isEmpty()) {
                this.recommendationReason = jsonObject.optString("reason", "");
            }
            this.address = jsonObject.optString("address", "未知地址");
            this.imageUrl = jsonObject.optString("image_url", "");
            this.distance = jsonObject.optString("distance", "");
            
            // 坐标处理
            if (jsonObject.has("coordinates")) {
                JSONArray coords = jsonObject.getJSONArray("coordinates");
                if (coords.length() >= 2) {
                    this.coordinates = new double[2];
                    this.coordinates[0] = coords.getDouble(0);
                    this.coordinates[1] = coords.getDouble(1);
                }
            }
            
            Log.d(TAG, "成功解析POI数据: " + name);
            
        } catch (Exception e) {
            Log.e(TAG, "解析POI数据失败: " + e.getMessage());
            // 使用默认值
            this.name = "未知景点";
            this.type = "未知类型";
            this.rating = 4.0;
            this.utilityScore = 0.0;
            this.recommendationReason = "";
            this.address = "未知地址";
            this.imageUrl = "";
            this.distance = "";
            this.coordinates = new double[]{0, 0};
        }
    }

    // 获取原始JSON数据
    public JSONObject getOriginalData() {
        return originalData;
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public double getUtilityScore() {
        return utilityScore;
    }

    public void setUtilityScore(double utilityScore) {
        this.utilityScore = utilityScore;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
    
    // 获取纬度
    public double getLatitude() {
        return coordinates != null && coordinates.length >= 2 ? coordinates[0] : 0;
    }
    
    // 获取经度
    public double getLongitude() {
        return coordinates != null && coordinates.length >= 2 ? coordinates[1] : 0;
    }
    
    // 获取景点类型的简化名称（去掉复杂分类）
    public String getSimpleType() {
        if (type == null || type.isEmpty()) {
            return "景点";
        }
        
        // 从类型字符串中提取第一个分类
        String[] typeParts = type.split("\\|");
        String firstType = typeParts[0];
        
        // 进一步处理，获取最后一个子分类
        String[] subTypes = firstType.split(";");
        if (subTypes.length > 0) {
            return subTypes[subTypes.length - 1];
        }
        
        return "景点";
    }
} 