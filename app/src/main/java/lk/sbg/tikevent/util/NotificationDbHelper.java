package lk.sbg.tikevent.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotificationDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Notifications.db";
    private static final int DATABASE_VERSION = 1;

    public NotificationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId TEXT, " +
                "title TEXT, " +
                "location TEXT, " +
                "date TEXT, " +
                "price TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notifications");
        onCreate(db);
    }

    public void addNotification(String userId, String title, String loc, String date, String price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("title", title);
        values.put("location", loc);
        values.put("date", date);
        values.put("price", price);
        db.insert("notifications", null, values);
    }

    public void deleteNotification(String title, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("notifications", "title = ? AND userId = ?", new String[]{title, userId});
    }
}