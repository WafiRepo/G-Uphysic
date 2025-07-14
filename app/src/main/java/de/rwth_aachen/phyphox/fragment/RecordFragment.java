package de.rwth_aachen.phyphox.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.rwth_aachen.phyphox.Experiment;
import de.rwth_aachen.phyphox.ExperimentList;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.FeedbackActivity;
import de.rwth_aachen.phyphox.databinding.FragmentRecordBinding;

public class RecordFragment extends Fragment {

    FragmentRecordBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnStart.setOnClickListener(v -> startActivity(new Intent(getActivity(), FeedbackActivity.class)));
    }
}
