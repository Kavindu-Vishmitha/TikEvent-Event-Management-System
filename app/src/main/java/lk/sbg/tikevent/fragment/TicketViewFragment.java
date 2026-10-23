package lk.sbg.tikevent.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.databinding.FragmentTicketViewBinding;
import lk.sbg.tikevent.model.PurchasedTicket;

public class TicketViewFragment extends DialogFragment {

    private static final String TAG = "TicketViewFragment";
    private FragmentTicketViewBinding binding;
    private PurchasedTicket ticket;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTicketViewBinding.inflate(inflater, container, false);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int dialogWidth = (int) (screenWidth * 0.85);
            int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setLayout(dialogWidth, dialogHeight);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            ticket = (PurchasedTicket) getArguments().getSerializable("selected_ticket");
        }

        if (ticket != null) {
            populateTicketDetails();
        } else {
            Log.e(TAG, "Ticket data is null");
            dismiss();
        }

        binding.btnDownloadTicket.setOnClickListener(v -> {
            saveTicketAsImage(binding.ticketScrollView);
        });

        binding.btnCloseDialog.setOnClickListener(v -> dismiss());
    }

    private void populateTicketDetails() {
        binding.dialogEventName.setText(ticket.getEventName());

        String dateTime = ticket.getEventStartDate();
        if (ticket.getEventStartTime() != null && !ticket.getEventStartTime().isEmpty()) {
            dateTime += " | " + ticket.getEventStartTime();
        }
        binding.dialogEventDateTime.setText(dateTime);

        binding.dialogTicketType.setText(ticket.getTicketType());
        binding.dialogPersonCount.setText(String.format("%02d", ticket.getQuantity()));
        binding.dialogPrice.setText("Rs. " + ticket.getTotalPrice());

        String fullPurchaseDate = ticket.getPurchaseAt();
        if (fullPurchaseDate != null && !fullPurchaseDate.isEmpty()) {
            String onlyDate = fullPurchaseDate.split(" ")[0];
            binding.dialogPurchasedDate.setText(onlyDate);
        } else {
            binding.dialogPurchasedDate.setText("N/A");
        }

        if (ticket.getQrUrl() != null && !ticket.getQrUrl().isEmpty()) {
            Glide.with(this)
                    .load(ticket.getQrUrl())
                    .placeholder(R.drawable.qr_code_24px)
                    .into(binding.dialogQrCode);
        }

        String documentId = ticket.getTicketId();
        if (documentId != null && !documentId.isEmpty()) {
            db.collection("tickets_purchased").document(documentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String realTicketId = documentSnapshot.getString("ticketId");
                            if (realTicketId != null && !realTicketId.trim().isEmpty()) {
                                binding.dialogTicketId.setText(realTicketId);
                            } else {
                                binding.dialogTicketId.setText("#TK-EMPTY");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching real ticketId field", e);
                        binding.dialogTicketId.setText("#TK-ERROR");
                    });
        } else {
            binding.dialogTicketId.setText("#TK-UNKNOWN");
        }

        if (ticket.getEventId() != null && !ticket.getEventId().isEmpty()) {
            db.collection("events").document(ticket.getEventId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String fullLocation = documentSnapshot.getString("locationAddress");
                            binding.dialogEventLocation.setText(fullLocation != null ? fullLocation : "N/A");

                            String imageUrl = documentSnapshot.getString("imageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(TicketViewFragment.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.game)
                                        .into(binding.dialogEventImage);
                            }
                        }
                    }).addOnFailureListener(e -> Log.e(TAG, "Error fetching event extra details", e));
        }

        if (ticket.getUserId() != null && !ticket.getUserId().isEmpty()) {
            db.collection("users").document(ticket.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String name = documentSnapshot.getString("fullName");
                            binding.dialogBuyerName.setText(name != null ? name : "Unknown");
                        }
                    }).addOnFailureListener(e -> Log.e(TAG, "Error fetching buyer name", e));
        }
    }

    private void saveTicketAsImage(View view) {
        try {
            binding.btnDownloadTicket.setVisibility(View.GONE);
            binding.btnCloseDialog.setVisibility(View.GONE);

            view.setBackgroundColor(android.graphics.Color.WHITE);

            int blackColor = android.graphics.Color.BLACK;
            binding.dialogEventName.setTextColor(blackColor);
            binding.dialogEventDateTime.setTextColor(blackColor);
            binding.dialogEventLocation.setTextColor(blackColor);
            binding.dialogTicketId.setTextColor(blackColor);
            binding.dialogTicketType.setTextColor(blackColor);
            binding.dialogPersonCount.setTextColor(blackColor);
            binding.dialogBuyerName.setTextColor(blackColor);
            binding.dialogPrice.setTextColor(blackColor);
            binding.dialogPurchasedDate.setTextColor(blackColor);

            int width = view.getWidth();
            int height = view.getHeight();

            if (view instanceof android.widget.ScrollView) {
                android.widget.ScrollView scrollView = (android.widget.ScrollView) view;
                if (scrollView.getChildCount() > 0) {
                    height = scrollView.getChildAt(0).getHeight();
                }
            }

            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            canvas.drawColor(android.graphics.Color.WHITE);
            view.draw(canvas);

            binding.btnDownloadTicket.setVisibility(View.VISIBLE);
            binding.btnCloseDialog.setVisibility(View.VISIBLE);

            view.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            int whiteColor = android.graphics.Color.WHITE;
            binding.dialogEventName.setTextColor(whiteColor);
            binding.dialogEventDateTime.setTextColor(whiteColor);
            binding.dialogEventLocation.setTextColor(whiteColor);
            binding.dialogTicketId.setTextColor(whiteColor);
            binding.dialogTicketType.setTextColor(whiteColor);
            binding.dialogPersonCount.setTextColor(whiteColor);
            binding.dialogBuyerName.setTextColor(whiteColor);
            binding.dialogPrice.setTextColor(android.graphics.Color.parseColor("#BB86FC"));
            binding.dialogPurchasedDate.setTextColor(whiteColor);

            String eventNameClean = (ticket != null && ticket.getEventName() != null) ? ticket.getEventName().replaceAll("[^a-zA-Z0-9_]", "_") : "Ticket";
            String fileName = "TikEvent_" + eventNameClean + "_" + System.currentTimeMillis() + ".png";

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/TikEvent");
            }

            android.content.ContentResolver resolver = requireContext().getContentResolver();
            android.net.Uri uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (java.io.OutputStream out = resolver.openOutputStream(uri)) {
                    if (out != null) {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                        showCustomDialog("success", "Success", "Ticket saved to Gallery!");
                    }
                }
            } else {
                showCustomDialog("error", "Failed", "Failed to save image.");
            }

        } catch (Exception e) {
            Log.e("TicketViewFragment", "Error saving ticket", e);
            binding.btnDownloadTicket.setVisibility(View.VISIBLE);
            binding.btnCloseDialog.setVisibility(View.VISIBLE);
            showCustomDialog("error", "Error", e.getMessage());
        }
    }

    private void showCustomDialog(String type, String title, String message) {
        if (getContext() == null) return;

        final android.app.Dialog dialog = new android.app.Dialog(getContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.6f);
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        android.widget.FrameLayout iconContainer = dialog.findViewById(R.id.dialogIconContainer);
        android.widget.TextView iconText = dialog.findViewById(R.id.dialogIconText);
        android.widget.TextView tvTitle = dialog.findViewById(R.id.customDialogTitle);
        android.widget.TextView tvMessage = dialog.findViewById(R.id.customDialogText);
        com.google.android.material.button.MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);
        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);

        if (type.equals("success")) {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_success);
            if (iconText != null) iconText.setText("✓");
            if (btnCancel != null) btnCancel.setVisibility(View.GONE);
            if (btnOk != null) {
                btnOk.setText("OK");
                btnOk.setOnClickListener(v -> {
                    dialog.dismiss();
                    dismiss();
                });
            }
        } else {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error);
            if (iconText != null) iconText.setText("✕");
            if (btnCancel != null) btnCancel.setVisibility(View.GONE);
            if (btnOk != null) {
                btnOk.setText("OK");
                btnOk.setOnClickListener(v -> dialog.dismiss());
            }
        }

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}