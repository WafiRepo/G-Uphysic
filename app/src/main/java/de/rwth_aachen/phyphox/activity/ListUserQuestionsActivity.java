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
import android.view.View;
import android.widget.TextView;

public class ListUserQuestionsActivity extends AppCompatActivity {
    ActivityListQuestionsBinding binding;
    private UserQuestionsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        // Set title and center it
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📚 Community Physics Questions");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Hide back button
            toolbar.setTitleMarginStart(0);
            toolbar.setTitleMarginEnd(0);
            for(int i = 0; i < toolbar.getChildCount(); i++) {
                View view = toolbar.getChildAt(i);
                if(view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setGravity(android.view.Gravity.CENTER);
                    textView.setLayoutParams(new Toolbar.LayoutParams(
                            Toolbar.LayoutParams.MATCH_PARENT,
                            Toolbar.LayoutParams.WRAP_CONTENT));
                }
            }
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
