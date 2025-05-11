package com.example.trave.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.trave.DatabaseHelper;
import com.example.trave.R;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameEditText, passwordEditText;
    private ConstraintLayout loginButton;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        dbHelper = new DatabaseHelper(this);
        if (dbHelper.getUserSize() == 0){
            dbHelper.addUser("hhh","123");
            dbHelper.addUser("jjj","123");
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (dbHelper.validateUser(username, password)) {

                    long userId = dbHelper.getUserId(username); // 获取用户ID
                    saveLoginState(userId); // 保存登录状态

                    // 跳转到MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveLoginState(long userId) {
        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("user_id", userId);  // 存储用户ID
        editor.apply();
        Log Log = null;
        Log.d("MyRouteActivity", "User ID: " + userId);
    }

}
