package Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.todo_app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import model.CalendarTaskModel;

public class CalendarTaskAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<CalendarTaskModel> taskList;
    private OnTaskDeleteListener listener;

    public interface OnTaskDeleteListener {
        void onTaskDelete(String taskId);
    }

    public CalendarTaskAdapter(Context context, ArrayList<CalendarTaskModel> taskList, OnTaskDeleteListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    public void updateList(ArrayList<CalendarTaskModel> newList) {
        this.taskList = newList;
        notifyDataSetChanged();
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
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_calendar, parent, false);
        }

        CalendarTaskModel task = taskList.get(position);

        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvTime = convertView.findViewById(R.id.tvTime);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteTaskCalendar);

        tvTitle.setText(task.getTitle());
        tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.getDate()));
        tvTime.setText(task.getTime() != null ? task.getTime() : "Chưa cài đặt giờ");

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDelete(task.getId());
            }
        });

        return convertView;
    }
}