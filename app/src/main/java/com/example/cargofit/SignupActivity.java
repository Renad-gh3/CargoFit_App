package com.example.cargofit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    FirebaseAuth auth;
    DatabaseReference db;
    EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    Button signupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup); // تأكدي أن XML اسمه activity_signup.xml

        // 🔹 الربط مع Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference("users"); // root "users" node

        // 🔹 الربط مع الواجهة
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signupBtn = findViewById(R.id.signupBtn);

        signupBtn.setOnClickListener(view -> {
            String username = usernameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if(username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
                Toast.makeText(SignupActivity.this,"Please fill all fields",Toast.LENGTH_SHORT).show();
                return;
            }

            if(!password.equals(confirmPassword)){
                Toast.makeText(SignupActivity.this,"Passwords do not match",Toast.LENGTH_SHORT).show();
                return;
            }

            createNewUser(username, email, password);
        });

        TextView tvSignUp = findViewById(R.id.tvSignIn);
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, SigninActivity.class);
            startActivity(intent);
        });
    }

    private void createNewUser(String username, String email, String password){
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = auth.getCurrentUser().getUid();

                    Map<String,Object> userData = new HashMap<>();
                    userData.put("username", username);
                    userData.put("email", email);
                    userData.put("userId", userId);
                    userData.put("phone", "");

                    db.child(userId).setValue(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(SignupActivity.this,"Account created successfully!",Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(SignupActivity.this,"Database Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this,"Auth Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
                });
    }
}
