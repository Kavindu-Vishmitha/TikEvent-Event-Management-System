package lk.sbg.tikevent.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import lk.sbg.tikevent.R;

public class EventEditFragment extends DialogFragment {

    private String eventId, currentImageUrl;
    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private TextInputLayout tlEventName, tlCategory, tlDescription, tlDate, tlStartTime, tlEndTime, tlLocation, tlOrgName, tlContact, tlSaleDate, tlStdPrice, tlStdQty;
    private TextInputEditText etEventName, etDescription, etDate, etStartTime, etEndTime, etLocation, etOrgName, etContact;
    private TextInputEditText etStdPrice, etStdQty, etVipPrice, etVipQty, etVvipPrice, etVvipQty, etSaleEndDate;
    private AutoCompleteTextView categoryDropdown;
    private ImageView imgEventPreview;
    private LinearLayout layoutUploadPlaceholder;
    private TextView tvDialogEventName;

    private ActivityResultLauncher<String> imagePickerLauncher;

    public EventEditFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                imgEventPreview.setImageURI(uri);
                imgEventPreview.setVisibility(View.VISIBLE);
                layoutUploadPlaceholder.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews(view);
        setupPickers();
        setupCategoryDropdown();
        setupResultListener();
        setupLocationClickListener();
        setupRealTimeValidation();

        if (getArguments() != null) {
            eventId = getArguments().getString("EVENT_ID");
            loadEventData(eventId);
        }

