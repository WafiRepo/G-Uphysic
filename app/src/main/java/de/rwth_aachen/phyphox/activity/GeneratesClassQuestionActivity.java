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
import de.rwth_aachen.phyphox.Helper.StorageUtil;
import de.rwth_aachen.phyphox.Helper.VersionHelper;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
            
            // Disable button to prevent multiple clicks
            binding.btnSave.setEnabled(false);

            ProgressDialog preparingDialog = new ProgressDialog(GeneratesClassQuestionActivity.this);
            preparingDialog.setTitle("Mempersiapkan Preview");
            preparingDialog.setMessage("Mengunggah gambar soal...");
            preparingDialog.setCancelable(false);
            preparingDialog.show();

            ensureQuestionImagesUploadedToStorage(
                    () -> {
                        try {
                            validateBase64Data();
                            preparingDialog.dismiss();
                            Intent intent = new Intent(GeneratesClassQuestionActivity.this, RecordPreviewActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            preparingDialog.dismiss();
                            Log.e("NEXT_ERROR", "Error validating data: " + e.getMessage());
                            binding.btnSave.setEnabled(true);
                            Toast.makeText(GeneratesClassQuestionActivity.this,
                                    "Terjadi kesalahan saat memvalidasi data. Silakan coba lagi.",
                                    Toast.LENGTH_LONG).show();
                        }
                    },
                    errMsg -> {
                        preparingDialog.dismiss();
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(GeneratesClassQuestionActivity.this,
                                "Gagal mengunggah gambar soal: " + errMsg,
                                Toast.LENGTH_LONG).show();
                    }
            );
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
                    
                    String userId = SessionManager.getId(GeneratesClassQuestionActivity.this);
                    String userName = SessionManager.getName(GeneratesClassQuestionActivity.this);
                        
                    FirestoreUtil.addOrUpdateDocumentWithVersioning("record", dataModel.getId(), dataModel,
                            userId, userName,
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

                    // Upload photoAnswer to Firebase Storage with metadata
                    String userId = SessionManager.getId(this);
                    String userName = SessionManager.getName(this);
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String fileName = userId + "_photo_answer_" + System.currentTimeMillis() + "_" + timestamp + ".jpg";
                    
                    StorageUtil.uploadBytesWithUserIdAndMetadata(
                            this,
                            imageBytes,
                            StorageUtil.STORAGE_PATH_DOCUMENTATION,
                            fileName,
                            "Buat Pertanyaan dengan AI", // Source metadata (GeneratesClassQuestionActivity is for AI questions)
                            userName, // User name metadata
                            (downloadUrl, fullPath) -> {
                                Log.d("DOC_PHOTO_CLASS", "Photo answer uploaded to Storage: " + downloadUrl);
                                Log.d("DOC_PHOTO_CLASS", "Storage path: " + fullPath + ", Source: Buat Pertanyaan dengan AI");
                                dataModel.setPhotoAnswerUrl(downloadUrl);
                                dataModel.setPhotoAnswerPath(fullPath);
                                // DO NOT set photoAnswer (Base64) anymore
                                App app = (App) getApplication();
                                app.setDataModel(dataModel);
                            },
                            e -> {
                                Log.e("DOC_PHOTO_CLASS", "Failed to upload photo answer: " + e.getMessage());
                                Toast.makeText(this, "Gagal mengunggah foto dokumentasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            },
                            null
                    );
                    
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
            
            // If compression fails, DO NOT truncate base64 (it will become invalid and cannot be decoded).
            // Instead, return empty so the app can handle it (or force upload to Storage in the future).
            Log.e("FIRESTORE_COMPRESS", "Compression failed; returning empty to avoid invalid Base64");
            return "";
        }
        
        return base64String;
    }
    
    /**
     * Validate all base64 data to ensure they fit within Firestore limits
     */
    /**
     * Convert ImageView to Base64 and save to DataModel
     * This ensures graph and table images are saved before navigating to RecordPreviewActivity
     * Always converts ImageView drawables to Base64 if ImageView is visible and has drawable
     */
    private void saveImageViewToDataModel() {
        App app = (App) getApplication();
        DataModel dataModel = app.getDataModel();
        
        try {
            // Save graph images (iv and iv2) - always convert if ImageView has drawable and is visible
            if (binding.iv.getVisibility() == View.VISIBLE && binding.iv.getDrawable() != null) {
                // Always convert ImageView to Base64 to ensure latest image is saved
                // Check if current base64 is just an index (numeric string) or empty/invalid
                String currentBase64 = dataModel.getBase64();
                boolean needsConversion = (currentBase64 == null || currentBase64.isEmpty() || 
                    currentBase64.trim().matches("^\\d+$") || currentBase64.length() < 100);
                
                // Legacy: No longer save to base64* fields - images are uploaded to Storage via uploadQuestionImagesToStorage()
                // This function is kept for backward compatibility but doesn't set legacy fields
                Log.d("SAVE_IMAGE_CLASS", "Graph 1 (iv) - Image will be uploaded to Storage via uploadQuestionImagesToStorage()");
            } else {
                Log.d("SAVE_IMAGE_CLASS", "Graph 1 (iv) is not visible or has no drawable");
            }
            
            if (binding.iv2.getVisibility() == View.VISIBLE && binding.iv2.getDrawable() != null) {
                String currentBase64_2 = dataModel.getBase64_2();
                boolean needsConversion = (currentBase64_2 == null || currentBase64_2.isEmpty() || 
                    currentBase64_2.trim().matches("^\\d+$") || currentBase64_2.length() < 100);
                
                // Legacy: No longer save to base64* fields - images are uploaded to Storage via uploadQuestionImagesToStorage()
                // This function is kept for backward compatibility but doesn't set legacy fields
                Log.d("SAVE_IMAGE_CLASS", "Graph 2 (iv2) - Image will be uploaded to Storage via uploadQuestionImagesToStorage()");
            } else {
                Log.d("SAVE_IMAGE_CLASS", "Graph 2 (iv2) is not visible or has no drawable");
            }
            
            // Save table images (iv3 and iv4) if they exist in layout
            try {
                if (binding.iv3 != null && binding.iv3.getVisibility() == View.VISIBLE && binding.iv3.getDrawable() != null) {
                    String currentBase64_3 = dataModel.getBase64_3();
                    boolean shouldSave = (currentBase64_3 == null || currentBase64_3.isEmpty() || currentBase64_3.length() < 100);
                    
                    // Legacy: No longer save to base64* fields - images are uploaded to Storage via uploadQuestionImagesToStorage()
                    // This function is kept for backward compatibility but doesn't set legacy fields
                    Log.d("SAVE_IMAGE_CLASS", "Table 1 (iv3) - Image will be uploaded to Storage via uploadQuestionImagesToStorage()");
                }
            } catch (Exception e) {
                Log.w("SAVE_IMAGE_CLASS", "iv3 not available in layout: " + e.getMessage());
            }
            
            try {
                if (binding.iv4 != null && binding.iv4.getVisibility() == View.VISIBLE && binding.iv4.getDrawable() != null) {
                    String currentBase64_4 = dataModel.getBase64_4();
                    boolean shouldSave = (currentBase64_4 == null || currentBase64_4.isEmpty() || currentBase64_4.length() < 100);
                    
                    // Legacy: No longer save to base64* fields - images are uploaded to Storage via uploadQuestionImagesToStorage()
                    // This function is kept for backward compatibility but doesn't set legacy fields
                    Log.d("SAVE_IMAGE_CLASS", "Table 2 (iv4) - Image will be uploaded to Storage via uploadQuestionImagesToStorage()");
                }
            } catch (Exception e) {
                Log.w("SAVE_IMAGE_CLASS", "iv4 not available in layout: " + e.getMessage());
            }
            
            // Update DataModel in App singleton
            app.setDataModel(dataModel);
            Log.d("SAVE_IMAGE_CLASS", "All images saved to DataModel successfully");
            
        } catch (Exception e) {
            Log.e("SAVE_IMAGE_CLASS", "Error saving images to DataModel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Convert ImageView drawable to Base64 string
     * Handles all drawable types including resource drawables
     */
    private String imageViewToBase64(ImageView imageView) {
        try {
            Drawable drawable = imageView.getDrawable();
            if (drawable == null) {
                Log.w("IMAGE_TO_BASE64", "ImageView drawable is null for " + getImageViewName(imageView));
                return null;
            }
            
            Bitmap bitmap = null;
            
            // Handle different drawable types
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
                if (bitmap != null) {
                    Log.d("IMAGE_TO_BASE64", "Got bitmap from BitmapDrawable - Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                }
            } else {
                // Convert other drawable types to bitmap
                int width = drawable.getIntrinsicWidth();
                int height = drawable.getIntrinsicHeight();
                
                Log.d("IMAGE_TO_BASE64", "Drawable intrinsic size: " + width + "x" + height);
                
                if (width <= 0 || height <= 0) {
                    // Use ImageView dimensions if drawable doesn't have intrinsic dimensions
                    width = imageView.getWidth();
                    height = imageView.getHeight();
                    Log.d("IMAGE_TO_BASE64", "Using ImageView size: " + width + "x" + height);
                    
                    // If ImageView hasn't been measured yet, use a default size
                    if (width <= 0 || height <= 0) {
                        // Force measure the ImageView
                        imageView.measure(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        );
                        width = imageView.getMeasuredWidth();
                        height = imageView.getMeasuredHeight();
                        Log.d("IMAGE_TO_BASE64", "After measure: " + width + "x" + height);
                        
                        // If still invalid, use reasonable defaults
                        if (width <= 0 || height <= 0) {
                            width = 800; // Default width
                            height = 600; // Default height
                            Log.d("IMAGE_TO_BASE64", "Using default size: " + width + "x" + height);
                        }
                    }
                }
                
                if (width > 0 && height > 0) {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                    drawable.setBounds(0, 0, width, height);
                    drawable.draw(canvas);
                    Log.d("IMAGE_TO_BASE64", "Created bitmap from drawable - Size: " + width + "x" + height);
                } else {
                    Log.e("IMAGE_TO_BASE64", "Cannot determine bitmap dimensions for " + getImageViewName(imageView));
                    return null;
                }
            }
            
            if (bitmap == null) {
                Log.e("IMAGE_TO_BASE64", "Failed to get bitmap from drawable for " + getImageViewName(imageView));
                return null;
            }
            
            // Compress bitmap to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Convert to Base64
            String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            
            Log.d("IMAGE_TO_BASE64", "Image converted to Base64 for " + getImageViewName(imageView) + " - Size: " + base64.length() + " chars");
            return base64;
            
        } catch (Exception e) {
            Log.e("IMAGE_TO_BASE64", "Error converting ImageView to Base64 for " + getImageViewName(imageView) + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

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

    private interface SuccessCallback { void onSuccess(); }
    private interface ErrorCallback { void onError(String msg); }
    private interface UrlCallback { void onUrl(String url); }

    /**
     * Upload question images (iv, iv2, iv3, iv4) to Firebase Storage and store download URLs
     * into DataModel fields: base64, base64_2, base64_3, base64_4.
     *
     * Ini menghindari Base64 besar di Firestore dan Preview cukup load via URL.
     */
    private void ensureQuestionImagesUploadedToStorage(@NonNull SuccessCallback onSuccess, @NonNull ErrorCallback onError) {
        App app = (App) getApplication();
        DataModel dm = app.getDataModel();

        String documentId = dm.getId();
        if (documentId == null || documentId.isEmpty()) {
            documentId = String.valueOf(System.currentTimeMillis());
            dm.setId(documentId);
            app.setDataModel(dm);
        }

        // Track owner for storage organization
        String userId = dm.getIdCustomer();
        if (userId == null || userId.isEmpty()) {
            userId = SessionManager.getId(this);
        }
        if (userId == null) userId = "";
        dm.setStorageUserId(userId);
        app.setDataModel(dm);

        // Jika sudah URL, skip upload
        boolean url1IsSet = dm.getQuestionImageUrl1() != null && dm.getQuestionImageUrl1().startsWith("http");
        boolean url2IsSet = dm.getQuestionImageUrl2() != null && dm.getQuestionImageUrl2().startsWith("http");
        boolean url3IsSet = dm.getQuestionImageUrl3() != null && dm.getQuestionImageUrl3().startsWith("http");
        boolean url4IsSet = dm.getQuestionImageUrl4() != null && dm.getQuestionImageUrl4().startsWith("http");

        AtomicInteger pending = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);

        Runnable maybeFinish = () -> {
            if (failed.get()) return;
            if (pending.get() == 0) onSuccess.onSuccess();
        };

        // iv
        if (!url1IsSet && binding.iv != null && binding.iv.getVisibility() == View.VISIBLE && binding.iv.getDrawable() != null) {
            pending.incrementAndGet();
            uploadImageViewToStorage(binding.iv, "records/" + documentId + "/question_images", "q_img_1.jpg",
                    (url, path) -> {
                        App a = (App) getApplication();
                        DataModel d = a.getDataModel();
                        d.setQuestionImageUrl1(url);
                        d.setQuestionImagePath1(path);
                        // Legacy base64* fields no longer used - only new questionImageUrl* fields
                        a.setDataModel(d);
                        pending.decrementAndGet();
                        maybeFinish.run();
                    },
                    err -> {
                        failed.set(true);
                        onError.onError(err);
                    });
        }

        // iv2
        if (!url2IsSet && binding.iv2 != null && binding.iv2.getVisibility() == View.VISIBLE && binding.iv2.getDrawable() != null) {
            pending.incrementAndGet();
            uploadImageViewToStorage(binding.iv2, "records/" + documentId + "/question_images", "q_img_2.jpg",
                    (url, path) -> {
                        App a = (App) getApplication();
                        DataModel d = a.getDataModel();
                        d.setQuestionImageUrl2(url);
                        d.setQuestionImagePath2(path);
                        // Legacy base64* fields no longer used - only new questionImageUrl* fields
                        a.setDataModel(d);
                        pending.decrementAndGet();
                        maybeFinish.run();
                    },
                    err -> {
                        failed.set(true);
                        onError.onError(err);
                    });
        }

        // iv3 (tabel 1)
        if (!url3IsSet && binding.iv3 != null && binding.iv3.getVisibility() == View.VISIBLE && binding.iv3.getDrawable() != null) {
            pending.incrementAndGet();
            uploadImageViewToStorage(binding.iv3, "records/" + documentId + "/question_images", "q_img_3.jpg",
                    (url, path) -> {
                        App a = (App) getApplication();
                        DataModel d = a.getDataModel();
                        d.setQuestionImageUrl3(url);
                        d.setQuestionImagePath3(path);
                        // Legacy base64* fields no longer used - only new questionImageUrl* fields
                        a.setDataModel(d);
                        pending.decrementAndGet();
                        maybeFinish.run();
                    },
                    err -> {
                        failed.set(true);
                        onError.onError(err);
                    });
        }

        // iv4 (tabel 2)
        if (!url4IsSet && binding.iv4 != null && binding.iv4.getVisibility() == View.VISIBLE && binding.iv4.getDrawable() != null) {
            pending.incrementAndGet();
            uploadImageViewToStorage(binding.iv4, "records/" + documentId + "/question_images", "q_img_4.jpg",
                    (url, path) -> {
                        App a = (App) getApplication();
                        DataModel d = a.getDataModel();
                        d.setQuestionImageUrl4(url);
                        d.setQuestionImagePath4(path);
                        // Legacy base64* fields no longer used - only new questionImageUrl* fields
                        a.setDataModel(d);
                        pending.decrementAndGet();
                        maybeFinish.run();
                    },
                    err -> {
                        failed.set(true);
                        onError.onError(err);
                    });
        }

        // Tidak ada yang perlu di-upload
        maybeFinish.run();
    }

    private interface UrlWithPathCallback { void onUrl(String url, String storagePath); }

    private void uploadImageViewToStorage(@NonNull ImageView imageView,
                                          @NonNull String storagePath,
                                          @NonNull String fileName,
                                          @NonNull UrlWithPathCallback onUrl,
                                          @NonNull ErrorCallback onError) {
        try {
            byte[] jpegBytes = imageViewToJpegBytes(imageView, 85);
            if (jpegBytes == null || jpegBytes.length == 0) {
                onError.onError("Gagal mengubah gambar menjadi bytes");
                return;
            }

            StorageUtil.uploadBytesWithUserId(
                    this,
                    jpegBytes,
                    storagePath,
                    fileName,
                    (downloadUrl, fullPath) -> {
                        Log.d("QUESTION_IMG_UPLOAD", "Uploaded " + fullPath + " -> " + downloadUrl);
                        onUrl.onUrl(downloadUrl, fullPath);
                    },
                    e -> onError.onError(e.getMessage() != null ? e.getMessage() : "Upload gagal"),
                    null
            );
        } catch (Exception e) {
            onError.onError(e.getMessage() != null ? e.getMessage() : "Error upload");
        }
    }

    private byte[] imageViewToJpegBytes(@NonNull ImageView imageView, int quality) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return null;

        Bitmap bitmap = null;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0 || height <= 0) {
                width = Math.max(1, imageView.getWidth());
                height = Math.max(1, imageView.getHeight());
                if (width <= 1 || height <= 1) {
                    imageView.measure(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    );
                    width = Math.max(1, imageView.getMeasuredWidth());
                    height = Math.max(1, imageView.getMeasuredHeight());
                }
                if (width <= 1 || height <= 1) {
                    width = 800;
                    height = 600;
                }
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
        }

        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
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
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Image");
        progressDialog.setMessage("Please wait while the image is being uploaded...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Generate filename with userId and timestamp
        String userId = SessionManager.getId(this);
        String userName = SessionManager.getName(this);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String finalFileName = userId + "_canvas_" + timestamp + "_" + System.currentTimeMillis() + ".jpg";

        // Determine source - GeneratesClassQuestionActivity is for class questions (AI-generated)
        String source = "Buat Pertanyaan dengan AI";

        // Upload using StorageUtil with userId organization and metadata
        StorageUtil.uploadBytesWithUserIdAndMetadata(
            this,
            imageData,
            StorageUtil.STORAGE_PATH_DRAWINGS,
            finalFileName,
            source, // Source metadata
            userName, // User name metadata
            (downloadUrl, storagePath) -> {
                // Save the download URL to DataModel
                App app = (App) getApplication();
                DataModel dataModel = app.getDataModel();
                ArrayList<String> photoDraws = dataModel.getPhotoDraw();
                photoDraws.add(downloadUrl);
                dataModel.setPhotoDraw(photoDraws);
                app.setDataModel(dataModel);

                // Enable the save button
                binding.btnSave.setEnabled(true);
                
                // Log success
                Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);
                Log.d("Firebase", "Storage path: " + storagePath + ", Source: " + source);
                progressDialog.dismiss();

                // Save data to Firestore after successful upload
                ProgressDialog saveProgressDialog = new ProgressDialog(GeneratesClassQuestionActivity.this);
                saveProgressDialog.setTitle("Save data to Server");
                saveProgressDialog.setMessage("Please wait...");
                saveProgressDialog.setCancelable(false);
                saveProgressDialog.show();

                // Get or create documentId - make it final for use in lambda
                String tempDocumentId = dataModel.getId();
                final String documentId = (tempDocumentId == null || tempDocumentId.isEmpty()) 
                        ? String.valueOf(System.currentTimeMillis()) 
                        : tempDocumentId;
                
                dataModel.setId(documentId);
                dataModel.setTypeData(binding.tvType.getText().toString());
                app.setDataModel(dataModel);

                // Upload question images to Storage and store URLs (base64 fields), then save to Firestore
                ensureQuestionImagesUploadedToStorage(
                        () -> {
                            App app2 = (App) getApplication();
                            DataModel dm2 = app2.getDataModel();
                            
                            // Clean empty base64* fields before saving to Firestore
                            cleanEmptyBase64Fields(dm2);
                            
                            FirestoreUtil.addOrUpdateDocumentWithVersioning("record", documentId, dm2,
                                    userId, userName,
                                    () -> {
                                        saveProgressDialog.dismiss();
                                        progressDialog.dismiss();
                                        Intent intent = new Intent(GeneratesClassQuestionActivity.this, RecordPreviewActivity.class);
                                        startActivity(intent);
                                        finish();
                                    },
                                    e -> {
                                        saveProgressDialog.dismiss();
                                        progressDialog.dismiss();
                                        String errorMessage = e.getMessage();
                                        Toast.makeText(GeneratesClassQuestionActivity.this,
                                                errorMessage != null ? errorMessage : "Gagal menyimpan data",
                                                Toast.LENGTH_LONG).show();
                                        binding.btnSave.setEnabled(true);
                                    });
                        },
                        errMsg -> {
                            saveProgressDialog.dismiss();
                            progressDialog.dismiss();
                            Toast.makeText(GeneratesClassQuestionActivity.this,
                                    "Gagal mengunggah gambar soal: " + errMsg,
                                    Toast.LENGTH_LONG).show();
                            binding.btnSave.setEnabled(true);
                        }
                );
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
        
        // Legacy: No longer store question index in base64* fields
        // Images will be uploaded to Storage when user clicks Save/Upload
        
        // Handle Graph Images - Load drawable resources first
        binding.iv.setImageDrawable(ContextCompat.getDrawable(GeneratesClassQuestionActivity.this, result.second.getImages().get(0)));
        binding.iv.setVisibility(View.VISIBLE);
        
        if (result.second.getImages().size() > 1) {
            binding.iv2.setImageDrawable(ContextCompat.getDrawable(GeneratesClassQuestionActivity.this, result.second.getImages().get(1)));
            binding.iv2.setVisibility(View.VISIBLE);
        }
        
        app.setDataModel(dataModel);
    }
    
    /**
     * Legacy function - no longer used
     * Images are now uploaded to Storage via uploadQuestionImagesToStorage() when user clicks Save/Upload
     * This function is kept for backward compatibility but doesn't do anything
     */
    private void convertImageViewToBase64AfterLoad() {
        // Legacy: No longer convert to Base64 - images will be uploaded to Storage
        Log.d("FETCH_QUESTION", "Images will be uploaded to Storage when user clicks Save/Upload");
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
        // Collection "user" is shared, but use VersionHelper for consistency
        String userCollection = VersionHelper.getCollectionName("user");
        DocumentReference userDocRef = firestore.collection(userCollection).document(SessionManager.getId(this));

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

    /**
     * Clean empty base64* fields from DataModel before saving to Firestore.
     * This prevents storing empty strings in Firestore.
     */
    private void cleanEmptyBase64Fields(DataModel dataModel) {
        if (dataModel == null) return;
        
        // Only clean if corresponding questionImageUrl* is available
        if (dataModel.getBase64() != null && (dataModel.getBase64().isEmpty() || dataModel.getBase64().trim().isEmpty())) {
            if (dataModel.getQuestionImageUrl1() != null && !dataModel.getQuestionImageUrl1().isEmpty()) {
                dataModel.setBase64(null); // Set to null instead of empty string
                Log.d("CLEAN_BASE64", "Cleaned empty base64 (questionImageUrl1 available)");
            }
        }
        if (dataModel.getBase64_2() != null && (dataModel.getBase64_2().isEmpty() || dataModel.getBase64_2().trim().isEmpty())) {
            if (dataModel.getQuestionImageUrl2() != null && !dataModel.getQuestionImageUrl2().isEmpty()) {
                dataModel.setBase64_2(null);
                Log.d("CLEAN_BASE64", "Cleaned empty base64_2 (questionImageUrl2 available)");
            }
        }
        if (dataModel.getBase64_3() != null && (dataModel.getBase64_3().isEmpty() || dataModel.getBase64_3().trim().isEmpty())) {
            if (dataModel.getQuestionImageUrl3() != null && !dataModel.getQuestionImageUrl3().isEmpty()) {
                dataModel.setBase64_3(null);
                Log.d("CLEAN_BASE64", "Cleaned empty base64_3 (questionImageUrl3 available)");
            }
        }
        if (dataModel.getBase64_4() != null && (dataModel.getBase64_4().isEmpty() || dataModel.getBase64_4().trim().isEmpty())) {
            if (dataModel.getQuestionImageUrl4() != null && !dataModel.getQuestionImageUrl4().isEmpty()) {
                dataModel.setBase64_4(null);
                Log.d("CLEAN_BASE64", "Cleaned empty base64_4 (questionImageUrl4 available)");
            }
        }
        if (dataModel.getBase64_5() != null && (dataModel.getBase64_5().isEmpty() || dataModel.getBase64_5().trim().isEmpty())) {
            if (dataModel.getQuestionImageUrl5() != null && !dataModel.getQuestionImageUrl5().isEmpty()) {
                dataModel.setBase64_5(null);
                Log.d("CLEAN_BASE64", "Cleaned empty base64_5 (questionImageUrl5 available)");
            }
        }
        
        // Clean photoAnswer if photoAnswerUrl is available
        if (dataModel.getPhotoAnswer() != null && (dataModel.getPhotoAnswer().isEmpty() || dataModel.getPhotoAnswer().trim().isEmpty())) {
            if (dataModel.getPhotoAnswerUrl() != null && !dataModel.getPhotoAnswerUrl().isEmpty()) {
                dataModel.setPhotoAnswer(null);
                Log.d("CLEAN_BASE64", "Cleaned empty photoAnswer (photoAnswerUrl available)");
            }
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
