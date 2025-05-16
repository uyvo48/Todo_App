package Adapter;

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

public class TaskAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<SubTaskModel> subTaskList;
    private String taskTitle;
    private CollectionReference subTasksRef;

    public TaskAdapter(Context context, ArrayList<SubTaskModel> subTaskList, String taskTitle, CollectionReference subTasksRef) {
        this.context = context;
        this.subTaskList = subTaskList != null ? subTaskList : new ArrayList<>();
        this.taskTitle = taskTitle;
        this.subTasksRef = subTasksRef;
        Log.d("TaskAdapter", "Initialized adapter with subTasksRef: " + (subTasksRef != null ? "not null" : "null"));
    }

    @Override
    public int getCount() {
        return subTaskList.size();
    }

    @Override
    public Object getItem(int position) {
        return subTaskList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false);
        }

        SubTaskModel subTask = subTaskList.get(position);

        TextInputEditText subTaskContent = convertView.findViewById(R.id.task_text);
        CheckBox subTaskCheckBox = convertView.findViewById(R.id.checkBox);
        ImageButton deleteButton = convertView.findViewById(R.id.deleteButton);

        if (subTaskContent == null || subTaskCheckBox == null || deleteButton == null) {
            Log.e("TaskAdapter", "View components not found for position: " + position);
            return convertView;
        }

        subTaskContent.setText(subTask.getContent());
        subTaskCheckBox.setChecked(subTask.getIsDone());

        subTaskContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String newContent = subTaskContent.getText().toString().trim();
                if (!TextUtils.isEmpty(newContent) && !newContent.equals(subTask.getContent())) {
                    subTask.setContent(newContent);
                    updateSubTaskInFirestore(subTask);
                }
            }
        });

        subTaskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            subTask.setIsDone(isChecked);
            updateSubTaskInFirestore(subTask);
        });

        deleteButton.setOnClickListener(v -> {
            String subTaskId = subTask.getId();
            if (subTasksRef != null && !TextUtils.isEmpty(subTaskId)) {
                subTasksRef.document(subTaskId).delete()
                        .addOnSuccessListener(aVoid -> {
                            subTaskList.remove(position);
                            notifyDataSetChanged();
                            Log.d("TaskAdapter", "Sub-task deleted: " + subTaskId);
                        })
                        .addOnFailureListener(e -> Log.e("TaskAdapter", "Failed to delete sub-task: " + e.getMessage()));
            }
        });

        return convertView;
    }

    private void updateSubTaskInFirestore(SubTaskModel subTask) {
        if (subTasksRef == null || TextUtils.isEmpty(subTask.getId())) {
            Log.e("TaskAdapter", "subTasksRef or subTaskId is null, cannot update Firestore");
            return;
        }

        subTasksRef.document(subTask.getId()).set(subTask)
                .addOnSuccessListener(aVoid -> Log.d("TaskAdapter", "Sub-task updated in Firestore: " + subTask.getId()))
                .addOnFailureListener(e -> Log.e("TaskAdapter", "Failed to update sub-task: " + e.getMessage()));
    }
}