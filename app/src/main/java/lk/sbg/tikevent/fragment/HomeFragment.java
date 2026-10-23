package lk.sbg.tikevent.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.adapter.CategoryAdapter;
import lk.sbg.tikevent.adapter.OtherEventAdapter;
import lk.sbg.tikevent.adapter.PopularEventAdapter;
import lk.sbg.tikevent.model.Category;
import lk.sbg.tikevent.model.OtherEvent;
import lk.sbg.tikevent.model.PopularEvent;

public class HomeFragment extends Fragment {

    private List<OtherEvent> allEventsList = new ArrayList<>();
    private OtherEventAdapter otherAdapter;
    private View categorySection, popularSection, otherEventSection, emptyStateView, otherSectionTitle;

    private PopularEventAdapter popularAdapter;

    private ListenerRegistration favoriteListenerRegistration;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup fragment_container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, fragment_container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categorySection = view.findViewById(R.id.layout_category_section);
        popularSection = view.findViewById(R.id.layout_popular_section);
        otherEventSection = view.findViewById(R.id.layout_other_event_section);
        emptyStateView = view.findViewById(R.id.layout_empty_state);
        otherSectionTitle = view.findViewById(R.id.other_section_title);

        EditText searchInput = view.findViewById(R.id.text_input_search);

        RecyclerView recyclerView = view.findViewById(R.id.category_recycler_view);
        List<Category> categoryList = new ArrayList<>();
        CategoryAdapter adapter = new CategoryAdapter(categoryList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        FirebaseFirestore.getInstance().collection("categories")
                .orderBy("index", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        categoryList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Category category = doc.toObject(Category.class);
                            if (category != null) {
                                categoryList.add(category);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        adapter.setOnCategoryClickListener(category -> {
            CategoryFragment fragment = new CategoryFragment();
            Bundle args = new Bundle();
            args.putString("category_name", category.getName());
            fragment.setArguments(args);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(fragment);
            }
        });

        RecyclerView popularRecyclerView = view.findViewById(R.id.popular_recycler_view);
        List<PopularEvent> popularEvents = new ArrayList<>();
        popularRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        popularAdapter = new PopularEventAdapter(popularEvents);
        popularRecyclerView.setAdapter(popularAdapter);

        FirebaseFirestore.getInstance().collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("FirestoreError", "Error: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        popularEvents.clear();
                        List<PopularEvent> tempList = new ArrayList<>();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            long soldCount = 0;
                            if (doc.contains("ticketsSold") && doc.get("ticketsSold") != null) {
                                try {
                                    soldCount = doc.getLong("ticketsSold");
                                } catch (Exception e) {
                                    try {
                                        soldCount = Long.parseLong(doc.get("ticketsSold").toString());
                                    } catch (Exception ex) {
                                        soldCount = 0;
                                    }
                                }
                            }

                            String category = doc.getString("category") != null ? doc.getString("category") : "Popular";

                            Object priceObj = doc.get("stdPrice");
                            String formattedPrice = "Rs. 0.00";
                            if (priceObj != null) {
                                try {
                                    double priceValue = Double.parseDouble(priceObj.toString());
                                    formattedPrice = "Rs. " + String.format("%.2f", priceValue);
                                } catch (Exception e) {
                                    formattedPrice = "Rs. " + priceObj.toString();
                                }
                            }

                            String time = "";
                            if (doc.getString("startTime") != null) {
                                time = doc.getString("startTime");
                            } else if (doc.getString("time") != null) {
                                time = doc.getString("time");
                            }

                            String fullAddress = doc.getString("locationAddress");
                            String mainCity = "Unknown Location";
                            if (fullAddress != null && !fullAddress.isEmpty()) {
                                String[] addressParts = fullAddress.split(",");
                                for (int i = addressParts.length - 1; i >= 0; i--) {
                                    String part = addressParts[i].trim();
                                    if (!part.equalsIgnoreCase("Sri Lanka") && !part.matches(".*\\d.*")) {
                                        mainCity = part;
                                        break;
                                    }
                                }
                            }

                            PopularEvent event = new PopularEvent(
                                    doc.getId(),
                                    doc.getString("eventName"),
                                    doc.getString("eventDate"),
                                    mainCity,
                                    doc.getString("imageUrl"),
                                    soldCount,
                                    category,
                                    formattedPrice,
                                    time
                            );
                            tempList.add(event);
                        }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            tempList.sort((e1, e2) -> Long.compare(e2.getTicketsSold(), e1.getTicketsSold()));
                        } else {
                            java.util.Collections.sort(tempList, (e1, e2) -> Long.compare(e2.getTicketsSold(), e1.getTicketsSold()));
                        }

                        int limit = Math.min(tempList.size(), 5);
                        for (int i = 0; i < limit; i++) {
                            popularEvents.add(tempList.get(i));
                        }

                        popularAdapter.notifyDataSetChanged();
                    }
                });

        RecyclerView otherRecyclerView = view.findViewById(R.id.other_event_recycler_view);
        otherAdapter = new OtherEventAdapter(new ArrayList<>(allEventsList), false, false);
        otherRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        otherRecyclerView.setAdapter(otherAdapter);

        FirebaseFirestore.getInstance().collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        allEventsList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {

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
                            allEventsList.add(event);
                        }

                        if (searchInput.getText().toString().isEmpty()) {
                            updateUI(allEventsList);
                        }
                    }
                });

        startFavoriteListener();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                filter(query);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void startFavoriteListener() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null && favoriteListenerRegistration == null) {
            String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            favoriteListenerRegistration = FirebaseFirestore.getInstance()
                    .collection("user_favorites").document(currentUid)
                    .collection("my_favorites")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;

                        if (popularAdapter != null) {
                            popularAdapter.notifyDataSetChanged();
                        }
                        if (otherAdapter != null) {
                            otherAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void filter(String text) {
        if (text.isEmpty()) {
            categorySection.setVisibility(View.VISIBLE);
            popularSection.setVisibility(View.VISIBLE);
            if (otherSectionTitle != null) otherSectionTitle.setVisibility(View.VISIBLE);
            updateUI(allEventsList);
        } else {
            categorySection.setVisibility(View.GONE);
            popularSection.setVisibility(View.GONE);
            if (otherSectionTitle != null) otherSectionTitle.setVisibility(View.GONE);

            List<OtherEvent> filteredList = new ArrayList<>();
            for (OtherEvent item : allEventsList) {
                if (item.getEventName().toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(item);
                }
            }
            updateUI(filteredList);
        }
    }

    private void updateUI(List<OtherEvent> list) {
        if (list.isEmpty()) {
            otherEventSection.setVisibility(View.GONE);
            if (emptyStateView != null) emptyStateView.setVisibility(View.VISIBLE);
        } else {
            otherEventSection.setVisibility(View.VISIBLE);
            if (emptyStateView != null) emptyStateView.setVisibility(View.GONE);
            otherAdapter.updateList(list);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (popularAdapter != null) {
            popularAdapter.notifyDataSetChanged();
        }
        if (otherAdapter != null) {
            otherAdapter.notifyDataSetChanged();
        }
        startFavoriteListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (favoriteListenerRegistration != null) {
            favoriteListenerRegistration.remove();
            favoriteListenerRegistration = null;
        }
    }

}