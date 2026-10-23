package lk.sbg.tikevent.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.adapter.TicketManagementAdapter;
import lk.sbg.tikevent.model.Ticket;

public class TicketManagementFragment extends Fragment {

    private TicketManagementAdapter adapter;
    private List<Ticket> ticketList;
    private List<Ticket> filteredList;
    private FirebaseFirestore db;
    private EditText searchEditText;
    private FrameLayout searchButton;
    private LinearLayout layoutEmptyState;
    private RecyclerView recyclerView;

    private TextView ticketPersonCount;

    public TicketManagementFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket_management, container, false);

        ticketPersonCount = view.findViewById(R.id.ticket_person_count);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.ticket_manage_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchEditText = view.findViewById(R.id.text_input_search);
        searchButton = view.findViewById(R.id.btn_search_action);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        ticketList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new TicketManagementAdapter(filteredList, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadEventsFromFirebase();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString();
            filterEvents(query);
        });
    }

    private void loadEventsFromFirebase() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ticketList.clear();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String docId = document.getId();
                            String eventName = document.getString("eventName");
                            String date = document.getString("eventDate");
                            String startTime = document.getString("startTime");
                            String fullPlace = document.getString("locationAddress");

                            String personCount = document.getString("personCount");
                            if (personCount == null) personCount = "00";

                            if (eventName == null) eventName = "Unknown Event";
                            if (date == null) date = "";
                            if (startTime == null) startTime = "";

                            String mainCity = "Unknown Location";
                            if (fullPlace != null && !fullPlace.isEmpty()) {
                                String[] addressParts = fullPlace.split(",");
                                if (addressParts.length > 0) {
                                    int index = addressParts.length - 1;
                                    String lastPart = addressParts[index].trim();

                                    if (lastPart.equalsIgnoreCase("Sri Lanka") && addressParts.length > 1) {
                                        mainCity = addressParts[addressParts.length - 2].trim();
                                    } else {
                                        mainCity = lastPart;
                                    }
                                    mainCity = mainCity.replaceAll("\\d+", "").trim();
                                }
                            }

                            String dateTimeDisplay = date;
                            if (!startTime.isEmpty()) {
                                if (!dateTimeDisplay.isEmpty()) {
                                    dateTimeDisplay += " | " + startTime;
                                } else {
                                    dateTimeDisplay = startTime;
                                }
                            }

                            ticketList.add(new Ticket(docId, eventName, mainCity, dateTimeDisplay, personCount));
                        }
                    } else {
                        Log.d("FirestoreData", "No events found in collection");
                    }

                    filteredList.clear();
                    filteredList.addAll(ticketList);
                    updateUiVisibility();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error loading events", e);
                    updateUiVisibility();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "No Internet Connection!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterEvents(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(ticketList);
        } else {
            String query = text.toLowerCase().trim();
            for (Ticket ticket : ticketList) {
                if (ticket.getEventName().toLowerCase().contains(query)) {
                    filteredList.add(ticket);
                }
            }
        }
        updateUiVisibility();
    }

    private void updateUiVisibility() {
        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    public void updateTicketCount(String eventId) {
        if (db == null || eventId == null || ticketPersonCount == null) return;

        db.collection("Buyer_attendance")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    long totalQuantity = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshots.getDocuments()) {
                        Long quantity = doc.getLong("quantity");
                        if (quantity != null) {
                            totalQuantity += quantity;
                        }
                    }

                    ticketPersonCount.setText(String.format(Locale.getDefault(), "%02d", totalQuantity));
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Count error", e);
                });
    }
}