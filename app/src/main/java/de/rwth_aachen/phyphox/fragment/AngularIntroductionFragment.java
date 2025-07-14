package de.rwth_aachen.phyphox.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import de.rwth_aachen.phyphox.R;

public class AngularIntroductionFragment extends Fragment {

    buttonClick click;

    public interface buttonClick {
        void buttonClicked(View v);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        click = (buttonClick) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_linier_introduction, container, false);
        Button btnStart = rootView.findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v ->
                click.buttonClicked(v)
        );
        return rootView;
    }
}
