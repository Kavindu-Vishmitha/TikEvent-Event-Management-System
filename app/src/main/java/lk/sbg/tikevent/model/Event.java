package lk.sbg.tikevent.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Event {
    public String userId, eventName, category, description, eventDate, startTime, endTime, locationAddress, organizerName, contactNumber, saleEndDate, imageUrl;
    public double latitude, longitude;
    public double stdPrice, vipPrice, vvipPrice;
    public int stdQty, vipQty, vvipQty;

    @ServerTimestamp
    public Date createdAt;

    public Event() {
    }

    public Event(String userId, String eventName, String category, String description, String eventDate, String startTime, String endTime, String locationAddress, double latitude, double longitude, String organizerName, String contactNumber, double stdPrice, int stdQty, double vipPrice, int vipQty, double vvipPrice, int vvipQty, String saleEndDate, String imageUrl) {
        this.userId = userId;
        this.eventName = eventName;
        this.category = category;
        this.description = description;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.organizerName = organizerName;
        this.contactNumber = contactNumber;
        this.stdPrice = stdPrice;
        this.stdQty = stdQty;
        this.vipPrice = vipPrice;
        this.vipQty = vipQty;
        this.vvipPrice = vvipPrice;
        this.vvipQty = vvipQty;
        this.saleEndDate = saleEndDate;
        this.imageUrl = imageUrl;
    }
}