package com.example.trave.Domains;

import android.os.Parcel;
import android.os.Parcelable;

public class ItineraryAttraction implements Parcelable {
    private long id;
    private long itineraryId;
    private long siteId;
    private int dayNumber;
    private int visitOrder;
    private String attractionName;
    private String transport;
    private String type;

    public ItineraryAttraction(long itineraryId, long siteId, int dayNumber, int visitOrder, String attractionName, String transport, String type) {
        this.itineraryId = itineraryId;
        this.siteId = siteId;
        this.dayNumber = dayNumber;
        this.visitOrder = visitOrder;
        this.attractionName = attractionName;
        this.transport = transport;
        this.type = type;
    }

    public ItineraryAttraction(long itineraryId, long siteId, int dayNumber, int visitOrder, String attractionName, String transport) {
        this.itineraryId = itineraryId;
        this.siteId = siteId;
        this.dayNumber = dayNumber;
        this.visitOrder = visitOrder;
        this.attractionName = attractionName;
        this.transport = transport;
        this.type = "景点";
    }

    public ItineraryAttraction(int dayNumber, int visitOrder, String attractionName, String transport) {
        this.dayNumber = dayNumber;
        this.visitOrder = visitOrder;
        this.attractionName = attractionName;
        this.transport = transport;
        this.type = "景点";
    }

    public ItineraryAttraction(long siteId, int dayNumber, int visitOrder, String attractionName, String transport) {
        this.siteId = siteId;
        this.dayNumber = dayNumber;
        this.visitOrder = visitOrder;
        this.attractionName = attractionName;
        this.transport = transport;
        this.type = "景点";
    }

    public long getSiteId() {
        return siteId;
    }

    public void setSiteId(long siteId) {
        this.siteId = siteId;
    }

    protected ItineraryAttraction(Parcel in) {
        id = in.readLong();
        itineraryId = in.readLong();
        siteId = in.readLong();
        dayNumber = in.readInt();
        visitOrder = in.readInt();
        attractionName = in.readString();
        transport = in.readString();
        type = in.readString();
    }

    public static final Creator<ItineraryAttraction> CREATOR = new Creator<ItineraryAttraction>() {
        @Override
        public ItineraryAttraction createFromParcel(Parcel in) {
            return new ItineraryAttraction(in);
        }

        @Override
        public ItineraryAttraction[] newArray(int size) {
            return new ItineraryAttraction[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getItineraryId() {
        return itineraryId;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public int getVisitOrder() {
        return visitOrder;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public String getTransport() {
        return transport;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setItineraryId(long itineraryId) {
        this.itineraryId = itineraryId;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public void setVisitOrder(int visitOrder) {
        this.visitOrder = visitOrder;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(itineraryId);
        dest.writeLong(siteId);
        dest.writeInt(dayNumber);
        dest.writeInt(visitOrder);
        dest.writeString(attractionName);
        dest.writeString(transport);
        dest.writeString(type);
    }
}
