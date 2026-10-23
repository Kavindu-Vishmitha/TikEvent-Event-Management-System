package lk.sbg.tikevent.model;

public class Attendance {
    private String fullName, nic, eventName, bookingDate, ticketType, personCount, ticketId;
    private String profilePicUrl;
    private boolean isSelected = false;
    private String status;

    public Attendance(String fullName, String nic, String eventName, String bookingDate,
                      String ticketType, String personCount, String profilePicUrl, String ticketId) {
        this.fullName = fullName;
        this.nic = nic;
        this.eventName = eventName;
        this.bookingDate = bookingDate;
        this.ticketType = ticketType;
        this.personCount = personCount;
        this.profilePicUrl = profilePicUrl;
        this.ticketId = ticketId;
        this.status = "Unmark";
    }

    public String getFullName() {
        return fullName;
    }

    public String getNic() {
        return nic;
    }

    public String getEventName() {
        return eventName;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public String getTicketType() {
        return ticketType;
    }

    public String getPersonCount() {
        return personCount;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public String getTicketId() {
        return ticketId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
}