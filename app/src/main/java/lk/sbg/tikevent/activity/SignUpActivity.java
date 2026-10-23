package lk.sbg.tikevent.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import lk.sbg.tikevent.R;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tlName, tlUser, tlNic, tlMobile, tlAddress, tlEmail, tlPass, tlConfirm;
    private EditText etName, etUser, etNic, etMobile, etAddress, etEmail, etPass, etConfirm;
    private RadioGroup rgRole;

    private ShapeableImageView profileImage;
    private FrameLayout profileImageContainer;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private Uri selectedImageUri = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        profileImage = findViewById(R.id.signup_profile_image);
        profileImageContainer = findViewById(R.id.profileImageContainer);

        rgRole = findViewById(R.id.signup_role_group);

        setupGalleryLauncher();
        profileImageContainer.setOnClickListener(v -> openGallery());

        initViews();

        MaterialButton signUpBtn = findViewById(R.id.signup_btn_signup);
        MaterialButton signInBtn = findViewById(R.id.signup_btn_signin);

        setupTextWatchers();

        signUpBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });

        signInBtn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            finish();
        });
    }

    @SuppressLint("CutPasteId")
    private void initViews() {
        tlName = (TextInputLayout) findViewById(R.id.signup_input_fullname).getParent().getParent();
        tlUser = (TextInputLayout) findViewById(R.id.signup_input_username).getParent().getParent();
        tlNic = (TextInputLayout) findViewById(R.id.signup_input_nic).getParent().getParent();
        tlMobile = (TextInputLayout) findViewById(R.id.signup_input_mobile).getParent().getParent();
        tlAddress = (TextInputLayout) findViewById(R.id.signup_input_address).getParent().getParent();
        tlEmail = (TextInputLayout) findViewById(R.id.signup_input_email).getParent().getParent();
        tlPass = (TextInputLayout) findViewById(R.id.signup_input_password).getParent().getParent();
        tlConfirm = (TextInputLayout) findViewById(R.id.signup_input_confirm_password).getParent().getParent();

        etName = findViewById(R.id.signup_input_fullname);
        etUser = findViewById(R.id.signup_input_username);
        etNic = findViewById(R.id.signup_input_nic);
        etMobile = findViewById(R.id.signup_input_mobile);
        etAddress = findViewById(R.id.signup_input_address);
        etEmail = findViewById(R.id.signup_input_email);
        etPass = findViewById(R.id.signup_input_password);
        etConfirm = findViewById(R.id.signup_input_confirm_password);
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPass.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        if (selectedImageUri != null) {
                            uploadProfileImage(uid);
                        } else {
                            saveUserData(uid, "");
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfileImage(String uid) {

        StorageReference ref = storage.getReference().child("profile_images/" + uid + ".jpg");

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {

                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveUserData(uid, uri.toString());
                    });
                })
                .addOnFailureListener(e -> {

                    android.util.Log.e("Firebase", "Image Upload Failed: " + e.getMessage());
                    saveUserData(uid, "");
                });
    }

    private void saveUserData(String uid, String imageUrl) {
        int selectedId = rgRole.getCheckedRadioButtonId();
        RadioButton rb = findViewById(selectedId);
        String role = (rb != null) ? rb.getText().toString() : "Buyer";

        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("fullName", etName.getText().toString().trim());
        user.put("username", etUser.getText().toString().trim());
        user.put("nic", etNic.getText().toString().trim());
        user.put("mobile", etMobile.getText().toString().trim());
        user.put("address", etAddress.getText().toString().trim());
        user.put("email", etEmail.getText().toString().trim());
        user.put("role", role);
        user.put("profileImage", imageUrl);

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("Firebase", "Firestore Success!");

                    Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Firebase", "Firestore Error: " + e.getMessage());
                    Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInputs() {
        clearAllErrors();

        if (etName.getText().toString().trim().isEmpty()) {
            showError(tlName, "Full Name is required");
            return false;
        }
        if (etUser.getText().toString().trim().isEmpty()) {
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

        String email = etEmail.getText().toString().trim();
        String emailRegex = "^[a-zA-Z0-9._%+-]+@(gmail|yahoo|hotmail|outlook|gov|live)\\.(com|lk|edu\\.lk|ac\\.lk|gov\\.lk|sch\\.lk|org\\.lk|net|in)$";
        if (email.isEmpty()) {
            showError(tlEmail, "Email is required");
            return false;
        } else if (!Pattern.compile(emailRegex).matcher(email).matches()) {
            showError(tlEmail, "Invalid email address");
            return false;
        }

        String pass = etPass.getText().toString();
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$";
        if (pass.isEmpty()) {
            showError(tlPass, "Password is required");
            return false;
        } else if (!Pattern.compile(passwordPattern).matcher(pass).matches()) {
            showError(tlPass, "6+ chars: A-z, 0-9 & symbols");
            return false;
        }
        String confirmPass = etConfirm.getText().toString();
        if (confirmPass.isEmpty()) {
            showError(tlConfirm, "Please confirm your password");
            return false;
        } else if (!confirmPass.equals(pass)) {
            showError(tlConfirm, "Passwords do not match");
            return false;
        }

        return true;
    }

    private void showError(TextInputLayout layout, String message) {
        layout.setErrorEnabled(true);
        layout.setError(message);
        layout.getEditText().requestFocus();
    }

    private void clearAllErrors() {
        TextInputLayout[] layouts = {tlName, tlUser, tlNic, tlMobile, tlAddress, tlEmail, tlPass, tlConfirm};
        for (TextInputLayout l : layouts) {
            l.setError(null);
            l.setErrorEnabled(false);
        }
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                profileImage.setImageURI(selectedImageUri);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void setupTextWatchers() {
        etName.addTextChangedListener(new MyErrorWatcher(tlName));
        etUser.addTextChangedListener(new MyErrorWatcher(tlUser));
        etNic.addTextChangedListener(new MyErrorWatcher(tlNic));
        etMobile.addTextChangedListener(new MyErrorWatcher(tlMobile));
        etAddress.addTextChangedListener(new MyErrorWatcher(tlAddress));
        etEmail.addTextChangedListener(new MyErrorWatcher(tlEmail));
        etPass.addTextChangedListener(new MyErrorWatcher(tlPass));
        etConfirm.addTextChangedListener(new MyErrorWatcher(tlConfirm));
    }

    private class MyErrorWatcher implements TextWatcher {
        private final TextInputLayout layout;

        public MyErrorWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (layout.isErrorEnabled()) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}