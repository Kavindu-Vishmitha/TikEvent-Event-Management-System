package lk.sbg.tikevent.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.sbg.tikevent.R;

public class SignInActivity extends AppCompatActivity {

    private TextInputLayout tlEmail, tlPass;
    private EditText etEmail, etPass;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        MaterialButton signInBtn = findViewById(R.id.signin_btn_signin);
        MaterialButton signUpBtn = findViewById(R.id.signin_btn_signup);
        android.widget.TextView tvForgotPassword = findViewById(R.id.signin_forgot_password);

        signInBtn.setOnClickListener(v -> {
            if (isFieldsFilled()) {
                loginUser();
            }
        });

        signUpBtn.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);

            if (!email.isEmpty()) {
                intent.putExtra("USER_EMAIL", email);
            }
            startActivity(intent);
        });

        setupTextWatchers();
    }

    private void initViews() {
        etEmail = findViewById(R.id.signin_input_email);
        etPass = findViewById(R.id.signin_input_password);

        tlEmail = (TextInputLayout) etEmail.getParent().getParent();
        tlPass = (TextInputLayout) etPass.getParent().getParent();

        tlEmail.setErrorIconDrawable(null);
        tlPass.setErrorIconDrawable(null);

        tlEmail.setErrorEnabled(false);
        tlPass.setErrorEnabled(false);
    }

    private boolean isFieldsFilled() {
        return !etEmail.getText().toString().trim().isEmpty() &&
                !etPass.getText().toString().trim().isEmpty();
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPass.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRole(mAuth.getCurrentUser().getUid());
                    } else {
                        tlPass.setErrorEnabled(true);
                        tlPass.setError("Invalid Email address or password");
                    }
                });
    }

    private void checkUserRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");

                            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            prefs.edit().putString("USER_ROLE", role).apply();

                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.putExtra("USER_ROLE", role);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tlPass.isErrorEnabled()) {
                    tlPass.setError(null);
                    tlPass.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etEmail.addTextChangedListener(watcher);
        etPass.addTextChangedListener(watcher);
    }
}