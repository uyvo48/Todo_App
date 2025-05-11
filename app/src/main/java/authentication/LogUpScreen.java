package authentication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todo_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.ApiStatus;

public class LogUpScreen extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TextInputEditText emailLogUp, passwordLogUp, confirmPasswordLogUp;
        Button btnRegister ;
        ImageButton btnBack;
        FirebaseAuth mAuth;


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_up_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    emailLogUp = findViewById(R.id.emailLogUp);
    passwordLogUp = findViewById(R.id.passwordLogUp);
    confirmPasswordLogUp = findViewById(R.id.confirmPasswordLogUp);
    btnRegister = findViewById(R.id.btnLogUp);
    btnBack = findViewById(R.id.btnBack);
    mAuth = FirebaseAuth.getInstance();
    // nút chuyển sang màn login
    btnBack.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(LogUpScreen.this, LoginScreen.class);
            startActivity(intent);
        }
    });

    // nút thực hiện logic logUp
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email, password , confirmPassword;
                email = String.valueOf(emailLogUp.getText());
                password = String.valueOf(passwordLogUp.getText());
                confirmPassword  = String.valueOf(confirmPasswordLogUp.getText());

                if(TextUtils.isEmpty(email)){
                    Toast.makeText(LogUpScreen.this, "Enter Email", Toast.LENGTH_SHORT).show();
                }
                if(TextUtils.isEmpty(password)){
                    Toast.makeText(LogUpScreen.this, "Enter Password", Toast.LENGTH_SHORT).show();
                }
                if(TextUtils.isEmpty(confirmPassword)){
                    Toast.makeText(LogUpScreen.this, "Enter ConfirmPassword", Toast.LENGTH_SHORT).show();
                }
                if(!password.equals(confirmPassword)){
                    Toast.makeText(LogUpScreen.this, "Confirm Password Error", Toast.LENGTH_SHORT).show();

                }
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LogUpScreen.this, "Authentication create.",
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LogUpScreen.this, LoginScreen.class);
                                    startActivity(intent);
                                } else {
                                    // If sign in fails, display a message to the user.

                                    Toast.makeText(LogUpScreen.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
            }

        });

    }
}