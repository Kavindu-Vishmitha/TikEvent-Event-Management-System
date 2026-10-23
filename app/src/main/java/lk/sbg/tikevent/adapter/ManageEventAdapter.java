package lk.sbg.tikevent.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.model.ManageEvent;

public class ManageEventAdapter extends RecyclerView.Adapter<ManageEventAdapter.ManageEventViewHolder> {

    private List<ManageEvent> eventList;
    private boolean isSelectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private Runnable onSelectionChanged;
    private OnEditClickListener editClickListener;

    public interface OnEditClickListener {
        void onEditClick(ManageEvent event);
    }

    public ManageEventAdapter(List<ManageEvent> eventList, Runnable onSelectionChanged, OnEditClickListener editClickListener) {
        this.eventList = eventList;
        this.onSelectionChanged = onSelectionChanged;
        this.editClickListener = editClickListener;
    }

    public List<String> getSelectedEventIds() {
        List<String> selectedIds = new ArrayList<>();
        for (Integer position : selectedPositions) {
            if (position < eventList.size()) {
                selectedIds.add(eventList.get(position).getEventId());
            }
        }
        return selectedIds;
    }

    public void clearSelection() {
        selectedPositions.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    public int getSelectedItemsCount() {
        return selectedPositions.size();
    }

    public void updateList(List<ManageEvent> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ManageEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_event_design, parent, false);
        return new ManageEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageEventViewHolder holder, int position) {
        ManageEvent event = eventList.get(position);
        holder.eventName.setText(event.getName());
        holder.eventPlace.setText(event.getLocation());
        holder.eventPrice.setText("Rs. " + event.getPrice());
        holder.eventDateTime.setText(event.getDate() + " | " + event.getTime());

        Glide.with(holder.itemView.getContext())
                .load(event.getImageUrl())
                .placeholder(R.drawable.game)
                .into(holder.eventImage);

        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedPositions.contains(position));
        holder.checkBox.setOnClickListener(v -> toggleSelection(position));
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(position);
            }
            return true;
        });

        holder.btnEdit.setOnClickListener(v -> editClickListener.onEditClick(event));
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        if (selectedPositions.isEmpty()) {
            isSelectionMode = false;
            notifyDataSetChanged();
        } else {
            notifyItemChanged(position);
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    static class ManageEventViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView eventImage;
        TextView eventName, eventPlace, eventPrice, eventDateTime;
        MaterialButton btnEdit;
        MaterialCheckBox checkBox;

        public ManageEventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.manage_event_image);
            eventName = itemView.findViewById(R.id.manage_event_name);
            eventPlace = itemView.findViewById(R.id.manage_event_place);
            eventPrice = itemView.findViewById(R.id.manage_event_price);
            eventDateTime = itemView.findViewById(R.id.manage_event_date_time);
            btnEdit = itemView.findViewById(R.id.btn_edit_event);
            checkBox = itemView.findViewById(R.id.manage_event_checkbox);
        }
    }
}