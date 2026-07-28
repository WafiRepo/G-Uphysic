package de.rwth_aachen.phyphox.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.adapter.UserQuestionsAdapter;
import de.rwth_aachen.phyphox.databinding.ActivityListQuestionsBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import android.view.View;
import android.widget.TextView;

public class ListUserQuestionsActivity extends AppCompatActivity {
    ActivityListQuestionsBinding binding;
    private UserQuestionsAdapter adapter;
    private List<DataModel> allQuestions = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📚 Pertanyaan Fisika Komunitas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new UserQuestionsAdapter(this, this.getApplication());
        binding.rvData.setLayoutManager(new LinearLayoutManager(this));
        binding.rvData.setAdapter(adapter);

        // Wire up search
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterByName(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Fetch data from Firestore
        FirestoreUtil.getAllDocuments("questions", DataModel.class, data -> {
            allQuestions = data;
            adapter.addData(data);
            updateStats(data);
        }, e -> {
            Toast.makeText(this, "Gagal memuat pertanyaan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void filterByName(String query) {
        if (query.isEmpty()) {
            adapter.addData(allQuestions);
            return;
        }
        List<DataModel> filtered = new ArrayList<>();
        for (DataModel item : allQuestions) {
            String name = item.getCustomerName();
            if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter.addData(filtered);
    }

    private void updateStats(List<DataModel> data) {
        binding.tvTotalQuestions.setText(String.valueOf(data.size()));
        Set<String> contributors = new HashSet<>();
        for (DataModel item : data) {
            String id = item.getIdCustomer();
            if (id != null && !id.isEmpty()) contributors.add(id);
        }
        binding.tvTotalContributors.setText(String.valueOf(contributors.size()));
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
