package lk.sbg.tikevent.model;

public class PurchasedTicket implements java.io.Serializable {
    private String eventId;
    private String eventName;
    private String purchaseAt;
    private String qrUrl;
    private int quantity;
    private String status;
    private String ticketId;
    private String ticketType;
    private long totalPrice;
    private String userId;
    private String eventPlace;
    private String eventStartDate;
    private String eventStartTime;

    public PurchasedTicket() {
    }

    public String getEventId() {
        return eventId != null ? eventId : "";
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName != null ? eventName : "Unknown Event";
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getPurchaseAt() {
        return purchaseAt != null ? purchaseAt : "Just now";
    }

    public void setPurchaseAt(String purchaseAt) {
        this.purchaseAt = purchaseAt;
    }

    public String getQrUrl() {
        return qrUrl != null ? qrUrl : "";
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTicketId() {
        return ticketId != null ? ticketId : "";
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketType() {
        return ticketType != null ? ticketType : "Standard";
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getUserId() {
        return userId != null ? userId : "";
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventPlace() {
        return eventPlace != null ? eventPlace : "Loading location...";
    }

    public void setEventPlace(String eventPlace) {
        this.eventPlace = eventPlace;
    }

    public String getEventStartDate() {
        return eventStartDate != null ? eventStartDate : "";
    }

    public void setEventStartDate(String eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public String getEventStartTime() {
        return eventStartTime != null ? eventStartTime : "";
    }

    public void setEventStartTime(String eventStartTime) {
        this.eventStartTime = eventStartTime;
    }
}