package lk.sbg.tikevent.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.SignUpActivity;

public class AccountFragment extends Fragment {

    private ShapeableImageView profileImage;
    private FrameLayout profileImageContainer;
    private FirebaseStorage storage;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private Uri selectedImageUri = null;
    private TextView tvAccountTitle;
    private TextInputLayout tlName, tlUser, tlNic, tlMobile, tlAddress;
    private EditText etFullName, etUsername, etNic, etMobile, etAddress, etEmail;
    private MaterialButton btnSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews(view);
        setupGalleryLauncher();

        profileImageContainer.setOnClickListener(v -> openGallery());
        profileImage.setOnClickListener(v -> openGallery());

        loadUserData();
        setupTextWatchers();

        view.findViewById(R.id.btn_new_account).setOnClickListener(v -> {

            Intent intent = new Intent(getActivity(), SignUpActivity.class);
            startActivity(intent);
        });

        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                if (selectedImageUri != null) {
                    uploadProfileImage();
                } else {
                    updateProfile(null);
                }
            }
        });

        return view;
    }

    private void initViews(View v) {
        profileImage = v.findViewById(R.id.signup_profile_image);
        profileImageContainer = v.findViewById(R.id.profileImageContainer);
        tvAccountTitle = v.findViewById(R.id.tv_account_title);
        btnSave = v.findViewById(R.id.btn_save_account);

        etFullName = v.findViewById(R.id.signup_input_fullname);
        etUsername = v.findViewById(R.id.signup_input_username);
        etNic = v.findViewById(R.id.signup_input_nic);
        etMobile = v.findViewById(R.id.signup_input_mobile);
        etAddress = v.findViewById(R.id.signup_input_address);
        etEmail = v.findViewById(R.id.signup_input_email);

        etEmail.setEnabled(false);
        etEmail.setFocusable(false);

        tlName = (TextInputLayout) etFullName.getParent().getParent();
        tlUser = (TextInputLayout) etUsername.getParent().getParent();
        tlNic = (TextInputLayout) etNic.getParent().getParent();
        tlMobile = (TextInputLayout) etMobile.getParent().getParent();
        tlAddress = (TextInputLayout) etAddress.getParent().getParent();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        etEmail.setText(user.getEmail());
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                etFullName.setText(doc.getString("fullName"));
                etUsername.setText(doc.getString("username"));
                etNic.setText(doc.getString("nic"));
                etMobile.setText(doc.getString("mobile"));
                etAddress.setText(doc.getString("address"));
                tvAccountTitle.setText(doc.getString("username") + "'s Account");

                String img = doc.getString("profileImage");
                if (img != null && !img.isEmpty() && selectedImageUri == null) {
                    Glide.with(this).load(img).placeholder(R.drawable.user).into(profileImage);
                }
            }
        });
    }

    private void uploadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        StorageReference ref = storage.getReference().child("profile_images/" + user.getUid() + ".jpg");

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateProfile(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    android.util.Log.e("Firebase", "Image Upload Failed: " + e.getMessage());
                    showCustomDialog("error", "Image Upload Failed", e.getMessage());
                });
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            profileImage.setImageURI(selectedImageUri);
                        }
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private boolean validateInputs() {
        if (etFullName.getText().toString().trim().isEmpty()) {
            showError(tlName, "Full Name is required");
            return false;
        }
        if (etUsername.getText().toString().trim().isEmpty()) {
            showError(tlUser, "Username is required");
            return false;
        }
        String nic = etNic.getText().toString().trim();
        if (nic.isEmpty()) {
            showError(tlNic, "NIC is required");
            return false;
        } else if (!Pattern.compile("^(([0-9]{9}[vVxX])|([0-9]{12}))$").matcher(nic).matches()) {
            showError(tlNic, "Invalid NIC number");
            return false;
        }
        String mobile = etMobile.getText().toString().trim();
        if (mobile.isEmpty()) {
            showError(tlMobile, "Mobile number is required");
            return false;
        } else if (!Pattern.compile("^(?:0|94|\\+94)?(7(0|1|2|4|5|6|7|8)\\d{7})$").matcher(mobile).matches()) {
            showError(tlMobile, "Invalid mobile number");
            return false;
        }
        if (etAddress.getText().toString().trim().isEmpty()) {
            showError(tlAddress, "Address is required");
            return false;
        }
        return true;
    }

    private void showError(TextInputLayout layout, String msg) {
        layout.setErrorEnabled(true);
        layout.setError(msg);
        layout.requestFocus();
    }

    private void setupTextWatchers() {
        TextWatcher commonWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (getActivity() != null) {
                    View focusedView = getActivity().getCurrentFocus();
                    if (focusedView instanceof EditText) {
                        try {
                            TextInputLayout layout = (TextInputLayout) focusedView.getParent().getParent();
                            if (layout.isErrorEnabled()) {
                                layout.setError(null);
                                layout.setErrorEnabled(false);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        };
        etFullName.addTextChangedListener(commonWatcher);
        etUsername.addTextChangedListener(commonWatcher);
        etNic.addTextChangedListener(commonWatcher);
        etMobile.addTextChangedListener(commonWatcher);
        etAddress.addTextChangedListener(commonWatcher);
    }

    private void updateProfile(String newImageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> up = new HashMap<>();
        up.put("fullName", etFullName.getText().toString().trim());
        up.put("username", etUsername.getText().toString().trim());
        up.put("nic", etNic.getText().toString().trim());
        up.put("mobile", etMobile.getText().toString().trim());
        up.put("address", etAddress.getText().toString().trim());
        up.put("email", user.getEmail());

        if (newImageUrl != null) {
            up.put("profileImage", newImageUrl);
        }

        db.collection("users").document(user.getUid()).update(up).addOnSuccessListener(aVoid -> {
            if (isAdded()) {
                tvAccountTitle.setText(etUsername.getText().toString() + "'s Account");
                showCustomDialog("success", "Success", "Your profile has been updated successfully.");
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                showCustomDialog("error", "Update Failed", e.getMessage());
            }
        });
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