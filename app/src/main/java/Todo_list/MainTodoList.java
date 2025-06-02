package Todo_list;

// Nhập các lớp cần thiết của Android để xử lý Activity, Fragment, và giao diện

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.todo_app.R;
import com.example.todo_app.databinding.ActivityMainTodoListBinding;

// Lớp Activity chính để quản lý giao diện và điều hướng giữa các Fragment
public class MainTodoList extends AppCompatActivity implements OnTaskCreatedListener {
    // Biến binding để truy cập các thành phần giao diện từ layout XML
    ActivityMainTodoListBinding binding;
    // Biến lưu Fragment hiện tại
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo binding và thiết lập layout
        binding = ActivityMainTodoListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hiển thị HomeFragment mặc định khi Activity được tạo lần đầu
        if (savedInstanceState == null) {
            currentFragment = new HomeFragment();
            replaceFragment(currentFragment);
            binding.bottomNavigationView.setSelectedItemId(R.id.Home);
        }

        // Xử lý sự kiện chọn mục trên BottomNavigationView
        binding.bottomNavigationView.setOnNavigationItemSelectedListener((MenuItem item) -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            // Xác định Fragment tương ứng với mục được chọn
            if (itemId == R.id.Home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.Task) {
                selectedFragment = new TaskFragment();
                ((TaskFragment) selectedFragment).setOnTaskCreatedListener(this);
            } else if (itemId == R.id.Alarm) {
                selectedFragment = new CalendarFragment();
            }

            // Chỉ thay thế Fragment nếu Fragment được chọn không null và khác với Fragment hiện tại
            if (selectedFragment != null && (currentFragment == null || !selectedFragment.getClass().equals(currentFragment.getClass()))) {
                currentFragment = selectedFragment;
                replaceFragment(currentFragment);
                return true;
            }
            return false;
        });
    }

    // Thay thế Fragment hiện tại bằng Fragment mới
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
        Log.d("MainTodoList", "Replaced fragment: " + fragment.getClass().getSimpleName());
    }

    @Override
    public void onTaskCreatedSuccessfully() {
        // Chuyển về HomeFragment
        currentFragment = new HomeFragment();

        binding.bottomNavigationView.post(() -> {
            MenuItem homeItem = binding.bottomNavigationView.getMenu().findItem(R.id.Home);
            if (homeItem != null) {
                binding.bottomNavigationView.setOnNavigationItemSelectedListener(null); // Tạm thời gỡ listener
                binding.bottomNavigationView.setSelectedItemId(R.id.Home);
                binding.bottomNavigationView.setOnNavigationItemSelectedListener((MenuItem item) -> {
                    int itemId = item.getItemId();
                    Fragment selectedFragment = null;

                    if (itemId == R.id.Home) {
                        selectedFragment = new HomeFragment();
                    } else if (itemId == R.id.Task) {
                        selectedFragment = new TaskFragment();
                        ((TaskFragment) selectedFragment).setOnTaskCreatedListener(this);
                    } else if (itemId == R.id.Alarm) {
                        selectedFragment = new CalendarFragment();
                    }

                    currentFragment = selectedFragment;
                    replaceFragment(currentFragment);

                    return true;
                });
                Log.d("MainTodoList", "Manually selected Home item, ID: " + R.id.Home);
            } else {
                Log.e("MainTodoList", "Home item not found in menu");
            }
        });

        Log.d("MainTodoList", "Task created successfully, navigated to HomeFragment");
    }
}