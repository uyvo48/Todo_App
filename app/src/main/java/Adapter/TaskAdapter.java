package Adapter;

// Nhập các lớp cần thiết của Android, Firebase, và Java để xử lý giao diện, log, và thao tác dữ liệu
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import java.util.ArrayList;
import model.SubTaskModel;
import com.example.todo_app.R;

// Lớp adapter tùy chỉnh để hiển thị danh sách các nhiệm vụ con (sub-task) trong ListView
public class TaskAdapter extends BaseAdapter {
    // Context để truy cập tài nguyên ứng dụng và inflate layout
    private Context context;
    // Danh sách các nhiệm vụ con để hiển thị, mặc định là rỗng nếu null
    private ArrayList<SubTaskModel> subTaskList;
    // Tiêu đề của nhiệm vụ chính (task cha) liên quan đến các nhiệm vụ con
    private String taskTitle;
    // Tham chiếu đến bộ sưu tập Firestore để lưu trữ và cập nhật nhiệm vụ con
    private CollectionReference subTasksRef;

    // Constructor để khởi tạo adapter với context, danh sách nhiệm vụ con, tiêu đề nhiệm vụ, và tham chiếu Firestore
    public TaskAdapter(Context context, ArrayList<SubTaskModel> subTaskList, String taskTitle, CollectionReference subTasksRef) {
        this.context = context;
        // Đảm bảo subTaskList không null, nếu null thì khởi tạo danh sách rỗng
        this.subTaskList = subTaskList != null ? subTaskList : new ArrayList<>();
        this.taskTitle = taskTitle;
        this.subTasksRef = subTasksRef;
        // Ghi log trạng thái của subTasksRef khi khởi tạo
        Log.d("TaskAdapter", "Initialized adapter with subTasksRef: " + (subTasksRef != null ? "not null" : "null"));
    }

    // Trả về số lượng nhiệm vụ con trong danh sách
    @Override
    public int getCount() {
        return subTaskList.size();
    }

    // Trả về đối tượng nhiệm vụ con tại vị trí chỉ định
    @Override
    public Object getItem(int position) {
        return subTaskList.get(position);
    }

    // Trả về vị trí làm ID của mục (không sử dụng trong trường hợp này, nhưng được yêu cầu bởi BaseAdapter)
    @Override
    public long getItemId(int position) {
        return position;
    }

    // Tạo hoặc tái sử dụng view cho một mục nhiệm vụ con và điền dữ liệu vào
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate layout cho mục danh sách nếu convertView là null (tái sử dụng view)
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false);
        }

        // Lấy đối tượng nhiệm vụ con tại vị trí hiện tại
        SubTaskModel subTask = subTaskList.get(position);

        // Tìm các thành phần giao diện trong layout của mục danh sách
        TextInputEditText subTaskContent = convertView.findViewById(R.id.task_text);
        CheckBox subTaskCheckBox = convertView.findViewById(R.id.checkBox);
        ImageButton deleteButton = convertView.findViewById(R.id.deleteButton);

        // Kiểm tra xem các thành phần giao diện có được tìm thấy không
        if (subTaskContent == null || subTaskCheckBox == null || deleteButton == null) {
            Log.e("TaskAdapter", "View components not found for position: " + position);
            return convertView;
        }

        // Thiết lập nội dung nhiệm vụ con
        subTaskContent.setText(subTask.getContent());
        // Thiết lập trạng thái hoàn thành của nhiệm vụ con
        subTaskCheckBox.setChecked(subTask.getIsDone());

        // Xử lý sự kiện thay đổi tiêu điểm của TextInputEditText
        subTaskContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // Lấy nội dung mới và loại bỏ khoảng trắng thừa
                String newContent = subTaskContent.getText().toString().trim();
                // Kiểm tra nội dung mới không rỗng và khác với nội dung cũ
                if (!TextUtils.isEmpty(newContent) && !newContent.equals(subTask.getContent())) {
                    subTask.setContent(newContent);
                    // Cập nhật nhiệm vụ con lên Firestore
                    updateSubTaskInFirestore(subTask);
                }
            }
        });

        // Xử lý sự kiện thay đổi trạng thái CheckBox
        subTaskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            subTask.setIsDone(isChecked);
            // Cập nhật trạng thái hoàn thành lên Firestore
            updateSubTaskInFirestore(subTask);
        });

        // Xử lý sự kiện nhấn nút xóa nhiệm vụ con
        deleteButton.setOnClickListener(v -> {
            String subTaskId = subTask.getId();
            // Kiểm tra subTasksRef và subTaskId không null hoặc rỗng
            if (subTasksRef != null && !TextUtils.isEmpty(subTaskId)) {
                // Xóa nhiệm vụ con từ Firestore
                subTasksRef.document(subTaskId).delete()
                        .addOnSuccessListener(aVoid -> {
                            // Xóa nhiệm vụ con khỏi danh sách và cập nhật giao diện
                            subTaskList.remove(position);
                            notifyDataSetChanged();
                            Log.d("TaskAdapter", "Sub-task deleted: " + subTaskId);
                        })
                        .addOnFailureListener(e -> Log.e("TaskAdapter", "Failed to delete sub-task: " + e.getMessage()));
            }
        });

        // Trả về view đã được điền dữ liệu cho mục danh sách
        return convertView;
    }

    // Phương thức riêng để cập nhật nhiệm vụ con lên Firestore
    private void updateSubTaskInFirestore(SubTaskModel subTask) {
        // Kiểm tra subTasksRef và ID của nhiệm vụ con
        if (subTasksRef == null || TextUtils.isEmpty(subTask.getId())) {
            Log.e("TaskAdapter", "subTasksRef or subTaskId is null, cannot update Firestore");
            return;
        }

        // Lưu đối tượng nhiệm vụ con vào Firestore
        subTasksRef.document(subTask.getId()).set(subTask)
                .addOnSuccessListener(aVoid -> Log.d("TaskAdapter", "Sub-task updated in Firestore: " + subTask.getId()))
                .addOnFailureListener(e -> Log.e("TaskAdapter", "Failed to update sub-task: " + e.getMessage()));
    }
}