package lk.sbg.tikevent.model;

public class OtherEvent {
    private String category;
    private String eventName;
    private String place;
    private String price;
    private String date;
    private String time;
    private String imageUrl;
    private String documentId;

    public OtherEvent() {
    }

    public OtherEvent(String category, String eventName, String place, String price, String date, String time, String imageUrl) {
        this.category = category;
        this.eventName = eventName;
        this.place = place;
        this.price = price;
        this.date = date;
        this.time = time;
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getEventName() {
        return eventName;
    }

    public String getPlace() {
        return place;
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

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}