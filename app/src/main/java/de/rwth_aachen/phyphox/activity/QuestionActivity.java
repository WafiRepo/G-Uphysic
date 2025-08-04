package de.rwth_aachen.phyphox.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.widget.Button;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityQuestionBinding;
import de.rwth_aachen.phyphox.model.DataModel;

public class QuestionActivity extends AppCompatActivity {

    ActivityQuestionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup back button
        binding.btnBack.setOnClickListener(v -> {
            onBackPressed();
        });

        // Initialize level buttons
        Button btnEasy = binding.btnEasy;
        Button btnIntermediate = binding.btnIntermediate;
        Button btnAdvance = binding.btnAdvance;

        // Set click listeners for the level buttons
        btnEasy.setOnClickListener(v -> onLevelButtonClicked(btnEasy, btnIntermediate, btnAdvance));
        btnIntermediate.setOnClickListener(v -> onLevelButtonClicked(btnIntermediate, btnEasy, btnAdvance));
        btnAdvance.setOnClickListener(v -> onLevelButtonClicked(btnAdvance, btnEasy, btnIntermediate));
        boolean isClassQuestion = getIntent().getBooleanExtra("isClassQuestion", false);
        // Setup the click listener for the AI-based problem button
        View.OnClickListener setOnClickListener = v -> {
            App app = (App) getApplication();
            DataModel dataModel = app.getDataModel();
            if (!dataModel.getTypeQuestion().isEmpty()) {
                if (isClassQuestion) {
                    startActivity(new Intent(QuestionActivity.this, GeneratesClassQuestionActivity.class));
                } else {
                    startActivity(new Intent(QuestionActivity.this, GeneratesActivity.class));
                }
                finish();
            } else {
                Toast.makeText(this, "Silakan pilih tingkat kesulitan terlebih dahulu", Toast.LENGTH_SHORT).show();
            }
        };
        binding.btnAiBased.setOnClickListener(setOnClickListener);
        binding.btnUserBased.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                App app = (App) getApplication();
                DataModel dataModel = app.getDataModel();
                if (!dataModel.getTypeQuestion().isEmpty()) {
                    Intent intent = new Intent(QuestionActivity.this, GeneratesActivity.class);
                    intent.putExtra("isFromMainMenu", false);
                    intent.putExtra("isCustomQuestion", true);
                    intent.putExtra("isCustomQuestionNew", true);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(QuestionActivity.this, "Silakan pilih tingkat kesulitan terlebih dahulu", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method to handle the level button clicks and change colors
    private void onLevelButtonClicked(Button clickedButton, Button... otherButtons) {
        // Set the clicked button as selected
        clickedButton.setSelected(true);
        clickedButton.setTextColor(ContextCompat.getColor(this, R.color.phyphox_white_100));

        // Reset the other buttons to unselected state
        for (Button button : otherButtons) {
            button.setSelected(false);
            button.setTextColor(ContextCompat.getColor(this, R.color.phyphox_purple));
        }

        // Save selected level to data model
        App app = (App) getApplication();
        DataModel dataModel = app.getDataModel();
        
        // Convert button text to Indonesian levels
        String selectedLevel = clickedButton.getText().toString();
        if (selectedLevel.contains("Mudah")) {
            dataModel.setTypeQuestion("Easy");
        } else if (selectedLevel.contains("Sedang")) {
            dataModel.setTypeQuestion("Intermediate");
        } else if (selectedLevel.contains("Sulit")) {
            dataModel.setTypeQuestion("Advanced");
        }
        
        app.setDataModel(dataModel);
        
        // Show confirmation toast
        Toast.makeText(this, "Tingkat kesulitan dipilih: " + selectedLevel, Toast.LENGTH_SHORT).show();
    }
}
