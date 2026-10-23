package lk.sbg.tikevent.model;

public class ManageEvent {
    private String eventId;
    private String name;
    private String location;
    private String price;
    private String date;
    private String time;
    private String imageUrl;

    public ManageEvent() {
    }

    public ManageEvent(String eventId, String name, String location, String price, String date, String time, String imageUrl) {
        this.eventId = eventId;
        this.name = name;
        this.location = location;
        this.price = price;
        this.date = date;
        this.time = time;
        this.imageUrl = imageUrl;
    }

    public String getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getPrice() {
        return price;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}