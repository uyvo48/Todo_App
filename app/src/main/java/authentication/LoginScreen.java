package authentication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todo_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

import FlagScreen.FlagScreen;
import Todo_list.MainList;

public class LoginScreen extends AppCompatActivity {
    TextInputEditText emailLogin, passwordLogin;
    Button btnLogin, btnLogUp;
FirebaseAuth mAth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_screen);
        emailLogin = findViewById(R.id.emailTextLogin);
        passwordLogin = findViewById(R.id.passwordTextLogin);
        btnLogin = findViewById(R.id.btnLogIn);
        btnLogUp = findViewById(R.id.btnLogUp);
         mAth = FirebaseAuth.getInstance()  ;


        //  nút chuyển sang màn login
        btnLogUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginScreen.this, LogUpScreen.class);
                startActivity(intent);
            }
        });
        // nút login của Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email, password , confirmPassword;
                email = String.valueOf(emailLogin.getText());
                password = String.valueOf(passwordLogin.getText());


                if(TextUtils.isEmpty(email)){
                    Toast.makeText(LoginScreen.this, "Enter Email", Toast.LENGTH_SHORT).show();
                }
                if(TextUtils.isEmpty(password)){
                    Toast.makeText(LoginScreen.this, "Enter Password", Toast.LENGTH_SHORT).show();
                }
                btnLogin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String email, password;
                        email = String.valueOf(emailLogin.getText()).trim();
                        password = String.valueOf(passwordLogin.getText()).trim();

                        // Validate input
                        if (TextUtils.isEmpty(email)) {
                            Toast.makeText(LoginScreen.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                            return; // Quan trọng: phải có return để dừng xử lý
                        }
                        if (TextUtils.isEmpty(password)) {
                            Toast.makeText(LoginScreen.this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                            return; // Quan trọng: phải có return để dừng xử lý
                        }

                        mAth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(LoginScreen.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(LoginScreen.this, MainList.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            try {
                                                throw Objects.requireNonNull(task.getException());
                                            } catch (FirebaseAuthInvalidUserException e) {
                                                Toast.makeText(LoginScreen.this, "Email không tồn tại", Toast.LENGTH_LONG).show();
                                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                                Toast.makeText(LoginScreen.this, "Mật khẩu không chính xác", Toast.LENGTH_LONG).show();
                                            } catch (Exception e) {
                                                Toast.makeText(LoginScreen.this, "Lỗi đăng nhập: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                });
                    }
                });
            }
        });





    }
}