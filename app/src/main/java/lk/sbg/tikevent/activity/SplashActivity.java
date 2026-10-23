package lk.sbg.tikevent.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.sbg.tikevent.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        LinearLayout splashContainer = findViewById(R.id.splashContainer);
        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        ProgressBar progressBar = findViewById(R.id.splashProgressBar);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.splash_animation);
        animation.setInterpolator(new OvershootInterpolator(1.5f));
        splashContainer.startAnimation(animation);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            btnGetStarted.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String savedRole = prefs.getString("USER_ROLE", "Buyer");

                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("USER_ROLE", savedRole);
                startActivity(intent);
                finish();
            }, 2500);
        } else {
            btnGetStarted.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }

        btnGetStarted.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(SplashActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }, 2000);
        });
    }
}