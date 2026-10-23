package lk.sbg.tikevent.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.fragment.TicketViewFragment;
import lk.sbg.tikevent.model.PurchasedTicket;

public class MyTicketAdapter extends RecyclerView.Adapter<MyTicketAdapter.TicketViewHolder> {

    private static final String TAG = "MyTicketAdapter";
    private List<PurchasedTicket> ticketList;
    private List<PurchasedTicket> selectedItems = new ArrayList<>();
    private boolean isMultiSelectMode = false;

    public MyTicketAdapter(List<PurchasedTicket> ticketList) {
        this.ticketList = ticketList;
    }

    public void updateList(List<PurchasedTicket> newList) {
        this.ticketList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_ticket_design, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        PurchasedTicket ticket = ticketList.get(position);

        if (ticket == null) return;

        holder.eventName.setText(ticket.getEventName());

        holder.place.setText(ticket.getEventPlace());

        holder.price.setText("Rs. " + String.valueOf(ticket.getTotalPrice()));

        String dateTimeText = ticket.getEventStartDate();
        if (ticket.getEventStartTime() != null && !ticket.getEventStartTime().isEmpty()) {
            dateTimeText += " | " + ticket.getEventStartTime();
        }
        holder.dateTime.setText(dateTimeText.isEmpty() ? "Loading date..." : dateTimeText);

        if (isMultiSelectMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(selectedItems.contains(ticket));
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.checkBox.setChecked(false);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true;
                toggleSelection(ticket, position);
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(ticket, position);
            } else {
                String qrUrl = ticket.getQrUrl();
                if (qrUrl == null || qrUrl.trim().isEmpty()) {
                    Log.w(TAG, "Click ignored: QR code URL is still missing.");
                    return;
                }

                android.content.Context context = v.getContext();
                while (context instanceof android.content.ContextWrapper) {
                    if (context instanceof AppCompatActivity) {
                        break;
                    }
                    context = ((android.content.ContextWrapper) context).getBaseContext();
                }

                if (context instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) context;

                    TicketViewFragment ticketViewFragment = new TicketViewFragment();
                    android.os.Bundle bundle = new android.os.Bundle();
                    bundle.putSerializable("selected_ticket", ticket);
                    ticketViewFragment.setArguments(bundle);

                    ticketViewFragment.show(activity.getSupportFragmentManager(), "TicketView");
                }
            }
        });

        holder.checkBox.setOnClickListener(v -> toggleSelection(ticket, position));
    }

    private void toggleSelection(PurchasedTicket ticket, int position) {
        if (selectedItems.contains(ticket)) {
            selectedItems.remove(ticket);
        } else {
            selectedItems.add(ticket);
        }

        if (selectedItems.isEmpty()) {
            isMultiSelectMode = false;
        }

        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(isMultiSelectMode);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return ticketList != null ? ticketList.size() : 0;
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, place, price, dateTime;
        MaterialCheckBox checkBox;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.my_ticket_event_name);
            place = itemView.findViewById(R.id.my_ticket_place);
            price = itemView.findViewById(R.id.my_ticket_price);
            dateTime = itemView.findViewById(R.id.my_ticket_date_time);
            checkBox = itemView.findViewById(R.id.my_ticket_checkbox);
        }
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(boolean isMultiSelectMode);
    }

    private OnSelectionChangeListener selectionChangeListener;

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public List<PurchasedTicket> getSelectedItems() {
        return selectedItems;
    }

    public void clearSelection() {
        selectedItems.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(false);
        }
    }
}