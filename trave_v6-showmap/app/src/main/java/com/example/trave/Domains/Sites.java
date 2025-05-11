package com.example.trave.Domains;

import android.os.Parcel;
import android.os.Parcelable;

public class Sites implements Parcelable {
    private long id;
    private String poiId;
    private String name;
    private double latitude;
    private double longitude;
    private String address;
    private String businessArea;
    private String tel;
    private String website;
    private String typeDesc;
    private String photos;

    public Sites() {
    }

    protected Sites(Parcel in) {
        id = in.readLong();
        poiId = in.readString();
        name = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        address = in.readString();
        businessArea = in.readString();
        tel = in.readString();
        website = in.readString();
        typeDesc = in.readString();
        photos = in.readString();
    }

    public static final Creator<Sites> CREATOR = new Creator<Sites>() {
        @Override
        public Sites createFromParcel(Parcel in) {
            return new Sites(in);
        }

        @Override
        public Sites[] newArray(int size) {
            return new Sites[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPoiId() {
        return poiId;
    }

    public void setPoiId(String poiId) {
        this.poiId = poiId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBusinessArea() {
        return businessArea;
    }

    public void setBusinessArea(String businessArea) {
        this.businessArea = businessArea;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTypeDesc() {
        return typeDesc;
    }

    public void setTypeDesc(String typeDesc) {
        this.typeDesc = typeDesc;
    }

    public String getPhotos() {
        return photos;
    }

    public void setPhotos(String photos) {
        this.photos = photos;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(poiId);
        dest.writeString(name);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(address);
        dest.writeString(businessArea);
        dest.writeString(tel);
        dest.writeString(website);
        dest.writeString(typeDesc);
        dest.writeString(photos);
    }
}