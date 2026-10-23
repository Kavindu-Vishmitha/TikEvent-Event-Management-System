package lk.sbg.tikevent.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
import lk.payhere.androidsdk.model.StatusResponse;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.databinding.FragmentEventSingleViewBinding;
import lk.sbg.tikevent.model.Event;

public class EventSingleViewFragment extends Fragment implements OnMapReadyCallback {

    private static final int PAYHERE_REQUEST = 11001;
    private static final String TAG = "PaymentCheck";

    private FragmentEventSingleViewBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private Event currentEvent;
    private GoogleMap googleMap;

    private String selectedTicketType = "Standard";
    private int quantity = 1;
    private double currentPricePerTicket = 0;
    private int currentAvailableQty = 0;

    private boolean isFavorite = false;
    private String currentUid;

    private OnBackPressedCallback onBackPressedCallback;

    public EventSingleViewFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUid = auth.getUid();

        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {

            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEventSingleViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String eventName = getArguments().getString("event_name");
            if (eventName != null) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setToolbarTitle(eventName);
                }
                loadEventData(eventName);
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        setupTicketTypeSelection();
        setupQuantityControls();
        setupFavoriteButton();

        binding.btnBookNow.setOnClickListener(v -> {
            if (currentEvent == null) return;

            if (currentAvailableQty <= 0 || quantity > currentAvailableQty) {

            }

            if (currentUid == null) {
                Toast.makeText(getContext(), "Please login to purchase tickets", Toast.LENGTH_SHORT).show();
                return;
            }

            startPayHerePayment();
        });
    }

    private void startPayHerePayment() {
        double totalCost = currentPricePerTicket * quantity;
        String uniqueOrderId = "ORDER_" + System.currentTimeMillis();

        InitRequest req = new InitRequest();
        req.setMerchantId("*********");
        req.setMerchantSecret("**********************");
        req.setCurrency("LKR");
        req.setAmount(totalCost);
        req.setOrderId(uniqueOrderId);
        req.setItemsDescription(currentEvent.eventName + " - " + selectedTicketType);

        req.getCustomer().setFirstName("Kavindu");
        req.getCustomer().setLastName("Vishmitha");
        req.getCustomer().setEmail("2003kavinduvishmitha@gmail.com");
        req.getCustomer().setPhone("+94751320801");
        req.getCustomer().getAddress().setAddress("No.1, Galle Road");
        req.getCustomer().getAddress().setCity("Colombo");
        req.getCustomer().getAddress().setCountry("Sri Lanka");
        req.setNotifyUrl("https://sandbox.payhere.lk/pay/checkout");

        req.getItems().add(new Item(null, selectedTicketType + " Ticket", quantity, currentPricePerTicket));

        Intent intent = new Intent(getContext(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);

        startActivityForResult(intent, PAYHERE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYHERE_REQUEST && data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
            PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

            if (resultCode == Activity.RESULT_OK) {
                if (response != null && response.isSuccess()) {
                    Log.d(TAG, "Payment Success: " + response.getData().toString());

                    showLoading(true);
                    savePurchaseToFirestore();
                } else {
                    String errorMsg = response != null ? response.toString() : "Unknown error";
                    Log.d(TAG, "Payment Failed: " + errorMsg);
                    Toast.makeText(getContext(), "Payment Failed! Try again.", Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (response != null) {
                    Log.d(TAG, "User Canceled: " + response.toString());
                } else {
                    Log.d(TAG, "User Canceled the request");
                }
                Toast.makeText(getContext(), "Payment Canceled by User", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePurchaseToFirestore() {
        if (currentEvent == null || currentUid == null) {
            showLoading(false);
            return;
        }

        double totalCost = currentPricePerTicket * quantity;
        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        int randomNumber = (int) (Math.random() * 9000) + 1000;
        String generatedTicketId = "#TK-" + randomNumber;

        db.collection("events")
                .whereEqualTo("eventName", currentEvent.eventName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    String fetchedEventId = "";
                    if (!queryDocumentSnapshots.isEmpty()) {
                        fetchedEventId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    }

                    Map<String, Object> purchaseData = new HashMap<>();
                    purchaseData.put("userId", currentUid);
                    purchaseData.put("ticketId", generatedTicketId);
                    purchaseData.put("eventId", fetchedEventId);
                    purchaseData.put("eventName", currentEvent.eventName);
                    purchaseData.put("ticketType", selectedTicketType);
                    purchaseData.put("quantity", quantity);
                    purchaseData.put("totalPrice", totalCost);
                    purchaseData.put("purchaseAt", currentDateTime);
                    purchaseData.put("status", "SUCCESS");
                    purchaseData.put("qrUrl", "");

                    String finalGeneratedTicketId = generatedTicketId;
                    db.collection("tickets_purchased")
                            .add(purchaseData)
                            .addOnSuccessListener(documentReference -> {
                                if (!isAdded()) return;

                                updateAvailableTicketQuantity();

                                String documentId = documentReference.getId();
                                generateAndUploadQRCode(finalGeneratedTicketId, documentId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving to Firestore", e);
                                showLoading(false);
                                if (isAdded())
                                    Toast.makeText(getContext(), "Firebase Save Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching Event ID", e);
                    showLoading(false);
                });
    }

    private void generateAndUploadQRCode(String ticketId, String documentId) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(ticketId, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            String safeFileName = ticketId.replace("#", "") + ".png";
            StorageReference qrStorageRef = storage.getReference().child("ticket_qrs/" + safeFileName);

            qrStorageRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        if (!isAdded()) return;
                        Log.d(TAG, "QR Code uploaded successfully to Storage: " + ticketId);

                        qrStorageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            if (!isAdded()) return;
                            String qrImageUrl = uri.toString();

                            db.collection("tickets_purchased").document(documentId)
                                    .update("qrUrl", qrImageUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        if (!isAdded()) return;
                                        Log.d(TAG, "Firestore updated with QR URL successfully!");

                                        showLoading(false);
                                    })
                                    .addOnFailureListener(e -> {
                                        showLoading(false);
                                        Log.e(TAG, "Failed to update QR URL in Firestore", e);
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "QR Upload Failed", e);
                        if (isAdded())
                            Toast.makeText(getContext(), "QR Code Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (WriterException e) {
            showLoading(false);
            Log.e(TAG, "QR Generation Failed", e);
        }
    }

    private void updateAvailableTicketQuantity() {
        db.collection("events")
                .whereEqualTo("eventName", currentEvent.eventName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || queryDocumentSnapshots.isEmpty()) return;

                    String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    DocumentReference eventRef = db.collection("events").document(docId);

                    String qtyField = "";
                    if (selectedTicketType.equals("Standard")) qtyField = "stdQty";
                    else if (selectedTicketType.equals("VIP")) qtyField = "vipQty";
                    else if (selectedTicketType.equals("VVIP")) qtyField = "vvipQty";

                    if (!qtyField.isEmpty()) {
                        int updatedQty = currentAvailableQty - quantity;
                        currentAvailableQty = updatedQty;

                        eventRef.update(qtyField, updatedQty)
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) updateTicketUi();
                                });
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (binding != null && binding.loadingOverlay != null) {
            if (isLoading) {
                binding.loadingOverlay.setVisibility(View.VISIBLE);
                onBackPressedCallback.setEnabled(true);
            } else {
                binding.loadingOverlay.setVisibility(View.GONE);
                onBackPressedCallback.setEnabled(false);
            }
        }
    }

    private void loadEventData(String eventName) {
        db.collection("events")
                .whereEqualTo("eventName", eventName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        currentEvent = queryDocumentSnapshots.getDocuments().get(0).toObject(Event.class);
                        if (currentEvent != null) {
                            displayEventDetails();
                            checkIfEventIsFavorite();
                        }
                    } else {
                        Toast.makeText(getContext(), "Event not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayEventDetails() {
        if (currentEvent.imageUrl != null && !currentEvent.imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentEvent.imageUrl)
                    .placeholder(R.drawable.game)
                    .error(R.drawable.game)
                    .into(binding.eventPoster);
        }

        binding.eventName.setText(currentEvent.eventName);
        binding.eventCategory.setText(currentEvent.category);

        String dateTimeStr = currentEvent.eventDate + " | " + currentEvent.startTime + " - " + currentEvent.endTime;
        binding.eventDateTime.setText(dateTimeStr);

        binding.eventLocationText.setText(currentEvent.locationAddress);
        binding.eventDescription.setText(currentEvent.description);
        binding.organizerName.setText(currentEvent.organizerName);
        binding.organizerContact.setText("Contact: " + currentEvent.contactNumber);
        binding.salesEndInfo.setText("Sales end on " + currentEvent.saleEndDate);

        updateMapLocation();
        updateTicketUi();
    }

    private void setupTicketTypeSelection() {
        binding.ticketTypeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_standard) {
                    selectedTicketType = "Standard";
                } else if (checkedId == R.id.btn_vip) {
                    selectedTicketType = "VIP";
                } else if (checkedId == R.id.btn_vvip) {
                    selectedTicketType = "VVIP";
                }
                quantity = 1;
                binding.textQuantity.setText(String.valueOf(quantity));
                updateTicketUi();
            }
        });
    }

    private void updateTicketUi() {
        if (currentEvent == null) return;
        switch (selectedTicketType) {
            case "Standard":
                currentPricePerTicket = currentEvent.stdPrice;
                currentAvailableQty = currentEvent.stdQty;
                break;
            case "VIP":
                currentPricePerTicket = currentEvent.vipPrice;
                currentAvailableQty = currentEvent.vipQty;
                break;
            case "VVIP":
                currentPricePerTicket = currentEvent.vvipPrice;
                currentAvailableQty = currentEvent.vvipQty;
                break;
        }

        if (currentPricePerTicket <= 0 || currentAvailableQty <= 0) {
            binding.pricePerTicket.setVisibility(View.GONE);
            binding.ticketAvailability.setText("Not Available");
            binding.ticketAvailability.setTextColor(Color.RED);
            binding.totalPrice.setText("Total Price: Rs. 0.00");
            binding.btnBookNow.setEnabled(false);
        } else {
            binding.pricePerTicket.setVisibility(View.VISIBLE);

            String formattedPrice = String.format(Locale.US, "%.2f", currentPricePerTicket);
            binding.pricePerTicket.setText("Rs. " + formattedPrice + " per ticket");

            binding.ticketAvailability.setText("Available : " + currentAvailableQty);
            binding.ticketAvailability.setTextColor(Color.parseColor("#2E7D32"));

            double total = currentPricePerTicket * quantity;
            String formattedTotal = String.format(Locale.US, "%.2f", total);
            binding.totalPrice.setText("Total Price: Rs. " + formattedTotal);

            binding.btnBookNow.setEnabled(true);
        }
    }

    private void setupQuantityControls() {
        binding.btnRemoveQty.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.textQuantity.setText(String.valueOf(quantity));
                updateTicketUi();
            }
        });

        binding.btnAddQty.setOnClickListener(v -> {
            if (quantity < currentAvailableQty) {
                quantity++;
                binding.textQuantity.setText(String.valueOf(quantity));
                updateTicketUi();
            }
        });
    }

    private void setupFavoriteButton() {
        binding.btnFavorite.setOnClickListener(v -> {
            if (currentUid == null) {
                Toast.makeText(getContext(), "Please login to add favorites", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentEvent == null) return;

            binding.btnFavorite.setEnabled(false);

            DocumentReference favDocRef = db.collection("user_favorites").document(currentUid)
                    .collection("my_favorites").document(currentEvent.eventName);

            if (isFavorite) {
                favDocRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            isFavorite = false;
                            binding.btnFavorite.setEnabled(true);
                            binding.btnFavorite.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_primary));
                        })
                        .addOnFailureListener(e -> {
                            binding.btnFavorite.setEnabled(true);
                        });
            } else {
                String fullAddress = currentEvent.locationAddress;
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

                String startTimeOnly = currentEvent.startTime != null ? currentEvent.startTime : "";

                Map<String, Object> favData = new HashMap<>();
                favData.put("category", currentEvent.category);
                favData.put("eventName", currentEvent.eventName);
                favData.put("place", mainCity);
                favData.put("price", "Rs. " + String.format(Locale.US, "%.2f", currentEvent.stdPrice));
                favData.put("date", currentEvent.eventDate);
                favData.put("time", startTimeOnly);
                favData.put("imageUrl", currentEvent.imageUrl);

                favDocRef.set(favData)
                        .addOnSuccessListener(aVoid -> {
                            isFavorite = true;
                            binding.btnFavorite.setEnabled(true);
                            binding.btnFavorite.setIconTint(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark));
                        })
                        .addOnFailureListener(e -> {
                            binding.btnFavorite.setEnabled(true);
                        });
            }
        });
    }

    private void checkIfEventIsFavorite() {
        if (currentUid == null || currentEvent == null) return;

        db.collection("user_favorites")
                .document(currentUid)
                .collection("my_favorites")
                .document(currentEvent.eventName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    if (documentSnapshot.exists()) {
                        isFavorite = true;
                        binding.btnFavorite.setIconTint(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark));
                    } else {
                        isFavorite = false;
                        binding.btnFavorite.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_primary));
                    }
                })
                .addOnFailureListener(e -> {
                    isFavorite = false;
                    if (isAdded())
                        binding.btnFavorite.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.md_theme_primary));
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        updateMapLocation();
    }

    private void updateMapLocation() {
        if (googleMap != null && currentEvent != null) {
            LatLng eventLatLng = new LatLng(currentEvent.latitude, currentEvent.longitude);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(eventLatLng).title(currentEvent.eventName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, 15f));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}