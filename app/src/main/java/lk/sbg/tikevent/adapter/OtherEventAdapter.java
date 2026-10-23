package lk.sbg.tikevent.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.model.OtherEvent;

public class OtherEventAdapter extends RecyclerView.Adapter<OtherEventAdapter.OtherViewHolder> {

    private List<OtherEvent> otherEventsList;
    private List<OtherEvent> selectedItems = new ArrayList<>();
    private boolean isSelectionEnabled;
    private boolean isMultiSelectMode = false;
    private boolean isCategoryView;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public interface OnFavoriteChangeListener {
        void onFavoriteRemoved(int position);
    }

    private OnFavoriteChangeListener favoriteChangeListener;

    public void setOnFavoriteChangeListener(OnFavoriteChangeListener listener) {
        this.favoriteChangeListener = listener;
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(boolean anySelected);
    }

    private OnSelectionChangeListener selectionChangeListener;

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public OtherEventAdapter(List<OtherEvent> otherEventsList, boolean isSelectionEnabled, boolean isCategoryView) {
        this.otherEventsList = otherEventsList;
        this.isSelectionEnabled = isSelectionEnabled;
        this.isCategoryView = isCategoryView;
    }

    public void updateList(List<OtherEvent> newList) {
        this.otherEventsList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OtherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.other_event_design, parent, false);
        return new OtherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OtherViewHolder holder, int position) {
        final OtherEvent event = otherEventsList.get(position);

        holder.name.setText(event.getEventName());
        holder.place.setText(event.getPlace());
        holder.price.setText(event.getPrice());
        holder.date.setText(event.getDate());
        holder.time.setText(event.getTime());

        Glide.with(holder.itemView.getContext())
                .load(event.getImageUrl())
                .placeholder(R.drawable.game)
                .into(holder.image);

        if (isCategoryView) {
            holder.category.setVisibility(View.GONE);
        } else {
            holder.category.setVisibility(View.VISIBLE);
            holder.category.setText(event.getCategory());
        }

        holder.checkBox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(event));

        checkIfFavorite(event.getEventName(), holder.btnFavoritePost);

        holder.btnFavorite.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(v.getContext(), "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            toggleFavorite(event, holder.btnFavoritePost, v.getContext(), holder.getAdapterPosition());
        });

        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(event);
            } else {
                Context context = v.getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openEventDetails(event.getEventName());
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (isSelectionEnabled && !isMultiSelectMode) {
                isMultiSelectMode = true;
                toggleSelection(event);
                return true;
            }
            return false;
        });

        holder.checkBox.setOnClickListener(v -> toggleSelection(event));
    }

    private void checkIfFavorite(String eventName, ImageView favIcon) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user_favorites").document(uid)
                .collection("my_favorites").document(eventName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        favIcon.setColorFilter(Color.RED);
                        favIcon.setTag(true);
                    } else {
                        favIcon.clearColorFilter();
                        favIcon.setTag(false);
                    }
                });
    }

    private void toggleFavorite(OtherEvent event, ImageView favIcon, Context context, int position) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference favRef = db.collection("user_favorites").document(uid)
                .collection("my_favorites").document(event.getEventName());

        Object tag = favIcon.getTag();
        boolean isFavorite = tag != null && (boolean) tag;

        if (!isFavorite) {
            String fullAddress = event.getPlace();
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

            String timeStr = event.getTime();
            String startTimeOnly = "";
            if (timeStr != null) {
                if (timeStr.contains("-")) {
                    startTimeOnly = timeStr.split("-")[0].trim();
                } else {
                    startTimeOnly = timeStr;
                }
            }

            Map<String, Object> favData = new HashMap<>();
            favData.put("category", event.getCategory());
            favData.put("eventName", event.getEventName());
            favData.put("place", mainCity);
            favData.put("price", event.getPrice());
            favData.put("date", event.getDate());
            favData.put("time", startTimeOnly);
            favData.put("imageUrl", event.getImageUrl());

            favRef.set(favData).addOnSuccessListener(aVoid -> {
                favIcon.setColorFilter(Color.RED);
                favIcon.setTag(true);
            });
        } else {
            favRef.delete().addOnSuccessListener(aVoid -> {
                favIcon.clearColorFilter();
                favIcon.setTag(false);
                if (favoriteChangeListener != null) {
                    favoriteChangeListener.onFavoriteRemoved(position);
                }
            });
        }
    }

    private void toggleSelection(OtherEvent event) {
        if (selectedItems.contains(event)) {
            selectedItems.remove(event);
        } else {
            selectedItems.add(event);
        }
        isMultiSelectMode = !selectedItems.isEmpty();
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(!selectedItems.isEmpty());
        }
    }

    public boolean isSelectionActive() {
        return isMultiSelectMode && !selectedItems.isEmpty();
    }

    public List<OtherEvent> getSelectedItems() {
        return selectedItems;
    }

    public void clearSelection() {
        selectedItems.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return otherEventsList.size();
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView category, name, place, price, date, time;
        ImageView image, btnFavoritePost;
        View btnFavorite;
        MaterialCheckBox checkBox;

        public OtherViewHolder(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.text_category_tag);
            name = itemView.findViewById(R.id.event_name_text);
            place = itemView.findViewById(R.id.event_place_text);
            price = itemView.findViewById(R.id.event_price_text);
            date = itemView.findViewById(R.id.event_date_text);
            time = itemView.findViewById(R.id.event_time_text);
            image = itemView.findViewById(R.id.event_image);
            btnFavorite = itemView.findViewById(R.id.btn_favorite_container);
            btnFavoritePost = itemView.findViewById(R.id.btn_favorite_post);
            checkBox = itemView.findViewById(R.id.event_checkbox);
        }
    }
}