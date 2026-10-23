package lk.sbg.tikevent.model;

public class PopularEvent {
    private String eventId;
    private String eventName;
    private String eventDate;
    private String locationAddress;
    private String imageUrl;
    private long ticketsSold;

    private String category;
    private String price;
    private String time;

    public PopularEvent() {
    }

    public PopularEvent(String eventId, String eventName, String eventDate, String locationAddress, String imageUrl, long ticketsSold, String category, String price, String time) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.locationAddress = locationAddress;
        this.imageUrl = imageUrl;
        this.ticketsSold = ticketsSold;
        this.category = category;
        this.price = price;
        this.time = time;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTicketsSold() {
        return ticketsSold;
    }

    public void setTicketsSold(long ticketsSold) {
        this.ticketsSold = ticketsSold;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}