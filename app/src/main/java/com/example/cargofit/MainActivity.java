package com.example.cargofit;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    DatabaseReference db;

    EditText email, password;
    Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 الربط مع Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference("users"); // root "users" node

        // 🔹 الربط مع الواجهة
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // 🔹 زر تسجيل الدخول
        btnLogin.setOnClickListener(view -> {
            String emailText = email.getText().toString().trim();
            String passText = password.getText().toString().trim();
            if(emailText.isEmpty() || passText.isEmpty()){
                Toast.makeText(MainActivity.this,"Please enter email & password",Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(emailText, passText)
                    .addOnSuccessListener(authResult -> {
                        String userId = auth.getCurrentUser().getUid();
                        Toast.makeText(MainActivity.this,"Login successful!",Toast.LENGTH_SHORT).show();
                        loadUserData(userId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this,"Login Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
                    });
        });

        // 🔹 زر تسجيل حساب جديد
        btnRegister.setOnClickListener(view -> {
            String emailText = email.getText().toString().trim();
            String passText = password.getText().toString().trim();
            if(emailText.isEmpty() || passText.isEmpty()){
                Toast.makeText(MainActivity.this,"Please enter email & password",Toast.LENGTH_SHORT).show();
                return;
            }

            createNewUser(emailText, passText);
        });
    }

    // 🔹 إنشاء مستخدم جديد وحفظه في Realtime Database
    private void createNewUser(String emailText, String passText) {
        auth.createUserWithEmailAndPassword(emailText, passText)
                .addOnSuccessListener(authResult -> {
                    String userId = auth.getCurrentUser().getUid();

                    Map<String,Object> userData = new HashMap<>();
                    userData.put("email", emailText);
                    userData.put("userId", userId);
                    userData.put("name", "");
                    userData.put("phone", "");

                    // 🔹 حفظ البيانات في Realtime Database
                    db.child(userId).setValue(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this,"Account created & saved in Realtime DB!",Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this,"Database Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this,"Auth Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
                });
    }

    // 🔹 تحميل بيانات المستخدم من Realtime Database
    private void loadUserData(String userId){
        db.child(userId).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if(dataSnapshot.exists()){
                        String emailFromDb = dataSnapshot.child("email").getValue(String.class);
                        Toast.makeText(MainActivity.this,"Welcome "+emailFromDb,Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this,"No Realtime DB data found",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this,"Database Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                });
    }
}
