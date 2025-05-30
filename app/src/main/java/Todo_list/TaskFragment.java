package Todo_list;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import Adapter.TaskAdapter;
import model.SubTaskModel;
import model.TaskModel;
import authentication.LoginScreen;
import com.example.todo_app.R;

public class TaskFragment extends Fragment {
    // Hằng số dùng để lưu trữ tham số truyền vào Fragment
    private static final String ARG_PARAM1 = "param1"; // Tiêu đề nhiệm vụ
    private static final String ARG_PARAM2 = "param2"; // ID của nhiệm vụ
    private static final String KEY_CURRENT_TITLE = "current_title"; // Key lưu tiêu đề trong trạng thái
    private static final String KEY_TASK_ID = "task_id"; // Key lưu ID nhiệm vụ trong trạng thái
    private static final String KEY_SUBTASKS_REF_PATH = "subtasks_ref_path"; // Key lưu đường dẫn subTasksRef
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1000; // Mã yêu cầu Google Play Services

    // Biến lưu trữ thông tin nhiệm vụ hiện tại
    private String taskId; // ID của nhiệm vụ hiện tại
    private String currentTitle; // Tiêu đề nhiệm vụ hiện tại

    // Biến quản lý danh sách sub-task và giao diện
    private ArrayList<SubTaskModel> subTaskList; // Danh sách các sub-task
    private TaskAdapter adapter; // Adapter để hiển thị danh sách sub-task trong ListView
    private ListView listView; // ListView hiển thị danh sách sub-task

    // Tham chiếu Firestore để lưu trữ và truy xuất dữ liệu
    private CollectionReference tasksRef; // Tham chiếu đến collection "tasks" trong Firestore
    private CollectionReference subTasksRef; // Tham chiếu đến collection "subTasks" trong Firestore

    // Biến quản lý xác thực và dữ liệu nhiệm vụ
    private FirebaseAuth mAuth; // Đối tượng FirebaseAuth để quản lý xác thực người dùng
    private TaskModel currentTask; // Mô hình dữ liệu của nhiệm vụ hiện tại

    // Constructor rỗng cần thiết cho Fragment
    public TaskFragment() {
    }

    // Tạo instance mới của TaskFragment với các tham số
    public static TaskFragment newInstance(String param1, String param2) {
        TaskFragment fragment = new TaskFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1); // Lưu tiêu đề
        args.putString(ARG_PARAM2, param2); // Lưu ID nhiệm vụ
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Giữ instance của Fragment khi cấu hình thay đổi (ví dụ: xoay màn hình)

        // Lấy tham số từ Bundle
        if (getArguments() != null) {
            taskId = getArguments().getString(ARG_PARAM2, ""); // Lấy ID nhiệm vụ
            currentTitle = getArguments().getString(ARG_PARAM1, ""); // Lấy tiêu đề nhiệm vụ
            Log.d("TaskFragment", "Received title from HomeFragment: " + currentTitle + ", taskId: " + taskId);
        } else {
            Log.w("TaskFragment", "Arguments are null, checking lifecycle"); // Ghi log nếu không có tham số
        }

