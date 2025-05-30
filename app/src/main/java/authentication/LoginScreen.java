package authentication;

// Nhập các lớp cần thiết của Android, Firebase, và Java để xử lý giao diện, xác thực, và chuyển màn hình
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

// Lớp Activity cho màn hình đăng nhập của ứng dụng
public class LoginScreen extends AppCompatActivity {
    // Khai báo các thành phần giao diện và Firebase Authentication
    private TextInputEditText emailLogin, passwordLogin; // Trường nhập email và mật khẩu
    private Button btnLogin, btnLogUp; // Nút đăng nhập và nút chuyển sang đăng ký
    private FirebaseAuth mAuth; // Đối tượng FirebaseAuth để xử lý xác thực

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kích hoạt chế độ hiển thị toàn màn hình (Edge-to-Edge)
        EdgeToEdge.enable(this);
        // Thiết lập layout cho màn hình đăng nhập
        setContentView(R.layout.activity_login_screen);

        // Khởi tạo các view từ layout
        emailLogin = findViewById(R.id.emailTextLogin);
        passwordLogin = findViewById(R.id.passwordTextLogin);
        btnLogin = findViewById(R.id.btnLogIn);
        btnLogUp = findViewById(R.id.btnLogUp);
        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Xử lý sự kiện nhấn nút chuyển sang màn hình đăng ký
        btnLogUp.setOnClickListener(v -> {
            // Tạo Intent để chuyển sang LogUpScreen
            Intent intent = new Intent(LoginScreen.this, LogUpScreen.class);
            startActivity(intent);
        });

        // Xử lý sự kiện nhấn nút đăng nhập
        btnLogin.setOnClickListener(v -> {
            // Lấy email và mật khẩu từ các trường nhập, loại bỏ khoảng trắng
            String email = emailLogin.getText() != null ? emailLogin.getText().toString().trim() : "";
            String password = passwordLogin.getText() != null ? passwordLogin.getText().toString().trim() : "";

            // Kiểm tra đầu vào
            if (TextUtils.isEmpty(email)) {
                // Hiển thị thông báo nếu email rỗng
                Toast.makeText(LoginScreen.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                // Hiển thị thông báo nếu mật khẩu rỗng
                Toast.makeText(LoginScreen.this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Đăng nhập bằng email và mật khẩu với Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Đăng nhập thành công, hiển thị thông báo và chuyển sang màn hình chính
                            Toast.makeText(LoginScreen.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginScreen.this, MainTodoList.class);
                            startActivity(intent);
                            // Kết thúc LoginScreen để người dùng không quay lại
                            finish();
                        } else {
                            // Xử lý lỗi đăng nhập
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException e) {
                                // Email không tồn tại trong Firebase
                                Toast.makeText(LoginScreen.this, "Email không tồn tại", Toast.LENGTH_LONG).show();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                // Mật khẩu không chính xác
                                Toast.makeText(LoginScreen.this, "Mật khẩu không chính xác", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                // Các lỗi khác, hiển thị thông báo với chi tiết lỗi
                                Toast.makeText(LoginScreen.this, "Lỗi đăng nhập: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }
}