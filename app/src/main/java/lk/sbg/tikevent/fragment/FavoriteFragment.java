package lk.sbg.tikevent.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.adapter.OtherEventAdapter;
import lk.sbg.tikevent.model.OtherEvent;

public class FavoriteFragment extends Fragment {

    private RecyclerView recyclerView;
    private OtherEventAdapter adapter;
    private LinearLayout emptyView;
    private List<OtherEvent> favoriteList = new ArrayList<>();

    public FavoriteFragment() {
    }

    public OtherEventAdapter getAdapter() {
        return adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.favorite_recycler_view);
        emptyView = view.findViewById(R.id.layout_empty_favorite);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OtherEventAdapter(favoriteList, true, false);
        recyclerView.setAdapter(adapter);

        adapter.setOnFavoriteChangeListener(position -> {
            if (position != RecyclerView.NO_POSITION && position < favoriteList.size()) {
                favoriteList.remove(position);
                adapter.notifyItemRemoved(position);
                showEmptyView(favoriteList.isEmpty());
            }
        });

        adapter.setOnSelectionChangeListener(anySelected -> {
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        });

        loadFavoriteData();
    }

    public void deleteSelectedNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || adapter == null) return;

        List<OtherEvent> selectedItems = adapter.getSelectedItems();
        if (selectedItems.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (OtherEvent event : selectedItems) {
            db.collection("user_favorites").document(uid)
                    .collection("my_favorites").document(event.getDocumentId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        int index = favoriteList.indexOf(event);
                        if (index != -1) {
                            favoriteList.remove(index);
                            adapter.notifyItemRemoved(index);
                            showEmptyView(favoriteList.isEmpty());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        adapter.clearSelection();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void loadFavoriteData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            showEmptyView(true);
            return;
        }

        FirebaseFirestore.getInstance().collection("user_favorites").document(uid)
                .collection("my_favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        OtherEvent event = doc.toObject(OtherEvent.class);
                        if (event != null) {
                            event.setDocumentId(doc.getId());
                            favoriteList.add(event);
                        }
                    }

                    adapter.updateList(favoriteList);
                    showEmptyView(favoriteList.isEmpty());
                })
                .addOnFailureListener(e -> showEmptyView(true));
    }

    private void showEmptyView(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}