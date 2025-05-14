package com.example.trave.Domains;

import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

/**
 * 推荐景点POI模型类
 */
public class RecommendedPOI {
    private static final String TAG = "RecommendedPOI";
    
    private String id;
    private String name;
    private double rating;
    private String distance;
    private String type;
    private String simpleType;
    private String address;
    private double lat;
    private double lng;
    private double[] coordinates;
    private String recommendationReason;
    private String openingHours;
    private String telephone;
    private JSONObject originalData; // 保存原始JSON数据

    public RecommendedPOI(JSONObject jsonObject) {
        try {
            this.originalData = jsonObject;
            
            // 基本信息
            this.id = jsonObject.optString("uid", jsonObject.optString("id", ""));
            this.name = jsonObject.optString("name", "未知景点");
            this.type = jsonObject.optString("type", "景点");
            this.address = jsonObject.optString("address", "未知地址");
            this.recommendationReason = jsonObject.optString("recommendation_reason", "");
            this.telephone = jsonObject.optString("tel", "无电话");
            this.openingHours = jsonObject.optString("opentime_week", "无营业时间");
            this.lat=jsonObject.optDouble("lat",0);
            this.lng=jsonObject.optDouble("lng",0);
            // 提取评分
            String ratingStr = jsonObject.optString("rating", "0");
            try {
                this.rating = Double.parseDouble(ratingStr);
            } catch (NumberFormatException e) {
                this.rating = 0;
            }
            
            // 提取距离
            this.distance = jsonObject.optString("distance", "");
            
//            // 提取坐标 - 简化处理，直接从lat和lng字段获取
//            if (jsonObject.has("lat") && jsonObject.has("lng")) {
//                this.coordinates = new double[2];
//                this.coordinates[0] = jsonObject.optDouble("lat", 0); // 纬度
//                this.coordinates[1] = jsonObject.optDouble("lng", 0); // 经度
//            } else {
//                this.coordinates = null;
//            }
            
            // 处理类型简化
            this.simpleType = simplifyType(this.type);
            
        } catch (Exception e) {
            Log.e(TAG, "解析POI数据失败: " + e.getMessage());
            // 使用默认值
            this.id = "";
            this.name = "未知景点";
            this.rating = 4.0;
            this.distance = "未知";
            this.type = "景点";
            this.simpleType = "景点";
            this.address = "未知地址";
            this.coordinates = null;
            this.recommendationReason = "";
            this.openingHours = "无营业时间";
            this.telephone = "无电话";
        }
    }

    // 简化类型名称
    private String simplifyType(String fullType) {
        if (fullType == null || fullType.isEmpty()) {
            return "景点";
        }
        
        // 如果包含分号，取第一部分
        if (fullType.contains(";")) {
            String[] parts = fullType.split(";");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                return parts[0];
            }
        }
        
        return fullType;
    }

    // Getter方法
    public String getId() {
        return id;
    }
    public double getLat() {
        return lat;
    }
    public double getLng() {
        return lng;
    }
    public String getName() {
        return name;
    }

    public double getRating() {
        return rating;
    }

    public String getDistance() {
        return distance;
    }

    public String getType() {
        return type;
    }

    public String getSimpleType() {
        return simpleType;
    }

    public String getAddress() {
        return address;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public String getTelephone() {
        return telephone;
    }

    public JSONObject getOriginalData() {
        return originalData;
    }

    // 重写equals方法，用于在列表中查找和比较对象
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        RecommendedPOI other = (RecommendedPOI) obj;
        
        // 如果ID非空且相同，则认为是同一个POI
        if (id != null && id.equals(other.id)) {
            return true;
        }
        
        // 如果名称和地址都相同，也认为是同一个POI
        return name.equals(other.name) && address.equals(other.address);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + name.hashCode();
        result = 31 * result + address.hashCode();
        return result;
    }
} 