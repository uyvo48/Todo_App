package Todo_list;

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

public class AccountFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "AccountFragment";

    private String mParam1;
    private String mParam2;
    private CalendarView calendarView;
    private ListView listView;
    private ImageButton addButton;
    private ImageButton backButton;
    private ArrayList<CalendarTaskModel> taskList;
    private CalendarTaskAdapter adapter;
    private long selectedDate;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public AccountFragment() {
    }

    public static AccountFragment newInstance(String param1, String param2) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean postNotificationsGranted = result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);
            Boolean scheduleExactAlarmGranted = result.getOrDefault(Manifest.permission.SCHEDULE_EXACT_ALARM, false);
            if (!postNotificationsGranted || !scheduleExactAlarmGranted) {
                Toast.makeText(requireContext(), "Cần cấp quyền thông báo và báo thức", Toast.LENGTH_LONG).show();
            }
        });

        requestPermissions();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        taskList = new ArrayList<>();
        adapter = new CalendarTaskAdapter(requireContext(), taskList, this::deleteTask);
        selectedDate = System.currentTimeMillis();
        signInAnonymously();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous auth success");
                        loadTasksFromFirestore();
                    }
//                    else {
//                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
//                        Toast.makeText(requireContext(), "Lỗi xác thực: " + errorMsg, Toast.LENGTH_SHORT).show();
//                        Log.e(TAG, "Anonymous auth failed: " + errorMsg);
//                    }
                });
    }

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
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        listView = view.findViewById(R.id.list_view_calendar);
        addButton = view.findViewById(R.id.imageButton5);
        backButton = view.findViewById(R.id.btnBack);

        if (calendarView == null || listView == null || addButton == null || backButton == null) {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy giao diện", Toast.LENGTH_SHORT).show();
            return view;
        }

        listView.setAdapter(adapter);
        listView.setClickable(true);
        listView.setLongClickable(true);
        listView.setEnabled(true);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate = calendar.getTimeInMillis();
            filterTasksByDate();
        });

        addButton.setOnClickListener(v -> showAddTaskDialog());

        listView.setOnItemLongClickListener((parent, view12, position, id) -> {
            if (taskList.isEmpty() || position < 0 || position >= taskList.size()) {
                Toast.makeText(requireContext(), "Không có task để đặt giờ", Toast.LENGTH_SHORT).show();
                return true;
            }
            showTimePickerDialog(position);
            return true;
        });

        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, new HomeFragment())
                    .commit();
        });

        filterTasksByDate();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadTasksFromFirestore();
        } else {
            signInAnonymously();
        }
        Log.d(TAG, "onResume: Loaded tasks, size: " + taskList.size());
    }

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
            task.setId(String.valueOf(System.currentTimeMillis()));
            saveTaskToFirestore(task);
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

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

            timePicker.setIs24HourView(true);

            AlertDialog dialog = builder.create();

            btnOk.setOnClickListener(v -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                task.setTime(time);

                taskList.set(finalPosition, task);
                saveTaskToFirestore(task);
                setAlarm(task);
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

    private void setAlarm(CalendarTaskModel task) {
        try {
            if (task.getTime() == null || !task.getTime().matches("\\d{2}:\\d{2}")) {
                Toast.makeText(requireContext(), "Lỗi: Giờ không hợp lệ", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid time format: " + task.getTime());
                return;
            }

            if (task.getId() == null) {
                Toast.makeText(requireContext(), "Lỗi: Task không hợp lệ", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Task ID is null");
                return;
            }

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
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                alarmTime = calendar.getTimeInMillis();
            }

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(requireContext(), "Vui lòng cấp quyền báo thức trong Cài đặt", Toast.LENGTH_LONG).show();
                startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                Log.w(TAG, "Exact alarm permission not granted");
                return;
            }

            Intent intent = new Intent(requireContext(), AlarmReceiver.class);
            intent.putExtra("title", task.getTitle());
            intent.putExtra("taskId", task.getId());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    task.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String alarmTimeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(alarmTime);
            Log.d(TAG, "Setting alarm for task: " + task.getTitle() + " at " + alarmTimeStr);

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
            signInAnonymously();
            return;
        }

        db.collection("users").document(user.getUid()).collection("calendartask").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
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
            }
        }
        adapter.updateList(filteredList);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered tasks: " + filteredList.size());
    }

    private void saveTaskToFirestore(CalendarTaskModel task) {
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
            signInAnonymously();
            return;
        }

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("title", task.getTitle());
        taskMap.put("date", task.getDate());
        taskMap.put("time", task.getTime());

        Log.d(TAG, "Attempting to save task to Firestore: " + task.getTitle() + ", id: " + task.getId());

        db.collection("users").document(user.getUid()).collection("calendartask").document(task.getId())
                .set(taskMap)
                .addOnSuccessListener(aVoid -> {
                    if (!taskList.contains(task)) {
                        taskList.add(task);
                    }
                    filterTasksByDate();
                    Toast.makeText(requireContext(), "Task đã được lưu", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Task saved to Firestore: " + task.getTitle() + ", id: " + task.getId());
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
            signInAnonymously();
            return;
        }

        db.collection("users").document(user.getUid()).collection("calendartask")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        taskList.clear();
                        try {
                            if (task.getResult().isEmpty()) {
                                Log.d(TAG, "No tasks found in Firestore collection: users/" + user.getUid() + "/calendartask");
                                filterTasksByDate();
                                return;
                            }
                            for (QueryDocumentSnapshot document : task.getResult()) {
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
                                    Log.d(TAG, "Loaded task: " + title + ", id: " + id);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing task document: " + document.getId(), e);
                                }
                            }
                            Log.d(TAG, "Loaded tasks from Firestore: " + taskList.size());
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing Firestore result", e);
                            Toast.makeText(requireContext(), "Lỗi khi xử lý dữ liệu tasks", Toast.LENGTH_SHORT).show();
                        }
                        filterTasksByDate();
                    } else {
                        Exception e = task.getException();
                        String errorMsg = e != null ? e.getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), "Lỗi khi tải tasks: " + errorMsg, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading tasks from Firestore: " + errorMsg, e);
                        filterTasksByDate();
                    }
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}