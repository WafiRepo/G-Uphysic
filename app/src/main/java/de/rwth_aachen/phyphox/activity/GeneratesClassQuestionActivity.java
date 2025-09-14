package de.rwth_aachen.phyphox.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import de.rwth_aachen.phyphox.R;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.AdvancedQuestionHelper;
import de.rwth_aachen.phyphox.Helper.EasyQuestionHelper;
import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.IntermediateQuestionHelper;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.NetworkConnection.ApiRequest;
import de.rwth_aachen.phyphox.NetworkConnection.ApiResponse;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.databinding.ActivityGeneratesBinding;
import de.rwth_aachen.phyphox.databinding.ActivityGeneratesNewBinding;
import de.rwth_aachen.phyphox.databinding.ActivityIntroductionClassBinding;
import de.rwth_aachen.phyphox.databinding.DialogIntroductionBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import de.rwth_aachen.phyphox.model.QuestionModel;
import retrofit2.Call;
import retrofit2.Callback;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;

public class GeneratesClassQuestionActivity extends AppCompatActivity {
    private long visitStartTime;
    ActivityGeneratesNewBinding binding;
    private DrawView drawView; // Inisialisasi untuk canvas menggambar
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Uri photoUri;
    private DataModel dataModel;
    private String currentPhotoPath;
    byte[] imageBytes = new byte[0];
    private LinearLayout colorPickerOverlay;
    private View currentColorIndicator;
    private boolean isColorPickerVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inisialisasi ViewBinding
        binding = ActivityGeneratesNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        App app = (App) getApplication();
        dataModel = app.getDataModel();

        // Initialize color picker
        colorPickerOverlay = binding.colorPickerOverlay;
        currentColorIndicator = binding.currentColorIndicator;
        setupColorPalette();

