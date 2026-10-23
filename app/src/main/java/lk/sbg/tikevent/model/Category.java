package lk.sbg.tikevent.model;

import com.google.firebase.firestore.PropertyName;

public class Category {
    private String name;
    private String imageUrl;
    private int index;

    public Category() {
    }

    public Category(String name, String imageUrl, int index) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.index = index;
    }

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("index")
    public int getIndex() {
        return index;
    }

    @PropertyName("index")
    public void setIndex(int index) {
        this.index = index;
    }
}