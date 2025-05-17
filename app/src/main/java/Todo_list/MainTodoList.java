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
public class MainTodoList extends AppCompatActivity {
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
            } else if (itemId == R.id.Alarm) {
                selectedFragment = new AccountFragment();
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

    // Cập nhật mục được chọn trên BottomNavigationView
    public void updateNavigationSelection(int itemId) {
        binding.bottomNavigationView.setSelectedItemId(itemId);
        Log.d("MainTodoList", "Updated navigation selection to item: " + itemId);
    }

    // Thay thế Fragment hiện tại bằng Fragment mới
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
        Log.d("MainTodoList", "Replaced fragment: " + fragment.getClass().getSimpleName());
    }

    // Xử lý sự kiện nhấn nút Back
    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (currentFragment instanceof TaskFragment) {
            // Nếu đang ở TaskFragment, quay lại Fragment trước đó và chọn mục Home
            Log.d("MainTodoList", "Handling back press in TaskFragment");
            getSupportFragmentManager().popBackStack();
            updateNavigationSelection(R.id.Home);
        } else {
            // Xử lý mặc định của nút Back
            super.onBackPressed();
        }
    }
}