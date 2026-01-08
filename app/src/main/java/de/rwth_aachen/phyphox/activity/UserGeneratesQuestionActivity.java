package de.rwth_aachen.phyphox.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.AdvancedQuestionHelper;
import de.rwth_aachen.phyphox.Helper.EasyQuestionHelper;
import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.IntermediateQuestionHelper;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.Helper.StorageUtil;
import de.rwth_aachen.phyphox.Helper.StorageUtil;
import de.rwth_aachen.phyphox.NetworkConnection.ApiRequest;
import de.rwth_aachen.phyphox.NetworkConnection.ApiResponse;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.databinding.ActivityUserGeneratesNewBinding;
import de.rwth_aachen.phyphox.databinding.DialogIntroductionBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import de.rwth_aachen.phyphox.model.QuestionModel;
import de.rwth_aachen.phyphox.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserGeneratesQuestionActivity extends AppCompatActivity {
    private long visitStartTime;
    ActivityUserGeneratesNewBinding binding;
    private DrawView drawView; // Inisialisasi untuk canvas menggambar
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Uri photoUri;
    private String currentPhotoPath;
    private ApiService apiService;
    byte[] imageBytes= new byte[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inisialisasi ViewBinding
        binding = ActivityUserGeneratesNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        App app = (App) getApplication();
        DataModel dataModel = app.getDataModel();
        boolean isFromMainMenu = getIntent().getBooleanExtra("isFromMainMenu", true);
        if (!dataModel.getTopics().equals("In Class")) {
            fetchAdvancedQuestion("indonesia", dataModel.getTypeQuestion());
        }
        // Inisialisasi DrawView
        drawView = binding.drawView;

        // Tombol Undo
        binding.btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.undo(); // Panggil fungsi undo di DrawView
            }
        });

        // Tombol Redo
        binding.btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.redo(); // Panggil fungsi redo di DrawView
            }
        });

        // Tombol Clear (menghapus semua coretan di canvas)
        binding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.clearCanvas(); // Menghapus canvas
            }
        });
        binding.ivSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showIntroductionDialog();
            }
        });
        binding.btnTakePhoto.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.d("btnTakePhoto ","permission failed");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                Log.d("btnTakePhoto ","takePictureIntent");
                dispatchTakePictureIntent();
            }
        });
        // Tombol Upload - Combines canvas and photo into final answer
        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.btnNext.setEnabled(false);
                
                try {
                    // Get canvas drawing
                    Bitmap canvasBitmap = getViewAsBitmap(binding.llDraw);
                    
                    // Check if we have both canvas and photo
                    if (imageBytes.length > 0 && canvasBitmap != null) {
                        // Combine canvas and photo
                        Bitmap combinedBitmap = combineCanvasAndPhoto(canvasBitmap);
                        if (combinedBitmap != null) {
                            byte[] combinedImageBytes = convertBitmapToBytes(combinedBitmap);
                            uploadImageToFirestore(combinedImageBytes, "combined_answer_" + System.currentTimeMillis());
                        } else {
                            // Fallback to photo only if combine fails
                            uploadImageCameraToFirestore(imageBytes, "photo_answer_" + System.currentTimeMillis());
                        }
                    } else if (imageBytes.length > 0) {
                        // Photo only
                        uploadImageCameraToFirestore(imageBytes, "photo_answer_" + System.currentTimeMillis());
                    } else if (canvasBitmap != null) {
                        // Canvas only
                        uploadImageToFirestore(convertBitmapToBytes(canvasBitmap), "canvas_answer_" + System.currentTimeMillis());
                    } else {
                        Toast.makeText(UserGeneratesQuestionActivity.this, "Tidak ada gambar untuk diupload", Toast.LENGTH_SHORT).show();
                        binding.btnNext.setEnabled(true);
                    }
                } catch (Exception e) {
                    Log.e("UPLOAD_ERROR", "Error during upload: " + e.getMessage());
                    Toast.makeText(UserGeneratesQuestionActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.btnNext.setEnabled(true);
                }
            }
        });

        // Tombol Next yang mengarahkan ke FeedbackActivity
        binding.btnNext.setOnClickListener(v -> {
            dataModel.setQuestion(binding.tvQuestion.getText().toString());
            dataModel.setCustomerName(SessionManager.getName(this));
            
            // Set creator fields for custom questions
            String userName = SessionManager.getName(this);
            if (userName == null || userName.trim().isEmpty()) {
                userName = dataModel.getCustomerName();
            }
            if (userName == null || userName.trim().isEmpty()) {
                userName = "Unknown";
            }
            dataModel.setCreatorName(userName);
            dataModel.setDibuatOleh(userName);
            dataModel.setCreatedBy(userName);
            
            // Remove location fields for custom questions
            dataModel.setLatitude(0.0);
            dataModel.setLongitude(0.0);
            dataModel.setLocationName(null);
            
            // Remove totalEdit and views for custom questions (not needed)
            // Note: These fields won't be saved to Firestore for custom questions
            // They are only relevant for experiments/records
            
            Log.d("btnsave","--> "+new Gson().toJson(dataModel));
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Save data to Server");
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            String userId = SessionManager.getId(UserGeneratesQuestionActivity.this);
            
            // Upload photoAnswer to Firebase Storage if exists
            if (dataModel.getPhotoAnswer() != null && !dataModel.getPhotoAnswer().isEmpty()) {
                uploadPhotoAnswerToStorage(dataModel, progressDialog, userId, userName);
            } else {
                // No photoAnswer, save directly
                saveDataModelToFirestore(dataModel, progressDialog, userId, userName);
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    // Save progress when back button is pressed
                    App app = (App) getApplication();
                    DataModel dataModel = app.getDataModel();
                    
                    if (getIntent().getBooleanExtra("isFromMainMenu", true)) {
                        // Simpan progress ke Firestore tanpa ProgressDialog untuk performa lebih baik
                        dataModel.setQuestion(binding.tvQuestion.getText().toString());
                        dataModel.setCustomerName(SessionManager.getName(UserGeneratesQuestionActivity.this));
                        dataModel.setTypeData(binding.tvType.getText().toString());
                        
                        // Set creator fields for custom questions
                        String userName = SessionManager.getName(UserGeneratesQuestionActivity.this);
                        if (userName == null || userName.trim().isEmpty()) {
                            userName = dataModel.getCustomerName();
                        }
                        if (userName == null || userName.trim().isEmpty()) {
                            userName = "Unknown";
                        }
                        dataModel.setCreatorName(userName);
                        dataModel.setDibuatOleh(userName);
                        dataModel.setCreatedBy(userName);
                        
                        // Remove location fields for custom questions
                        dataModel.setLatitude(0.0);
                        dataModel.setLongitude(0.0);
                        dataModel.setLocationName(null);
                        
                        String userId = SessionManager.getId(UserGeneratesQuestionActivity.this);
                        
                        // Upload photoAnswer to Firebase Storage if exists
                        if (dataModel.getPhotoAnswer() != null && !dataModel.getPhotoAnswer().isEmpty()) {
                            uploadPhotoAnswerToStorage(dataModel, null, userId, userName);
                        } else {
                            // No photoAnswer, save directly
                            saveDataModelToFirestore(dataModel, null, userId, userName);
                        }
                    } else {
                        finish();
                    }
                } catch (Exception e) {
                    Log.e("BACK_BUTTON", "Unexpected error in handleOnBackPressed: " + e.getMessage());
                    // Fallback: just finish the activity
                    finish();
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void dispatchTakePictureIntent() {
        Log.d("btnTakePhoto", "dispatchTakePictureIntent");

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.d("btnTakePhoto", "takePictureIntent");

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
        }

        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".exportProvider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                // Show photo preview container
                binding.llPhotoPreview.setVisibility(View.VISIBLE);
                
                // Load and process the captured photo
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                if (bitmap != null) {
                    // Optimize bitmap for better performance and storage
                    bitmap = optimizeBitmapForStorage(bitmap);
                    
                    // Convert to byte array with compression
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                    imageBytes = baos.toByteArray();
                    
                    // Convert to base64 for storage
                    String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                    
                    // Display in preview with PhotoView for zoom capability
                    BitmapDrawable drawable = base64ToDrawable(base64Image, UserGeneratesQuestionActivity.this);
                    if (drawable != null) {
                        loadImageWithGlide(drawable, binding.ivPhoto);
                        
                        // Configure PhotoView for photo preview
                        if (binding.ivPhoto instanceof com.github.chrisbanes.photoview.PhotoView) {
                            com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) binding.ivPhoto;
                            photoView.setMaximumScale(4.0f);
                            photoView.setMediumScale(2.0f);
                            photoView.setMinimumScale(0.8f);
                            photoView.setZoomable(true);
                        }
                    }
                    
                    // Save to DataModel for later use
                    App app = (App) getApplication();
                    DataModel dataModel = app.getDataModel();
                    dataModel.setPhotoAnswer(base64Image);
                    app.setDataModel(dataModel);
                    
                    // Show success message
                    Toast.makeText(this, "Foto berhasil diambil dan ditampilkan di preview", Toast.LENGTH_SHORT).show();
                    
                    Log.d("PHOTO_CAPTURE", "Photo successfully captured and processed");
                } else {
                    Toast.makeText(this, "Gagal memproses foto", Toast.LENGTH_SHORT).show();
                    Log.e("PHOTO_CAPTURE", "Failed to decode bitmap from photo path");
                }
            } catch (Exception e) {
                Log.e("PHOTO_CAPTURE", "Error processing captured photo: " + e.getMessage());
                Toast.makeText(this, "Error memproses foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void uploadImageCameraToFirestore(byte[] imageData, String fileName) {
        // Create and configure ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Image Camera");
        progressDialog.setMessage("Please wait while the image is being uploaded...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Generate filename with userId and timestamp
        String userId = SessionManager.getId(this);
        String userName = SessionManager.getName(this);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String uniqueFileName = userId + "_photo_" + timestamp + "_" + System.currentTimeMillis() + ".jpg";

        // Upload using StorageUtil with userId organization and metadata
        StorageUtil.uploadBytesWithUserIdAndMetadata(
            this,
            imageData,
            StorageUtil.STORAGE_PATH_DOCUMENTATION,
            uniqueFileName,
            "Buat Pertanyaan Sendiri", // Source metadata
            userName, // User name metadata
            (downloadUrl, storagePath) -> {
                // Save the download URL to DataModel
                App app = (App) getApplication();
                DataModel dataModel = app.getDataModel();
                dataModel.setPhoto(downloadUrl);
                dataModel.setTypeData(binding.tvType.getText().toString());
                app.setDataModel(dataModel);
                progressDialog.dismiss();
                
                // Upload canvas drawing next
                String canvasFileName = userId + "_canvas_" + timestamp + "_" + System.currentTimeMillis() + ".jpg";
                uploadImageToFirestore(convertBitmapToBytes(getViewAsBitmap(binding.llDraw)), canvasFileName);
            },
            e -> {
                // Handle upload failure
                Log.e("Firebase", "Image upload failed", e);
                progressDialog.dismiss();
                Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            },
            progress -> {
                // Update progress
                progressDialog.setMessage("Uploaded: " + (int) progress + "%");
            }
        );
    }


    public Bitmap getViewAsBitmap(View view) {
        // Enable drawing cache
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        // Create a bitmap from the view's drawing cache
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        // Disable drawing cache
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    /**
     * Optimize bitmap for storage by resizing if necessary
     * @param originalBitmap Original bitmap to optimize
     * @return Optimized bitmap with reasonable dimensions
     */
    private Bitmap optimizeBitmapForStorage(Bitmap originalBitmap) {
        if (originalBitmap == null) return null;
        
        // Check if bitmap is already reasonably sized
        int maxDimension = 1200; // Maximum dimension for storage
        if (originalBitmap.getWidth() <= maxDimension && originalBitmap.getHeight() <= maxDimension) {
            return originalBitmap; // No optimization needed
        }
        
        // Calculate new dimensions while maintaining aspect ratio
        float scale = Math.min((float) maxDimension / originalBitmap.getWidth(), 
                              (float) maxDimension / originalBitmap.getHeight());
        
        int newWidth = Math.round(originalBitmap.getWidth() * scale);
        int newHeight = Math.round(originalBitmap.getHeight() * scale);
        
        Log.d("BITMAP_OPTIMIZE", "Resizing bitmap from " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight() + 
              " to " + newWidth + "x" + newHeight);
        
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    /**
     * Combine canvas drawing and captured photo into a single image
     * @param canvasBitmap The canvas drawing bitmap
     * @return Combined bitmap with photo on top and canvas below
     */
    private Bitmap combineCanvasAndPhoto(Bitmap canvasBitmap) {
        try {
            if (imageBytes.length == 0 || canvasBitmap == null) {
                return canvasBitmap; // Return canvas only if no photo
            }
            
            // Convert photo bytes back to bitmap
            Bitmap photoBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (photoBitmap == null) {
                Log.e("COMBINE_IMAGE", "Failed to decode photo bitmap");
                return canvasBitmap;
            }
            
            // Calculate dimensions for combined image
            int maxWidth = Math.max(canvasBitmap.getWidth(), photoBitmap.getWidth());
            int totalHeight = canvasBitmap.getHeight() + photoBitmap.getHeight() + 20; // 20px spacing
            
            // Create combined bitmap
            Bitmap combinedBitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888);
            Canvas combinedCanvas = new Canvas(combinedBitmap);
            
            // Fill with white background
            combinedCanvas.drawColor(android.graphics.Color.WHITE);
            
            // Draw photo at the top (centered horizontally)
            float photoX = (maxWidth - photoBitmap.getWidth()) / 2f;
            combinedCanvas.drawBitmap(photoBitmap, photoX, 0, null);
            
            // Draw canvas below photo with spacing
            float canvasY = photoBitmap.getHeight() + 20;
            float canvasX = (maxWidth - canvasBitmap.getWidth()) / 2f;
            combinedCanvas.drawBitmap(canvasBitmap, canvasX, canvasY, null);
            
            Log.d("COMBINE_IMAGE", "Successfully combined photo (" + photoBitmap.getWidth() + "x" + photoBitmap.getHeight() + 
                  ") and canvas (" + canvasBitmap.getWidth() + "x" + canvasBitmap.getHeight() + 
                  ") into " + maxWidth + "x" + totalHeight);
            
            return combinedBitmap;
            
        } catch (Exception e) {
            Log.e("COMBINE_IMAGE", "Error combining images: " + e.getMessage());
            return canvasBitmap; // Return canvas only as fallback
        }
    }

    public byte[] convertBitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); // Reduce quality
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Compress or truncate base64 string to stay under Firestore limit (1MB)
     * @param base64String Original base64 string
     * @return Compressed or truncated base64 string
     */
    private String compressBase64ForFirestore(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return base64String;
        }
        
        // Firestore limit is 1048487 bytes (~1MB)
        final int FIRESTORE_LIMIT = 1000000; // Leave some margin
        
        if (base64String.length() > FIRESTORE_LIMIT) {
            Log.w("FIRESTORE_COMPRESS", "Base64 string too large (" + base64String.length() + " bytes), compressing...");
            
            try {
                // Try to compress the image data
                String base64Data = base64String;
                if (base64String.contains(",")) {
                    base64Data = base64String.split(",")[1];
                }
                
                byte[] decodedBytes = Base64.decode(base64Data, Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                
                if (bitmap != null) {
                    // Compress with lower quality
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    int quality = 30; // Start with low quality
                    
                    do {
                        outputStream.reset();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                        quality -= 5; // Reduce quality further if still too large
                    } while (outputStream.size() > FIRESTORE_LIMIT && quality > 5);
                    
                    String compressedBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
                    Log.d("FIRESTORE_COMPRESS", "Compressed from " + base64String.length() + " to " + compressedBase64.length() + " bytes");
                    return compressedBase64;
                }
            } catch (Exception e) {
                Log.e("FIRESTORE_COMPRESS", "Error compressing image: " + e.getMessage());
            }
            
            // If compression fails, truncate the string
            String truncated = base64String.substring(0, FIRESTORE_LIMIT);
            Log.w("FIRESTORE_COMPRESS", "Compression failed, truncated to " + truncated.length() + " bytes");
            return truncated;
        }
        
        return base64String;
    }

    public void uploadImageToFirestore(byte[] imageData, String fileName) {
        // Create and configure ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Image");
        progressDialog.setMessage("Please wait while the image is being uploaded...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Generate filename with userId and timestamp if not already provided
        String userId = SessionManager.getId(this);
        String userName = SessionManager.getName(this);
        String finalFileName = fileName;
        if (!fileName.contains(userId)) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            finalFileName = userId + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_") + "_" + timestamp + ".jpg";
        }

        // Upload using StorageUtil with userId organization and metadata
        StorageUtil.uploadBytesWithUserIdAndMetadata(
            this,
            imageData,
            StorageUtil.STORAGE_PATH_DOCUMENTATION,
            finalFileName,
            "Buat Pertanyaan Sendiri", // Source metadata
            userName, // User name metadata
            (downloadUrl, storagePath) -> {
                // Save the download URL to DataModel
                App app = (App) getApplication();
                DataModel dataModel = app.getDataModel();
                dataModel.setBase64(downloadUrl);
                app.setDataModel(dataModel);

                // Enable the save button
                binding.btnNext.setEnabled(true);
                binding.btnNext.performClick();
                
                // Log success
                Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);
                Log.d("Firebase", "Storage path: " + storagePath);

                // Dismiss the progress dialog
                progressDialog.dismiss();
            },
            e -> {
                // Handle upload failure
                Log.e("Firebase", "Image upload failed", e);
                progressDialog.dismiss();
                Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            },
            progress -> {
                // Update progress
                progressDialog.setMessage("Uploaded: " + (int) progress + "%");
            }
        );
    }

    private void fetchAdvancedQuestion(String language, String type) {
        ProgressDialog progressDialog = new ProgressDialog(this); // Replace 'this' with 'requireContext()' if inside a Fragment
        progressDialog.setTitle("");
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        ApiRequest request = new ApiRequest(language, de.rwth_aachen.phyphox.Helper.SessionManager.getId(this));

        Callback<ApiResponse> callback = new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    App app = (App) getApplication();
                    DataModel dataModel = app.getDataModel();
                    dataModel.setQuestion(apiResponse.getQuestions());
                    dataModel.setIdCustomer(SessionManager.getId(UserGeneratesQuestionActivity.this));
//                    binding.tvQuestion.setText(apiResponse.getQuestions());

                    // Reset all image views to GONE at the start
                    binding.iv.setVisibility(View.GONE);
                    binding.iv2.setVisibility(View.GONE);
                    binding.iv3.setVisibility(View.GONE);
                    binding.iv4.setVisibility(View.GONE);
                    binding.iv5.setVisibility(View.GONE);

                    // Handle Graph Images (graphImages -> iv)
                    if (apiResponse.getGraphImages() != null && !apiResponse.getGraphImages().isEmpty()) {
                        try {
                            String originalBase64 = apiResponse.getGraphImages().get(0);
                            if (originalBase64 != null && !originalBase64.trim().isEmpty()) {
                                String compressedBase64 = compressBase64ForFirestore(originalBase64);
                                dataModel.setBase64(compressedBase64);
                                Log.d("getGraphImages --&> ", "" + originalBase64.substring(0, Math.min(50, originalBase64.length())));
                                
                                BitmapDrawable drawable = base64ToDrawable(originalBase64, UserGeneratesQuestionActivity.this);
                                if (drawable != null) {
                                    loadImageWithGlide(drawable, binding.iv);
                                    binding.iv.setVisibility(View.VISIBLE);
                                } else {
                                    Log.e("IMAGE_LOAD", "Failed to convert graph image to drawable");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("IMAGE_LOAD", "Error loading graph image: " + e.getMessage());
                        }
                    }
                    // Handle Image (image1_base64 -> iv2)
                    if (apiResponse.getImage1_base64() != null && !apiResponse.getImage1_base64().isEmpty()) {
                        String originalBase64_2 = apiResponse.getImage1_base64();
                        String compressedBase64_2 = compressBase64ForFirestore(originalBase64_2);
                        dataModel.setBase64_2(compressedBase64_2);
                        Log.d("getImage1_base64 --&> ", "" + originalBase64_2.substring(0, Math.min(50, originalBase64_2.length())));
                        loadImageWithGlide(base64ToDrawable(originalBase64_2, UserGeneratesQuestionActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }
                    // Handle Table Images
                    boolean hasTableImages = false;
                    
                    // Handle Table 1 (table_img_base64_1 -> iv3)
                    if (apiResponse.getTable_img_base64_1() != null && !apiResponse.getTable_img_base64_1().isEmpty()) {
                        try {
                            String originalBase64_3 = apiResponse.getTable_img_base64_1();
                            if (originalBase64_3 != null && !originalBase64_3.trim().isEmpty()) {
                                String compressedBase64_3 = compressBase64ForFirestore(originalBase64_3);
                                dataModel.setBase64_3(compressedBase64_3);
                                Log.d("getTable_img_base64_1 --&> ", "" + originalBase64_3.substring(0, Math.min(50, originalBase64_3.length())));
                                
                                BitmapDrawable drawable = base64ToDrawable(originalBase64_3, UserGeneratesQuestionActivity.this);
                                if (drawable != null) {
                                    loadImageWithGlide(drawable, binding.iv3);
                                    binding.iv3.setVisibility(View.VISIBLE);
                                    hasTableImages = true;
                                } else {
                                    Log.e("IMAGE_LOAD", "Failed to convert table image 1 to drawable");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("IMAGE_LOAD", "Error loading table image 1: " + e.getMessage());
                        }
                    }
                    
                    // Handle Table 2 (table_img_base64_2 -> iv4)
                    if (apiResponse.getTable_img_base64_2() != null && !apiResponse.getTable_img_base64_2().isEmpty()) {
                        try {
                            String originalBase64_4 = apiResponse.getTable_img_base64_2();
                            if (originalBase64_4 != null && !originalBase64_4.trim().isEmpty()) {
                                String compressedBase64_4 = compressBase64ForFirestore(originalBase64_4);
                                dataModel.setBase64_4(compressedBase64_4);
                                Log.d("getTable_img_base64_2 --&> ", "" + originalBase64_4.substring(0, Math.min(50, originalBase64_4.length())));
                                
                                BitmapDrawable drawable = base64ToDrawable(originalBase64_4, UserGeneratesQuestionActivity.this);
                                if (drawable != null) {
                                    loadImageWithGlide(drawable, binding.iv4);
                                    binding.iv4.setVisibility(View.VISIBLE);
                                    hasTableImages = true;
                                } else {
                                    Log.e("IMAGE_LOAD", "Failed to convert table image 2 to drawable");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("IMAGE_LOAD", "Error loading table image 2: " + e.getMessage());
                        }
                    }
                    
                    // Show table images container if there are table images
                    if (hasTableImages) {
                        binding.tableImagesContainer.setVisibility(View.VISIBLE);
                    }

                    // (Optional) Handle table_img_base64 (lama) jika masih dipakai untuk iv3
                    if (apiResponse.getTable_img_base64() != null && !apiResponse.getTable_img_base64().isEmpty()) {
                        String originalFallback3 = apiResponse.getTable_img_base64();
                        String compressedFallback3 = compressBase64ForFirestore(originalFallback3);
                        dataModel.setBase64_3(compressedFallback3);
                        Log.d("getTable_img_base64 --&> ", "" + originalFallback3.substring(0, Math.min(50, originalFallback3.length())));
                        loadImageWithGlide(base64ToDrawable(originalFallback3, UserGeneratesQuestionActivity.this), binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                    }

                    // (Optional) Handle local_image_base64 jika ingin tetap tampilkan di iv2
                    if (apiResponse.getLocal_image_base64() != null && !apiResponse.getLocal_image_base64().isEmpty()) {
                        String originalFallback2 = apiResponse.getLocal_image_base64();
                        String compressedFallback2 = compressBase64ForFirestore(originalFallback2);
                        dataModel.setBase64_2(compressedFallback2);
                        Log.d("getLocal_image_base64 --&> ", "" + originalFallback2.substring(0, Math.min(50, originalFallback2.length())));
                        loadImageWithGlide(base64ToDrawable(originalFallback2, UserGeneratesQuestionActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }

                    // Handle image2_base64 (baru) -> iv5
                    if (apiResponse.getImage2_base64() != null && !apiResponse.getImage2_base64().isEmpty()) {
                        String originalBase64_5 = apiResponse.getImage2_base64();
                        String compressedBase64_5 = compressBase64ForFirestore(originalBase64_5);
                        dataModel.setBase64_5(compressedBase64_5);
                        Log.d("getImage2_base64 --&> ", "" + originalBase64_5.substring(0, Math.min(50, originalBase64_5.length())));
                        loadImageWithGlide(base64ToDrawable(originalBase64_5, UserGeneratesQuestionActivity.this), binding.iv5);
                        binding.iv5.setVisibility(View.VISIBLE);
                    }

                    app.setDataModel(dataModel);

                    // Handle Experiment Data
                    if (apiResponse.getExperiment1Data() != null) {
                        Log.d("Retrofit", "Experiment 1 Data: " + apiResponse.getExperiment1Data().size());
                    }
                    if (apiResponse.getExperiment2Data() != null) {
                        Log.d("Retrofit", "Experiment 2 Data: " + apiResponse.getExperiment2Data().size());
                    }
                } else {
                    Log.e("Retrofit", "Failed to fetch question");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("Retrofit", "Error: " + t.getMessage());
                progressDialog.dismiss();
                Toast.makeText(UserGeneratesQuestionActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
            }
        };
        if (type.equals("Easy")) {
            apiService.getEasyQuestion(request).enqueue(callback);
        } else if (type.equals("Intermediate")) {
            apiService.getIntermediateQuestion(request).enqueue(callback);
        } else {
            apiService.getAdvancedQuestion(request).enqueue(callback);
        }
    }

    //    @NonNull
//    @Override
//    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
//        Intent intent = new Intent(GeneratesActivity.this, HistoryRecordActivity.class);
//        startActivity(intent);
//        return super.getOnBackInvokedDispatcher();
//    }
    private void loadImageWithGlide(BitmapDrawable url, ImageView imageView) {
        Glide.with(this)
                .load(url)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        imageView.setImageDrawable(resource); // Set as image for PhotoView zoom functionality
                        
                        // Optimize image view size for table images
                        if (imageView.getId() == R.id.iv3 || imageView.getId() == R.id.iv4) {
                            optimizeImageViewForTable(imageView, resource);
                        }
                        
                        // Enhanced zoom for other PhotoView images (excluding table images iv3, iv4)
                        if (imageView instanceof com.github.chrisbanes.photoview.PhotoView) {
                            com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) imageView;
                            
                            // Configure zoom based on image type - set in correct order
                            if (imageView.getId() == R.id.iv5) {
                                // Additional image - moderate zoom
                                photoView.setMinimumScale(0.7f);
                                photoView.setMediumScale(2.5f);
                                photoView.setMaximumScale(4.0f);
                            } else if (imageView.getId() == R.id.ivDraw) {
                                // Drawing image - high zoom for detail
                                photoView.setMinimumScale(0.5f);
                                photoView.setMediumScale(3.0f);
                                photoView.setMaximumScale(6.0f);
                            } else if (imageView.getId() != R.id.iv3 && imageView.getId() != R.id.iv4) {
                                // Default zoom for other images, excluding table images (iv3, iv4)
                                photoView.setMinimumScale(0.8f);
                                photoView.setMediumScale(2.0f);
                                photoView.setMaximumScale(3.0f);
                            }
                            // Note: iv3 and iv4 (table images) are handled by optimizeImageViewForTable
                            
                            // Enable smooth zoom transitions
                            photoView.setZoomTransitionDuration(300);
                            
                            // Enable double tap to zoom (PhotoView handles this automatically)
                            photoView.setZoomable(true);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle case when the image is cleared
                    }
                });
    }
    
    /**
     * Optimize ImageView size for table images to prevent overlapping
     */
    private void optimizeImageViewForTable(ImageView imageView, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) {
                int imageWidth = bitmap.getWidth();
                int imageHeight = bitmap.getHeight();
                
                // Calculate optimal height based on image aspect ratio
                int screenWidth = getResources().getDisplayMetrics().widthPixels - 64; // Account for margins
                float aspectRatio = (float) imageHeight / imageWidth;
                int optimalHeight = Math.round(screenWidth * aspectRatio);
                
                // Set constraints for table images
                int minHeight = 200;
                int maxHeight = 600;
                optimalHeight = Math.max(minHeight, Math.min(maxHeight, optimalHeight));
                
                // Update layout parameters
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                params.height = optimalHeight;
                imageView.setLayoutParams(params);
                
                // Enhanced zoom configuration for PhotoView - RESET first to avoid conflicts
                if (imageView instanceof com.github.chrisbanes.photoview.PhotoView) {
                    com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) imageView;
                    
                    try {
                        // CRITICAL: Reset to default PhotoView state first to clear any existing zoom conflicts
                        photoView.setScale(1.0f, true);
                        
                        // Set maximum first to establish upper bound, then work downward
                        photoView.setMaximumScale(5.0f);
                        photoView.setMediumScale(2.5f);  // Safe value less than max
                        photoView.setMinimumScale(0.5f);
                        
                        // Enable smooth zoom transitions
                        photoView.setZoomTransitionDuration(300);
                        
                        // Enable double tap to zoom (PhotoView handles this automatically)
                        photoView.setZoomable(true);
                        
                        Log.d("ZOOM_CONFIG", "Table image zoom configured successfully: " + 
                              "min=" + photoView.getMinimumScale() + 
                              ", medium=" + photoView.getMediumScale() + 
                              ", max=" + photoView.getMaximumScale());
                              
                    } catch (Exception e) {
                        Log.e("ZOOM_CONFIG", "Error setting zoom for table image: " + e.getMessage());
                        // Fallback: just enable zoom without custom scales
                        try {
                            photoView.setZoomable(true);
                            Log.d("ZOOM_CONFIG", "Fallback: Basic zoom enabled for table image");
                        } catch (Exception fallbackError) {
                            Log.e("ZOOM_CONFIG", "Even basic zoom failed: " + fallbackError.getMessage());
                        }
                    }
                }
                
                Log.d("TABLE_OPTIMIZE", "Image: " + imageWidth + "x" + imageHeight + 
                      ", Optimal height: " + optimalHeight + "dp");
            }
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
    public void showIntroductionDialog() {
        visitStartTime = System.currentTimeMillis();
        // Inflate the binding layout manually
        LayoutInflater inflater = LayoutInflater.from(this);
        DialogIntroductionBinding binding = DialogIntroductionBinding.inflate(inflater);

        // Build the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setCancelable(true)
                .create();

        // Show the dialog
        dialog.show();
        binding.tvClose.setOnClickListener(v -> {
            updateTotalVisitingIntroduction(visitStartTime);
            dialog.dismiss();
        });

    }
    public void updateTotalVisitingIntroduction(long visitStartTimeMillis) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = firestore.collection("user").document(SessionManager.getId(this));

        long visitDuration = System.currentTimeMillis() - visitStartTimeMillis;

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);

            Long currentTotal = snapshot.getLong("totalVisitingIntroduction");
            if (currentTotal == null) currentTotal = 0L;
            Long newTotal = currentTotal + 1;

            Long totalDuration = snapshot.getLong("totalVisitDurationMillis");
            if (totalDuration == null) totalDuration = 0L;
            long newTotalDuration = totalDuration + visitDuration;

            // Convert durasi ke format readable
            String lastDurationFormatted = formatDuration(visitDuration);
            String totalDurationFormatted = formatDuration(newTotalDuration);

            // Update Firestore
            transaction.update(userDocRef, "totalVisitingIntroduction", newTotal);
            transaction.update(userDocRef, "totalVisitDurationMillis", newTotalDuration);
            transaction.update(userDocRef, "totalVisitDurationFormatted", totalDurationFormatted);

            // Simpan juga ke SessionManager
            SessionManager.setKeyVisitingInto(this, newTotal);
            SessionManager.setTotalVisitDuration(this, newTotalDuration);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("Firestore", "Berhasil update durasi dan format waktu");
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Gagal update durasi", e);
        });
    }
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d jam %d menit %d detik", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d menit %d detik", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d detik", seconds);
        }
    }

    /**
     * Upload photoAnswer to Firebase Storage for custom questions
     */
    private void uploadPhotoAnswerToStorage(DataModel dataModel, ProgressDialog progressDialog, String userId, String userName) {
        try {
            String photoAnswerBase64 = dataModel.getPhotoAnswer();
            if (photoAnswerBase64 == null || photoAnswerBase64.isEmpty()) {
                // No photoAnswer to upload, save directly
                saveDataModelToFirestore(dataModel, progressDialog, userId, userName);
                return;
            }
            
            // Convert base64 to bytes
            String base64Data = photoAnswerBase64;
            if (photoAnswerBase64.contains(",")) {
                base64Data = photoAnswerBase64.split(",")[1];
            }
            byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
            // Generate filename
            String fileName = "photo_answer_" + dataModel.getId() + "_" + System.currentTimeMillis() + ".jpg";
            
            // Upload to Firebase Storage with metadata (userId, source, userName)
            StorageUtil.uploadBytesWithUserIdAndMetadata(
                this,
                imageBytes,
                StorageUtil.STORAGE_PATH_DOCUMENTATION,
                fileName,
                "Buat Pertanyaan Sendiri", // Source metadata
                userName, // User name metadata
                (downloadUrl, storagePath) -> {
                    Log.d("PHOTO_ANSWER_UPLOAD", "Photo answer uploaded successfully: " + downloadUrl);
                    Log.d("PHOTO_ANSWER_UPLOAD", "Storage path: " + storagePath + ", Source: Buat Pertanyaan Sendiri");
                    
                    // Set photoAnswerUrl and photoAnswerPath
                    dataModel.setPhotoAnswerUrl(downloadUrl);
                    dataModel.setPhotoAnswerPath(storagePath);
                    dataModel.setPhotoAnswer(null); // Clear base64
                    
                    // Save to Firestore
                    saveDataModelToFirestore(dataModel, progressDialog, userId, userName);
                },
                e -> {
                    Log.e("PHOTO_ANSWER_UPLOAD", "Failed to upload photo answer: " + e.getMessage());
                    Toast.makeText(this, "Gagal mengunggah foto jawaban: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Save without photoAnswer if upload fails
                    dataModel.setPhotoAnswer(null);
                    saveDataModelToFirestore(dataModel, progressDialog, userId, userName);
                },
                null
            );
            
        } catch (Exception e) {
            Log.e("PHOTO_ANSWER_UPLOAD", "Error uploading photo answer: " + e.getMessage());
            Toast.makeText(this, "Error mengunggah foto jawaban: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Save without photoAnswer if error
            dataModel.setPhotoAnswer(null);
            saveDataModelToFirestore(dataModel, progressDialog, userId, userName);
        }
    }

    /**
     * Save DataModel to Firestore
     */
    private void saveDataModelToFirestore(DataModel dataModel, ProgressDialog progressDialog, String userId, String userName) {
        FirestoreUtil.addOrUpdateDocumentWithVersioning("questions", dataModel.getId(), dataModel,
                userId, userName,
                () -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    Intent intent = new Intent(UserGeneratesQuestionActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                },
                e -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(UserGeneratesQuestionActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    // Still navigate to home even if save fails
                    try {
                        Intent homeIntent = new Intent(UserGeneratesQuestionActivity.this, MainActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(homeIntent);
                        finish();
                    } catch (Exception ex) {
                        Log.e("SAVE_ERROR", "Error navigating to home after save failure: " + ex.getMessage());
                        finish();
                    }
                });
    }

}
