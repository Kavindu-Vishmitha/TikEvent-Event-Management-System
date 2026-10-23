package lk.sbg.tikevent.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.HashMap;
import java.util.Map;

import lk.sbg.tikevent.R;

public class ScanFragment extends Fragment {

    private DecoratedBarcodeView barcodeView;
    private FirebaseFirestore db;
    private String eventId;

    public ScanFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            eventId = getArguments().getString("event_id");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        barcodeView = view.findViewById(R.id.barcode_scanner);

        barcodeView.decodeContinuous(result -> {
            String scannedId = result.getText().trim();
            barcodeView.pause();
            markAttendance(scannedId);
        });

        return view;
    }

    private void markAttendance(String scannedTicketId) {
        if (scannedTicketId == null || scannedTicketId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid QR Data", Toast.LENGTH_SHORT).show();
            barcodeView.resume();
            return;
        }

        db.collection("tickets_purchased")
                .whereEqualTo("ticketId", scannedTicketId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot ticketDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String ticketEventId = ticketDoc.getString("eventId");
                        String userId = ticketDoc.getString("userId");
                        Long quantity = ticketDoc.getLong("quantity");

                        if (ticketEventId == null || !ticketEventId.equals(eventId)) {
                            showCustomDialog("error", "Wrong Event", "This ticket does not belong to this event.");
                            return;
                        }

                        db.collection("Buyer_attendance").document(scannedTicketId).get()
                                .addOnSuccessListener(attendanceDoc -> {
                                    if (attendanceDoc.exists()) {
                                        showCustomDialog("error", "Already Scanned", "This ticket has already been marked as attended.");
                                    } else {
                                        saveAttendance(scannedTicketId, userId, ticketEventId, quantity);
                                    }
                                });
                    } else {
                        showCustomDialog("error", "Error", "Ticket not found!");
                    }
                })
                .addOnFailureListener(e -> {
                    showCustomDialog("error", "Error", "Database error: " + e.getMessage());
                });
    }

    private void saveAttendance(String scannedTicketId, String userId, String eventId, Long quantity) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = userDoc.getString("fullName");
                    String nic = userDoc.getString("nic");

                    Map<String, Object> attendanceData = new HashMap<>();
                    attendanceData.put("ticketId", scannedTicketId);
                    attendanceData.put("eventId", eventId);
                    attendanceData.put("fullName", fullName != null ? fullName : "N/A");
                    attendanceData.put("nic", nic != null ? nic : "N/A");
                    attendanceData.put("quantity", quantity != null ? quantity : 0);
                    attendanceData.put("status", "Marked");
                    attendanceData.put("timestamp", FieldValue.serverTimestamp());

                    db.collection("Buyer_attendance").document(scannedTicketId).set(attendanceData)
                            .addOnSuccessListener(aVoid -> {
                                showCustomDialog("success", "Success", "Attendance Marked Successfully!");
                            })
                            .addOnFailureListener(e -> {
                                showCustomDialog("error", "Error", "Save failed: " + e.getMessage());
                            });
                });
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
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        FrameLayout iconContainer = dialog.findViewById(R.id.dialogIconContainer);
        TextView iconText = dialog.findViewById(R.id.dialogIconText);
        TextView tvTitle = dialog.findViewById(R.id.customDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.customDialogText);
        MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);

        if (type.equals("success")) {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_success);
            if (iconText != null) iconText.setText("✓");
        } else {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error);
            if (iconText != null) iconText.setText("✕");
        }

        if (btnOk != null) {
            btnOk.setOnClickListener(v -> {
                dialog.dismiss();
                if (type.equals("success")) {
                    getParentFragmentManager().popBackStack();
                } else {
                    barcodeView.resume();
                }
            });
        }
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (barcodeView != null) {
            barcodeView.resume();
        }

        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeView != null) barcodeView.pause();
    }
}