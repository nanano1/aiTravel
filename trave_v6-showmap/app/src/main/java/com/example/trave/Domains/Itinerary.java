package com.example.trave.Domains;

import android.os.Parcel;
import android.os.Parcelable;

public class Itinerary implements Parcelable {
    private long id;
    private String title;
    private String location;
    private String pic;
    private int days;
    private long userId;  // 新增字段：用户ID
    private int status;
    public Itinerary(long id,String title, String location, String pic, int days) {
        this.id=id;
        this.title = title;
        this.location = location;
        this.pic = pic;
        this.days = days;
    }
    public Itinerary(long id, String title, String location, String pic, int days, long userId, int status) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.pic = pic;
        this.days=days;
        this.userId = userId;  // 设置用户ID
        this.status = status;  // 设置发布状态
    }
    public Itinerary( String title, String location, String pic,  long userId, int status) {
        this.title = title;
        this.location = location;
        this.pic = pic;
        this.userId = userId;  // 设置用户ID
        this.status = status;  // 设置发布状态
    }
    public Itinerary(String title, String location, String pic, int days, long userId, int status) {
        this.title = title;
        this.location = location;
        this.pic = pic;
        this.days = days;
        this.userId = userId;
        this.status = status;
    }

    public Itinerary(String title, String location, String pic, int days) {

        this.title = title;
        this.location = location;
        this.pic = pic;
        this.days = days;
    }
    public Itinerary(String title, String location, String pic) {

        this.title = title;
        this.location = location;
        this.pic = pic;
    }
    public Itinerary(String title, String location) {

        this.title = title;
        this.location = location;
    }

    protected Itinerary(Parcel in) {
        id = in.readLong();
        title = in.readString();
        location = in.readString();
        pic = in.readString();
        days = in.readInt();
    }

    public void setId(long id) {
        this.id = id;
    }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public void setTitle(String title) {
        this.title = title;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public long getId() {
        return id;
    }

    public String getTittle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getPic() {
        return pic;
    }

    public int getDays() {
        return days;
    }

    public void setTittle(String title) {
        this.title = title;
    }

    public void setDays(int days) {

        this.days = days;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }
    public static final Creator<Itinerary> CREATOR = new Creator<Itinerary>() {
        @Override
        public Itinerary createFromParcel(Parcel in) {
            return new Itinerary(in);
        }
        @Override
        public Itinerary[] newArray(int size) {
            return new Itinerary[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(location);
        dest.writeString(pic);
        dest.writeInt(days);
    }
}
