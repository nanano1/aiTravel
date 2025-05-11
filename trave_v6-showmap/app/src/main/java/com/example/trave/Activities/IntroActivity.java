package com.example.trave.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.trave.R;

public class IntroActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        ConstraintLayout introBtn = findViewById(R.id.introBtn);
        introBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
                long userId = sharedPreferences.getLong("user_id", -1);  // 获取用户ID

                if (userId != -1) {
                    // 用户已登录，跳转到MainActivity
                    Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // 用户未登录，跳转到LoginActivity
                    Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }

            }
        });
    }


}