        view.findViewById(R.id.btn_close_dialog).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_cancel_event).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btn_publish_event).setOnClickListener(v -> {
            if (validateInputs()) {
                if (selectedImageUri != null) {
                    uploadImageAndUpdate();
                } else {
                    updateFirestore(currentImageUrl);
                }
            }
        });

        layoutUploadPlaceholder.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        imgEventPreview.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void initViews(View view) {
        tvDialogEventName = view.findViewById(R.id.tv_dialog_event_name);
        tlEventName = view.findViewById(R.id.tl_event_name);
        tlCategory = view.findViewById(R.id.tl_category);
        tlDescription = view.findViewById(R.id.tl_description);
        tlDate = view.findViewById(R.id.tl_event_date);
        tlStartTime = view.findViewById(R.id.tl_start_time);
        tlEndTime = view.findViewById(R.id.tl_end_time);
        tlLocation = view.findViewById(R.id.tl_event_location);
        tlOrgName = view.findViewById(R.id.tl_organizer_name);
        tlContact = view.findViewById(R.id.tl_contact_number);
        tlSaleDate = view.findViewById(R.id.tl_sale_end_date);
        tlStdPrice = view.findViewById(R.id.tl_std_price);
        tlStdQty = view.findViewById(R.id.tl_std_qty);

        etEventName = view.findViewById(R.id.et_event_name);
        categoryDropdown = view.findViewById(R.id.category_dropdown);
        etDescription = view.findViewById(R.id.et_description);
        etDate = view.findViewById(R.id.et_event_date);
        etStartTime = view.findViewById(R.id.et_start_time);
        etEndTime = view.findViewById(R.id.et_end_time);
        etLocation = view.findViewById(R.id.et_location);
        etOrgName = view.findViewById(R.id.et_organizer_name);
        etContact = view.findViewById(R.id.et_contact);
        etStdPrice = view.findViewById(R.id.et_std_price);
        etStdQty = view.findViewById(R.id.et_std_qty);
        etVipPrice = view.findViewById(R.id.et_vip_price);
        etVipQty = view.findViewById(R.id.et_vip_qty);
        etVvipPrice = view.findViewById(R.id.et_vvip_price);
        etVvipQty = view.findViewById(R.id.et_vvip_qty);
        etSaleEndDate = view.findViewById(R.id.et_sale_end_date);

        if (etLocation != null) {
            etLocation.setFocusable(false);
            etLocation.setClickable(true);
        }
        imgEventPreview = view.findViewById(R.id.img_event_preview);
        layoutUploadPlaceholder = view.findViewById(R.id.layout_upload_placeholder);
    }

    private void setupRealTimeValidation() {
        addErrorClearWatcher(etEventName, tlEventName);
        addErrorClearWatcher(etDescription, tlDescription);
        addErrorClearWatcher(etOrgName, tlOrgName);
        addErrorClearWatcher(etContact, tlContact);
        addErrorClearWatcher(etStdPrice, tlStdPrice);
        addErrorClearWatcher(etStdQty, tlStdQty);
        categoryDropdown.setOnItemClickListener((parent, v, position, id) -> clearSingleError(tlCategory));
    }

    private void addErrorClearWatcher(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) clearSingleError(layout);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void clearSingleError(TextInputLayout layout) {
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }

    private boolean validateInputs() {
        clearErrors();

        if (isEmpty(etEventName)) {
            setFieldError(tlEventName, "Event name required");
            return false;
        }
        if (categoryDropdown.getText().toString().isEmpty()) {
            setFieldError(tlCategory, "Select category");
            return false;
        }
        if (isEmpty(etDescription)) {
            setFieldError(tlDescription, "Description required");
            return false;
        }
        if (isEmpty(etDate)) {
            setFieldError(tlDate, "Date required");
            return false;
        }
        if (isEmpty(etStartTime)) {
            setFieldError(tlStartTime, "Time Required");
            return false;
        }
        if (isEmpty(etEndTime)) {
            setFieldError(tlEndTime, "Time Required");
            return false;
        }
        if (isEmpty(etLocation)) {
            setFieldError(tlLocation, "Location required");
            return false;
        }
        if (isEmpty(etOrgName)) {
            setFieldError(tlOrgName, "Organizer name required");
            return false;
        }

        String contact = etContact.getText().toString().trim();
        if (contact.isEmpty()) {
            setFieldError(tlContact, "Contact required");
            return false;
        } else if (!Pattern.compile("^(?:0|94|\\+94)?(7(0|1|2|4|5|6|7|8)\\d{7})$").matcher(contact).matches()) {
            setFieldError(tlContact, "Invalid number");
            return false;
        }

        if (isEmpty(etStdPrice)) {
            setFieldError(tlStdPrice, "Required");
            return false;
        }
        if (isEmpty(etStdQty)) {
            setFieldError(tlStdQty, "Required");
            return false;
        }

        if (isEmpty(etSaleEndDate)) {
            setFieldError(tlSaleDate, "Date Required");
            return false;
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date eventDate = sdf.parse(etDate.getText().toString().trim());
                Date saleEndDate = sdf.parse(etSaleEndDate.getText().toString().trim());
                if (eventDate != null && saleEndDate != null && saleEndDate.after(eventDate)) {
                    setFieldError(tlSaleDate, "Can not be after Event Date");
                    return false;
                }
            } catch (Exception e) {
                setFieldError(tlSaleDate, "Invalid format");
                return false;
            }
        }
        return true;
    }

    private void clearErrors() {
        TextInputLayout[] layouts = {tlEventName, tlCategory, tlDescription, tlDate, tlStartTime, tlEndTime, tlLocation, tlOrgName, tlContact, tlSaleDate, tlStdPrice, tlStdQty};
        for (TextInputLayout l : layouts) {
            if (l != null) {
                l.setError(null);
                l.setErrorEnabled(false);
            }
        }
    }

    private void setFieldError(TextInputLayout tl, String msg) {
        if (tl != null) {
            tl.setErrorEnabled(true);
            tl.setError(msg);
            tl.requestFocus();
        }
    }

    private boolean isEmpty(TextInputEditText et) {
        return et == null || et.getText() == null || et.getText().toString().trim().isEmpty();
    }

    private void setupPickers() {
        etDate.setOnClickListener(v -> showDatePicker(etDate, tlDate));
        etSaleEndDate.setOnClickListener(v -> showDatePicker(etSaleEndDate, tlSaleDate));
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime, tlStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime, tlEndTime));
    }

    private void showTimePicker(TextInputEditText editText, TextInputLayout layout) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setTitleText("Select Time")
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            int hour = picker.getHour();
            int minute = picker.getMinute();
            String amPm = (hour >= 12) ? "PM" : "AM";
            int hourIn12Format = (hour == 0 || hour == 12) ? 12 : hour % 12;
            editText.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12Format, minute, amPm));
            clearSingleError(layout);
        });
        picker.show(getChildFragmentManager(), "TIME_PICKER");
    }

    private void showDatePicker(TextInputEditText editText, TextInputLayout layout) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editText.setText(sdf.format(new Date(selection)));
            clearSingleError(layout);
        });
        picker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void setupLocationClickListener() {
        if (etLocation != null) {
            etLocation.setOnClickListener(v -> {
                EventLocationFragment locationFragment = new EventLocationFragment();
                locationFragment.show(getParentFragmentManager(), "EventLocationFragment");
            });
        }
    }

    private void setupResultListener() {
        getParentFragmentManager().setFragmentResultListener("locationKey", getViewLifecycleOwner(), (requestKey, bundle) -> {
            String address = bundle.getString("selectedAddress");
            if (address != null) {
                etLocation.setText(address);
                selectedLat = bundle.getDouble("lat");
                selectedLng = bundle.getDouble("lng");
                clearSingleError(tlLocation);
            }
        });
    }

    private void loadEventData(String id) {
        db.collection("events").document(id).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvDialogEventName.setText(doc.getString("eventName"));
                etEventName.setText(doc.getString("eventName"));
                categoryDropdown.setText(doc.getString("category"), false);
                etDescription.setText(doc.getString("description"));
                etDate.setText(doc.getString("eventDate"));
                etStartTime.setText(doc.getString("startTime"));
                etEndTime.setText(doc.getString("endTime"));
                etLocation.setText(doc.getString("locationAddress"));
                etOrgName.setText(doc.getString("organizerName"));
                etContact.setText(doc.getString("contactNumber"));

                if (doc.contains("latitude")) selectedLat = doc.getDouble("latitude");
                if (doc.contains("longitude")) selectedLng = doc.getDouble("longitude");

                Double stdPrice = doc.getDouble("stdPrice");
                etStdPrice.setText(stdPrice != null ? String.format(Locale.US, "%.2f", stdPrice) : "0.00");
                etStdQty.setText(String.valueOf(doc.get("stdQty") != null ? doc.get("stdQty") : "0"));

                Double vipPrice = doc.getDouble("vipPrice");
                etVipPrice.setText(vipPrice != null ? String.format(Locale.US, "%.2f", vipPrice) : "0.00");
                etVipQty.setText(String.valueOf(doc.get("vipQty") != null ? doc.get("vipQty") : "0"));

                Double vvipPrice = doc.getDouble("vvipPrice");
                etVvipPrice.setText(vvipPrice != null ? String.format(Locale.US, "%.2f", vvipPrice) : "0.00");
                etVvipQty.setText(String.valueOf(doc.get("vvipQty") != null ? doc.get("vvipQty") : "0"));

                etSaleEndDate.setText(doc.getString("saleEndDate"));

                currentImageUrl = doc.getString("imageUrl");
                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                    imgEventPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(currentImageUrl).listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onResourceReady(Drawable r, Object m, Target<Drawable> t, DataSource d, boolean f) {
                            layoutUploadPlaceholder.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object m, Target<Drawable> t, boolean f) {
                            return false;
                        }
                    }).into(imgEventPreview);
                }
            }
        });
    }

    private void updateFirestore(String imageUrl) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventName", etEventName.getText().toString().trim());
        event.put("category", categoryDropdown.getText().toString().trim());
        event.put("description", etDescription.getText().toString().trim());
        event.put("eventDate", etDate.getText().toString().trim());
        event.put("startTime", etStartTime.getText().toString().trim());
        event.put("endTime", etEndTime.getText().toString().trim());
        event.put("locationAddress", etLocation.getText().toString().trim());
        event.put("latitude", selectedLat);
        event.put("longitude", selectedLng);
        event.put("organizerName", etOrgName.getText().toString().trim());
        event.put("contactNumber", etContact.getText().toString().trim());
        event.put("imageUrl", imageUrl);
        event.put("saleEndDate", etSaleEndDate.getText().toString().trim());

        try {
            event.put("stdPrice", Double.parseDouble(etStdPrice.getText().toString()));
            event.put("stdQty", Integer.parseInt(etStdQty.getText().toString()));
            event.put("vipPrice", Double.parseDouble(etVipPrice.getText().toString().isEmpty() ? "0" : etVipPrice.getText().toString()));
            event.put("vipQty", Integer.parseInt(etVipQty.getText().toString().isEmpty() ? "0" : etVipQty.getText().toString()));
            event.put("vvipPrice", Double.parseDouble(etVvipPrice.getText().toString().isEmpty() ? "0" : etVvipPrice.getText().toString()));
            event.put("vvipQty", Integer.parseInt(etVvipQty.getText().toString().isEmpty() ? "0" : etVvipQty.getText().toString()));
        } catch (NumberFormatException e) {
            Log.e("ParseError", "Error parsing number fields");
        }

        db.collection("events").document(eventId).update(event)
                .addOnSuccessListener(aVoid -> showCustomDialog("success", "Success", "Event Updated Successfully!"))
                .addOnFailureListener(e -> showCustomDialog("error", "Update Failed", e.getMessage()));
    }

    private void uploadImageAndUpdate() {
        String fileName = "event_images/" + UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> updateFirestore(uri.toString())))
                .addOnFailureListener(e -> showCustomDialog("error", "Upload Failed", "Image upload failed: " + e.getMessage()));
    }

    private void setupCategoryDropdown() {
        String[] items = {"Music", "Gaming", "Sports", "Education", "Tech", "Art", "Food", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, items);
        categoryDropdown.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
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
            if (btnOk != null) {
                btnOk.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.md_theme_primary));
                btnOk.setOnClickListener(v -> {
                    dialog.dismiss();
                    dismiss();
                });
            }
        } else {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_error);
            if (iconText != null) iconText.setText("✕");
            if (btnOk != null) {
                btnOk.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.md_theme_error));
                btnOk.setOnClickListener(v -> dialog.dismiss());
            }
        }

        dialog.show();
    }
}