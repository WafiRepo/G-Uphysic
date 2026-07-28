package de.rwth_aachen.phyphox.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.adapter.HistoryRecordAdapter;
import de.rwth_aachen.phyphox.databinding.ActivityHistoryRecordBinding;
import de.rwth_aachen.phyphox.model.DataModel;

public class HistoryRecordActivity extends AppCompatActivity {
    private ActivityHistoryRecordBinding binding;
    private HistoryRecordAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        adapter = new HistoryRecordAdapter(this, getApplication());
        binding.rvData.setLayoutManager(new LinearLayoutManager(this));
        binding.rvData.setAdapter(adapter);

        FirestoreUtil.getAllDocuments("record", DataModel.class,"idCustomer", SessionManager.getId(this), data -> {
            adapter.addData(data);
        }, new FirestoreUtil.OnFailureCallback() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(HistoryRecordActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