        // Kiểm tra Google Play Services
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("TaskFragment", "Google Play Services error: " + resultCode);
            if (isAdded()) {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    // Hiển thị dialog để người dùng khắc phục lỗi Google Play Services
                    googleApiAvailability.getErrorDialog(requireActivity(), resultCode, REQUEST_GOOGLE_PLAY_SERVICES).show();
                } else {
                    // Thiết bị không hỗ trợ Google Play Services, chuyển hướng về màn hình đăng nhập
                    Toast.makeText(requireContext(), "This device does not support Google Play Services", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(requireContext(), LoginScreen.class));
                    requireActivity().finish();
                }
            }
            return;
        }

        // Khởi tạo danh sách sub-task và Firebase
        subTaskList = new ArrayList<>(); // Khởi tạo danh sách sub-task
        mAuth = FirebaseAuth.getInstance(); // Khởi tạo FirebaseAuth
        FirebaseUser currentUser = mAuth.getCurrentUser(); // Lấy thông tin người dùng hiện tại
        if (currentUser == null) {
            // Không có người dùng đã đăng nhập, chuyển hướng về màn hình đăng nhập
            Log.e("TaskFragment", "No authenticated user found, redirecting to LoginScreen");
            startActivity(new Intent(requireContext(), LoginScreen.class));
            requireActivity().finish();
            return;
        }

        // Khởi tạo Firestore và tham chiếu đến collection "tasks"
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        tasksRef = db.collection("users").document(currentUser.getUid()).collection("tasks");

        // Khôi phục trạng thái từ savedInstanceState nếu có
        if (savedInstanceState != null) {
            String savedTaskId = savedInstanceState.getString(KEY_TASK_ID, "");
            String savedTitle = savedInstanceState.getString(KEY_CURRENT_TITLE, "");
            if (!TextUtils.isEmpty(savedTaskId)) taskId = savedTaskId; // Khôi phục taskId
            if (!TextUtils.isEmpty(savedTitle)) currentTitle = savedTitle; // Khôi phục tiêu đề
            String subTasksRefPath = savedInstanceState.getString(KEY_SUBTASKS_REF_PATH);
            if (subTasksRefPath != null) {
                subTasksRef = db.collection(subTasksRefPath); // Khôi phục tham chiếu subTasksRef
            }
            Log.d("TaskFragment", "Restored from savedInstanceState: currentTitle=" + currentTitle + ", taskId=" + taskId);
        }

        // Khởi tạo subTasksRef nếu taskId không rỗng
        if (!TextUtils.isEmpty(taskId)) {
            subTasksRef = tasksRef.document(taskId).collection("subTasks");
            Log.d("TaskFragment", "Initialized subTasksRef with taskId: " + taskId);
        } else {
            Log.d("TaskFragment", "taskId is empty, this is a new task"); // Ghi log nếu đây là nhiệm vụ mới
        }

        // Khởi tạo adapter cho ListView
        adapter = new TaskAdapter(requireContext(), subTaskList, currentTitle, subTasksRef);
        Log.d("TaskFragment", "Initialized adapter in onCreate with subTasksRef: " + (subTasksRef != null ? "not null" : "null"));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu trạng thái để khôi phục khi cần
        outState.putString(KEY_CURRENT_TITLE, currentTitle); // Lưu tiêu đề
        outState.putString(KEY_TASK_ID, taskId); // Lưu ID nhiệm vụ
        if (subTasksRef != null) {
            outState.putString(KEY_SUBTASKS_REF_PATH, subTasksRef.getPath()); // Lưu đường dẫn subTasksRef
        }
        Log.d("TaskFragment", "Saved state: currentTitle=" + currentTitle + ", taskId=" + taskId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Tạo giao diện từ layout fragment_task
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        // Ánh xạ các thành phần giao diện
        TextInputEditText taskInputTitle = view.findViewById(R.id.textFieldTitle); // Ô nhập tiêu đề
        ImageButton addButton = view.findViewById(R.id.btnAdd_Task); // Nút thêm sub-task
        Button saveButton = view.findViewById(R.id.button); // Nút lưu nhiệm vụ
        listView = view.findViewById(R.id.list_item_task); // ListView hiển thị sub-task
        TextView taskMainTitle = view.findViewById(R.id.task_main_title); // Tiêu đề chính

        // Kiểm tra xem các thành phần giao diện có được tìm thấy không
        if (taskInputTitle == null || addButton == null || listView == null || saveButton == null || taskMainTitle == null) {
            Log.e("TaskFragment", "One or more views not found");
            return view;
        }

        // Thiết lập adapter cho ListView
        adapter = new TaskAdapter(requireContext(), subTaskList, currentTitle, subTasksRef);
        listView.setAdapter(adapter);
        Log.d("TaskFragment", "Adapter initialized and set to ListView");

        // Hiển thị tiêu đề nếu có
        if (!TextUtils.isEmpty(currentTitle)) {
            taskMainTitle.setText(currentTitle);
            taskMainTitle.setVisibility(View.VISIBLE);
            taskInputTitle.setText(currentTitle);
            Log.d("TaskFragment", "Displayed title in onCreateView: " + currentTitle);
        } else {
            taskMainTitle.setVisibility(View.GONE);
            Log.d("TaskFragment", "currentTitle is empty in onCreateView");
        }

        // Tải sub-task từ Firestore nếu có taskId
        if (subTasksRef != null && !TextUtils.isEmpty(taskId)) {
            Log.d("TaskFragment", "Loading sub-tasks for taskId: " + taskMainTitle);
            loadSubTasksFromFirestore(); // Gọi phương thức tải sub-task
        } else {
            Log.d("TaskFragment", "subTasksRef or taskId is null/empty, skipping sub-task load");
        }

        // Xử lý sự kiện nút thêm sub-task
        addButton.setOnClickListener(v -> {
            String title = taskInputTitle.getText() != null ? taskInputTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            currentTitle = title;
            taskMainTitle.setText(title);
            taskMainTitle.setVisibility(View.VISIBLE);
            taskInputTitle.setText(title);

            addNewSubTask(); // Thêm sub-task mới
        });

        // Xử lý sự kiện nút lưu
        saveButton.setOnClickListener(v -> {
            String title = taskInputTitle.getText() != null ? taskInputTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            currentTitle = title;
            currentTask = new TaskModel(title); // Tạo đối tượng TaskModel với tiêu đề

            // Lưu nhiệm vụ mới hoặc cập nhật nhiệm vụ hiện có
            if (TextUtils.isEmpty(taskId)) {
                DocumentReference newTaskRef = tasksRef.document();
                taskId = newTaskRef.getId();
                subTasksRef = tasksRef.document(taskId).collection("subTasks");
                newTaskRef.set(currentTask)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("TaskFragment", "Task created with taskId: " + taskId);
                            saveSubTasks(); // Lưu sub-task
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TaskFragment", "Failed to create task: " + e.getMessage());
                            Toast.makeText(requireContext(), "Failed to create task", Toast.LENGTH_SHORT).show();
                        });
            } else {
                tasksRef.document(taskId).set(currentTask)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("TaskFragment", "Task updated with taskId: " + taskId);
                            saveSubTasks(); // Lưu sub-task
                            // Chuyển về HomeFragment sau khi cập nhật thành công
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.frameLayout, new HomeFragment())
                                    .commit();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TaskFragment", "Failed to update task: " + e.getMessage());
                            Toast.makeText(requireContext(), "Failed to update task", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // Xử lý sự kiện nút quay lại
        ImageButton backButton = view.findViewById(R.id.btnBack);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // Thay thế Fragment hiện tại bằng HomeFragment
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frameLayout, new HomeFragment())
                        .commit();
                // Xóa toàn bộ back stack để đảm bảo trạng thái sạch
                requireActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                // Cập nhật BottomNavigationView để chọn mục "Home"
                if (requireActivity() instanceof MainTodoList) {
                    ((MainTodoList) requireActivity()).updateNavigationSelection(R.id.Home);
                }
                Log.d("TaskFragment", "Back button pressed, navigated to HomeFragment");
            });
        } else {
            Log.e("TaskFragment", "BackButton not found");
        }

        return view;
    }

    // Thêm sub-task mới vào danh sách
    private void addNewSubTask() {
        if (subTaskList == null) {
            subTaskList = new ArrayList<>(); // Khởi tạo danh sách nếu null
        }
        // Tạo ID cho sub-task mới
        String subTaskId = subTasksRef != null ? subTasksRef.document().getId() : FirebaseFirestore.getInstance().collection("temp").document().getId();
        SubTaskModel newSubTask = new SubTaskModel(subTaskId, "New Subtask", false); // Tạo sub-task mới

        subTaskList.add(newSubTask);
        Log.d("TaskFragment", "Sub-task added to list: id=" + subTaskId + ", content=" + newSubTask.getContent());

        // Cập nhật giao diện
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (listView != null) {
                listView.invalidateViews();
            }
            Log.d("TaskFragment", "Adapter notified, subTaskList size: " + subTaskList.size());
        } else {
            Log.e("TaskFragment", "Adapter is null when adding subtask");
        }
    }

    // Lưu danh sách sub-task vào Firestore
    private void saveSubTasks() {
        if (subTasksRef == null) {
            Log.e("TaskFragment", "subTasksRef is null");
            Toast.makeText(requireContext(), "Failed to save sub-tasks", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lưu từng sub-task vào Firestore
        for (SubTaskModel subTask : subTaskList) {
            if (!TextUtils.isEmpty(subTask.getContent()) && subTask.getId() != null) {
                subTasksRef.document(subTask.getId()).set(subTask)
                        .addOnSuccessListener(aVoid -> Log.d("TaskFragment", "Sub-task saved: " + subTask.getId()))
                        .addOnFailureListener(e -> Log.e("TaskFragment", "Failed to save sub-task: " + e.getMessage()));
            }
        }
        Toast.makeText(requireContext(), "Tasks saved", Toast.LENGTH_SHORT).show();
        // Chuyển về HomeFragment sau khi lưu
        requireActivity().getSupportFragmentManager().popBackStack();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new HomeFragment())
                .commit();
    }

    // Tải danh sách sub-task từ Firestore
    private void loadSubTasksFromFirestore() {
        if (subTasksRef == null || TextUtils.isEmpty(taskId)) {
            Log.e("TaskFragment", "subTasksRef or taskId is null/empty, cannot load sub-tasks. taskId: " + taskId);
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Cannot load sub-tasks: Invalid task ID", Toast.LENGTH_SHORT).show()
                );
            }
            return;
        }

        // Kiểm tra sự tồn tại của nhiệm vụ
        tasksRef.document(taskId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Log.e("TaskFragment", "Task does not exist in Firestore: " + taskId);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Task does not exist", Toast.LENGTH_SHORT).show()
                    );
                }
                return;
            }

            // C update title nếu chưa có
            if (TextUtils.isEmpty(currentTitle)) {
                currentTitle = documentSnapshot.getString("title");
                if (!TextUtils.isEmpty(currentTitle) && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        TextView taskMainTitle = getView().findViewById(R.id.task_main_title);
                        TextInputEditText taskInputTitle = getView().findViewById(R.id.textFieldTitle);
                        if (taskMainTitle != null && taskInputTitle != null) {
                            taskMainTitle.setText(currentTitle);
                            taskMainTitle.setVisibility(View.VISIBLE);
                            taskInputTitle.setText(currentTitle);
                            Log.d("TaskFragment", "Updated title from Firestore: " + currentTitle);
                        }
                    });
                }
            }

            // Tải danh sách sub-task
            subTasksRef.get().addOnSuccessListener(querySnapshot -> {
                if (!isAdded()) {
                    Log.w("TaskFragment", "Fragment not attached, skipping UI update");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    subTaskList.clear(); // Xóa danh sách sub-task hiện tại
                    Log.d("TaskFragment", "Cleared subTaskList, loading new data for taskId: " + taskId);
                    if (querySnapshot.isEmpty()) {
                        Log.d("TaskFragment", "No sub-tasks found in Firestore for taskId: " + taskId);
                        Toast.makeText(requireContext(), "No sub-tasks to display", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            SubTaskModel subTask = document.toObject(SubTaskModel.class);
                            subTask.setId(document.getId());
                            subTaskList.add(subTask); // Thêm sub-task vào danh sách
                            Log.d("TaskFragment", "Loaded sub-task: id=" + subTask.getId() + ", content=" + subTask.getContent());
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged(); // Thông báo adapter cập nhật dữ liệu
                        if (listView != null) {
                            listView.invalidateViews(); // Làm mới ListView
                            Log.d("TaskFragment", "Adapter notified, subTaskList size: " + subTaskList.size());
                        } else {
                            Log.e("TaskFragment", "ListView is null when updating sub-tasks");
                        }
                    } else {
                        Log.e("TaskFragment", "Adapter is null when updating sub-tasks");
                    }
                });
            }).addOnFailureListener(e -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to load sub-tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
                Log.e("TaskFragment", "Failed to load sub-tasks for taskId: " + taskId + ", error: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load task: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
            Log.e("TaskFragment", "Failed to check task existence for taskId: " + taskId + ", error: " + e.getMessage());
        });
    }

    // Phương thức xử lý kết quả từ Google Play Services (hiện bị comment)
    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            if (resultCode == RESULT_OK) {
                // Google Play Services đã được khắc phục, thử khởi tạo lại
                Log.d("TaskFragment", "Google Play Services resolved, retrying initialization");
                requireActivity().recreate();
            } else {
                // Người dùng hủy khắc phục Google Play Services
                Log.e("TaskFragment", "User cancelled Google Play Services resolution");
                Toast.makeText(requireContext(), "Google Play Services is required for this app", Toast.LENGTH_LONG).show();
                startActivity(new Intent(requireContext(), LoginScreen.class));
                requireActivity().finish();
            }
        }
    }
    */
}