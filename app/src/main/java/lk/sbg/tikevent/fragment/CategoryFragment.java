package lk.sbg.tikevent.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.adapter.OtherEventAdapter;
import lk.sbg.tikevent.model.OtherEvent;

public class CategoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private OtherEventAdapter adapter;
    private List<OtherEvent> eventList;
    private List<OtherEvent> fullEventList;
    private EditText searchInput;
    private String categoryName;
    private FirebaseFirestore db;

    public CategoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryName = getArguments().getString("category_name");
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.other_event_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        searchInput = view.findViewById(R.id.text_input_search);

        eventList = new ArrayList<>();
        fullEventList = new ArrayList<>();

        adapter = new OtherEventAdapter(eventList, false, true); //[cite: 10]

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle(getCategoryName());
        }

        searchInput.addTextChangedListener(new TextWatcher() {
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

        loadEventsFromFirestore();
    }

    private void filterEvents(String query) {
        List<OtherEvent> filteredList = new ArrayList<>();

        for (OtherEvent event : fullEventList) {
            if (event.getEventName().toLowerCase().contains(query.toLowerCase().trim())) {
                filteredList.add(event);
            }
        }

        eventList.clear();
        eventList.addAll(filteredList);
        adapter.notifyDataSetChanged();

        updateUIState();
    }

    private void updateUIState() {
        if (eventList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void loadEventsFromFirestore() {
        db.collection("events")
                .whereEqualTo("category", categoryName)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Error: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        fullEventList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                String fullAddress = doc.getString("locationAddress");
                                String displayCity = "Unknown Location";
                                if (fullAddress != null && !fullAddress.isEmpty()) {
                                    String[] addressParts = fullAddress.split(",");
                                    for (int i = addressParts.length - 1; i >= 0; i--) {
                                        String part = addressParts[i].trim();
                                        if (!part.equalsIgnoreCase("Sri Lanka") && !part.matches(".*\\d.*")) {
                                            displayCity = part;
                                            break;
                                        }
                                    }
                                }

                                Object priceObj = doc.get("stdPrice");
                                String formattedPrice = "0.00";
                                if (priceObj != null) {
                                    try {
                                        double priceValue = Double.parseDouble(priceObj.toString());
                                        formattedPrice = "Rs. " + String.format("%.2f", priceValue);
                                    } catch (Exception e) {
                                        formattedPrice = "Rs. " + priceObj.toString();
                                    }
                                }

                                OtherEvent event = new OtherEvent(
                                        doc.getString("category"),
                                        doc.getString("eventName"),
                                        displayCity,
                                        formattedPrice,
                                        doc.getString("eventDate"),
                                        doc.getString("startTime"),
                                        doc.getString("imageUrl")
                                );
                                fullEventList.add(event);

                            } catch (Exception e) {
                                Log.e("MappingError", "Crash prevented for doc: " + doc.getId());
                            }
                        }
                        filterEvents(searchInput.getText().toString());
                    }
                });
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "Category";
    }
}