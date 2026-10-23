package lk.sbg.tikevent.model;

public class Ticket {

    private String eventId;
    private String eventName;
    private String place;
    private String dateTime;
    private String personCount;
    private boolean isSelected = false;

    public Ticket(String eventId, String eventName, String place, String dateTime, String personCount) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.place = place;
        this.dateTime = dateTime;
        this.personCount = personCount;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getPlace() {
        return place;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getPersonCount() {
        return personCount;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}