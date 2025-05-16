package Todo_list;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.todo_app.R;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "TaskAlarmChannel";
    private static final String CHANNEL_NAME = "Task Alarm Notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered");

        // Lấy thông tin từ Intent
        String title = intent.getStringExtra("title");
        String taskId = intent.getStringExtra("taskId");
        if (title == null || taskId == null) {
            Log.e(TAG, "Invalid alarm data: title=" + title + ", taskId=" + taskId);
            return;
        }

        Log.d(TAG, "Received alarm for task: " + title + ", taskId: " + taskId);

        // Tạo kênh thông báo (cho Android 8.0+)
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo nhắc nhở task");
            notificationManager.createNotificationChannel(channel);
        }

        // Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm) // Thay bằng icon của bạn
                .setContentTitle("Nhắc nhở: " + title)
                .setContentText("Đã đến giờ thực hiện task!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Hiển thị thông báo
        notificationManager.notify(taskId.hashCode(), builder.build());
        Log.d(TAG, "Notification displayed for task: " + title);
    }
}