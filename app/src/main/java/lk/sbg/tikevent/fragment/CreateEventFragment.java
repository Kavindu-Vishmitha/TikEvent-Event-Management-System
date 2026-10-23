package lk.sbg.tikevent.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.model.Event;

public class CreateEventFragment extends Fragment {

    private AutoCompleteTextView categoryDropdown;
    private TextInputEditText startTimeInput, endTimeInput, eventDateInput, locationInput, saleEndDateInput;
    private TextInputEditText etEventName, etDescription, etOrganizerName, etContact;
    private TextInputEditText etStdPrice, etStdQty, etVipPrice, etVipQty, etVvipPrice, etVvipQty;
    private TextInputLayout tlEventName, tlCategory, tlDescription, tlDate, tlStartTime, tlEndTime, tlLocation, tlOrganizer, tlContact, tlSaleDate, tlStdPrice, tlStdQty, tlVipPrice, tlVipQty, tlVvipPrice, tlVvipQty;

    private ImageView ivSelectedImage;
    private LinearLayout layoutUploadPlaceholder;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri selectedImageUri;

    private TextInputLayout tlEventImage;
    private MaterialCardView cardUploadImage;

    private double selectedLat = 0.0;
    private double selectedLng = 0.0;

    public CreateEventFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                displaySelectedImage(uri);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setToolbarTitle("Create Event");
            mainActivity.hideBottomNavigation(true);
            mainActivity.invalidateOptionsMenu();
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        initViews(view);

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

        setupListeners(view);
        setupCategoryDropdown();

        if (selectedImageUri != null) {
            displaySelectedImage(selectedImageUri);
        }

