package Todo_list;

// Nhập các lớp cần thiết của Android, Firebase, và Java để xử lý giao diện, xác thực, Firestore, báo thức, và quyền
import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.todo_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import Adapter.CalendarTaskAdapter;
import model.CalendarTaskModel;

// Lớp Fragment để quản lý lịch và nhiệm vụ của người dùng
public class CalendarFragment extends Fragment {
    // Các hằng số và biến tĩnh
    private static final String ARG_PARAM1 = "param1"; // Tham số 1
    private static final String ARG_PARAM2 = "param2"; // Tham số 2
    private static final String TAG = "AccountFragment"; // Tag để ghi log
    private static final int MAX_AUTH_RETRIES = 3; // Số lần thử xác thực tối đa
    private int authRetryCount = 0; // Đếm số lần thử xác thực

    // Các biến instance
    private String mParam1, mParam2; // Tham số đầu vào
    private CalendarView calendarView; // Lịch để chọn ngày
    private ListView listView; // Danh sách hiển thị nhiệm vụ
    private ImageButton addButton, backButton; // Nút thêm nhiệm vụ và quay lại
    private ArrayList<CalendarTaskModel> taskList; // Danh sách nhiệm vụ
    private CalendarTaskAdapter adapter; // Adapter để hiển thị nhiệm vụ
    private long selectedDate; // Ngày được chọn trên lịch
    private ActivityResultLauncher<String[]> permissionLauncher; // Launcher để yêu cầu quyền
    private FirebaseFirestore db; // Đối tượng Firestore
    private FirebaseAuth mAuth; // Đối tượng FirebaseAuth

    // Constructor mặc định
    public CalendarFragment() {
    }

    // Factory method để tạo instance mới của Fragment với tham số
    public static CalendarFragment newInstance(String param1, String param2) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lấy tham số từ Bundle nếu có
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Khởi tạo launcher để yêu cầu quyền thông báo và báo thức
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean postNotificationsGranted = result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);
            Boolean scheduleExactAlarmGranted = result.getOrDefault(Manifest.permission.SCHEDULE_EXACT_ALARM, false);
            if (!postNotificationsGranted || !scheduleExactAlarmGranted) {
                Toast.makeText(requireContext(), "Cần cấp quyền thông báo và báo thức", Toast.LENGTH_LONG).show();
            }
        });

        // Yêu cầu quyền
        requestPermissions();

        // Khởi tạo Firestore, FirebaseAuth, danh sách nhiệm vụ, và adapter
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        taskList = new ArrayList<>();
        adapter = new CalendarTaskAdapter(requireContext(), taskList, this::deleteTask);
        selectedDate = System.currentTimeMillis(); // Thiết lập ngày mặc định là hiện tại
        attemptAnonymousSignIn(); // Thử đăng nhập ẩn danh
    }

    // Thử đăng nhập ẩn danh với Firebase
    private void attemptAnonymousSignIn() {
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "Không có kết nối mạng, hiển thị tasks cục bộ", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No network connection, skipping anonymous auth");
            filterTasksByDate();
            return;
        }

        if (authRetryCount >= MAX_AUTH_RETRIES) {
            Toast.makeText(requireContext(), "Không thể xác thực sau " + MAX_AUTH_RETRIES + " lần thử, vui lòng kiểm tra cấu hình", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Max auth retries reached: " + MAX_AUTH_RETRIES);
            filterTasksByDate();
            return;
        }

        authRetryCount++;
        Log.d(TAG, "Attempting anonymous auth, attempt: " + authRetryCount);
        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous auth success");
                        authRetryCount = 0; // Đặt lại số lần thử
                        loadTasksFromFirestore(); // Tải nhiệm vụ từ Firestore
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Anonymous auth failed: " + errorMsg + ", retry count: " + authRetryCount);
                        if (errorMsg.contains("network") && authRetryCount < MAX_AUTH_RETRIES) {
                            // Thử lại sau 2 giây nếu lỗi mạng
                            new Handler(Looper.getMainLooper()).postDelayed(this::attemptAnonymousSignIn, 2000);
                        }
//                        else {
//                            Toast.makeText(requireContext(), "Lỗi xác thực: " + errorMsg, Toast.LENGTH_SHORT).show();
//                            filterTasksByDate();
//                        }
                    }
                });
    }

    // Yêu cầu quyền thông báo và báo thức
    private void requestPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SCHEDULE_EXACT_ALARM)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM);
            }
        }
        if (!permissions.isEmpty()) {
            permissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout cho Fragment
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        // Khởi tạo các thành phần giao diện
        calendarView = view.findViewById(R.id.calendarView);
        listView = view.findViewById(R.id.list_view_calendar);
        addButton = view.findViewById(R.id.imageButton5);
        backButton = view.findViewById(R.id.btnBack);

        // Kiểm tra các thành phần giao diện
        if (calendarView == null || listView == null || addButton == null || backButton == null) {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy giao diện", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Thiết lập adapter cho ListView
        listView.setAdapter(adapter);
        listView.setClickable(true);
        listView.setLongClickable(true);
        listView.setEnabled(true);

        // Xử lý sự kiện chọn ngày trên CalendarView
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate = calendar.getTimeInMillis();
            filterTasksByDate(); // Lọc nhiệm vụ theo ngày được chọn
        });

        // Xử lý sự kiện nhấn nút thêm nhiệm vụ
        addButton.setOnClickListener(v -> showAddTaskDialog());

        // Xử lý sự kiện nhấn giữ trên mục nhiệm vụ để đặt giờ
        listView.setOnItemLongClickListener((parent, view12, position, id) -> {
            if (taskList.isEmpty() || position < 0 || position >= taskList.size()) {
                Toast.makeText(requireContext(), "Không có task để đặt giờ", Toast.LENGTH_SHORT).show();
                return true;
            }
            showTimePickerDialog(position); // Hiển thị dialog chọn giờ
            return true;
        });

        // Xử lý sự kiện nhấn nút quay lại
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // Chuyển về HomeFragment
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frameLayout, new HomeFragment())
                        .commit();
                // Xóa back stack
                requireActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                // Cập nhật BottomNavigationView
