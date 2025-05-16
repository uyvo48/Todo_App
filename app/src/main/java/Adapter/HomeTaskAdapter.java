package Adapter;

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

public class HomeTaskAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<TaskModel> taskList;
    private BiConsumer<String, String> onTaskClickListener;
    private FirebaseFirestore db;
    private String userId;

    public HomeTaskAdapter(Context context, ArrayList<TaskModel> taskList, BiConsumer<String, String> onTaskClickListener, String userId) {
        this.context = context;
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.onTaskClickListener = onTaskClickListener;
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
        Log.d("HomeTaskAdapter", "Initialized adapter with taskList size: " + this.taskList.size());
    }

    @Override
    public int getCount() {
        return taskList.size();
    }

    @Override
    public Object getItem(int position) {
        return taskList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_home, parent, false);
        }

        TaskModel task = taskList.get(position);

        TextView taskTitle = convertView.findViewById(R.id.textView14);
        TextView taskHeader = convertView.findViewById(R.id.textView11);
        ImageButton deleteButton = convertView.findViewById(R.id.imageButtonDeleteHome);

        if (taskTitle == null || taskHeader == null || deleteButton == null) {
            Log.e("HomeTaskAdapter", "View components not found for position: " + position);
            return convertView;
        }

        // Hiển thị số thứ tự trong textView11
        String numberedHeader = "Task " + (position + 1);
        taskHeader.setText(numberedHeader);

        // Giữ nguyên tiêu đề gốc trong textView14
        taskTitle.setText(task.getTitle());

        // Cấu hình lại ImageButton để đảm bảo icon hiển thị đúng
        if (deleteButton.getDrawable() == null) {
            Log.w("HomeTaskAdapter", "Icon for deleteButton is null at position: " + position);
            deleteButton.setImageResource(R.drawable.ic_delete_home);
            deleteButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            deleteButton.setPadding(0, 0, 0, 0);
        }

        // Xử lý sự kiện nhấn vào task để mở TaskFragment, sử dụng tiêu đề và ID gốc
        convertView.setOnClickListener(v -> {
            if (onTaskClickListener != null && task != null) {
                String taskId = task.getId();
                String title = task.getTitle();
                onTaskClickListener.accept(taskId, title);
                Log.d("HomeTaskAdapter", "Task clicked: id=" + taskId + ", title=" + title);
            } else {
                Log.e("HomeTaskAdapter", "onTaskClickListener or task is null at position: " + position);
            }
        });

        // Xử lý sự kiện xóa task với thông báo xác nhận, sử dụng tiêu đề gốc
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa task \"" + task.getTitle() + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        String taskId = task.getId();
                        if (taskId != null && userId != null) {
                            db.collection("users")
                                    .document(userId)
                                    .collection("tasks")
                                    .document(taskId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        taskList.remove(position);
                                        notifyDataSetChanged();
                                        Log.d("HomeTaskAdapter", "Task deleted: " + taskId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("HomeTaskAdapter", "Failed to delete task: " + e.getMessage());
                                    });
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        return convertView;
    }
}