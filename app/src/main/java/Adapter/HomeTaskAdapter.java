package Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import model.TaskModel;
import com.example.todo_app.R;

public class HomeTaskAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<TaskModel> taskList;
    private BiConsumer<String, String> onTaskClickListener;

    public HomeTaskAdapter(Context context, ArrayList<TaskModel> taskList, BiConsumer<String, String> onTaskClickListener) {
        this.context = context;
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.onTaskClickListener = onTaskClickListener;
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

        if (taskTitle == null) {
            Log.e("HomeTaskAdapter", "View components not found for position: " + position);
            return convertView;
        }

        taskTitle.setText(task.getTitle());

        convertView.setOnClickListener(v -> {
            if (onTaskClickListener != null) {
                onTaskClickListener.accept(task.getId(), task.getTitle());
                Log.d("HomeTaskAdapter", "Task clicked: id=" + task.getId() + ", title=" + task.getTitle());
            }
        });

        return convertView;
    }
}