//                if (requireActivity() instanceof MainTodoList) {
//                    ((MainTodoList) requireActivity()).updateNavigationSelection(R.id.Home);
//                }
                Log.d("TaskFragment", "Back button pressed, navigated to HomeFragment");
            });
        } else {
            Log.e("TaskFragment", "BackButton not found");
        }

        filterTasksByDate(); // Lọc nhiệm vụ theo ngày hiện tại
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && isNetworkAvailable()) {
            loadTasksFromFirestore(); // Tải nhiệm vụ từ Firestore nếu đã đăng nhập
        } else if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "Không có kết nối mạng, hiển thị tasks cục bộ", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No network connection in onResume");
            filterTasksByDate();
        } else {
            attemptAnonymousSignIn(); // Thử đăng nhập ẩn danh nếu chưa đăng nhập
        }
        Log.d(TAG, "onResume: Loaded tasks, size: " + taskList.size());
    }

    // Hiển thị dialog để thêm nhiệm vụ mới
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Thêm Task");

        final EditText input = new EditText(requireContext());
        input.setHint("Nhập tiêu đề task");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(requireContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            CalendarTaskModel task = new CalendarTaskModel(title, selectedDate, null);
            task.setId(String.valueOf(System.currentTimeMillis())); // ID dựa trên thời gian
            saveTaskToFirestore(task); // Lưu nhiệm vụ
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Hiển thị dialog để chọn giờ cho nhiệm vụ
    private void showTimePickerDialog(int position) {
        if (!isAdded()) {
            Toast.makeText(requireContext(), "Lỗi: Không thể mở dialog", Toast.LENGTH_SHORT).show();
            return;
        }

        if (taskList.isEmpty() || position < 0 || position >= taskList.size()) {
            Toast.makeText(requireContext(), "Không có task để đặt giờ", Toast.LENGTH_SHORT).show();
            return;
        }

        final CalendarTaskModel task = taskList.get(position);
        final int finalPosition = position;

        try {
            // Inflate layout cho dialog chọn giờ
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_picker, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setView(dialogView);

            TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
            Button btnOk = dialogView.findViewById(R.id.btnOk);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            if (timePicker == null || btnOk == null || btnCancel == null) {
                Toast.makeText(requireContext(), "Lỗi: Không tải được giao diện", Toast.LENGTH_SHORT).show();
                return;
            }

            timePicker.setIs24HourView(true); // Định dạng 24 giờ

            AlertDialog dialog = builder.create();

            btnOk.setOnClickListener(v -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                Log.d(TAG, "Selected time: " + time + " for task: " + task.getTitle() + ", id: " + task.getId());

                if (task.getTitle() == null || task.getId() == null) {
                    Toast.makeText(requireContext(), "Lỗi: Task không hợp lệ", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Invalid task: title=" + task.getTitle() + ", id=" + task.getId());
                    dialog.dismiss();
                    return;
                }

                task.setTime(time);
                taskList.set(finalPosition, task);
                Log.d(TAG, "Updated taskList at position: " + finalPosition + ", time: " + task.getTime());
                filterTasksByDate();
                saveTaskToFirestore(task);
                setAlarm(task); // Đặt báo thức
                Toast.makeText(requireContext(), "Giờ đã được cập nhật", Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Lỗi khi mở dialog", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error in showTimePickerDialog: ", e);
        }
    }

    // Đặt báo thức cho nhiệm vụ
    private void setAlarm(CalendarTaskModel task) {
        try {
            // Kiểm tra định dạng giờ
            if (task.getTime() == null || !task.getTime().matches("\\d{2}:\\d{2}")) {
                Toast.makeText(requireContext(), "Lỗi: Giờ không hợp lệ", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid time format: " + task.getTime());
                return;
            }

            // Kiểm tra tính hợp lệ của nhiệm vụ
            if (task.getId() == null || task.getTitle() == null) {
                Toast.makeText(requireContext(), "Lỗi: Task không hợp lệ", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid task: id=" + task.getId() + ", title=" + task.getTitle());
                return;
            }

            // Thiết lập thời gian báo thức
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(task.getDate());
            String[] timeParts = task.getTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long alarmTime = calendar.getTimeInMillis();
            if (alarmTime <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1); // Chuyển sang ngày tiếp theo nếu thời gian đã qua
                alarmTime = calendar.getTimeInMillis();
                Log.d(TAG, "Alarm time was in the past, rescheduled to next day: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(alarmTime));
            }

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            // Kiểm tra quyền báo thức chính xác trên Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(requireContext(), "Vui lòng cấp quyền báo thức trong Cài đặt", Toast.LENGTH_LONG).show();
                startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                Log.w(TAG, "Exact alarm permission not granted");
                return;
            }

            // Tạo Intent cho báo thức
            Intent intent = new Intent(requireContext(), AlarmReceiver.class);
            intent.putExtra("title", task.getTitle());
            intent.putExtra("taskId", task.getId());
            Log.d(TAG, "Creating PendingIntent for task: " + task.getTitle() + ", taskId: " + task.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    task.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String alarmTimeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(alarmTime);
            Log.d(TAG, "Setting alarm for task: " + task.getTitle() + " at " + alarmTimeStr);

            // Đặt báo thức chính xác
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
            );

            Toast.makeText(requireContext(), "Đã đặt báo thức cho " + task.getTitle() + " lúc " + task.getTime(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Lỗi khi đặt báo thức", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error in setAlarm: ", e);
        }
    }

    // Xóa nhiệm vụ từ Firestore
    private void deleteTask(String taskId) {
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "Không có kết nối mạng, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No network connection for deleteTask");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập, thử lại", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No authenticated user for deleteTask");
            attemptAnonymousSignIn();
            return;
        }

        // Xóa nhiệm vụ từ Firestore
        db.collection("users").document(user.getUid()).collection("calendartask").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Xóa nhiệm vụ khỏi danh sách cục bộ
                    for (int i = 0; i < taskList.size(); i++) {
                        if (taskList.get(i).getId().equals(taskId)) {
                            taskList.remove(i);
                            break;
                        }
                    }
                    filterTasksByDate();
                    Toast.makeText(requireContext(), "Task đã được xóa", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Task deleted from Firestore: " + taskId);
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    Toast.makeText(requireContext(), "Lỗi khi xóa task: " + errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting task: " + errorMsg, e);
                });
    }

    // Lọc danh sách nhiệm vụ theo ngày được chọn
    private void filterTasksByDate() {
        ArrayList<CalendarTaskModel> filteredList = new ArrayList<>();
        for (CalendarTaskModel task : taskList) {
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTimeInMillis(task.getDate());
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTimeInMillis(selectedDate);
            if (taskCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                    taskCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                    taskCal.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH)) {
                filteredList.add(task);
                Log.d(TAG, "Filtered task: " + task.getTitle() + ", time: " + task.getTime());
            }
        }
        adapter.updateList(filteredList);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered tasks: " + filteredList.size());
    }

    // Lưu nhiệm vụ vào Firestore
    private void saveTaskToFirestore(CalendarTaskModel task) {
        // Kiểm tra tính hợp lệ của nhiệm vụ
        if (task.getId() == null || task.getTitle() == null || task.getDate() <= 0) {
            Toast.makeText(requireContext(), "Dữ liệu task không hợp lệ", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid task data: id=" + task.getId() + ", title=" + task.getTitle() + ", date=" + task.getDate());
            if (!taskList.contains(task)) {
                taskList.add(task);
            }
            filterTasksByDate();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "Không có kết nối mạng, task lưu tạm cục bộ", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No network connection for saveTask");
            if (!taskList.contains(task)) {
                taskList.add(task);
            }
            filterTasksByDate();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập, thử lại", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No authenticated user");
            attemptAnonymousSignIn();
            return;
        }

        // Tạo dữ liệu nhiệm vụ để lưu vào Firestore
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("title", task.getTitle());
        taskMap.put("date", task.getDate());
        taskMap.put("time", task.getTime());

        Log.d(TAG, "Attempting to save task to Firestore: " + task.getTitle() + ", id: " + task.getId() + ", time: " + task.getTime());

        // Lưu nhiệm vụ vào Firestore
        db.collection("users").document(user.getUid()).collection("calendartask").document(task.getId())
                .set(taskMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task saved to Firestore: " + task.getTitle() + ", id: " + task.getId());
                    Toast.makeText(requireContext(), "Task đã được lưu", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    Toast.makeText(requireContext(), "Lỗi khi lưu task: " + errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving task: " + errorMsg + ", task: " + task.getTitle() + ", id: " + task.getId(), e);
                    if (!taskList.contains(task)) {
                        taskList.add(task);
                    }
                    filterTasksByDate();
                });
    }

    // Tải danh sách nhiệm vụ từ Firestore
    private void loadTasksFromFirestore() {
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "Không có kết nối mạng, hiển thị tasks cục bộ", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No network connection for loadTasks");
            filterTasksByDate();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập, thử lại", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No authenticated user for loadTasks");
            attemptAnonymousSignIn();
            return;
        }

        // Lắng nghe snapshot từ Firestore để tải và cập nhật nhiệm vụ
        db.collection("users").document(user.getUid()).collection("calendartask")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(requireContext(), "Lỗi khi tải tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading tasks: ", e);
                        filterTasksByDate();
                        return;
                    }

                    taskList.clear();
                    if (snapshots.isEmpty()) {
                        Log.d(TAG, "No tasks found in Firestore");
                        filterTasksByDate();
                        return;
                    }

                    // Duyệt qua các tài liệu để tạo danh sách nhiệm vụ
                    for (QueryDocumentSnapshot document : snapshots) {
                        try {
                            String id = document.getString("id");
                            String title = document.getString("title");
                            Long date = document.getLong("date");
                            String time = document.getString("time");

                            if (id == null || title == null || date == null) {
                                Log.w(TAG, "Invalid task document: " + document.getId() + ", id=" + id + ", title=" + title + ", date=" + date);
                                continue;
                            }

                            CalendarTaskModel calendarTask = new CalendarTaskModel();
                            calendarTask.setId(id);
                            calendarTask.setTitle(title);
                            calendarTask.setDate(date);
                            calendarTask.setTime(time);
                            taskList.add(calendarTask);
                            Log.d(TAG, "Loaded task: " + title + ", id: " + id + ", time: " + time);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing task document: " + document.getId(), ex);
                        }
                    }
                    Log.d(TAG, "Loaded tasks from Firestore: " + taskList.size());
                    filterTasksByDate();
                });
    }

    // Kiểm tra kết nối mạng
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "Network available: " + isConnected);
        return isConnected;
    }
}