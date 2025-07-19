package de.rwth_aachen.phyphox.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.adapter.UserQuestionsAdapter;
import de.rwth_aachen.phyphox.databinding.ActivityListQuestionsBinding;
import de.rwth_aachen.phyphox.model.DataModel;

public class ListUserQuestionsFragment extends Fragment {

    private ActivityListQuestionsBinding binding;
    private UserQuestionsAdapter adapter;

    public ListUserQuestionsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = ActivityListQuestionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize adapter and RecyclerView
        adapter = new UserQuestionsAdapter(requireContext(), requireActivity().getApplication());
        binding.rvData.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvData.setAdapter(adapter);

        // Fetch data from Firestore
        FirestoreUtil.getAllDocuments("questions", DataModel.class, data -> {
            adapter.addData(data);
        }, e -> {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
