package lk.sbg.tikevent.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import lk.sbg.tikevent.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tlEmail;
    private EditText etEmail;
    private MaterialButton resetBtn, backBtn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        initViews();

        String passedEmail = getIntent().getStringExtra("USER_EMAIL");
        if (passedEmail != null) {
            etEmail.setText(passedEmail);
        }

        resetBtn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                showError("Please enter your email address");
            } else {
                sendResetLink(email);
            }
        });

        backBtn.setOnClickListener(v -> finish());

        setupTextWatcher();
    }

    private void initViews() {
        etEmail = findViewById(R.id.forgot_input_email);
        tlEmail = (TextInputLayout) etEmail.getParent().getParent();
        resetBtn = findViewById(R.id.forgot_btn_reset);
        backBtn = findViewById(R.id.forgot_btn_back);

        tlEmail.setErrorEnabled(false);
        tlEmail.setErrorIconDrawable(null);
    }

    private void sendResetLink(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        tlEmail.setErrorEnabled(true);
                        tlEmail.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#388E3C")));
                        tlEmail.setError("Check your email for the reset link");

                        new android.os.Handler().postDelayed(() -> {
                            finish();
                        }, 4000);

                    } else {
                        showError("Invalid email address");
                    }
                });
    }

    private void showError(String message) {
        tlEmail.setErrorEnabled(true);
        tlEmail.setErrorTextAppearance(R.style.ErrorTextAppearance);
        tlEmail.setError(message);
    }

    private void setupTextWatcher() {
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tlEmail.isErrorEnabled()) {
                    tlEmail.setError(null);
                    tlEmail.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}