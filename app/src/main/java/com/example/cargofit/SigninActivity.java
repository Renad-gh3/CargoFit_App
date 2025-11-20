package com.example.cargofit;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.widget.TextView;

public class SigninActivity extends AppCompatActivity {

    FirebaseAuth auth;
    EditText emailEditText, passwordEditText;
    Button signInBtn;
    TextView tvForgot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin); // XML صفحة Sign In

        // 🔹 الربط مع Firebase
        auth = FirebaseAuth.getInstance();

        // 🔹 الربط مع الواجهة
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText); // اضيفي android:id لهذا الحقل في XML
        signInBtn = findViewById(R.id.signInBtn); // اضيفي android:id لهذا الزر في XML
        tvForgot = findViewById(R.id.tvForgotPassword);

        signInBtn.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(SigninActivity.this,"Please enter email & password",Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(SigninActivity.this,"Login successful!",Toast.LENGTH_SHORT).show();
                        // هنا يمكنك التوجيه للصفحة الرئيسية مثلاً:
                        startActivity(new Intent(SigninActivity.this, UploadExcelActivity.class));
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SigninActivity.this,"Login Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
                    });
        });

        //go to sign up page
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SigninActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // reset password
        tvForgot.setOnClickListener(v -> {
            // إنشاء EditText داخل LinearLayout
            EditText resetMail = new EditText(SigninActivity.this);
            resetMail.setHint("Enter your email");
            resetMail.setPadding(50, 20, 50, 20); // padding لإعطاء مساحة داخل البوب أب

            androidx.appcompat.app.AlertDialog.Builder passwordResetDialog = new androidx.appcompat.app.AlertDialog.Builder(SigninActivity.this);
            passwordResetDialog.setTitle("Reset Password");
            passwordResetDialog.setMessage("Enter your email to receive reset link");

            // ضع EditText داخل Dialog
            passwordResetDialog.setView(resetMail);

            passwordResetDialog.setPositiveButton("Send", (dialog, which) -> {
                String email = resetMail.getText().toString().trim();
                if(email.isEmpty()){
                    Toast.makeText(SigninActivity.this, "Enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }

                auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(SigninActivity.this, "Reset link sent to your email", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(SigninActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            });

            passwordResetDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            // عرض Dialog
            androidx.appcompat.app.AlertDialog dialog = passwordResetDialog.create();
            dialog.show();
        });


    }
}
