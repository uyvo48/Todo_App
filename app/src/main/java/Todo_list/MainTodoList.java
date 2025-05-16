package Todo_list;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.todo_app.R;
import com.example.todo_app.databinding.ActivityMainTodoListBinding;

public class MainTodoList extends AppCompatActivity {
    ActivityMainTodoListBinding binding;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainTodoListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hiển thị HomeFragment mặc định
        if (savedInstanceState == null) {
            currentFragment = new HomeFragment();
            replaceFragment(currentFragment);
            binding.bottomNavigationView.setSelectedItemId(R.id.Home);
        }

        // Truy cập bottomNavigationView
        binding.bottomNavigationView.setOnNavigationItemSelectedListener((MenuItem item) -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.Home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.Task) {
                selectedFragment = new TaskFragment();
            } else if (itemId == R.id.Account) {
                selectedFragment = new AccountFragment();
            }

            if (selectedFragment != null && (currentFragment == null || !selectedFragment.getClass().equals(currentFragment.getClass()))) {
                currentFragment = selectedFragment;
                replaceFragment(currentFragment);
                return true;
            }
            return false;
        });
    }

    public void updateNavigationSelection(int itemId) {
        binding.bottomNavigationView.setSelectedItemId(itemId);
        Log.d("MainTodoList", "Updated navigation selection to item: " + itemId);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
        Log.d("MainTodoList", "Replaced fragment: " + fragment.getClass().getSimpleName());
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (currentFragment instanceof TaskFragment) {
            Log.d("MainTodoList", "Handling back press in TaskFragment");
            getSupportFragmentManager().popBackStack();
            updateNavigationSelection(R.id.Home);
        } else {
            super.onBackPressed();
        }
    }
}