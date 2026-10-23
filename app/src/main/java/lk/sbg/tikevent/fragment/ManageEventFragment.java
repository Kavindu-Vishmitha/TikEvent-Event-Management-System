package lk.sbg.tikevent.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.adapter.ManageEventAdapter;
import lk.sbg.tikevent.model.ManageEvent;

public class ManageEventFragment extends Fragment {

    private RecyclerView recyclerView;
    private ManageEventAdapter adapter;
    private List<ManageEvent> eventList = new ArrayList<>();
    private List<ManageEvent> fullList = new ArrayList<>();
    private EditText searchInput;
    private FirebaseFirestore db;
    private static final String SAVED_LIST_KEY = "saved_event_list";
    private View emptyStateLayout;
    private View eventScrollView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.manage_event_recycler_view);
        searchInput = view.findViewById(R.id.text_input_search);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
        eventScrollView = view.findViewById(R.id.scroll_view_events);

        adapter = new ManageEventAdapter(eventList, () -> {
            if (getActivity() != null) getActivity().invalidateOptionsMenu();
        }, this::openEditEventFragment);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_LIST_KEY)) {
            ArrayList<ManageEvent> savedData = (ArrayList<ManageEvent>) savedInstanceState.getSerializable(SAVED_LIST_KEY);
            if (savedData != null && !savedData.isEmpty()) {
                eventList.clear();
                fullList.clear();
                eventList.addAll(savedData);
                fullList.addAll(savedData);
                updateUIState(false);
                adapter.notifyDataSetChanged();
            } else {
                loadEvents();
            }
        } else {
            loadEvents();
        }
        setupSearch();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!eventList.isEmpty()) {
            outState.putSerializable(SAVED_LIST_KEY, (Serializable) new ArrayList<>(eventList));
        }
    }

    public void confirmDeleteSelectedEvents() {
        showCustomDialog("confirm", "Confirm Delete", "Are you sure you want to delete the selected event(s)?");
    }

    private void deleteSelectedEvents() {
        List<String> selectedIds = adapter.getSelectedEventIds();
        if (selectedIds.isEmpty()) return;

        WriteBatch batch = db.batch();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        for (String id : selectedIds) {
            for (ManageEvent event : eventList) {
                if (event.getEventId().equals(id)) {
                    String imageUrl = event.getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                        photoRef.delete().addOnFailureListener(e -> {

                        });
                    }
                    break;
                }
            }
            batch.delete(db.collection("events").document(id));
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            adapter.clearSelection();
            loadEvents();
        }).addOnFailureListener(e -> {
            showCustomDialog("error", "Failed", "Could not delete events: " + e.getMessage());
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
            dialog.getWindow().setDimAmount(0.6f);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        FrameLayout iconContainer = dialog.findViewById(R.id.dialogIconContainer);
        TextView iconText = dialog.findViewById(R.id.dialogIconText);
        TextView tvTitle = dialog.findViewById(R.id.customDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.customDialogText);
        MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);

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
                    showCustomDialog("success", "Success", "The selected event(s) have been deleted.");
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
                    deleteSelectedEvents();
                });
            }
        } else {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error);
            if (iconText != null) iconText.setText("✕");
            if (btnCancel != null) btnCancel.setVisibility(View.GONE);
            if (btnOk != null) btnOk.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void loadEvents() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("events")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(value -> {
                    if (value == null) return;
                    eventList.clear();
                    fullList.clear();

                    if (value.isEmpty()) {
                        updateUIState(true);
                    } else {
                        updateUIState(false);
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String fullAddress = doc.getString("locationAddress");
                            String displayCity = "Unknown Location";

                            if (fullAddress != null && !fullAddress.isEmpty()) {
                                String[] addressParts = fullAddress.split(",");
                                for (int i = addressParts.length - 1; i >= 0; i--) {
                                    String part = addressParts[i].trim();
                                    if (!part.equalsIgnoreCase("Sri Lanka") && !containsDigit(part)) {
                                        displayCity = part;
                                        break;
                                    }
                                }
                            }

                            Object priceObj = doc.get("stdPrice");
                            String formattedPrice = "0.00";
                            if (priceObj != null) {
                                try {
                                    double price = Double.parseDouble(priceObj.toString());
                                    formattedPrice = String.format("%.2f", price);
                                } catch (Exception e) {
                                    formattedPrice = priceObj.toString();
                                }
                            }

                            ManageEvent ev = new ManageEvent(
                                    doc.getId(),
                                    doc.getString("eventName"),
                                    displayCity,
                                    formattedPrice,
                                    doc.getString("eventDate"),
                                    doc.getString("startTime"),
                                    doc.getString("imageUrl")
                            );
                            eventList.add(ev);
                            fullList.add(ev);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (getActivity() != null) getActivity().invalidateOptionsMenu();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUIState(boolean isEmpty) {
        if (isEmpty) {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
            if (eventScrollView != null) eventScrollView.setVisibility(View.GONE);
        } else {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
            if (eventScrollView != null) eventScrollView.setVisibility(View.VISIBLE);
        }
    }

    private boolean containsDigit(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filter(String text) {
        List<ManageEvent> filteredList = new ArrayList<>();
        for (ManageEvent item : fullList) {
            if (item.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        updateUIState(filteredList.isEmpty());

        adapter.updateList(filteredList);
    }

    public ManageEventAdapter getAdapter() {
        return adapter;
    }

    private void openEditEventFragment(ManageEvent event) {
        EventEditFragment editFragment = new EventEditFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", event.getEventId());
        editFragment.setArguments(args);
        editFragment.show(getParentFragmentManager(), "EventEditFragment");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
    }
}