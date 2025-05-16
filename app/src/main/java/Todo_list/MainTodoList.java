package Todo_list;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.todo_app.R;
import com.example.todo_app.databinding.ActivityMainTodoListBinding;

public class MainTodoList extends AppCompatActivity {
    ActivityMainTodoListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainTodoListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hiển thị HomeFragment mặc định
        replaceFragment(new HomeFragment());

        // Truy cập bottomNavigationView
        binding.bottomNavigationView.setOnNavigationItemSelectedListener((MenuItem item) -> {
            int itemId = item.getItemId();
            if (R.id.Home == itemId) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.Task) {
                replaceFragment(new TaskFragment());
                return true;
            } else if (itemId == R.id.Account) {
                replaceFragment(new AccountFragment());
                return true;
            }
            return false;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();

    }
}