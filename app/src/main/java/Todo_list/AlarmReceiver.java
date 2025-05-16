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
    private static final String CHANNEL_ID = "TASK_ALARM_CHANNEL";
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String taskId = intent.getStringExtra("taskId");
        Log.d(TAG, "Alarm triggered for task: " + title + ", taskId: " + taskId);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_add)
                .setContentTitle("Nhắc nhở: " + title)
                .setContentText("Đã đến giờ thực hiện công việc!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(taskId.hashCode(), builder.build());
        Log.d(TAG, "Notification sent for task: " + title);
    }
}