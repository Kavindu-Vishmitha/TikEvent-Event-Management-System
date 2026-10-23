package lk.sbg.tikevent.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.fragment.EventAttendanceFragment;
import lk.sbg.tikevent.model.Attendance;

public class EventAttendanceAdapter extends RecyclerView.Adapter<EventAttendanceAdapter.AttendanceViewHolder> {
    private List<Attendance> list;
    private EventAttendanceFragment fragment;
    private boolean isSelectionMode = false;

    public EventAttendanceAdapter(List<Attendance> list, EventAttendanceFragment fragment) {
        this.list = list;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_attendance_design, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Attendance attendance = list.get(position);

        holder.name.setText(attendance.getFullName());
        holder.nic.setText("NIC: " + attendance.getNic());
        holder.bookingDate.setText("Booking Date: " + attendance.getBookingDate());
        holder.ticketType.setText("Ticket Type: " + attendance.getTicketType());
        holder.count.setText("Person Count: " + attendance.getPersonCount());

        if (attendance.getProfilePicUrl() != null && !attendance.getProfilePicUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(attendance.getProfilePicUrl())
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(holder.profilePic);
        } else {
            holder.profilePic.setImageResource(R.drawable.user);
        }

        LinearLayout statusContainer = holder.itemView.findViewById(R.id.status_unmark_container);
        TextView statusText = holder.itemView.findViewById(R.id.attendance_status_text);

        if ("Marked".equalsIgnoreCase(attendance.getStatus())) {
            statusContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
            statusText.setText("Marked");
        } else {
            statusContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF5350")));
            statusText.setText("Unmark");
        }

        boolean isMarked = "Marked".equalsIgnoreCase(attendance.getStatus());

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setVisibility((isSelectionMode && isMarked) ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(attendance.isSelected());

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode && isMarked) {
                isSelectionMode = true;
                attendance.setSelected(true);
                updateUI();
                notifyDataSetChanged();
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode && isMarked) {
                boolean newState = !attendance.isSelected();
                attendance.setSelected(newState);
                holder.checkBox.setChecked(newState);
                checkSelectionState();
            }
        });

        holder.checkBox.setOnClickListener(v -> {
            attendance.setSelected(holder.checkBox.isChecked());
            checkSelectionState();
        });
    }

    public void clearSelection() {
        isSelectionMode = false;
        for (Attendance a : list) {
            a.setSelected(false);
        }
        notifyDataSetChanged();
        updateUI();
    }

    public List<Attendance> getSelectedItems() {
        List<Attendance> selectedList = new ArrayList<>();
        for (Attendance a : list) {
            if (a.isSelected()) selectedList.add(a);
        }
        return selectedList;
    }

    private void checkSelectionState() {
        if (getSelectedItemsCount() == 0) {
            isSelectionMode = false;
            notifyDataSetChanged();
        }
        updateUI();
    }

    private void updateUI() {
        if (fragment.getActivity() instanceof MainActivity) {
            ((MainActivity) fragment.getActivity()).invalidateOptionsMenu();
        }
    }

    public int getSelectedItemsCount() {
        int count = 0;
        for (Attendance a : list) {
            if (a.isSelected()) count++;
        }
        return count;
    }

    public boolean isSelectionActive() {
        return isSelectionMode;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView name, nic, bookingDate, ticketType, count;
        ImageView profilePic;
        CheckBox checkBox;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.attendance_full_name);
            nic = itemView.findViewById(R.id.attendance_nic);
            bookingDate = itemView.findViewById(R.id.attendance_booking_date);
            ticketType = itemView.findViewById(R.id.attendance_ticket_type);
            count = itemView.findViewById(R.id.attendance_person_count);
            profilePic = itemView.findViewById(R.id.attendance_profile_pic);
            checkBox = itemView.findViewById(R.id.manage_ticket_checkbox);
        }
    }
}