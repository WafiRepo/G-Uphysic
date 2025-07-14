package de.rwth_aachen.phyphox.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import de.rwth_aachen.phyphox.fragment.AngularIntroductionFragment;
import de.rwth_aachen.phyphox.fragment.LinearIntroductionFragment;
import de.rwth_aachen.phyphox.fragment.RecordFragment;

public class MyAngularAdapter extends FragmentStateAdapter {


    public MyAngularAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AngularIntroductionFragment();
            case 1:
                return new RecordFragment();
            default:
                return new AngularIntroductionFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
