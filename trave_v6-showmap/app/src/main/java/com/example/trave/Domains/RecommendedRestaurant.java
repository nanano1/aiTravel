package com.example.trave.Domains;

import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Log;

/**
 * 推荐餐厅模型类
 */
public class RecommendedRestaurant {
    private static final String TAG = "RecommendedRestaurant";
    private String id;
    private String name;
    private double rating;
    private String distance;
    private String reason;
    private String cuisineType;
    private String imageUrl;
    private String address;
    private double priceLevel;
    private double latitude;
    private double longitude;
    private String telephone;
    private String shopHours;
    private String commentNum;
    private JSONObject originalData; // 保存原始JSON数据

    public RecommendedRestaurant(JSONObject jsonObject) {
        try {
            this.originalData = jsonObject;
            
            // 基本信息
            this.id = jsonObject.optString("uid", "");
            this.name = jsonObject.optString("name", "未知餐厅");
            this.cuisineType = jsonObject.optString("label", "未知菜系");
            this.reason = jsonObject.optString("reason", "");
            this.address = jsonObject.optString("address", "未知地址");
            this.telephone = jsonObject.optString("telephone", "无电话");
            
            // 评分和坐标
            this.rating = jsonObject.optDouble("overall_rating", 4.5);
            this.latitude = jsonObject.optDouble("latitude", 0);
            this.longitude = jsonObject.optDouble("longitude", 0);
            
            // 处理res_detail中的信息
            if (jsonObject.has("res_detail") && !jsonObject.isNull("res_detail")) {
                JSONObject resDetail = jsonObject.getJSONObject("res_detail");
                this.priceLevel = resDetail.optDouble("price", 0);
                this.shopHours = resDetail.optString("shop_hours", "暂无营业时间");
                this.commentNum = resDetail.optString("comment_num", "0");
            } else {
                this.priceLevel = 0;
                this.shopHours = "暂无营业时间";
                this.commentNum = "0";
            }
            
            // 设置默认图片URL和距离
            this.imageUrl = "";
            this.distance = "未知";
            
            Log.d(TAG, "成功解析餐厅数据: " + name);
            
        } catch (Exception e) {
            Log.e(TAG, "解析餐厅数据失败: " + e.getMessage());
            // 使用默认值
            this.id = "";
            this.name = "未知餐厅";
            this.rating = 4.5;
            this.distance = "未知";
            this.reason = "";
            this.cuisineType = "未知菜系";
            this.imageUrl = "";
            this.address = "未知地址";
            this.priceLevel = 0;
            this.latitude = 0;
            this.longitude = 0;
            this.telephone = "无电话";
            this.shopHours = "暂无营业时间";
            this.commentNum = "0";
        }
    }

    public RecommendedRestaurant(String id, String name, double rating, String distance, String reason,
                                String cuisineType, String imageUrl, String address,
                                double priceLevel, double latitude, double longitude,
                                String telephone, String shopHours, String commentNum) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.distance = distance;
        this.reason = reason;
        this.cuisineType = cuisineType;
        this.imageUrl = imageUrl;
        this.address = address;
        this.priceLevel = priceLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.telephone = telephone;
        this.shopHours = shopHours;
        this.commentNum = commentNum;
    }

    // 获取原始JSON数据
    public JSONObject getOriginalData() {
        if (originalData == null) {
            // 如果没有原始数据，构建一个新的JSONObject
            try {
                JSONObject data = new JSONObject();
                data.put("uid", id);
                data.put("name", name);
                data.put("overall_rating", rating);
                data.put("distance", distance);
                data.put("reason", reason);
                data.put("label", cuisineType);
                data.put("address", address);
                data.put("telephone", telephone);
                
                // 添加res_detail信息
                JSONObject resDetail = new JSONObject();
                resDetail.put("price", priceLevel);
                resDetail.put("shop_hours", shopHours);
                resDetail.put("comment_num", commentNum);
                data.put("res_detail", resDetail);
                
                // 添加坐标
                data.put("latitude", latitude);
                data.put("longitude", longitude);
                
                return data;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        return originalData;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(double priceLevel) {
        this.priceLevel = priceLevel;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    
    public String getShopHours() {
        return shopHours;
    }
    
    public void setShopHours(String shopHours) {
        this.shopHours = shopHours;
    }
    
    public String getCommentNum() {
        return commentNum;
    }
    
    public void setCommentNum(String commentNum) {
        this.commentNum = commentNum;
    }
} 