package lk.sbg.tikevent.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.adapter.MyTicketAdapter;
import lk.sbg.tikevent.model.PurchasedTicket;

public class MyTicketFragment extends Fragment {

    private static final String TAG = "MyTicketFragment";
    private RecyclerView recyclerView;
    private MyTicketAdapter adapter;
    private View emptyStateView;
    private List<PurchasedTicket> ticketList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ListenerRegistration ticketListener;
    private ListenerRegistration eventsListener;

    private FirebaseStorage storage;

    public MyTicketFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_ticket, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.my_ticket_recycler_view);
        emptyStateView = view.findViewById(R.id.layout_empty_state);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ticketList = new ArrayList<>();
        adapter = new MyTicketAdapter(ticketList);

        adapter.setOnSelectionChangeListener(isMultiSelectMode -> {
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        });

        recyclerView.setAdapter(adapter);
        loadUserPurchasedTickets();
    }

    private void loadUserPurchasedTickets() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "loadUserPurchasedTickets: User is not logged in.");
            return;
        }

        String currentUserId = currentUser.getUid();

        ticketListener = db.collection("tickets_purchased")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {

                    if (!isAdded() || getContext() == null) return;

                    if (error != null) {
                        Log.e(TAG, "Firestore listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        ticketList.clear();
                        List<String> eventIds = new ArrayList<>();

                        for (DocumentSnapshot document : value.getDocuments()) {
                            try {
                                if (document.exists()) {
                                    PurchasedTicket ticket = new PurchasedTicket();
                                    ticket.setTicketId(document.getId());

                                    String eventId = document.getString("eventId");
                                    ticket.setEventId(eventId);
                                    ticket.setEventName(document.getString("eventName"));
                                    ticket.setPurchaseAt(document.getString("purchaseAt"));

                                    String qrUrl = document.getString("qrUrl");
                                    ticket.setQrUrl(qrUrl != null ? qrUrl : "");

                                    ticket.setStatus(document.getString("status"));
                                    ticket.setTicketType(document.getString("ticketType"));
                                    ticket.setUserId(document.getString("userId"));

                                    Long qty = document.getLong("quantity");
                                    ticket.setQuantity(qty != null ? qty.intValue() : 1);

                                    Long price = document.getLong("totalPrice");
                                    ticket.setTotalPrice(price != null ? price : 0L);

                                    ticketList.add(ticket);

                                    if (eventId != null && !eventId.isEmpty() && !eventIds.contains(eventId)) {
                                        eventIds.add(eventId);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Skipping faulty document due to parsing error", e);
                            }
                        }

                        if (!eventIds.isEmpty()) {
                            if (emptyStateView != null) emptyStateView.setVisibility(View.GONE);
                            fetchEventDetailsAndPopulate(eventIds);
                        } else {
                            adapter.updateList(ticketList);
                            if (emptyStateView != null) emptyStateView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void fetchEventDetailsAndPopulate(List<String> eventIds) {
        if (eventsListener != null) {
            eventsListener.remove();
        }

        db.collection("events")
                .whereIn(FieldPath.documentId(), eventIds)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || getContext() == null) return;

                    if (error != null) {
                        Log.e(TAG, "Failed to fetch event details", error);
                        return;
                    }

                    if (value != null) {
                        Map<String, DocumentSnapshot> eventMap = new HashMap<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            eventMap.put(doc.getId(), doc);
                        }

                        for (PurchasedTicket ticket : ticketList) {
                            DocumentSnapshot eventDoc = eventMap.get(ticket.getEventId());
                            if (eventDoc != null && eventDoc.exists()) {

                                String fullAddress = eventDoc.getString("locationAddress");
                                String mainCity = extractMainCity(fullAddress);
                                ticket.setEventPlace(mainCity);

                                String date = eventDoc.getString("saleEndDate");
                                ticket.setEventStartDate(date != null ? date : "N/A");

                                String time = eventDoc.getString("startTime");
                                ticket.setEventStartTime(time != null ? time : "");
                            } else {
                                ticket.setEventPlace("Unknown Place");
                                ticket.setEventStartDate("N/A");
                            }
                        }

                        adapter.updateList(ticketList);

                        if (emptyStateView != null) {
                            if (ticketList.isEmpty()) {
                                emptyStateView.setVisibility(View.VISIBLE);
                            } else {
                                emptyStateView.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    private String extractMainCity(String fullAddress) {
        if (fullAddress == null || fullAddress.isEmpty()) {
            return "Unknown Location";
        }
        String[] parts = fullAddress.split(",");
        if (parts.length >= 2) {
            return parts[parts.length - 2].trim();
        }
        return fullAddress;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ticketListener != null) {
            ticketListener.remove();
            Log.d(TAG, "Ticket snapshot listener removed.");
        }
        if (eventsListener != null) {
            eventsListener.remove();
            Log.d(TAG, "Events snapshot listener removed.");
        }
    }

    public boolean isSelectionActive() {
        return adapter != null && adapter.isMultiSelectMode();
    }

    public void confirmDeleteSelectedTickets() {
        showCustomDialog("confirm", "Confirm Delete", "Are you sure you want to delete the selected ticket(s)?");
    }

    private void deleteSelectedTickets() {
        List<PurchasedTicket> selectedTickets = adapter.getSelectedItems();
        if (selectedTickets.isEmpty()) return;

        for (PurchasedTicket ticket : selectedTickets) {
            String ticketId = ticket.getTicketId();
            String qrUrl = ticket.getQrUrl();

            if (qrUrl != null && !qrUrl.trim().isEmpty() && qrUrl.startsWith("http")) {
                try {
                    StorageReference qrRef = storage.getReferenceFromUrl(qrUrl.trim());
                    qrRef.delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Storage QR deleted successfully for ticket: " + ticketId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to delete Storage QR for ticket: " + ticketId, e));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid URL format for Storage Reference: " + qrUrl, e);
                } catch (Exception e) {
                    Log.e(TAG, "Storage delete execution error: " + e.getMessage(), e);
                }
            } else {
                Log.w(TAG, "No valid QR URL found for ticket: " + ticketId + ", skipping storage deletion.");
            }

            if (ticketId != null && !ticketId.isEmpty()) {
                db.collection("tickets_purchased").document(ticketId)
                        .delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore document deleted for ticket: " + ticketId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete Firestore document: " + ticketId, e));
            }
        }

        adapter.clearSelection();
    }

    private void showCustomDialog(String type, String title, String message) {
        if (getContext() == null) return;

        final android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.6f);
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        android.widget.FrameLayout iconContainer = dialog.findViewById(R.id.dialogIconContainer);
        android.widget.TextView iconText = dialog.findViewById(R.id.dialogIconText);
        android.widget.TextView tvTitle = dialog.findViewById(R.id.customDialogTitle);
        android.widget.TextView tvMessage = dialog.findViewById(R.id.customDialogText);
        com.google.android.material.button.MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);
        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);

        if (type.equals("confirm")) {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error);
            if (iconText != null) iconText.setText("?");
            if (btnOk != null) {
                btnOk.setText("Yes");
                btnOk.setOnClickListener(v -> {
                    dialog.dismiss();
                    showCustomDialog("success", "Success", "The selected ticket(s) have been deleted.");
                });
            }
            if (btnCancel != null) {
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setText("No");
                btnCancel.setOnClickListener(v -> {
                    adapter.clearSelection();
                    dialog.dismiss();
                    if (getActivity() != null) getActivity().invalidateOptionsMenu();
                });
            }
        } else if (type.equals("success")) {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_success);
            if (iconText != null) iconText.setText("✓");
            if (btnCancel != null) btnCancel.setVisibility(View.GONE);
            if (btnOk != null) {
                btnOk.setText("OK");
                btnOk.setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteSelectedTickets();
                });
            }
        } else {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error);
            if (iconText != null) iconText.setText("✕");
            if (btnCancel != null) btnCancel.setVisibility(View.GONE);
            if (btnOk != null) {
                btnOk.setOnClickListener(v -> dialog.dismiss());
            }
        }

        dialog.show();
    }
}