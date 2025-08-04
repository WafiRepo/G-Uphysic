package de.rwth_aachen.phyphox.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.adapter.UserQuestionsAdapter;
import de.rwth_aachen.phyphox.databinding.ActivityListQuestionsBinding;
import de.rwth_aachen.phyphox.model.DataModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListUserQuestionsFragment extends Fragment {

    private ActivityListQuestionsBinding binding;
    private UserQuestionsAdapter adapter;
    private List<DataModel> allQuestions = new ArrayList<>();
    private List<DataModel> filteredQuestions = new ArrayList<>();

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

        setupRecyclerView();
        setupSearchFunctionality();
        loadQuestions();
    }

    private void setupRecyclerView() {
        adapter = new UserQuestionsAdapter(requireContext(), requireActivity().getApplication());
        binding.rvData.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvData.setAdapter(adapter);
    }

    private void setupSearchFunctionality() {
        EditText etSearch = binding.etSearch;
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterQuestions(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadQuestions() {
        // Fetch data from Firestore
        FirestoreUtil.getAllDocuments("questions", DataModel.class, data -> {
            allQuestions = data;
            filteredQuestions = new ArrayList<>(data);
            adapter.addData(filteredQuestions);
            updateStatistics();
            updateEmptyState();
        }, e -> {
            Toast.makeText(requireContext(), "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updateEmptyState();
        });
    }

    private void filterQuestions(String query) {
        filteredQuestions.clear();

        if (query.isEmpty()) {
            filteredQuestions.addAll(allQuestions);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (DataModel question : allQuestions) {
                // Search in topic, question text, and creator name
                boolean matchesTopic = question.getTopics() != null &&
                    question.getTopics().toLowerCase().contains(lowerCaseQuery);
                boolean matchesQuestion = question.getQuestion() != null &&
                    question.getQuestion().toLowerCase().contains(lowerCaseQuery);
                boolean matchesCreator = question.getCustomerName() != null &&
                    question.getCustomerName().toLowerCase().contains(lowerCaseQuery);

                if (matchesTopic || matchesQuestion || matchesCreator) {
                    filteredQuestions.add(question);
                }
            }
        }

        adapter.addData(filteredQuestions);
        updateEmptyState();
    }

    private void updateStatistics() {
        if (binding == null) return;

        // Update total questions count
        TextView tvTotalQuestions = binding.tvTotalQuestions;
        if (tvTotalQuestions != null) {
            tvTotalQuestions.setText(String.valueOf(allQuestions.size()));
        }

        // Calculate unique contributors
        Set<String> uniqueContributors = new HashSet<>();
        for (DataModel question : allQuestions) {
            if (question.getCustomerName() != null && !question.getCustomerName().isEmpty()) {
                uniqueContributors.add(question.getCustomerName());
            }
        }

        TextView tvTotalContributors = binding.tvTotalContributors;
        if (tvTotalContributors != null) {
            tvTotalContributors.setText(String.valueOf(uniqueContributors.size()));
        }

        // Calculate unique categories
        Set<String> uniqueCategories = new HashSet<>();
        for (DataModel question : allQuestions) {
            if (question.getTopics() != null && !question.getTopics().isEmpty()) {
                uniqueCategories.add(question.getTopics());
            }
        }

//        TextView tvTotalCategories = binding.tvTotalCategories;
//        if (tvTotalCategories != null) {
//            tvTotalCategories.setText(String.valueOf(uniqueCategories.size()));
//        }
    }

    private void updateEmptyState() {
        if (binding == null) return;

        View llEmptyState = binding.llEmptyState;
        if (llEmptyState != null) {
            if (filteredQuestions.isEmpty()) {
                llEmptyState.setVisibility(View.VISIBLE);
                binding.rvData.setVisibility(View.GONE);
            } else {
                llEmptyState.setVisibility(View.GONE);
                binding.rvData.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