        boolean isFromMainMenu = getIntent().getBooleanExtra("isFromMainMenu", true);
        Log.d("getTypeQuestion ", "--> " + isFromMainMenu);
        Log.d("getTypeQuestion ", "--> " + dataModel.getTopics());
        Log.d("getTypeQuestion ", "--> " + dataModel.getTypeQuestion());
        Log.d("getTypeQuestion ", "--> " + dataModel.getTopics().equals("Out Class"));
        Log.d("getTypeQuestion ", "--> " + dataModel.getTypeQuestion().equals("Advanced"));
        dataModel.setDesc(SessionManager.getName(this) + " mengerjakan pertanyaan dari Sistem");
        if (isFromMainMenu) {
            fetchAdvancedQuestion("en", dataModel.getTypeQuestion());
        } else {
            Log.d("getTypeQuestion ", "---> " + dataModel.getBase64());
            binding.tvQuestion.setText(dataModel.getQuestion());
            binding.tvType.setText(dataModel.getTypeData());
            if(!dataModel.getPhotoDraw().isEmpty()){
                binding.ivDraw.setVisibility(View.VISIBLE);
                loadImage(dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size()-1), binding.ivDraw);
            }
            int totalEdit =dataModel.getTotalEdit()+1;
            dataModel.setTotalEdit(totalEdit);
            // Handle Graph Images
            if (dataModel.getTypeQuestion().equals("Advanced")) {
                try {
                    String base64Value = dataModel.getBase64();
                    Log.d("getTypeQuestion ", "---> " + base64Value);
                    // Only parse if purely numeric (represents index), otherwise skip and let edit-mode Base64 loader handle it
                    if (base64Value != null && base64Value.trim().matches("^\\d+$")) {
                        int questionIndex = Integer.parseInt(base64Value.trim());
                        List<Integer> images = AdvancedQuestionHelper.generateQuestionList().get(questionIndex).getImages();
                binding.iv.setImageDrawable(
                        ContextCompat.getDrawable(GeneratesClassQuestionActivity.this,
                                images.get(0)
                        ));
                if (images.size() > 1) {
                    binding.iv2.setImageDrawable(
                            ContextCompat.getDrawable(GeneratesClassQuestionActivity.this,
                                    images.get(1)
                            ));
                }
                    } else {
                        Log.d("getTypeQuestion ", "Non-numeric base64Value detected; using saved Base64 images for edit mode");
                    }
                } catch (Exception e) {
                    Log.e("GeneratesClassQuestionActivity", "Error loading Advanced question images: " + e.getMessage());
                }
            } else if (dataModel.getTypeQuestion().equals("Easy")) {
                try {
                    String base64Value = dataModel.getBase64();
                    if (base64Value != null && base64Value.trim().matches("^\\d+$")) {
                        int questionIndex = Integer.parseInt(base64Value.trim());
                        List<Integer> images = EasyQuestionHelper.generateQuestionList().get(questionIndex).getImages();
                        binding.iv.setImageDrawable(
                                ContextCompat.getDrawable(GeneratesClassQuestionActivity.this,
                                        images.get(0)
                                ));
                        if (images.size() > 1) {
                            binding.iv2.setImageDrawable(
                                    ContextCompat.getDrawable(GeneratesClassQuestionActivity.this,
                                            images.get(1)
                                    ));
                        }
                    } else {
                        Log.d("GeneratesClassQuestionActivity", "Easy: Non-numeric base64Value; skipping drawable load");
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    Log.e("GeneratesClassQuestionActivity", "Error loading Easy question images: " + e.getMessage());
                }
            } else {
                try {
                    String base64Value = dataModel.getBase64();
                    if (base64Value != null && base64Value.trim().matches("^\\d+$")) {
                        int questionIndex = Integer.parseInt(base64Value.trim());
                        List<Integer> images = IntermediateQuestionHelper.generateQuestionList().get(questionIndex).getImages();
                        binding.iv.setImageDrawable(
                                ContextCompat.getDrawable(GeneratesClassQuestionActivity.this,
                                        images.get(0)
                                ));
                        if (images.size() > 1) {
                            binding.iv2.setImageDrawable(
                                    ContextCompat.getDrawable(GeneratesClassQuestionActivity.this,
                                            images.get(1)
                                    ));
                        }
                    } else {
                        Log.d("GeneratesClassQuestionActivity", "Intermediate: Non-numeric base64Value; skipping drawable load");
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    Log.e("GeneratesClassQuestionActivity", "Error loading Intermediate question images: " + e.getMessage());
                }
            }
            binding.iv.setVisibility(View.VISIBLE);
            
            // Load saved Base64 images for edit mode (Class version)
            // These take priority over drawable resources when editing existing questions
            if (!dataModel.getBase64().isEmpty()) {
                Log.d("EDIT_IMAGE_CLASS", "Loading saved Base64 (iv) - Length: " + dataModel.getBase64().length());
                loadImage(dataModel.getBase64(), binding.iv);
            binding.iv.setVisibility(View.VISIBLE);
            }
            if (!dataModel.getBase64_2().isEmpty()) {
                Log.d("EDIT_IMAGE_CLASS", "Loading saved Base64_2 (iv2) - Length: " + dataModel.getBase64_2().length());
                loadImage(dataModel.getBase64_2(), binding.iv2);
                binding.iv2.setVisibility(View.VISIBLE);
            }
            
            // Note: GeneratesClassQuestionActivity uses activity_generates_new.xml
            // which only has iv and iv2 (no iv3, iv4, iv5, tableImagesContainer)
            // Debug logging for available data
            if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
                Log.d("EDIT_IMAGE_CLASS", "Base64_3 data available but no iv3 in layout - Length: " + dataModel.getBase64_3().length());
            }
            if (dataModel.getBase64_4() != null && !dataModel.getBase64_4().isEmpty()) {
                Log.d("EDIT_IMAGE_CLASS", "Base64_4 data available but no iv4 in layout - Length: " + dataModel.getBase64_4().length());
            }
            if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                Log.d("EDIT_IMAGE_CLASS", "Base64_5 data available but no iv5 in layout - Length: " + dataModel.getBase64_5().length());
            }
            Log.d("EDIT_IMAGE_CLASS", "GeneratesClassQuestionActivity layout only supports iv and iv2 (graph images)");
            
