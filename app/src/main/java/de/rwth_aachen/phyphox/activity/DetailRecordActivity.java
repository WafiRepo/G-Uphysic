package de.rwth_aachen.phyphox.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import de.rwth_aachen.phyphox.Helper.AdvancedQuestionHelper;
import de.rwth_aachen.phyphox.Helper.EasyQuestionHelper;
import de.rwth_aachen.phyphox.Helper.IntermediateQuestionHelper;
import de.rwth_aachen.phyphox.databinding.ActivityDetailRecordBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import de.rwth_aachen.phyphox.App;

public class DetailRecordActivity extends AppCompatActivity {

    private ActivityDetailRecordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        binding = ActivityDetailRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Enable back button
            getSupportActionBar().setTitle("Details");
        }

        // Handle Back Button Click
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());

        // Ambil DataModel dari App singleton, bukan dari Bundle
        App app = (App) getApplication();
        DataModel data = app.getDataModel();
            binding.tvQuestion.setText(data.getQuestion());

        // Handle Graph/Image 1 (base64)
            if (data.getBase64() != null && !data.getBase64().isEmpty()) {
                if (data.getTopics().equals("Out Class") || data.getTopics().equals("In Class")) {
                    if (data.getTypeQuestion() == "Advanced") {
                        binding.ivQuestion.setBackground(
                                ContextCompat.getDrawable(DetailRecordActivity.this,
                                        AdvancedQuestionHelper.generateQuestionList().get(Integer.parseInt(data.getBase64())).getImages().get(0)
                                ));
                    } else if (data.getTypeQuestion() == "Easy") {
                        binding.ivQuestion.setBackground(
                                ContextCompat.getDrawable(DetailRecordActivity.this,
                                        EasyQuestionHelper.generateQuestionList().get(Integer.parseInt(data.getBase64())).getImages().get(0)
                                ));
                    } else {
                        binding.ivQuestion.setBackground(
                                ContextCompat.getDrawable(DetailRecordActivity.this,
                                        IntermediateQuestionHelper.generateQuestionList().get(Integer.parseInt(data.getBase64())).getImages().get(0)
                                ));
                    }
                } else {
                    binding.ivQuestion.setBackground(base64ToDrawable(data.getBase64(), DetailRecordActivity.this));
                }
                binding.tvGraphQuestion.setVisibility(View.VISIBLE);
                binding.ivQuestion.setVisibility(View.VISIBLE);
            } else {
                binding.tvGraphQuestion.setVisibility(View.GONE);
                binding.cvGraph.setVisibility(View.GONE);
            }

        // Handle Image 2 (base64_2) - hanya untuk Advanced question
        if (data.getBase64_2() != null && !data.getBase64_2().isEmpty()) {
            if (data.getTopics().equals("Out Class") || data.getTopics().equals("In Class")) {
                if (data.getTypeQuestion() == "Advanced") {
                    // Hanya untuk Advanced question yang memiliki 2 image
                    binding.ivImage2.setBackground(
                            ContextCompat.getDrawable(DetailRecordActivity.this,
                                    AdvancedQuestionHelper.generateQuestionList().get(Integer.parseInt(data.getBase64_2())).getImages().get(1)
                            ));
                    binding.tvImage2.setVisibility(View.VISIBLE);
                    binding.cvImage2.setVisibility(View.VISIBLE);
                }
                // Untuk Easy dan Intermediate, tidak menampilkan image kedua
            } else {
                // Untuk non-class question, tetap tampilkan jika ada base64_2
                binding.ivImage2.setBackground(base64ToDrawable(data.getBase64_2(), DetailRecordActivity.this));
                binding.tvImage2.setVisibility(View.VISIBLE);
                binding.cvImage2.setVisibility(View.VISIBLE);
            }
        }

        // Handle Table 1 (base64_3)
        if (data.getBase64_3() != null && !data.getBase64_3().isEmpty()) {
            binding.ivTable1.setBackground(base64ToDrawable(data.getBase64_3(), DetailRecordActivity.this));
            binding.tvTable1.setVisibility(View.VISIBLE);
            binding.cvTable1.setVisibility(View.VISIBLE);
        }

        // Handle Table 2 (base64_4)
        if (data.getBase64_4() != null && !data.getBase64_4().isEmpty()) {
            binding.ivTable2.setBackground(base64ToDrawable(data.getBase64_4(), DetailRecordActivity.this));
            binding.tvTable2.setVisibility(View.VISIBLE);
            binding.cvTable2.setVisibility(View.VISIBLE);
        }

        // Handle Image 3 (base64_5)
        if (data.getBase64_5() != null && !data.getBase64_5().isEmpty()) {
            binding.ivImage3.setBackground(base64ToDrawable(data.getBase64_5(), DetailRecordActivity.this));
            binding.tvImage3.setVisibility(View.VISIBLE);
            binding.cvImage3.setVisibility(View.VISIBLE);
        }

        // Tampilkan foto jawaban jika ada
        if (data.getPhoto() != null && !data.getPhoto().isEmpty()) {
            loadImageWithGlide(data.getPhoto(), binding.ivImage);
            binding.ivImage.setVisibility(View.VISIBLE);
            binding.cvImage.setVisibility(View.VISIBLE);
        } else {
            binding.ivImage.setVisibility(View.GONE);
            binding.cvImage.setVisibility(View.GONE);
        }
        if(!data.getPhotoDraw().isEmpty()){
            loadImageWithGlide(data.getPhotoDraw().get(data.getPhotoDraw().size()-1), binding.ivFeedBack);
        }
    }

    private void loadImageWithGlide(String url, ImageView imageView) {
        Glide.with(this)
                .load(url)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        imageView.setBackground(resource); // Set as background
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle case when the image is cleared
                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;  // Prevent memory leaks
    }

    public BitmapDrawable base64ToDrawable(String base64String, Context context) {
        try {
            // Remove Base64 headers (if any)
            if (base64String.contains(",")) {
                base64String = base64String.split(",")[1];
            }

            // Remove spaces & newlines
            base64String = base64String.replaceAll("\\s+", "").trim();

            // Decode Base64 safely
            byte[] decodedByte = Base64.decode(base64String, Base64.NO_WRAP);

            // Convert to Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);

            // Return Drawable
            return new BitmapDrawable(context.getResources(), bitmap);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.e("Base64Error", "Invalid Base64 String: " + e.getMessage());
            return null;
        }
    }
}
