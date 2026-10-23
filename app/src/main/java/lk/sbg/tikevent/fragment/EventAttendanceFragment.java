package lk.sbg.tikevent.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.adapter.EventAttendanceAdapter;
import lk.sbg.tikevent.model.Attendance;

public class EventAttendanceFragment extends Fragment {
    private String eventName;
    private String eventId;
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private EventAttendanceAdapter adapter;
    private List<Attendance> attendanceList;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            eventName = getArguments().getString("event_name");
            eventId = getArguments().getString("event_id");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.event_attendance_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.hideBottomNavigation(true);
            mainActivity.setupAttendanceToolbar(eventName);
        }

        recyclerView = view.findViewById(R.id.event_attendance_recycler_view);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        attendanceList = new ArrayList<>();
        adapter = new EventAttendanceAdapter(attendanceList, this);
        recyclerView.setAdapter(adapter);

        loadAttendanceData();
    }

    private void loadAttendanceData() {
        if (eventId != null && !eventId.isEmpty()) {
            fetchTicketsByEventId(eventId);
        } else if (eventName != null && !eventName.isEmpty()) {
            db.collection("events")
                    .whereEqualTo("eventName", eventName.trim())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot eventDoc = queryDocumentSnapshots.getDocuments().get(0);
                            eventId = eventDoc.getId();
                            fetchTicketsByEventId(eventId);
                        } else {
                            checkEmptyState();
                            Toast.makeText(getContext(), "No event found with name: " + eventName, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        checkEmptyState();
                        Toast.makeText(getContext(), "Error finding event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            checkEmptyState();
            Toast.makeText(getContext(), "Event details not found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchTicketsByEventId(String targetEventId) {
        db.collection("tickets_purchased")
                .whereEqualTo("eventId", targetEventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        checkEmptyState();
                        return;
                    }

                    int totalTickets = queryDocumentSnapshots.size();
                    final int[] processedTickets = {0};

                    for (QueryDocumentSnapshot ticketDoc : queryDocumentSnapshots) {
                        String userId = ticketDoc.getString("userId");
                        String ticketId = ticketDoc.getString("ticketId");

                        String fullDate = ticketDoc.getString("purchaseAt");
                        String bookingDate = (fullDate != null && fullDate.contains(" ")) ? fullDate.split(" ")[0] : fullDate;
                        if (bookingDate == null) bookingDate = "N/A";

                        String ticketType = ticketDoc.getString("ticketType");
                        if (ticketType == null) ticketType = "Standard";

                        String personCount = "1";
                        if (ticketDoc.get("quantity") != null) {
                            personCount = String.valueOf(ticketDoc.get("quantity"));
                        }

                        if (userId != null && !userId.isEmpty()) {
                            String finalBookingDate = bookingDate;
                            String finalTicketType = ticketType;
                            String finalPersonCount = personCount;

                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        String fullName = (userDoc.exists() && userDoc.getString("fullName") != null) ? userDoc.getString("fullName") : "Unknown User";
                                        String nic = (userDoc.exists() && userDoc.getString("nic") != null) ? userDoc.getString("nic") : "N/A";
                                        String profilePicUrl = userDoc.exists() ? userDoc.getString("profileImage") : null;

                                        db.collection("Buyer_attendance").document(ticketId != null ? ticketId : "unknown")
                                                .get()
                                                .addOnCompleteListener(task -> {
                                                    String status = "Unmark";
                                                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                                        String attendanceEventId = task.getResult().getString("eventId");

                                                        if (attendanceEventId != null && attendanceEventId.equals(targetEventId)) {
                                                            status = task.getResult().getString("status");
                                                        }
                                                    }

                                                    Attendance attendance = new Attendance(fullName, nic, eventName, finalBookingDate, finalTicketType, finalPersonCount, profilePicUrl, ticketId);
                                                    attendance.setStatus(status);
                                                    attendanceList.add(attendance);

                                                    processedTickets[0]++;
                                                    if (processedTickets[0] == totalTickets) {
                                                        adapter.notifyDataSetChanged();
                                                        checkEmptyState();
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        processedTickets[0]++;
                                        if (processedTickets[0] == totalTickets) {
                                            adapter.notifyDataSetChanged();
                                            checkEmptyState();
                                        }
                                    });
                        } else {
                            processedTickets[0]++;
                            if (processedTickets[0] == totalTickets) {
                                adapter.notifyDataSetChanged();
                                checkEmptyState();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    checkEmptyState();
                    Toast.makeText(getContext(), "Error loading tickets: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkEmptyState() {
        if (attendanceList == null || attendanceList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public EventAttendanceAdapter getAdapter() {
        return adapter;
    }

    public void deleteSelectedAttendance() {
        List<Attendance> toRemove = new ArrayList<>();
        for (Attendance a : attendanceList) {
            if (a.isSelected()) {
                toRemove.add(a);
            }
        }

        for (Attendance item : toRemove) {
            if (item.getTicketId() != null) {
                db.collection("Buyer_attendance").document(item.getTicketId()).delete();
            }
        }

        attendanceList.removeAll(toRemove);
        adapter.clearSelection();
        adapter.notifyDataSetChanged();
        checkEmptyState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.hideBottomNavigation(true);
            mainActivity.setupAttendanceToolbar(eventName);
        }
    }

    private void showCustomDialog(String type, String title, String message) {
        if (getContext() == null) return;

        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.6f);
        }

        FrameLayout iconContainer = dialog.findViewById(R.id.dialogIconContainer);
        TextView iconText = dialog.findViewById(R.id.dialogIconText);
        TextView tvTitle = dialog.findViewById(R.id.customDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.customDialogText);
        MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (type.equals("confirm")) {
            iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error); // අවශ්‍ය පරිදි වෙනස් කරන්න
            iconText.setText("?");
            btnOk.setText("Yes");
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setText("No");

            btnOk.setOnClickListener(v -> {
                dialog.dismiss();
                deleteSelectedAttendance();
                showCustomDialog("success", "Success", "Selected attendance deleted successfully.");
            });

            btnCancel.setOnClickListener(v -> {
                dialog.dismiss();

                if (adapter != null) {
                    adapter.clearSelection();
                }
            });

        } else {
            iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_success);
            iconText.setText("✓");
            btnCancel.setVisibility(View.GONE);
            btnOk.setText("OK");
            btnOk.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    public void onBinIconClicked() {
        showCustomDialog("confirm", "Delete Attendance", "Are you sure you want to delete the selected records?");
    }
}