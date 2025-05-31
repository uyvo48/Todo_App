package Todo_list;

// Nhập các lớp cần thiết của Android, Firebase, và Google Play Services để xử lý giao diện, xác thực, Firestore, và chuyển hướng
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import Adapter.HomeTaskAdapter;
import model.TaskModel;
import authentication.LoginScreen;
import com.example.todo_app.R;

// Lớp Fragment hiển thị danh sách nhiệm vụ trên màn hình chính
public class HomeFragment extends Fragment {
    // Khai báo các biến instance
    private ArrayList<TaskModel> taskList; // Danh sách nhiệm vụ
    private HomeTaskAdapter adapter; // Adapter để hiển thị nhiệm vụ trong ListView
    private ListView listView; // ListView hiển thị danh sách nhiệm vụ
    private CollectionReference tasksRef; // Tham chiếu đến bộ sưu tập Firestore chứa nhiệm vụ
    private FirebaseAuth mAuth; // Đối tượng FirebaseAuth để quản lý xác thực
    private String userId; // ID người dùng hiện tại

    // Constructor mặc định
    public HomeFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Giữ instance Fragment khi cấu hình thay đổi (ví dụ: xoay màn hình)

        // Kiểm tra Google Play Services
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("HomeFragment", "Google Play Services error: " + resultCode);
            if (isAdded()) {
                GoogleApiAvailability.getInstance().getErrorDialog(requireActivity(), resultCode, 0).show();
            }
            return;
        }

        // Khởi tạo danh sách nhiệm vụ và Firebase
        taskList = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Nếu chưa đăng nhập, chuyển hướng về LoginScreen
            startActivity(new Intent(requireContext(), LoginScreen.class));
            requireActivity().finish();
            return;
        }

        // Lấy userId và thiết lập tham chiếu Firestore
        userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        tasksRef = db.collection("users").document(userId).collection("tasks");

        // Khởi tạo adapter với callback để mở TaskFragment
        adapter = new HomeTaskAdapter(requireContext(), taskList, this::openTaskFragment, userId);
        loadTasksFromFirestore(); // Tải nhiệm vụ từ Firestore
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout cho Fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Khởi tạo các thành phần giao diện
        listView = view.findViewById(R.id.list_item_task);
        ImageButton addButton = view.findViewById(R.id.btnAdd_home);
        ImageButton logoutButton = view.findViewById(R.id.btnLogout);

        // Kiểm tra các thành phần giao diện
        if (listView == null || addButton == null || logoutButton == null) {
            Log.e("HomeFragment", "One or more views not found");
            return view;
        }

        // Thiết lập adapter cho ListView
        listView.setAdapter(adapter);

        // Xử lý sự kiện nhấn nút thêm nhiệm vụ
        addButton.setOnClickListener(v -> openTaskFragment(null, null));

        // Xử lý sự kiện nhấn nút đăng xuất
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Đăng xuất người dùng
            Log.d("HomeFragment", "User signed out");
            Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginScreen.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa stack hoạt động
            startActivity(intent);
//            requireActivity().finish(); // Kết thúc Activity hiện tại
        });

        return view;
    }

    // Tải danh sách nhiệm vụ từ Firestore
    private void loadTasksFromFirestore() {
        if (tasksRef == null) {
            Log.d("HomeFragment", "tasksRef is null, skipping load");
            return;
        }

        tasksRef.get().addOnSuccessListener(querySnapshot -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    taskList.clear(); // Xóa danh sách hiện tại
                    if (querySnapshot.isEmpty()) {
                        Log.d("HomeFragment", "No tasks in Firestore");
                        Toast.makeText(requireContext(), "No tasks to display", Toast.LENGTH_SHORT).show();
                    } else {
                        // Duyệt qua các tài liệu để tạo danh sách nhiệm vụ
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            TaskModel task = document.toObject(TaskModel.class);
                            task.setId(document.getId());
                            taskList.add(task);
                            Log.d("HomeFragment", "Loaded task: id=" + document.getId() + ", title=" + task.getTitle());
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged(); // Cập nhật giao diện
                        Log.d("HomeFragment", "Tasks loaded: " + taskList.size());
                    } else {
                        Log.e("HomeFragment", "Adapter is null when loading tasks");
                    }
                });
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
            Log.e("HomeFragment", "Failed to load tasks: " + e.getMessage());
        });
    }

    // Mở TaskFragment để xem hoặc thêm nhiệm vụ
    private void openTaskFragment(String taskId, String title) {
        TaskFragment taskFragment = TaskFragment.newInstance(title != null ? title : "", taskId != null ? taskId : "");
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, taskFragment)
                .addToBackStack(null) // Thêm vào back stack để quay lại
                .commit();
    }
}