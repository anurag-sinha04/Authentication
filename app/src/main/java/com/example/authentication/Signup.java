package com.example.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Signup extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // UI refs
    private EditText nameEt, emailEt, mobEt, passEt, cpassEt;
    private Button registerBtn;
    private TextView loginTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // bind views
        nameEt   = findViewById(R.id.name);
        emailEt  = findViewById(R.id.email);
        mobEt    = findViewById(R.id.mob);
        passEt   = findViewById(R.id.pass);
        cpassEt  = findViewById(R.id.conpass);
        registerBtn = findViewById(R.id.reg);
        loginTv  = findViewById(R.id.login);

        // init Firebase (outside inset listener)
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // login click -> go to login activity
        loginTv.setOnClickListener(v -> {
            startActivity(new Intent(Signup.this, MainActivity.class));
            finish();
        });

        // register click -> validate -> create auth -> write Firestore using email as doc id
        registerBtn.setOnClickListener(v -> {
            String name  = nameEt.getText().toString().trim();
            String email = emailEt.getText().toString().trim();
            String mobile= mobEt.getText().toString().trim();
            String pass  = passEt.getText().toString();
            String cpass = cpassEt.getText().toString();

            if (!validateInput(name, email, pass, cpass)) return;

            // disable to prevent double clicks
            registerBtn.setEnabled(false);

            // 1) Create auth user
            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // 2) Auth succeeded — get current user
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser == null) {
                                Toast.makeText(Signup.this, "Registration succeeded but no Firebase user.", Toast.LENGTH_LONG).show();
                                registerBtn.setEnabled(true);
                                return;
                            }

                            // Prepare map to save (DO NOT save password)
                            Map<String, Object> map = new HashMap<>();
                            map.put("name", name);
                            map.put("email", email.toLowerCase());
                            map.put("mobile", mobile);
                            map.put("password",pass);
                            map.put("createdAt", System.currentTimeMillis());

                            // 3) Save to Firestore using email as document id (as you requested)
                            db.collection("User_Data")
                                    .document(email)
                                    .set(map, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(Signup.this, "User saved", Toast.LENGTH_SHORT).show();
                                        // move to home (change as needed)
//                                        startActivity(new Intent(Signup.this, HomeActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(Signup.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        // Re-enable so user can retry or cancel
                                        registerBtn.setEnabled(true);
                                    });

                        } else {
                            // auth failed
                            String msg = task.getException() != null ? task.getException().getMessage() : "Auth failed";
                            Toast.makeText(Signup.this, "Auth error: " + msg, Toast.LENGTH_LONG).show();
                            registerBtn.setEnabled(true);
                        }
                    });
        });

        // Keep the inset listener strictly for padding only
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean validateInput(String name, String email, String pass, String cpass) {
        if (name.isEmpty()) {
            nameEt.setError("Name required");
            nameEt.requestFocus();
            return false;
        }
        if (email.isEmpty()) {
            emailEt.setError("Email required");
            emailEt.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEt.setError("Enter a valid email");
            emailEt.requestFocus();
            return false;
        }
        if (pass.length() < 6) {
            passEt.setError("Password must be at least 6 characters");
            passEt.requestFocus();
            return false;
        }
        if (!pass.equals(cpass)) {
            cpassEt.setError("Passwords do not match");
            cpassEt.requestFocus();
            return false;
        }
        return true;
    }
}
