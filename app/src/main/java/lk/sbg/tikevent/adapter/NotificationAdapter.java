package lk.sbg.tikevent.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.model.OtherEvent;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<OtherEvent> notificationList;
    private List<OtherEvent> selectedItems = new ArrayList<>();
    private boolean isMultiSelectMode = false;
    private String userRole;

    public interface OnSelectionChangeListener {
        void onSelectionChanged(boolean anySelected);
    }

    private OnSelectionChangeListener selectionChangeListener;

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public NotificationAdapter(List<OtherEvent> notificationList, String userRole) {
        this.notificationList = notificationList;
        this.userRole = userRole;
    }

    public List<OtherEvent> getSelectedItems() {
        return selectedItems;
    }

    public void clearSelection() {
        isMultiSelectMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(false);
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_design, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        OtherEvent item = notificationList.get(position);

        holder.name.setText(item.getEventName());
        holder.place.setText(item.getPlace());

        if ("Seller".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
            String fullDate = item.getDate();
            if (fullDate != null && fullDate.contains("|")) {
                holder.dateTime.setText("Sale Ends: " + fullDate.split("\\|")[0].trim());
            } else {
                holder.dateTime.setText("Sale Ends: " + (fullDate != null ? fullDate : "N/A"));
            }
            holder.price.setVisibility(View.GONE);
        } else {
            holder.price.setText(item.getPrice());
            holder.dateTime.setText(item.getDate());
            holder.price.setVisibility(View.VISIBLE);
        }

        holder.checkBox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(item));

        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true;
                toggleSelection(item);
                return true;
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(item);
            }
        });

        holder.checkBox.setOnClickListener(v -> toggleSelection(item));
    }

    private void toggleSelection(OtherEvent item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }

        if (selectedItems.isEmpty()) {
            isMultiSelectMode = false;
        }

        notifyDataSetChanged();

        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(!selectedItems.isEmpty());
        }
    }

    public boolean isSelectionActive() {
        return !selectedItems.isEmpty();
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView name, place, price, dateTime;
        MaterialCheckBox checkBox;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.my_ticket_event_name);
            place = itemView.findViewById(R.id.my_ticket_place);
            price = itemView.findViewById(R.id.my_ticket_price);
            dateTime = itemView.findViewById(R.id.my_ticket_date_time);
            checkBox = itemView.findViewById(R.id.my_ticket_checkbox);
        }
    }
}