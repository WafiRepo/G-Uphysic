package de.rwth_aachen.phyphox.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import de.rwth_aachen.phyphox.fragment.LinearIntroductionFragment;
import de.rwth_aachen.phyphox.fragment.RecordFragment;

public class MyLinearAdapter extends FragmentStateAdapter {

    public MyLinearAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new LinearIntroductionFragment();
            case 1:
                return new RecordFragment();
            default:
                return new LinearIntroductionFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
