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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

import de.rwth_aachen.phyphox.Helper.AdvancedQuestionHelper;
import de.rwth_aachen.phyphox.Helper.EasyQuestionHelper;
import de.rwth_aachen.phyphox.Helper.IntermediateQuestionHelper;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.databinding.ActivityDetailRecordBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import de.rwth_aachen.phyphox.App;

public class DetailRecordActivity extends AppCompatActivity {

    private ActivityDetailRecordBinding binding;
    private DataModel dataModel;
    private DrawingAdapter drawingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        binding = ActivityDetailRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📋 Detail Lengkap");
        }

        // Handle Back Button Click
        binding.toolbar.setNavigationOnClickListener(view -> onBackPressed());

        // Get DataModel from App singleton
        App app = (App) getApplication();
        dataModel = app.getDataModel();

        setupUI();
        setupImageCarouselButton();
        setupDrawingsList();
    }

    private void setupUI() {
        if (dataModel == null) return;

        // Set basic info
        binding.tvQuestion.setText(dataModel.getQuestion() != null ? dataModel.getQuestion() : "Tidak ada pertanyaan");
        
        // Format type question to remove "Advanced"
        String typeQuestion = dataModel.getTypeQuestion();
        if (typeQuestion != null && typeQuestion.equalsIgnoreCase("Advanced")) {
            binding.tvType.setText("Sulit");
        } else {
            binding.tvType.setText(typeQuestion != null ? typeQuestion : "Sedang");
        }
        
        binding.tvDateTime.setText(dataModel.getDateTime() != null ? dataModel.getDateTime() : "Unknown");
        binding.tvCustomerName.setText(dataModel.getCustomerName() != null ? dataModel.getCustomerName() : "Unknown");
        binding.tvId.setText(dataModel.getId() != null ? dataModel.getId() : "Unknown");
        binding.tvTotalEdit.setText(dataModel.getTotalEdit() + " kali");
        binding.tvStatus.setText(dataModel.getFinished() != null && dataModel.getFinished() ? "Completed" : "In Progress");

        // Load images into grid
        loadImageIntoView(dataModel.getBase64(), binding.ivImage, binding.cvImage);
        loadImageIntoView(dataModel.getBase64_2(), binding.ivImage2, binding.cvImage2);
        loadImageIntoView(dataModel.getBase64_3(), binding.ivImage3, binding.cvImage3);
        loadImageIntoView(dataModel.getBase64_4(), binding.ivImage4, binding.cvImage4);
        loadImageIntoView(dataModel.getBase64_5(), binding.ivImage5, binding.cvImage5);
    }

    private void loadImageIntoView(String base64Data, ImageView imageView, View cardView) {
        if (base64Data != null && !base64Data.isEmpty()) {
            // Check if it's from class questions (uses drawable resources)
            if (dataModel.getTopics() != null && 
                (dataModel.getTopics().equals("Out Class") || dataModel.getTopics().equals("In Class"))) {
                
                try {
                    int index = Integer.parseInt(base64Data);
                    int drawableId = getDrawableIdFromQuestionType(index);
                    if (drawableId != 0) {
                        imageView.setImageDrawable(ContextCompat.getDrawable(this, drawableId));
                        cardView.setVisibility(View.VISIBLE);
                        return;
                    }
                } catch (NumberFormatException e) {
                    // Continue with base64 loading
                }
            }
            
            // Load base64 image
            BitmapDrawable drawable = base64ToDrawable(base64Data, this);
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
                cardView.setVisibility(View.VISIBLE);
            } else {
                cardView.setVisibility(View.GONE);
            }
        } else {
            cardView.setVisibility(View.GONE);
        }
    }

    private int getDrawableIdFromQuestionType(int index) {
        String typeQuestion = dataModel.getTypeQuestion();
        if (typeQuestion == null) return 0;

        try {
            switch (typeQuestion) {
                case "Easy":
                case "Mudah":
                    return EasyQuestionHelper.generateQuestionList().get(index).getImages().get(0);
                case "Intermediate":
                case "Sedang":
                    return IntermediateQuestionHelper.generateQuestionList().get(index).getImages().get(0);
                case "Advanced":
                case "Sulit":
                case "Lanjutan":
                    return AdvancedQuestionHelper.generateQuestionList().get(index).getImages().get(0);
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private void setupImageCarouselButton() {
        binding.btnViewCarousel.setOnClickListener(v -> {
            Intent intent = new Intent(DetailRecordActivity.this, ImageCarouselActivity.class);
            startActivity(intent);
        });
    }

    private void setupDrawingsList() {
        if (dataModel.getPhotoDraw() != null && !dataModel.getPhotoDraw().isEmpty()) {
            binding.llNoDrawings.setVisibility(View.GONE);
            binding.rvDrawings.setVisibility(View.VISIBLE);
            
            drawingAdapter = new DrawingAdapter(this, dataModel.getPhotoDraw());
            binding.rvDrawings.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            );
            binding.rvDrawings.setAdapter(drawingAdapter);
        } else {
            binding.llNoDrawings.setVisibility(View.VISIBLE);
            binding.rvDrawings.setVisibility(View.GONE);
        }
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

    // Adapter for drawing images
    public static class DrawingAdapter extends RecyclerView.Adapter<DrawingAdapter.ViewHolder> {
        private Context context;
        private List<String> drawingUrls;

        public DrawingAdapter(Context context, List<String> drawingUrls) {
            this.context = context;
            this.drawingUrls = drawingUrls != null ? drawingUrls : new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_drawing_thumbnail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String drawingUrl = drawingUrls.get(position);
            
            if (drawingUrl != null && !drawingUrl.isEmpty()) {
                Glide.with(context)
                    .load(drawingUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(holder.ivDrawing);
            }
        }

        @Override
        public int getItemCount() {
            return drawingUrls.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivDrawing;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivDrawing = itemView.findViewById(R.id.iv_drawing_thumbnail);
            }
        }
    }
}

