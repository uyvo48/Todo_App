package onboarding_screen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.example.todo_app.R;

public class ViewPagerAdapter extends PagerAdapter {
    // khai báo biến
    private final Context context;
    private final int[] slideAllImage = {R.drawable.intro1, R.drawable.intro2, R.drawable.intro3};
    private final int[] sliderAllTitle = {R.string.intro1, R.string.intro2, R.string.intro3};
    private final int[] sliderAllDesc = {R.string.intro1des, R.string.intro2des, R.string.intro3des};

    public ViewPagerAdapter(Context context) {
        this.context = context;
    }

    // trả về số lượng slide trong viewpage
    @Override
    public int getCount() {
        return slideAllImage.length;
    }

    // kiểm tra xem đối tượng view có phù hợp với  1 đối tượng trong viewpage
    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }


    // tạo và hiển thị slide cho 1 vị trí tại position
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_screen, container, false);

        ImageView sliderImage = view.findViewById(R.id.sliderImage);
        TextView sliderTitle = view.findViewById(R.id.sliderTitle);
        TextView sliderDesc = view.findViewById(R.id.sliderDesc);

        sliderImage.setImageResource(slideAllImage[position]);
        sliderTitle.setText(sliderAllTitle[position]);
        sliderDesc.setText(sliderAllDesc[position]);

        container.addView(view);
        return view;
    }


    // xóa 1 slide khỏi viewpape nếu ko sử dụng

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}