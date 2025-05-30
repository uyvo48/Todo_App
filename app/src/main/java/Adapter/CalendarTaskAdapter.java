package Adapter;

// Nhập các lớp cần thiết của Android và Java để xử lý giao diện, định dạng ngày giờ và dữ liệu
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

// Lớp adapter tùy chỉnh để hiển thị danh sách các nhiệm vụ lịch trong ListView
public class CalendarTaskAdapter extends BaseAdapter {
    // Context để truy cập tài nguyên ứng dụng và inflate layout
    private Context context;
    // Danh sách các nhiệm vụ để hiển thị
    private ArrayList<CalendarTaskModel> taskList;
    // Listener để xử lý sự kiện xóa nhiệm vụ
    private OnTaskDeleteListener listener;

    // Interface định nghĩa callback cho sự kiện xóa nhiệm vụ
    public interface OnTaskDeleteListener {
        // Phương thức được triển khai bởi activity/fragment để xử lý xóa nhiệm vụ
        void onTaskDelete(String taskId);
    }

    // Constructor để khởi tạo adapter với context, danh sách nhiệm vụ và listener xóa
    public CalendarTaskAdapter(Context context, ArrayList<CalendarTaskModel> taskList, OnTaskDeleteListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    // Phương thức để cập nhật danh sách nhiệm vụ và làm mới ListView
    public void updateList(ArrayList<CalendarTaskModel> newList) {
        this.taskList = newList;
        // Thông báo adapter rằng dữ liệu đã thay đổi để làm mới giao diện
        notifyDataSetChanged();
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
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_calendar, parent, false);
        }

        // Lấy đối tượng nhiệm vụ tại vị trí hiện tại
        CalendarTaskModel task = taskList.get(position);

        // Tìm các thành phần giao diện trong layout của mục danh sách
        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvTime = convertView.findViewById(R.id.tvTime);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteTaskCalendar);

        // Thiết lập tiêu đề nhiệm vụ
        tvTitle.setText(task.getTitle());
        // Định dạng và thiết lập ngày nhiệm vụ (ví dụ: "dd/MM/yyyy")
        tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.getDate()));
        // Thiết lập giờ nhiệm vụ, hoặc hiển thị thông báo nếu giờ chưa được cài đặt
        tvTime.setText(task.getTime() != null ? task.getTime() : "Chưa cài đặt giờ");

        // Thiết lập listener cho nút xóa
        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                // Kích hoạt callback xóa với ID của nhiệm vụ
                listener.onTaskDelete(task.getId());
            }
        });

        // Trả về view đã được điền dữ liệu cho mục danh sách
        return convertView;
    }
}