        return view;
    }

    private void initViews(View view) {
        ivSelectedImage = view.findViewById(R.id.iv_selected_image);
        layoutUploadPlaceholder = view.findViewById(R.id.layout_upload_placeholder);
        cardUploadImage = view.findViewById(R.id.card_upload_image);

        etEventName = view.findViewById(R.id.et_event_name);
        categoryDropdown = view.findViewById(R.id.category_dropdown);
        etDescription = view.findViewById(R.id.et_description);
        eventDateInput = view.findViewById(R.id.et_event_date);
        startTimeInput = view.findViewById(R.id.start_time);
        endTimeInput = view.findViewById(R.id.end_time);
        locationInput = view.findViewById(R.id.et_event_location);
        etOrganizerName = view.findViewById(R.id.et_organizer_name);
        etContact = view.findViewById(R.id.et_contact_number);
        etStdPrice = view.findViewById(R.id.et_std_price);
        etStdQty = view.findViewById(R.id.et_std_qty);
        etVipPrice = view.findViewById(R.id.et_vip_price);
        etVipQty = view.findViewById(R.id.et_vip_qty);
        etVvipPrice = view.findViewById(R.id.et_vvip_price);
        etVvipQty = view.findViewById(R.id.et_vvip_qty);
        saleEndDateInput = view.findViewById(R.id.sale_end_date);

        tlEventName = view.findViewById(R.id.tl_event_name);
        tlCategory = view.findViewById(R.id.tl_category);
        tlDescription = view.findViewById(R.id.tl_description);
        tlDate = view.findViewById(R.id.tl_event_date);
        tlStartTime = view.findViewById(R.id.tl_start_time);
        tlEndTime = view.findViewById(R.id.tl_end_time);
        tlLocation = view.findViewById(R.id.tl_event_location);
        tlEventImage = view.findViewById(R.id.tl_event_image);
        tlOrganizer = view.findViewById(R.id.tl_organizer_name);
        tlContact = view.findViewById(R.id.tl_contact_number);
        tlSaleDate = view.findViewById(R.id.tl_sale_end_date);
        tlStdPrice = view.findViewById(R.id.tl_std_price);
        tlStdQty = view.findViewById(R.id.tl_std_qty);
        tlVipPrice = view.findViewById(R.id.tl_vip_price);
        tlVipQty = view.findViewById(R.id.tl_vip_qty);
        tlVvipPrice = view.findViewById(R.id.tl_vvip_price);
        tlVvipQty = view.findViewById(R.id.tl_vvip_qty);
    }

    private void setupListeners(View view) {
        view.findViewById(R.id.card_upload_image).setOnClickListener(v -> galleryLauncher.launch("image/*"));

        startTimeInput.setOnClickListener(v -> showMaterialTimePicker(startTimeInput, tlStartTime, "Select Start Time"));
        endTimeInput.setOnClickListener(v -> showMaterialTimePicker(endTimeInput, tlEndTime, "Select End Time"));
        eventDateInput.setOnClickListener(v -> showDatePicker());
        saleEndDateInput.setOnClickListener(v -> showSaleEndDatePicker());

        addErrorClearWatcher(etEventName, tlEventName);
        addErrorClearWatcher(etDescription, tlDescription);
        addErrorClearWatcher(etOrganizerName, tlOrganizer);
        addErrorClearWatcher(etContact, tlContact);
        addErrorClearWatcher(etStdPrice, tlStdPrice);
        addErrorClearWatcher(etStdQty, tlStdQty);
        addErrorClearWatcher(etVipPrice, tlVipPrice);
        addErrorClearWatcher(etVipQty, tlVipQty);
        addErrorClearWatcher(etVvipPrice, tlVvipPrice);
        addErrorClearWatcher(etVvipQty, tlVvipQty);

        locationInput.setOnClickListener(v -> {
            EventLocationFragment locationFragment = new EventLocationFragment();
            locationFragment.show(getParentFragmentManager(), "EventLocationFragment");
        });

        getParentFragmentManager().setFragmentResultListener("locationKey", getViewLifecycleOwner(), (requestKey, bundle) -> {
            String address = bundle.getString("selectedAddress");
            if (address != null) {
                locationInput.setText(address);

                selectedLat = bundle.getDouble("lat");
                selectedLng = bundle.getDouble("lng");

                clearSingleError(tlLocation);
            }
        });

        categoryDropdown.setOnItemClickListener((parent, v, position, id) -> clearSingleError(tlCategory));

        view.findViewById(R.id.btn_publish_event).setOnClickListener(v -> {
            if (validateInputs()) {
                publishEvent();
            }
        });

        view.findViewById(R.id.btn_cancel_event).setOnClickListener(v -> resetFields());
    }

    private void clearSingleError(TextInputLayout layout) {
        if (layout != null && layout.getError() != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }

    private void addErrorClearWatcher(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearSingleError(layout);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private boolean validateInputs() {
        clearErrors();

        if (isEmpty(etEventName)) {
            setFieldError(tlEventName, "Event name required");
            etEventName.requestFocus();
            return false;
        }

        if (categoryDropdown.getText().toString().isEmpty()) {
            setFieldError(tlCategory, "Select category");
            categoryDropdown.requestFocus();
            return false;
        }

        if (isEmpty(etDescription)) {
            setFieldError(tlDescription, "Description required");
            etDescription.requestFocus();
            return false;
        }

        if (isEmpty(eventDateInput)) {
            setFieldError(tlDate, "Date required");
            eventDateInput.performClick();
            return false;
        }

        if (isEmpty(startTimeInput)) {
            setFieldError(tlStartTime, "Time Required");
            startTimeInput.performClick();
            return false;
        }

        if (isEmpty(endTimeInput)) {
            setFieldError(tlEndTime, "Time Required");
            endTimeInput.performClick();
            return false;
        }

        if (isEmpty(locationInput)) {
            setFieldError(tlLocation, "Location required");
            locationInput.requestFocus();
            return false;
        }

        if (selectedImageUri == null) {
            setFieldError(tlEventImage, "Please add a cover image");
            cardUploadImage.requestFocus();
            return false;
        }

        if (isEmpty(etOrganizerName)) {
            setFieldError(tlOrganizer, "Organizer name required");
            etOrganizerName.requestFocus();
            return false;
        }

        String contact = etContact.getText().toString().trim();
        if (contact.isEmpty()) {
            setFieldError(tlContact, "Contact required");
            etContact.requestFocus();
            return false;
        } else if (!Pattern.compile("^(?:0|94|\\+94)?(7(0|1|2|4|5|6|7|8)\\d{7})$").matcher(contact).matches()) {
            setFieldError(tlContact, "Invalid number");
            etContact.requestFocus();
            return false;
        }

        if (isEmpty(etStdPrice)) {
            setFieldError(tlStdPrice, "Required");
            etStdPrice.requestFocus();
            return false;
        }
        if (isEmpty(etStdQty)) {
            setFieldError(tlStdQty, "Required");
            etStdQty.requestFocus();
            return false;
        }
        if (isEmpty(saleEndDateInput)) {
            setFieldError(tlSaleDate, "Date Required");
            saleEndDateInput.performClick();
            return false;
        } else {
            try {

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

                Date eventDate = sdf.parse(eventDateInput.getText().toString().trim());
                Date saleEndDate = sdf.parse(saleEndDateInput.getText().toString().trim());

                if (eventDate != null && saleEndDate != null) {
                    if (saleEndDate.after(eventDate)) {
                        setFieldError(tlSaleDate, "Can not be after Event Date");
                        saleEndDateInput.performClick();
                        return false;
                    }
                }
            } catch (Exception e) {
                setFieldError(tlSaleDate, "Invalid format");
                return false;
            }
        }

        return true;
    }

    private void setFieldError(TextInputLayout layout, String error) {
        if (layout != null) {
            layout.setError(error);
            layout.setErrorEnabled(true);
        }
    }

    private boolean isEmpty(TextInputEditText et) {
        return et == null || et.getText() == null || et.getText().toString().trim().isEmpty();
    }

    private void resetFields() {
        TextInputEditText[] fields = {etEventName, etDescription, eventDateInput, startTimeInput, endTimeInput,
                locationInput, etOrganizerName, etContact, etStdPrice, etStdQty,
                etVipPrice, etVipQty, etVvipPrice, etVvipQty, saleEndDateInput};
        for (TextInputEditText f : fields) {
            if (f != null) f.setText("");
        }
        categoryDropdown.setText("", false);
        selectedImageUri = null;
        ivSelectedImage.setVisibility(View.GONE);
        layoutUploadPlaceholder.setVisibility(View.VISIBLE);
        clearErrors();

    }

    private void clearErrors() {
        TextInputLayout[] layouts = {tlEventName, tlCategory, tlDescription, tlDate, tlStartTime, tlEndTime,
                tlLocation, tlOrganizer, tlContact, tlStdPrice, tlStdQty, tlVipPrice, tlVipQty, tlVvipPrice, tlVvipQty, tlSaleDate, tlEventImage};
        for (TextInputLayout l : layouts) {
            if (l != null) {
                l.setError(null);
                l.setErrorEnabled(false);
            }
        }

        if (cardUploadImage != null) {
            cardUploadImage.setStrokeColor(getResources().getColor(R.color.md_theme_primary, null));
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        String uriString = savedInstanceState.getString("selected_image_uri");
        if (uriString != null) {
            selectedImageUri = Uri.parse(uriString);

        }
        eventDateInput.setText(savedInstanceState.getString("event_date"));
        startTimeInput.setText(savedInstanceState.getString("start_time"));
        endTimeInput.setText(savedInstanceState.getString("end_time"));
        locationInput.setText(savedInstanceState.getString("location"));
        saleEndDateInput.setText(savedInstanceState.getString("sale_date"));
        categoryDropdown.setText(savedInstanceState.getString("category"), false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedImageUri != null)
            outState.putString("selected_image_uri", selectedImageUri.toString());
        if (eventDateInput != null)
            outState.putString("event_date", eventDateInput.getText().toString());
        if (startTimeInput != null)
            outState.putString("start_time", startTimeInput.getText().toString());
        if (endTimeInput != null) outState.putString("end_time", endTimeInput.getText().toString());
        if (locationInput != null)
            outState.putString("location", locationInput.getText().toString());
        if (saleEndDateInput != null)
            outState.putString("sale_date", saleEndDateInput.getText().toString());
        if (categoryDropdown != null)
            outState.putString("category", categoryDropdown.getText().toString());
    }

    private void displaySelectedImage(Uri uri) {
        if (ivSelectedImage != null && layoutUploadPlaceholder != null) {
            ivSelectedImage.setImageURI(uri);
            ivSelectedImage.setVisibility(View.VISIBLE);
            layoutUploadPlaceholder.setVisibility(View.GONE);

            if (tlEventImage != null) {
                tlEventImage.setError(null);
                tlEventImage.setErrorEnabled(false);
                cardUploadImage.setStrokeColor(getResources().getColor(R.color.md_theme_primary, null));
            }
        }
    }

    private void showMaterialTimePicker(TextInputEditText targetField, TextInputLayout targetLayout, String title) {

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setTitleText(title)
                .build();

        picker.show(getChildFragmentManager(), "TIME_PICKER");

        picker.addOnPositiveButtonClickListener(v -> {
            int hour = picker.getHour();
            int minute = picker.getMinute();

            String amPm = (hour >= 12) ? "PM" : "AM";
            int hourIn12Format = (hour == 0 || hour == 12) ? 12 : hour % 12;

            @SuppressLint("DefaultLocale")
            String formattedTime = String.format("%02d:%02d %s", hourIn12Format, minute, amPm);

            targetField.setText(formattedTime);
            clearSingleError(targetLayout);
        });
    }

    private void setupCategoryDropdown() {
        String[] items = {"Music", "Gaming", "Sports", "Education", "Tech", "Art", "Food", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, items);
        categoryDropdown.setAdapter(adapter);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Event Date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        datePicker.addOnPositiveButtonClickListener(selection -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            eventDateInput.setText(sdf.format(new java.util.Date(selection)));
            clearSingleError(tlDate);
        });
    }

    private void showSaleEndDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Sale End Date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();
        datePicker.show(getParentFragmentManager(), "SALE_DATE_PICKER");
        datePicker.addOnPositiveButtonClickListener(selection -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            saleEndDateInput.setText(sdf.format(new java.util.Date(selection)));
            clearSingleError(tlSaleDate);
        });
    }

    private void publishEvent() {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null) {
            showCustomDialog("error", "Auth Error", "Please login again to publish events.");
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("event_covers/" + java.util.UUID.randomUUID().toString());

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {

                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String ImageUrl = uri.toString();

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        String eventId = db.collection("events").document().getId();

                        double stdPrice = parseDoubleSafe(etStdPrice);
                        int stdQty = parseIntSafe(etStdQty);
                        double vipPrice = parseDoubleSafe(etVipPrice);
                        int vipQty = parseIntSafe(etVipQty);
                        double vvipPrice = parseDoubleSafe(etVvipPrice);
                        int vvipQty = parseIntSafe(etVvipQty);

                        String eventName = etEventName.getText().toString().trim();
                        String eventPlace = locationInput.getText().toString().trim();
                        String eventDate = eventDateInput.getText().toString().trim();
                        String eventTime = startTimeInput.getText().toString().trim();

                        Event event = new Event(
                                currentUserId,
                                eventName,
                                categoryDropdown.getText().toString(),
                                etDescription.getText().toString().trim(),
                                eventDate,
                                eventTime,
                                endTimeInput.getText().toString(),
                                eventPlace,
                                selectedLat,
                                selectedLng,
                                etOrganizerName.getText().toString().trim(),
                                etContact.getText().toString().trim(),
                                stdPrice, stdQty,
                                vipPrice, vipQty,
                                vvipPrice, vvipQty,
                                saleEndDateInput.getText().toString(),
                                ImageUrl
                        );

                        db.collection("events").document(eventId)
                                .set(event)
                                .addOnSuccessListener(aVoid -> {

                                    try {
                                        lk.sbg.tikevent.util.NotificationDbHelper dbHelper = new lk.sbg.tikevent.util.NotificationDbHelper(requireContext());
                                        dbHelper.addNotification(
                                                currentUserId,
                                                eventName,
                                                eventPlace,
                                                eventDate,
                                                String.valueOf(stdPrice)
                                        );
                                    } catch (Exception e) {
                                        android.util.Log.e("SQLiteError", "Notification save failed: " + e.getMessage());
                                    }

                                    db.collection("users")
                                            .whereEqualTo("role", "Buyer")
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                if (!querySnapshot.isEmpty()) {
                                                    for (com.google.firebase.firestore.DocumentSnapshot buyerDoc : querySnapshot) {
                                                        String buyerId = buyerDoc.getId();

                                                        java.util.Map<String, Object> buyerNotification = new java.util.HashMap<>();
                                                        buyerNotification.put("userId", buyerId);
                                                        buyerNotification.put("eventName", eventName);
                                                        buyerNotification.put("place", eventPlace);
                                                        buyerNotification.put("date", eventDate);
                                                        buyerNotification.put("time", eventTime);
                                                        buyerNotification.put("price", String.valueOf(stdPrice));
                                                        buyerNotification.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                                                        db.collection("user_notifications").add(buyerNotification);
                                                    }
                                                }
                                            });

                                    showCustomDialog("success", "Success", "Event Published Successfully!");
                                    resetFields();
                                })
                                .addOnFailureListener(e -> {
                                    showCustomDialog("error", "Firestore Error", e.getMessage());
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    showCustomDialog("error", "Upload Failed", "Image Upload Failed: " + e.getMessage());
                });
    }

    private double parseDoubleSafe(TextInputEditText et) {
        String val = et.getText().toString().trim();
        return val.isEmpty() ? 0.0 : Double.parseDouble(val);
    }

    private int parseIntSafe(TextInputEditText et) {
        String val = et.getText().toString().trim();
        return val.isEmpty() ? 0 : Integer.parseInt(val);
    }

    private void showCustomDialog(String type, String title, String message) {
        if (getContext() == null) return;

        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.6f);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        FrameLayout iconContainer = dialog.findViewById(R.id.dialogIconContainer);
        TextView iconText = dialog.findViewById(R.id.dialogIconText);
        TextView tvTitle = dialog.findViewById(R.id.customDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.customDialogText);
        MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);

        if (type.equals("success")) {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_success);
            if (iconText != null) iconText.setText("✓");
            btnOk.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.md_theme_primary));
        } else {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error);
            if (iconText != null) iconText.setText("✕");
            btnOk.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.md_theme_error));
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}