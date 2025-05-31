package Todo_list;

// Nhập các lớp cần thiết của Android để xử lý thông báo và BroadcastReceiver
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.todo_app.R;

// Lớp BroadcastReceiver để xử lý báo thức và hiển thị thông báo
public class AlarmReceiver extends BroadcastReceiver {
    // Các hằng số tĩnh
    private static final String TAG = "AlarmReceiver"; // Tag để ghi log
    private static final String CHANNEL_ID = "TaskAlarmChannel"; // ID kênh thông báo
    private static final String CHANNEL_NAME = "Task Alarm Notifications"; // Tên kênh thông báo

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered");

        // Lấy thông tin từ Intent (tiêu đề và ID nhiệm vụ)
        String title = intent.getStringExtra("title");
        String taskId = intent.getStringExtra("taskId");
        // Kiểm tra tính hợp lệ của dữ liệu
        if (title == null || taskId == null) {
            Log.e(TAG, "Invalid alarm data: title=" + title + ", taskId=" + taskId);
            return;
        }

        Log.d(TAG, "Received alarm for task: " + title + ", taskId: " + taskId);

        // Tạo kênh thông báo cho Android 8.0+ (API 26+)
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // Độ ưu tiên cao
            );
            channel.setDescription("Thông báo nhắc nhở task");
            notificationManager.createNotificationChannel(channel);
        }

        // Tạo thông báo sử dụng NotificationCompat
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm) // Biểu tượng thông báo
                .setContentTitle("Nhắc nhở task: " + title) // Tiêu đề thông báo
                .setContentText("Đã đến giờ thực hiện task!") // Nội dung thông báo
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Độ ưu tiên cao
                .setAutoCancel(true); // Tự động hủy khi nhấn vào thông báo

        // Hiển thị thông báo với ID dựa trên taskId
        notificationManager.notify(taskId.hashCode(), builder.build());
        Log.d(TAG, "Notification displayed for task: " + title);
    }
}