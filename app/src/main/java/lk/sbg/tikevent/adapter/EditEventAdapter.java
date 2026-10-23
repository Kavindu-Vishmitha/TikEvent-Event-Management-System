package lk.sbg.tikevent.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.model.ManageEvent;

public class EditEventAdapter extends RecyclerView.Adapter<EditEventAdapter.EditEventViewHolder> {

    private List<ManageEvent> eventList;
    private OnEditClickListener listener;

    public interface OnEditClickListener {
        void onEditItemClick(ManageEvent event);
    }

    public EditEventAdapter(List<ManageEvent> eventList, OnEditClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EditEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_event_design, parent, false);
        return new EditEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EditEventViewHolder holder, int position) {
        ManageEvent event = eventList.get(position);

        holder.eventName.setText(event.getName());
        holder.eventPlace.setText(event.getLocation());
        holder.eventPrice.setText("Rs. " + event.getPrice());
        holder.eventDateTime.setText(event.getDate() + " | " + event.getTime());

        Glide.with(holder.itemView.getContext())
                .load(event.getImageUrl())
                .placeholder(R.drawable.game)
                .into(holder.eventImage);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditItemClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    static class EditEventViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView eventImage;
        TextView eventName, eventPlace, eventPrice, eventDateTime;
        MaterialButton btnEdit;

        public EditEventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.manage_event_image);
            eventName = itemView.findViewById(R.id.manage_event_name);
            eventPlace = itemView.findViewById(R.id.manage_event_place);
            eventPrice = itemView.findViewById(R.id.manage_event_price);
            eventDateTime = itemView.findViewById(R.id.manage_event_date_time);
            btnEdit = itemView.findViewById(R.id.btn_edit_event);
        }
    }
}