package com.example.cargofit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        //If wanted to edit truck database
        TruckData td = new TruckData();
        td.onCreate(savedInstanceState);
        */
    }

    public void GoToPage(View view) {
        int id = view.getId();

        if (id == R.id.signinBtn) {
            // الانتقال لصفحة تسجيل الدخول
            Intent goToSignin = new Intent(MainActivity.this, SigninActivity.class);
            startActivity(goToSignin);
        } else if (id == R.id.signupBtn) {
            // الانتقال لصفحة إنشاء حساب
            Intent goToSignup = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(goToSignup);
        }
    }
}
