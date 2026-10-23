package lk.sbg.tikevent.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.model.PopularEvent;

public class PopularEventAdapter extends RecyclerView.Adapter<PopularEventAdapter.ViewHolder> {

    private List<PopularEvent> popularEventList;
    private Context context;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public PopularEventAdapter(List<PopularEvent> popularEventList) {
        this.popularEventList = popularEventList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.popular_design, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PopularEvent event = popularEventList.get(position);

        holder.tvEventName.setText(event.getEventName());
        holder.tvEventDate.setText(event.getEventDate());

        String fullAddress = event.getLocationAddress();
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
        holder.tvEventLocation.setText(displayCity);

        Glide.with(context)
                .load(event.getImageUrl())
                .placeholder(R.drawable.music)
                .into(holder.imgEventImage);

        checkIfFavorite(event.getEventName(), holder.imgFavoriteIcon);

        holder.btnFavorite.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            toggleFavorite(event, holder.imgFavoriteIcon);
        });

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).openEventDetails(event.getEventName());
            }
        });
    }

    private void checkIfFavorite(String eventName, ImageView favIcon) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("user_favorites").document(uid)
                .collection("my_favorites").document(eventName)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) return;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        favIcon.setColorFilter(Color.RED);
                        favIcon.setTag(true);
                    } else {
                        favIcon.clearColorFilter();
                        favIcon.setTag(false);
                    }
                });
    }

    private void toggleFavorite(PopularEvent event, ImageView favIcon) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference favRef = db.collection("user_favorites").document(uid)
                .collection("my_favorites").document(event.getEventName());

        Object tag = favIcon.getTag();
        boolean isFavorite = tag != null && (boolean) tag;

        if (!isFavorite) {
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("eventName", event.getEventName());
            favoriteData.put("place", event.getLocationAddress());
            favoriteData.put("date", event.getEventDate());
            favoriteData.put("imageUrl", event.getImageUrl());
            favoriteData.put("category", event.getCategory());
            favoriteData.put("price", event.getPrice() != null ? event.getPrice() : "Rs. 0.00");
            favoriteData.put("time", event.getTime());

            favRef.set(favoriteData).addOnSuccessListener(aVoid -> {
                favIcon.setColorFilter(Color.RED);
                favIcon.setTag(true);
            });
        } else {
            favRef.delete().addOnSuccessListener(aVoid -> {
                favIcon.clearColorFilter();
                favIcon.setTag(false);
            });
        }
    }

    @Override
    public int getItemCount() {
        return popularEventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgEventImage, imgFavoriteIcon;
        TextView tvEventName, tvEventDate, tvEventLocation;
        FrameLayout btnFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgEventImage = itemView.findViewById(R.id.popular_event_image);
            tvEventName = itemView.findViewById(R.id.popular_event_name);
            tvEventDate = itemView.findViewById(R.id.popular_event_date);
            tvEventLocation = itemView.findViewById(R.id.popular_event_location);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);

            imgFavoriteIcon = btnFavorite.findViewById(R.id.favorite_icon_view);
            if (imgFavoriteIcon == null) {
                for (int i = 0; i < btnFavorite.getChildCount(); i++) {
                    View child = btnFavorite.getChildAt(i);
                    if (child instanceof ImageView) {
                        imgFavoriteIcon = (ImageView) child;
                        break;
                    }
                }
            }
        }
    }
}