package authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todo_app.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import Todo_list.MainTodoList;

public class LoginScreen extends AppCompatActivity {
    private TextInputEditText emailLogin, passwordLogin;
    private Button btnLogin, btnLogUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_screen);

        // Khởi tạo view
        emailLogin = findViewById(R.id.emailTextLogin);
        passwordLogin = findViewById(R.id.passwordTextLogin);
        btnLogin = findViewById(R.id.btnLogIn);
        btnLogUp = findViewById(R.id.btnLogUp);
        mAuth = FirebaseAuth.getInstance();

        // Nút chuyển sang màn đăng ký
        btnLogUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginScreen.this, LogUpScreen.class);
            startActivity(intent);
        });

        // Nút đăng nhập
        btnLogin.setOnClickListener(v -> {
            String email = emailLogin.getText() != null ? emailLogin.getText().toString().trim() : "";
            String password = passwordLogin.getText() != null ? passwordLogin.getText().toString().trim() : "";

            // Validate input
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginScreen.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(LoginScreen.this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Đăng nhập với Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginScreen.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginScreen.this, MainTodoList.class);
                            startActivity(intent);
                            finish();
                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException e) {
                                Toast.makeText(LoginScreen.this, "Email không tồn tại", Toast.LENGTH_LONG).show();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                Toast.makeText(LoginScreen.this, "Mật khẩu không chính xác", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(LoginScreen.this, "Lỗi đăng nhập: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }
}