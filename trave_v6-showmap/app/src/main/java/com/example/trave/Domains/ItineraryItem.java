package com.example.trave.Domains;

public class ItineraryItem {
    private int day;
    private String time;
    private String title;
    private String type;
    private String itemId;

    public ItineraryItem(int day, String time, String title, String type, String itemId) {
        this.day = day;
        this.time = time;
        this.title = title;
        this.type = type;
        this.itemId = itemId;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
} 