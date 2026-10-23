package lk.sbg.tikevent.fragment;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.adapter.NotificationAdapter;
import lk.sbg.tikevent.model.OtherEvent;
import lk.sbg.tikevent.util.NotificationDbHelper;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyView;
    private NotificationAdapter adapter;
    private List<OtherEvent> notificationList;
    private FirebaseFirestore db;
    private String currentUserId;
    private String userRole = "Buyer";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notification_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);

        notificationList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupAdapter();

        checkUserRoleAndLoadData();
    }

    private void setupAdapter() {
        adapter = new NotificationAdapter(notificationList, userRole);
        adapter.setOnSelectionChangeListener(anySelected -> {
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
        checkUserRoleAndLoadData();
    }

    private void checkUserRoleAndLoadData() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fetchedRole = documentSnapshot.getString("role");
                        userRole = (fetchedRole != null) ? fetchedRole : "Buyer";

                        setupAdapter();

                        if ("Seller".equalsIgnoreCase(userRole)) {
                            loadNotificationsFromInternal();
                        } else {
                            loadNotificationsFromFirestore();
                        }
                    }
                });
    }

    public boolean isSelectionActive() {
        return adapter != null && adapter.isSelectionActive();
    }

    public void deleteSelectedNotifications() {
        if (adapter == null) return;

        List<OtherEvent> selectedItems = new ArrayList<>(adapter.getSelectedItems());
        if (selectedItems.isEmpty()) return;

        if ("Seller".equalsIgnoreCase(userRole)) {
            NotificationDbHelper dbHelper = new NotificationDbHelper(requireContext());
            for (OtherEvent item : selectedItems) {
                dbHelper.deleteNotification(item.getEventName(), currentUserId);
            }
            loadNotificationsFromInternal();
        } else {
            for (OtherEvent item : selectedItems) {
                if (item.getCategory() != null && !item.getCategory().isEmpty()) {
                    db.collection("user_notifications").document(item.getCategory()).delete();
                }
            }
            loadNotificationsFromFirestore();
        }

        adapter.clearSelection();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    public void loadNotificationsFromFirestore() {
        if (currentUserId == null) return;

        db.collection("user_notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String docId = doc.getId();
                        String title = doc.getString("eventName");
                        String fullLocation = doc.getString("place");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String price = doc.getString("price");

                        String displayCity = formatCity(fullLocation);
                        String displayPrice = (price != null && !price.isEmpty()) ? "Rs. " + price : "";
                        String dateTime = (date != null ? date : "") + " | " + (time != null ? time : "");

                        OtherEvent event = new OtherEvent(docId, title, displayCity, displayPrice, dateTime, "", "");
                        notificationList.add(event);
                    }
                    updateUIState(notificationList.isEmpty());
                    adapter.notifyDataSetChanged();
                });
    }

    public void loadNotificationsFromInternal() {
        if (currentUserId == null) return;

        notificationList.clear();
        NotificationDbHelper dbHelper = new NotificationDbHelper(getContext());
        SQLiteDatabase dbLite = dbHelper.getReadableDatabase();

        Cursor cursor = dbLite.rawQuery("SELECT * FROM notifications WHERE userId = ?", new String[]{currentUserId});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String title = cursor.getString(2);
                String fullLocation = cursor.getString(3);
                String date = cursor.getString(4);
                String rawPrice = cursor.getString(5);

                String displayCity = formatCity(fullLocation);
                String displayPrice = (rawPrice != null && !rawPrice.isEmpty()) ? "Rs. " + rawPrice : "";

                OtherEvent event = new OtherEvent("", title, displayCity, displayPrice, date, "", "");
                notificationList.add(event);
            } while (cursor.moveToNext());
            cursor.close();
        }

        updateUIState(notificationList.isEmpty());
        adapter.notifyDataSetChanged();
    }

    private String formatCity(String fullLocation) {
        String displayCity = "Unknown Location";
        if (fullLocation != null && !fullLocation.isEmpty()) {
            String[] addressParts = fullLocation.split(",");
            for (int i = addressParts.length - 1; i >= 0; i--) {
                String part = addressParts[i].trim();
                if (!part.equalsIgnoreCase("Sri Lanka") && !containsDigit(part)) {
                    displayCity = part;
                    break;
                }
            }
        }
        return displayCity;
    }

    private boolean containsDigit(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private void updateUIState(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        }
    }
}