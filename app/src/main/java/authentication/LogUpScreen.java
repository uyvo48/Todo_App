package authentication;

// Nhập các lớp cần thiết của Android, Firebase, và các thư viện khác để xử lý giao diện, xác thực, và chuyển màn hình
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

// Lớp Activity cho màn hình đăng ký của ứng dụng
public class LogUpScreen extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Khai báo các thành phần giao diện và Firebase Authentication
        TextInputEditText emailLogUp, passwordLogUp, confirmPasswordLogUp; // Trường nhập email, mật khẩu, và xác nhận mật khẩu
        Button btnRegister; // Nút đăng ký
        ImageButton btnBack; // Nút quay lại
        FirebaseAuth mAuth; // Đối tượng FirebaseAuth để xử lý xác thực

        super.onCreate(savedInstanceState);
        // Kích hoạt chế độ hiển thị toàn màn hình (Edge-to-Edge)
        EdgeToEdge.enable(this);
        // Thiết lập layout cho màn hình đăng ký
        setContentView(R.layout.activity_log_up_screen);
        // Xử lý padding cho các thanh hệ thống (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo các view từ layout
        emailLogUp = findViewById(R.id.emailLogUp);
        passwordLogUp = findViewById(R.id.passwordLogUp);
        confirmPasswordLogUp = findViewById(R.id.confirmPasswordLogUp);
        btnRegister = findViewById(R.id.btnLogUp);
        btnBack = findViewById(R.id.btnBack);
        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Xử lý sự kiện nhấn nút quay lại để chuyển sang màn hình đăng nhập
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogUpScreen.this, LoginScreen.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện nhấn nút đăng ký
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy dữ liệu từ các trường nhập
                String email, password, confirmPassword;
                email = String.valueOf(emailLogUp.getText());
                password = String.valueOf(passwordLogUp.getText());
                confirmPassword = String.valueOf(confirmPasswordLogUp.getText());

                // Kiểm tra đầu vào
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LogUpScreen.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return; // Thoát nếu email rỗng
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LogUpScreen.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return; // Thoát nếu mật khẩu rỗng
                }
                if (TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(LogUpScreen.this, "Enter ConfirmPassword", Toast.LENGTH_SHORT).show();
                    return; // Thoát nếu xác nhận mật khẩu rỗng
                }
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(LogUpScreen.this, "Confirm Password Error", Toast.LENGTH_SHORT).show();
                    return; // Thoát nếu mật khẩu và xác nhận không khớp
                }

                // Đăng ký tài khoản mới với Firebase
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Đăng ký thành công, hiển thị thông báo và chuyển về màn hình đăng nhập
                                    Toast.makeText(LogUpScreen.this, "Authentication create.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LogUpScreen.this, LoginScreen.class);
                                    startActivity(intent);
                                } else {
                                    // Đăng ký thất bại, hiển thị thông báo lỗi chung
                                    Toast.makeText(LogUpScreen.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}