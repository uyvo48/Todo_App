package onboarding_screen;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.todo_app.R;

public class getStarted extends AppCompatActivity {
    Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        startButton = findViewById(R.id.startButton);
        if (startButton == null) {
            throw new RuntimeException("startButton not found in activity_get_started.xml");
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getStarted.this, NavigationActivity.class);
                startActivity(i);
                finish(); // Kết thúc getStarted để không quay lại
            }
        });
    }
}