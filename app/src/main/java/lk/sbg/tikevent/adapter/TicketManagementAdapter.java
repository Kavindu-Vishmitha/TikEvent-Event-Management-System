package lk.sbg.tikevent.adapter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.fragment.EventAttendanceFragment;
import lk.sbg.tikevent.fragment.ScanFragment;
import lk.sbg.tikevent.fragment.TicketManagementFragment;
import lk.sbg.tikevent.model.Ticket;

public class TicketManagementAdapter extends RecyclerView.Adapter<TicketManagementAdapter.TicketViewHolder> {
    private List<Ticket> list;
    private TicketManagementFragment fragment;

    public TicketManagementAdapter(List<Ticket> list, TicketManagementFragment fragment) {
        this.list = list;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ticket_manage_design, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = list.get(position);

        holder.eventName.setText(ticket.getEventName());
        holder.place.setText(ticket.getPlace());
        holder.dateTime.setText(ticket.getDateTime());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Buyer_attendance")
                .whereEqualTo("eventId", ticket.getEventId())
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    long totalQuantity = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshots.getDocuments()) {
                        Long quantity = doc.getLong("quantity");
                        if (quantity != null) {
                            totalQuantity += quantity;
                        }
                    }
                    holder.personCount.setText(String.format(Locale.getDefault(), "%02d", totalQuantity));
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Count error", e);
                    holder.personCount.setText("00");
                });

        holder.countContainer.setOnClickListener(v -> {
            if (fragment.getActivity() instanceof MainActivity) {
                EventAttendanceFragment attendanceFragment = new EventAttendanceFragment();
                Bundle args = new Bundle();
                args.putString("event_name", ticket.getEventName());
                attendanceFragment.setArguments(args);
                ((MainActivity) fragment.getActivity()).loadFragment(attendanceFragment);
            }
        });

        View qrContainer = holder.itemView.findViewById(R.id.qr_only_container);
        if (qrContainer != null) {
            qrContainer.setOnClickListener(v -> {
                if (fragment.getActivity() instanceof MainActivity) {
                    ScanFragment scanFragment = new ScanFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("event_id", ticket.getEventId());
                    scanFragment.setArguments(bundle);
                    ((MainActivity) fragment.getActivity()).loadFragment(scanFragment);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, place, dateTime, personCount;
        LinearLayout countContainer;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.ticket_event_name);
            place = itemView.findViewById(R.id.ticket_place);
            dateTime = itemView.findViewById(R.id.ticket_date_time);
            personCount = itemView.findViewById(R.id.ticket_person_count);
            countContainer = itemView.findViewById(R.id.count_square_container);
        }
    }
}