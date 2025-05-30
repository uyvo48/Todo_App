package Adapter;

// Nhập các lớp cần thiết của Android, Firebase, và Java để xử lý giao diện, log, và thao tác dữ liệu
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import model.TaskModel;
import com.example.todo_app.R;

// Lớp adapter tùy chỉnh để hiển thị danh sách nhiệm vụ trên màn hình chính trong ListView
public class HomeTaskAdapter extends BaseAdapter {
    // Context để truy cập tài nguyên ứng dụng và inflate layout
    private Context context;
    // Danh sách các nhiệm vụ để hiển thị, mặc định là rỗng nếu null
    private ArrayList<TaskModel> taskList;
    // Listener để xử lý sự kiện nhấn vào nhiệm vụ (truyền ID và tiêu đề)
    private BiConsumer<String, String> onTaskClickListener;
    // Đối tượng Firestore để tương tác với cơ sở dữ liệu
    private FirebaseFirestore db;
    // ID người dùng để xác định dữ liệu trong Firestore
    private String userId;

    // Constructor để khởi tạo adapter với context, danh sách nhiệm vụ, listener nhấn, và userId
    public HomeTaskAdapter(Context context, ArrayList<TaskModel> taskList, BiConsumer<String, String> onTaskClickListener, String userId) {
        this.context = context;
        // Đảm bảo taskList không null, nếu null thì khởi tạo danh sách rỗng
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.onTaskClickListener = onTaskClickListener;
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
        // Ghi log kích thước danh sách nhiệm vụ khi khởi tạo
        Log.d("HomeTaskAdapter", "Initialized adapter with taskList size: " + this.taskList.size());
    }

    // Trả về số lượng nhiệm vụ trong danh sách
    @Override
    public int getCount() {
        return taskList.size();
    }

    // Trả về đối tượng nhiệm vụ tại vị trí chỉ định
    @Override
    public Object getItem(int position) {
        return taskList.get(position);
    }

    // Trả về vị trí làm ID của mục (không sử dụng trong trường hợp này, nhưng được yêu cầu bởi BaseAdapter)
    @Override
    public long getItemId(int position) {
        return position;
    }

    // Tạo hoặc tái sử dụng view cho một mục nhiệm vụ và điền dữ liệu vào
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate layout cho mục danh sách nếu convertView là null (tái sử dụng view)
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_home, parent, false);
        }

        // Lấy đối tượng nhiệm vụ tại vị trí hiện tại
        TaskModel task = taskList.get(position);

        // Tìm các thành phần giao diện trong layout của mục danh sách
        TextView taskTitle = convertView.findViewById(R.id.textView14);
        TextView taskHeader = convertView.findViewById(R.id.textView11);
        ImageButton deleteButton = convertView.findViewById(R.id.imageButtonDeleteHome);

        // Kiểm tra xem các thành phần giao diện có được tìm thấy không
        if (taskTitle == null || taskHeader == null || deleteButton == null) {
            Log.e("HomeTaskAdapter", "View components not found for position: " + position);
            return convertView;
        }

        // Hiển thị số thứ tự nhiệm vụ trong textView11 (Task 1, Task 2, ...)
        String numberedHeader = "Task " + (position + 1);
        taskHeader.setText(numberedHeader);

        // Thiết lập tiêu đề nhiệm vụ trong textView14
        taskTitle.setText(task.getTitle());

        // Cấu hình lại ImageButton để đảm bảo hiển thị đúng biểu tượng xóa
        if (deleteButton.getDrawable() == null) {
            Log.w("HomeTaskAdapter", "Icon for deleteButton is null at position: " + position);
            deleteButton.setImageResource(R.drawable.ic_delete_home);
            deleteButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            deleteButton.setPadding(0, 0, 0, 0);
        }

        // Xử lý sự kiện nhấn vào mục nhiệm vụ để mở TaskFragment
        convertView.setOnClickListener(v -> {
            if (onTaskClickListener != null && task != null) {
                String taskId = task.getId();
                String title = task.getTitle();
                // Gọi listener với ID và tiêu đề nhiệm vụ
                onTaskClickListener.accept(taskId, title);
                Log.d("HomeTaskAdapter", "Task clicked: id=" + taskId + ", title=" + title);
            } else {
                Log.e("HomeTaskAdapter", "onTaskClickListener or task is null at position: " + position);
            }
        });

        // Xử lý sự kiện nhấn nút xóa với hộp thoại xác nhận
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa task \"" + task.getTitle() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        String taskId = task.getId();
                        if (taskId != null && userId != null) {
                            // Xóa nhiệm vụ từ Firestore
                            db.collection("users")
                                    .document(userId)
                                    .collection("tasks")
                                    .document(taskId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Xóa nhiệm vụ khỏi danh sách và cập nhật giao diện
                                        taskList.remove(position);
                                        notifyDataSetChanged();
                                        Log.d("HomeTaskAdapter", "Task deleted: " + taskId);
                                    })
                                    .addOnFailureListener(e -> {
                                        // Ghi log lỗi nếu xóa thất bại
                                        Log.e("HomeTaskAdapter", "Failed to delete task: " + e.getMessage());
                                    });
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Trả về view đã được điền dữ liệu cho mục danh sách
        return convertView;
    }
}