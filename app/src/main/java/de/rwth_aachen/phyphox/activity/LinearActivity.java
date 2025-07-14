package de.rwth_aachen.phyphox.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.adapter.MyLinearAdapter;
import de.rwth_aachen.phyphox.fragment.LinearIntroductionFragment;

public class LinearActivity extends AppCompatActivity implements LinearIntroductionFragment.buttonClick {

    TabLayout tabLayout;
    ViewPager2 viewPager2;
    MyLinearAdapter myLinearAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_angular);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager2 = findViewById(R.id.view_pager_2_linear);
        myLinearAdapter = new MyLinearAdapter(this);
        viewPager2.setAdapter(myLinearAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.getTabAt(position).select();
            }
        });
    }

    @Override
    public void buttonClicked(View v) {
        viewPager2.setCurrentItem(1);
    }
}
