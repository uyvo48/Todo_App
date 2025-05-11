package onboarding_screen;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.example.todo_app.R;


import Todo_list.MainList;
import authentication.LoginScreen;

public class NavigationActivity extends AppCompatActivity {

    ViewPager slideViewPager;
    LinearLayout dotIndicator;
    Button nextButton, skipButton;
    TextView[] dots;
    ViewPagerAdapter viewPagerAdapter;

    ViewPager.OnPageChangeListener viewPagerListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            setDotIndicator(position);
            if (position == 2) {
                nextButton.setText("Finish");
            } else {
                nextButton.setText("Continue");
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // Khởi tạo các view
        slideViewPager = findViewById(R.id.slideViewPager);
        dotIndicator = findViewById(R.id.dotIndicator);
        nextButton = findViewById(R.id.nextButton);
        skipButton = findViewById(R.id.skipButton);

        // Kiểm tra null
        if (slideViewPager == null || dotIndicator == null || nextButton == null || skipButton == null) {
            throw new RuntimeException("One or more views not found in activity_navigation.xml");
        }

        // Thiết lập ViewPager
        viewPagerAdapter = new ViewPagerAdapter(this);
        slideViewPager.setAdapter(viewPagerAdapter);
        slideViewPager.addOnPageChangeListener(viewPagerListener);

        // Sự kiện nút Next
        nextButton.setOnClickListener(v -> {
            if (slideViewPager.getCurrentItem() < 2) {
                slideViewPager.setCurrentItem(slideViewPager.getCurrentItem() + 1, true);
            } else {
                startActivity(new Intent(NavigationActivity.this, LoginScreen.class));
                finish();
            }
        });

        // Sự kiện nút Skip
        skipButton.setOnClickListener(v -> {
            startActivity(new Intent(NavigationActivity.this, LoginScreen.class));
            finish();
        });

        // Thiết lập dot indicator ban đầu
        setDotIndicator(0);
    }

    private void setDotIndicator(int position) {
        dots = new TextView[3];
        dotIndicator.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;", Html.FROM_HTML_MODE_LEGACY));
            dots[i].setTextSize(35);
            dots[i].setTextColor(getResources().getColor(R.color.ColorText, getApplicationContext().getTheme()));
            dots[i].setPadding(8, 0, 8, 0);
            dotIndicator.addView(dots[i]);
        }
        dots[position].setTextColor(getResources().getColor(R.color.white, getApplicationContext().getTheme()));
    }
}