            // Load table images if available (iv3 and iv4)
            boolean hasTableImages = false;
            if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
                Log.d("EDIT_IMAGE_CLASS", "Loading Base64_3 (iv3) - Length: " + dataModel.getBase64_3().length());
                loadImage(dataModel.getBase64_3(), binding.iv3);
                binding.iv3.setVisibility(View.VISIBLE);
                hasTableImages = true;
            }
            if (dataModel.getBase64_4() != null && !dataModel.getBase64_4().isEmpty()) {
                Log.d("EDIT_IMAGE_CLASS", "Loading Base64_4 (iv4) - Length: " + dataModel.getBase64_4().length());
                loadImage(dataModel.getBase64_4(), binding.iv4);
                binding.iv4.setVisibility(View.VISIBLE);
                hasTableImages = true;
            }
            
            // Show/hide table images container
            if (hasTableImages) {
                Log.d("EDIT_IMAGE_CLASS", "Showing table images container for Edit mode");
                binding.tableImagesContainer.setVisibility(View.VISIBLE);
                
                // Log summary of all loaded images for Edit mode
                Log.d("EDIT_IMAGE_CLASS", "=== EDIT MODE IMAGE LOADING SUMMARY ===");
                Log.d("EDIT_IMAGE_CLASS", "iv (base64): " + (dataModel.getBase64() != null ? dataModel.getBase64().length() : "null") + " chars");
                Log.d("EDIT_IMAGE_CLASS", "iv2 (base64_2): " + (dataModel.getBase64_2() != null ? dataModel.getBase64_2().length() : "null") + " chars");
                Log.d("EDIT_IMAGE_CLASS", "iv3 (base64_3): " + (dataModel.getBase64_3() != null ? dataModel.getBase64_3().length() : "null") + " chars");
                Log.d("EDIT_IMAGE_CLASS", "iv4 (base64_4): " + (dataModel.getBase64_4() != null ? dataModel.getBase64_4().length() : "null") + " chars");
                Log.d("EDIT_IMAGE_CLASS", "===============================");
            } else {
                Log.d("EDIT_IMAGE_CLASS", "No table images found for Edit mode - hiding container");
                binding.tableImagesContainer.setVisibility(View.GONE);
            }
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
                Log.d("btnTakePhoto ", "permission failed");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                Log.d("btnTakePhoto ", "takePictureIntent");
                dispatchTakePictureIntent();
            }
        });
        // Tombol Submit (fungsionalitas belum diimplementasikan)
        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.btnSave.setEnabled(false);
                
                try {
                    // Always upload canvas drawing (for Preview Jawaban section)
                    byte[] canvasData = convertBitmapToBytes(getViewAsBitmap(binding.llDraw));
                    
                    Log.d("CANVAS_UPLOAD_CLASS", "Uploading CANVAS ONLY (size: " + canvasData.length + " bytes) - Documentation photo stays as Base64");
                    
                    // Validate canvas size before upload
                    if (canvasData.length > 800000) { // ~800KB limit
                        Toast.makeText(GeneratesClassQuestionActivity.this, 
                            "Gambar canvas terlalu besar. Silakan coba dengan gambar yang lebih sederhana.", 
                            Toast.LENGTH_LONG).show();
                        binding.btnSave.setEnabled(true);
                        return;
                    }
                    
                    // Upload canvas drawing (this goes to photoDraw for Preview Jawaban)
                    uploadImageToFirestore(canvasData, "canvas_answer_" + System.currentTimeMillis());
                } catch (Exception e) {
                    Log.e("UPLOAD_ERROR", "Error preparing image for upload: " + e.getMessage());
                    Toast.makeText(GeneratesClassQuestionActivity.this, 
                        "Terjadi kesalahan saat mempersiapkan gambar. Silakan coba lagi.", 
                        Toast.LENGTH_LONG).show();
                    binding.btnSave.setEnabled(true);
                }
            }
        });

        // Tombol Next yang mengarahkan ke FeedbackActivity
        binding.btnSave.setOnClickListener(v -> {
            // Check if data has been uploaded first
            if (dataModel.getPhotoDraw() == null || dataModel.getPhotoDraw().isEmpty()) {
                Toast.makeText(GeneratesClassQuestionActivity.this, "Klik Upload terlebih dahulu untuk menyimpan gambar", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Validate all base64 data before proceeding
            try {
                validateBase64Data();
                
                // If validation passes, proceed to next activity
                Intent intent = new Intent(GeneratesClassQuestionActivity.this, RecordPreviewActivity.class);
                startActivity(intent);
                finish();
                
            } catch (Exception e) {
                Log.e("NEXT_ERROR", "Error validating data: " + e.getMessage());
                Toast.makeText(GeneratesClassQuestionActivity.this, 
                    "Terjadi kesalahan saat memvalidasi data. Silakan coba lagi.", 
                    Toast.LENGTH_LONG).show();
            }
        });
        binding.btnType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.tvType.getVisibility() == View.VISIBLE) {
                    binding.tvType.setVisibility(View.GONE);
                } else {
                    binding.tvType.setVisibility(View.VISIBLE);
                }
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                if (getIntent().getBooleanExtra("isFromMainMenu", true)) {
                        // Simpan progress tanpa ProgressDialog untuk performa lebih baik
                    dataModel.setTypeData(binding.tvType.getText().toString());
                    app.setDataModel(dataModel);
                        
                    FirestoreUtil.addOrUpdateDocument("record", dataModel.getId(), dataModel,
                            () -> {
                                    // Setelah simpan, langsung ke homepage
                                    try {
                                Intent homeIntent = new Intent(GeneratesClassQuestionActivity.this, MainActivity.class);
                                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(homeIntent);
                                        finish();
                                    } catch (Exception ex) {
                                        Log.e("BACK_BUTTON", "Error navigating to home: " + ex.getMessage());
                                        finish();
                                    }
                            },
                            e -> {
                                    // Jika gagal simpan, tetap kembali ke homepage
                                    Log.e("BACK_BUTTON", "Failed to save progress: " + e.getMessage());
                                    try {
                                        Intent homeIntent = new Intent(GeneratesClassQuestionActivity.this, MainActivity.class);
                                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(homeIntent);
                                        finish();
                                    } catch (Exception ex) {
                                        Log.e("BACK_BUTTON", "Error navigating to home after save failure: " + ex.getMessage());
                                        finish();
                                    }
                                }
                        );
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
//        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
//            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
//            Log.d("btnTakePhoto", "NO camera app available");
//            return;
//        }

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
        Log.d("onactivityresutl ", "--> " + resultCode);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d("onactivityresutl ", "--> ");
            binding.ivPhoto.setVisibility(View.VISIBLE);
            
            try {
            // Optional: Konversi foto ke base64 dan simpan ke dataModel
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                if (bitmap != null) {
                    // Optimize bitmap before processing
                    Bitmap optimizedBitmap = optimizeBitmapForStorage(bitmap);
                    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            imageBytes = baos.toByteArray();
                    
                    // Check if the optimized image is still too large
                    if (imageBytes.length > 800000) {
                        Toast.makeText(this, 
                            "Gambar terlalu besar setelah optimisasi. Silakan coba dengan gambar yang lebih kecil.", 
                            Toast.LENGTH_LONG).show();
                        return;
                    }
                    
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            loadImageWithGlide(base64ToDrawable(base64Image, GeneratesClassQuestionActivity.this), binding.ivPhoto);

                    // IMPORTANT: Documentation photo is saved as Base64 only (NOT uploaded to Firebase)
            dataModel.setPhotoAnswer(base64Image);
                    
                    Log.d("DOC_PHOTO_CLASS", "Documentation photo saved as Base64 (length: " + base64Image.length() + ") - NOT uploaded to Firebase");
                    
                    // Clean up if we created a new bitmap
                    if (optimizedBitmap != bitmap) {
                        optimizedBitmap.recycle();
                    }
                } else {
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("ImageProcess", "Error processing image: " + e.getMessage());
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // REMOVED: uploadImageToFirestoreAnswer method
    // Documentation photos should NOT be uploaded to Firebase Storage in GeneratesClassQuestionActivity
    // They are saved as Base64 in dataModel.setPhotoAnswer() for local storage only
    // Canvas drawings are uploaded to Firebase and saved in dataModel.setPhotoDraw()


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

    public byte[] convertBitmapToBytes(Bitmap bitmap) {
        // Optimize bitmap before converting to bytes
        Bitmap optimizedBitmap = optimizeBitmapForStorage(bitmap);
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream); // Use 85% quality for good balance
        
        // Clean up if we created a new bitmap
        if (optimizedBitmap != bitmap) {
            optimizedBitmap.recycle();
        }
        
        return byteArrayOutputStream.toByteArray();
    }
    
    /**
     * Optimize bitmap for storage by resizing if too large
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
     * Compress base64 string to fit Firestore's 1MB limit with improved compression
     * @param base64String The original base64 string
     * @return Compressed or optimized base64 string
     */
    private String compressBase64ForFirestore(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return base64String;
        }
        
        // Firestore limit is 1048487 bytes (~1MB)
        final int FIRESTORE_LIMIT = 900000; // Leave more margin for safety
        
        if (base64String.length() > FIRESTORE_LIMIT) {
            Log.w("FIRESTORE_COMPRESS", "Base64 string too large (" + base64String.length() + " bytes), compressing...");
            
            try {
                // Extract base64 data without prefix
                String base64Data = base64String;
                if (base64String.contains(",")) {
                    base64Data = base64String.split(",")[1];
                }
                
                byte[] decodedBytes = Base64.decode(base64Data, Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                
                if (bitmap != null) {
                    // Calculate target size (in bytes, not base64 length)
                    int targetSizeBytes = FIRESTORE_LIMIT * 3 / 4; // Base64 is ~33% larger than binary
                    
                    // Resize bitmap if it's too large
                    int maxDimension = 1024; // Maximum dimension
                    if (bitmap.getWidth() > maxDimension || bitmap.getHeight() > maxDimension) {
                        float scale = Math.min((float) maxDimension / bitmap.getWidth(), 
                                             (float) maxDimension / bitmap.getHeight());
                        int newWidth = Math.round(bitmap.getWidth() * scale);
                        int newHeight = Math.round(bitmap.getHeight() * scale);
                        
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                        bitmap.recycle(); // Free memory
                        bitmap = resizedBitmap;
                        Log.d("FIRESTORE_COMPRESS", "Resized bitmap to " + newWidth + "x" + newHeight);
                    }
                    
                    // Compress with adaptive quality
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    int quality = 80; // Start with good quality
                    int minQuality = 10; // Minimum quality
                    
                    do {
                        outputStream.reset();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                        
                        if (outputStream.size() <= targetSizeBytes) {
                            break; // Success
                        }
                        
                        quality -= 10; // Reduce quality more aggressively
                        
                        // If quality is too low, try resizing further
                        if (quality < minQuality && bitmap.getWidth() > 512) {
                            float scale = 0.8f; // Reduce size by 20%
                            int newWidth = Math.round(bitmap.getWidth() * scale);
                            int newHeight = Math.round(bitmap.getHeight() * scale);
                            
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                            bitmap.recycle();
                            bitmap = resizedBitmap;
                            quality = 80; // Reset quality
                            Log.d("FIRESTORE_COMPRESS", "Further resized to " + newWidth + "x" + newHeight);
                        }
                        
                    } while (quality >= minQuality && bitmap.getWidth() > 256);
                    
                    // If still too large, use RGB format (remove alpha channel)
                    if (outputStream.size() > targetSizeBytes) {
                        outputStream.reset();
                        // Convert to RGB (remove alpha channel) and compress as JPEG
                        Bitmap rgbBitmap = bitmap.copy(Bitmap.Config.RGB_565, false);
                        rgbBitmap.compress(Bitmap.CompressFormat.JPEG, minQuality, outputStream);
                        rgbBitmap.recycle();
                        Log.d("FIRESTORE_COMPRESS", "Converted to RGB and compressed with minimum quality");
                    }
                    
                    String compressedBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
                    Log.d("FIRESTORE_COMPRESS", "Compressed from " + base64String.length() + " to " + compressedBase64.length() + " bytes (quality: " + quality + ")");
                    
                    // Clean up
                    bitmap.recycle();
                    
                    return compressedBase64;
                }
            } catch (Exception e) {
                Log.e("FIRESTORE_COMPRESS", "Error compressing image: " + e.getMessage());
            }
            
            // If compression fails, try to truncate intelligently
            Log.w("FIRESTORE_COMPRESS", "Compression failed, truncating...");
            return base64String.substring(0, FIRESTORE_LIMIT);
        }
        
        return base64String;
    }
    
    /**
     * Validate all base64 data to ensure they fit within Firestore limits
     */
    private void validateBase64Data() throws Exception {
        final int FIRESTORE_LIMIT = 900000; // Same limit as compression function
        
        // Check base64 fields
        String[] base64Fields = {
            dataModel.getBase64(),
            dataModel.getBase64_2(),
            dataModel.getBase64_3(),
            dataModel.getBase64_4(),
            dataModel.getBase64_5()
        };
        
        for (int i = 0; i < base64Fields.length; i++) {
            String base64Data = base64Fields[i];
            if (base64Data != null && base64Data.length() > FIRESTORE_LIMIT) {
                Log.w("BASE64_VALIDATION", "Base64 field " + (i + 1) + " is too large: " + base64Data.length() + " bytes");
                throw new Exception("Gambar " + (i + 1) + " terlalu besar. Silakan coba dengan gambar yang lebih kecil.");
            }
        }
        
        Log.d("BASE64_VALIDATION", "All base64 data validated successfully");
    }

    public void uploadImageToFirestore(byte[] imageData, String fileName) {
        // Validate image size before upload
        if (imageData.length > 800000) { // ~800KB limit
            Toast.makeText(this, 
                "Gambar terlalu besar. Silakan coba dengan gambar yang lebih kecil atau gunakan fitur gambar yang lebih sederhana.", 
                Toast.LENGTH_LONG).show();
            binding.btnSave.setEnabled(true);
            return;
        }
        
        // Create and configure ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this); // Replace 'this' with 'requireContext()' if inside a Fragment
        progressDialog.setTitle("Uploading Image");
        progressDialog.setMessage("Please wait while the image is being uploaded...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get Firebase Storage instance
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to the image
        StorageReference imageRef = storageRef.child("images/" + fileName);

        // Upload the image
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask
                .addOnProgressListener(snapshot -> {
                    // Update the ProgressDialog with the upload progress
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploaded: " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Save the download URL to DataModel
                        App app = (App) getApplication();
                        DataModel dataModel = app.getDataModel();
                        ArrayList<String> photoDraws= dataModel.getPhotoDraw();
                        photoDraws.add(downloadUrl);
                        dataModel.setPhotoDraw(photoDraws);
                        app.setDataModel(dataModel);

                        // Enable the save button
                        binding.btnSave.setEnabled(true);
                        // Log success
                        Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);

                        // Save data to Firestore after successful upload
                        ProgressDialog saveProgressDialog = new ProgressDialog(GeneratesClassQuestionActivity.this);
                        saveProgressDialog.setTitle("Save data to Server");
                        saveProgressDialog.setMessage("Please wait...");
                        saveProgressDialog.setCancelable(false);
                        saveProgressDialog.show();

                        String documentId = dataModel.getId();
                        if (documentId == null || documentId.isEmpty()) {
                            documentId = String.valueOf(System.currentTimeMillis());
                        }
                        dataModel.setId(documentId);
                        dataModel.setTypeData(binding.tvType.getText().toString());
                        app.setDataModel(dataModel);
                        
                        FirestoreUtil.addOrUpdateDocument("record", documentId, dataModel,
                                () -> {
                                    saveProgressDialog.dismiss();
                                    progressDialog.dismiss();
                                    // Navigate to RecordPreviewActivity after successful save
                                    Intent intent = new Intent(GeneratesClassQuestionActivity.this, RecordPreviewActivity.class);
                                    startActivity(intent);
                                    finish();
                                },
                                e -> {
                                    saveProgressDialog.dismiss();
                                    progressDialog.dismiss();
                                    
                                    // Handle specific base64 size limit error
                                    String errorMessage = e.getMessage();
                                    if (errorMessage != null && errorMessage.contains("1MB")) {
                                        Toast.makeText(GeneratesClassQuestionActivity.this, 
                                            "Gambar terlalu besar. Silakan coba lagi dengan gambar yang lebih kecil atau gunakan fitur gambar yang lebih sederhana.", 
                                            Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(GeneratesClassQuestionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                    }
                                    
                                    binding.btnSave.setEnabled(true); // Re-enable button if failed
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle upload failure
                    Log.e("Firebase", "Image upload failed", e);

                    // Dismiss the progress dialog
                    progressDialog.dismiss();

                    // Notify the user of the error
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchAdvancedQuestion(String language, String type) {
        Pair<Integer, QuestionModel> result;
        if (Objects.equals(type, "Easy")) {
            result = EasyQuestionHelper.getRandomQuestion();
        } else if (Objects.equals(type, "Intermediate")) {
            result = IntermediateQuestionHelper.getRandomQuestion();
        } else {
            result = AdvancedQuestionHelper.getRandomQuestion();
        }
        App app = (App) getApplication();
        DataModel dataModel = app.getDataModel();
        dataModel.setQuestion(result.second.getQuestion());
        dataModel.setTypeQuestion(result.second.getType());
        dataModel.setIdCustomer(SessionManager.getId(GeneratesClassQuestionActivity.this));
        binding.tvQuestion.setText(result.second.getQuestion());
        // Compress base64 data if it exists
        String base64Data = String.valueOf(result.first);
        String compressedBase64 = compressBase64ForFirestore(base64Data);
        dataModel.setBase64(compressedBase64);
        
        // Handle Graph Images
        binding.iv.setImageDrawable(ContextCompat.getDrawable(GeneratesClassQuestionActivity.this, result.second.getImages().get(0)));
        binding.iv.setVisibility(View.VISIBLE);
        
        if (result.second.getImages().size() > 1) {
            String compressedBase64_2 = compressBase64ForFirestore(base64Data);
            dataModel.setBase64_2(compressedBase64_2);
            binding.iv2.setImageDrawable(ContextCompat.getDrawable(GeneratesClassQuestionActivity.this, result.second.getImages().get(1)));
            binding.iv2.setVisibility(View.VISIBLE);
        }
        app.setDataModel(dataModel);
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
                        imageView.setImageDrawable(resource); // Use setImageDrawable for PhotoView compatibility
                        
                        // Note: No table images (iv3, iv4) in GeneratesClassQuestionActivity layout
                        
                        // Enhanced zoom for PhotoView images (excluding table images iv3, iv4)
                        if (imageView instanceof com.github.chrisbanes.photoview.PhotoView) {
                            com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) imageView;
                            
                            try {
                                // CRITICAL: Reset PhotoView state first to clear any existing zoom conflicts
                                photoView.setScale(1.0f, true);
                                
                                // Configure zoom based on image type - set maximum first
                                if (imageView.getId() == R.id.ivDraw) {
                                    // Drawing image - high zoom for detail
                                    photoView.setMaximumScale(6.0f);
                                    photoView.setMediumScale(3.0f);
                                    photoView.setMinimumScale(0.5f);
                                } else {
                                    // Default zoom for graph images (iv, iv2)
                                    photoView.setMaximumScale(3.0f);
                                    photoView.setMediumScale(2.0f);
                                    photoView.setMinimumScale(0.8f);
                                }
                                // Note: GeneratesClassQuestionActivity only has iv, iv2, ivDraw
                                
                                // Enable smooth zoom transitions
                                photoView.setZoomTransitionDuration(300);
                                photoView.setZoomable(true);
                                
                                Log.d("ZOOM_CONFIG_CLASS", "PhotoView zoom configured for " + getImageViewName(imageView));
                                      
                            } catch (Exception e) {
                                Log.e("ZOOM_CONFIG_CLASS", "Error setting zoom levels for " + getImageViewName(imageView) + ": " + e.getMessage());
                                // Fallback to basic zoom
                                try {
                                    photoView.setZoomable(true);
                                } catch (Exception fallbackError) {
                                    Log.e("ZOOM_CONFIG_CLASS", "Even basic zoom failed: " + fallbackError.getMessage());
                                }
                            }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle case when the image is cleared
                    }
                });
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
    private void loadImage(String url, ImageView imageView) {
        String imageViewName = getImageViewName(imageView);
        Log.d("LOAD_IMAGE_CLASS", "Loading image for " + imageViewName + " - URL length: " + (url != null ? url.length() : 0));
        
        if (url.contains("http")) {
            Log.d("LOAD_IMAGE_CLASS", "Loading from URL for " + imageViewName);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                String base64 = imageUrlToBase64(url);

                handler.post(() -> {
                    if (!base64.isEmpty()) {
                        Log.d("LOAD_IMAGE_CLASS", "URL converted to Base64 for " + imageViewName + " - Length: " + base64.length());
                        loadImageWithGlide(base64ToDrawable(base64, this), imageView);
                    } else {
                        Log.e("LOAD_IMAGE_CLASS", "Failed to convert URL to base64 for " + imageViewName);
                    }
                });
            });

        } else {
            Log.d("LOAD_IMAGE_CLASS", "Loading from Base64 for " + imageViewName + " - Length: " + url.length());
            BitmapDrawable drawable = base64ToDrawable(url, GeneratesClassQuestionActivity.this);
            if (drawable != null) {
                Log.d("LOAD_IMAGE_CLASS", "Base64 converted to drawable successfully for " + imageViewName);
                loadImageWithGlide(drawable, imageView);
            } else {
                Log.e("LOAD_IMAGE_CLASS", "Failed to convert Base64 to drawable for " + imageViewName);
            }
        }
    }
    
    private String getImageViewName(ImageView imageView) {
        if (imageView.getId() == R.id.iv) return "iv (Graph 1)";
        if (imageView.getId() == R.id.iv2) return "iv2 (Graph 2)";
        if (imageView.getId() == R.id.ivDraw) return "ivDraw (Canvas)";
        if (imageView.getId() == R.id.iv3) return "iv3 (Table 1)";
        if (imageView.getId() == R.id.iv4) return "iv4 (Table 2)";
        // Note: GeneratesClassQuestionActivity layout doesn't have iv3, iv4, iv5
        return "Unknown ImageView";
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
    public static String imageUrlToBase64(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // or JPEG
            byte[] byteArray = outputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void setupColorPalette() {
        // Setup color picker button
        FloatingActionButton btnColorPicker = findViewById(R.id.btn_color_picker);
        btnColorPicker.setOnClickListener(v -> toggleColorPicker());

        // Setup color buttons
        setupColorButtons();

        // Set initial color
        setCurrentColor("#000000");
    }

    private void toggleColorPicker() {
        isColorPickerVisible = !isColorPickerVisible;
        colorPickerOverlay.setVisibility(isColorPickerVisible ? View.VISIBLE : View.GONE);
    }

    private void setupColorButtons() {
        // Row 1
        setupColorButton(R.id.color_black, "#000000");
        setupColorButton(R.id.color_red, "#FF0000");
        setupColorButton(R.id.color_blue, "#0000FF");
        setupColorButton(R.id.color_green, "#00FF00");

        // Row 2
        setupColorButton(R.id.color_orange, "#FF9800");
        setupColorButton(R.id.color_purple, "#9C27B0");
        setupColorButton(R.id.color_pink, "#E91E63");
        setupColorButton(R.id.color_brown, "#795548");
    }

    private void setupColorButton(int viewId, String colorHex) {
        View colorButton = findViewById(viewId);
        colorButton.setOnClickListener(v -> {
            setCurrentColor(colorHex);
            toggleColorPicker();
        });
    }

    private void setCurrentColor(String colorHex) {
        try {
            int color = Color.parseColor(colorHex);
            
            // Update current color indicator
            GradientDrawable indicator = new GradientDrawable();
            indicator.setShape(GradientDrawable.OVAL);
            indicator.setColor(color);
            indicator.setStroke(4, Color.WHITE);
            currentColorIndicator.setBackground(indicator);
            
            // Update drawing view color
            if (drawView != null) {
                drawView.setColor(color);
            }
            
            Toast.makeText(this, "Warna dipilih: " + colorHex, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error memilih warna", Toast.LENGTH_SHORT).show();
        }
    }
}
