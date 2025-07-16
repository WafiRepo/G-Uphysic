package de.rwth_aachen.phyphox.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.adapter.HistoryRecordAdapter;
import de.rwth_aachen.phyphox.adapter.UserQuestionsAdapter;
import de.rwth_aachen.phyphox.databinding.ActivityListQuestionsBinding;
import de.rwth_aachen.phyphox.databinding.ActivityMainBinding;
import de.rwth_aachen.phyphox.model.DataModel;

public class ListUserQuestionsActivity extends AppCompatActivity {
    ActivityListQuestionsBinding binding;
    private UserQuestionsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        // Set up toolbar
        setSupportActionBar(toolbar);
        setSupportActionBar(toolbar);

        // Set title and enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("User Questions");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
        }

        adapter = new UserQuestionsAdapter(this, this.getApplication());
        binding.rvData.setLayoutManager(new LinearLayoutManager(this));
        binding.rvData.setAdapter(adapter);

        // Fetch data from Firestore
        FirestoreUtil.getAllDocuments("questions", DataModel.class, data -> {
            adapter.addData(data);
        }, e -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });

    }
}
