package de.rwth_aachen.phyphox.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

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

        // Initialize level buttons
        MaterialButton btnEasy = binding.btnEasy;
        MaterialButton btnIntermediate = binding.btnIntermediate;
        MaterialButton btnAdvance = binding.btnAdvance;

        // Set click listeners for the level buttons
        btnEasy.setOnClickListener(v -> onLevelButtonClicked(btnEasy, btnIntermediate, btnAdvance));
        btnIntermediate.setOnClickListener(v -> onLevelButtonClicked(btnIntermediate, btnEasy, btnAdvance));
        btnAdvance.setOnClickListener(v -> onLevelButtonClicked(btnAdvance, btnEasy, btnIntermediate));
        boolean isClassQuestion = getIntent().getBooleanExtra("isClassQuestion", false);
        // Setup the click listener for the AI-based problem button
        View.OnClickListener setOnClickListener = v -> {
            App app = (App) getApplication();
            DataModel dataModel = app.getDataModel();
            if(!dataModel.getTypeQuestion().isEmpty()){
                if(isClassQuestion){
                    startActivity(new Intent(QuestionActivity.this, GeneratesClassQuestionActivity.class));
                }else{
                    startActivity(new Intent(QuestionActivity.this, GeneratesActivity.class));
                }
            }
            finish();

        };
        binding.btnAiBased.setOnClickListener(setOnClickListener);
    }

    // Method to handle the level button clicks and change colors
    private void onLevelButtonClicked(MaterialButton clickedButton, MaterialButton... otherButtons) {
        // Change the background color of the clicked button to orange
        clickedButton.setBackgroundTintList(getResources().getColorStateList(R.color.phyphox_primary_weak));

        // Reset the background color of the other buttons to the default purple
        for (MaterialButton button : otherButtons) {
            button.setBackgroundTintList(getResources().getColorStateList(R.color.phyphox_white_100)); // Default background color
            button.setTextColor(getResources().getColor(R.color.phyphox_purple)); // Reset text color to purple
        }

        // Change the text color of the clicked button to white
        App app = (App) getApplication();
        DataModel dataModel = app.getDataModel();
        dataModel.setTypeQuestion(clickedButton.getText().toString());
        app.setDataModel(dataModel);
        clickedButton.setTextColor(getResources().getColor(R.color.phyphox_white_50_black_50));
    }
}
