package com.example.trave.Domains;

public class ChatMessage {
    private String message;
    private boolean fromUser;
    private boolean isRecommendation;
    private String itemId;
    private String title;
    private String distance;
    private String reason;
    private float rating;
    private String type; // "restaurant" or "attraction"

    // 普通消息构造函数
    public ChatMessage(String message, boolean fromUser) {
        this.message = message;
        this.fromUser = fromUser;
        this.isRecommendation = false;
    }

    // 推荐卡片构造函数
    public ChatMessage(String itemId, String title, String distance, String reason, float rating, String type) {
        this.itemId = itemId;
        this.title = title;
        this.distance = distance;
        this.reason = reason;
        this.rating = rating;
        this.type = type;
        this.isRecommendation = true;
        this.fromUser = false;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFromUser() {
        return fromUser;
    }

    public void setFromUser(boolean fromUser) {
        this.fromUser = fromUser;
    }

    public boolean isRecommendation() {
        return isRecommendation;
    }

    public String getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public String getDistance() {
        return distance;
    }

    public String getReason() {
        return reason;
    }

    public float getRating() {
        return rating;
    }

    public String getType() {
        return type;
    }
} 