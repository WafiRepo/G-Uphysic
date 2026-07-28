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
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.se.omapi.Session;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.QuestionImageFirestoreSync;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.Helper.StorageUtil;
import de.rwth_aachen.phyphox.Helper.VersionHelper;
import de.rwth_aachen.phyphox.NetworkConnection.ApiRequest;
import de.rwth_aachen.phyphox.NetworkConnection.ApiResponse;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.databinding.ActivityGeneratesBinding;
import de.rwth_aachen.phyphox.databinding.DialogIntroductionBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import de.rwth_aachen.phyphox.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;


public class GeneratesActivity extends AppCompatActivity {
    private long visitStartTime;
    ActivityGeneratesBinding binding;
    private ApiService apiService;
    private DrawView drawView; // Inisialisasi untuk canvas menggambar
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Uri photoUri;
    private String currentPhotoPath;
    byte[] imageBytes;
    private DataModel dataModel;
    private boolean isCustomQuestion;
    private boolean suppressBackSave = true; // do not save anything on back press
    private String originalTypeDataFromList; // Store original typeData from list question for reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inisialisasi ViewBinding
        binding = ActivityGeneratesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        App app = (App) getApplication();
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        dataModel = app.getDataModel();
        boolean isFromMainMenu = getIntent().getBooleanExtra("isFromMainMenu", true);
        isCustomQuestion = getIntent().getBooleanExtra("isCustomQuestion", false);
        boolean isCustomQuestionNew = getIntent().getBooleanExtra("isCustomQuestionNew", false);
        
        // Hide tvTypeData and tvType by default
        if (binding.tvTypeData != null) {
            binding.tvTypeData.setVisibility(View.GONE);
        }
        // Ensure tvType is hidden and empty by default
        binding.tvType.setVisibility(View.GONE);
        binding.tvType.setText("");

        // Lanjutkan selalu di-disable sampai Simpan berhasil
        binding.btnSave.setEnabled(false);
        binding.btnSave.setAlpha(0.5f);

        if (isFromMainMenu) {
            dataModel.setDesc(SessionManager.getName(this) + " mengerjakan pertanyaan dari Sistem");
            fetchAdvancedQuestion("indonesia", dataModel.getTypeQuestion());
        } else {
            if (isCustomQuestion) {
                // From question list or create new custom: disable Lanjutkan (btnSave) until data tersimpan
                binding.btnSave.setEnabled(false);
                binding.btnSave.setAlpha(0.5f);
                if (isCustomQuestionNew) {
                    binding.etQuestion.setVisibility(View.VISIBLE);
                    binding.tvQuestion.setVisibility(View.GONE);
                    // Set description for new custom question
                    dataModel.setDesc(SessionManager.getName(this) + " Buat Pertanyaan Sendiri");

                    // Set custom title for Out Class questions
                    if (dataModel.getTypeData() != null && (dataModel.getTypeData().contains("Out Class") || dataModel.getTypeData().contains("SA3"))) {
                        dataModel.setTypeData("Out - Class (By AI)");
                        // Update UI if there's a TextView to show this
                        if (binding.tvType != null) {
                            binding.tvType.setText("Out - Class (By AI)");
                        }
                    }

                    if (!dataModel.getTopics().equals("In Class")) {
                        fetchAdvancedQuestion("indonesia", dataModel.getTypeQuestion());
                    }
                } else {
                    // For questions from the list, ensure proper progress tracking
                    int totalEdit = dataModel.getTotalEdit() + 1;
                    dataModel.setTotalEdit(totalEdit);
                    
                    // Store original typeData from list question separately for reference display
                    // Student will input their own typeData in tvType (separate field)
                    originalTypeDataFromList = dataModel.getTypeData();
                    if (originalTypeDataFromList == null || originalTypeDataFromList.isEmpty()) {
                        originalTypeDataFromList = "";
                    }
                    // Clear typeData in dataModel - student will input their own
                    dataModel.setTypeData("");
                    Log.d("LIST_QUESTION_LOAD", "Stored original typeData from list question for reference: " + originalTypeDataFromList);
                    
                    // Determine original question owner (do NOT overwrite original owner fields)
                    String originalOwnerName = dataModel.getCustomerName();
                    String originalOwnerId = dataModel.getIdCustomer();
                    String workerName = SessionManager.getName(this);

                    if (originalOwnerName != null && !originalOwnerName.isEmpty()) {
                        dataModel.setDesc(workerName + " Mengerjakan pertanyaan dari " + originalOwnerName + ")");
                    } else if (originalOwnerId != null && !originalOwnerId.isEmpty()) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                        // Collection "user" is shared, but use VersionHelper for consistency
                        String userCollection = VersionHelper.getCollectionName("user");
                        db.collection(userCollection).document(originalOwnerId).get().addOnSuccessListener(userDoc -> {
                            String userName = userDoc.getString("name");
                            if (userName == null || userName.trim().isEmpty()) userName = "Unknown";
                            dataModel.setDesc(workerName + " Mengerjakan pertanyaan dari " + userName + ")");
                        }).addOnFailureListener(e -> {
                            dataModel.setDesc(workerName + " Mengerjakan pertanyaan dari Unknown)");
                        });
                    } else {
                        dataModel.setDesc(workerName + " Mengerjakan pertanyaan dari Unknown)");
                    }


                    // Load question images (base64) first
                    if (!dataModel.getBase64().isEmpty()) {
                        loadImage(dataModel.getBase64(), binding.iv);
                        binding.iv.setVisibility(View.VISIBLE);
                        binding.iv.setAdjustViewBounds(true);
                        binding.iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        binding.iv.requestLayout();
                        binding.iv.bringToFront();
                        Log.d("LIST_QUESTION_LOAD", "Loaded base64 (iv) - Length: " + dataModel.getBase64().length());
                    }
                    
                    // Remove duplicate loading for iv2 - only load once with fallback logic
                    if (dataModel.getBase64_2() != null && !dataModel.getBase64_2().isEmpty()) {
                        loadImage(dataModel.getBase64_2(), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                        binding.iv2.setAdjustViewBounds(true);
                        binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        binding.iv2.requestLayout();
                        binding.iv2.bringToFront();
                        Log.d("LIST_QUESTION_LOAD", "Loaded base64_2 (iv2) - Length: " + dataModel.getBase64_2().length());
                    } else {
                        // Fallbacks: try other available base64 fields to populate iv2 in order: 5 -> 3 (avoid 4 to prevent duplication)
                        boolean shown = false;
                        if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                            loadImage(dataModel.getBase64_5(), binding.iv2);
                            binding.iv2.setVisibility(View.VISIBLE);
                            binding.iv2.setAdjustViewBounds(true);
                            binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv2.requestLayout();
                            binding.iv2.bringToFront();
                            shown = true;
                            android.util.Log.w("LIST_QUESTION_LOAD", "Base64_2 empty, using Base64_5 as fallback for iv2");
                        } else if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
                            loadImage(dataModel.getBase64_3(), binding.iv2);
                            binding.iv2.setVisibility(View.VISIBLE);
                            binding.iv2.setAdjustViewBounds(true);
                            binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv2.requestLayout();
                            binding.iv2.bringToFront();
                            shown = true;
                            android.util.Log.w("LIST_QUESTION_LOAD", "Base64_2 empty, using Base64_3 as fallback for iv2");
                        }
                        if (!shown) {
                            android.util.Log.w("LIST_QUESTION_LOAD", "Base64_2 is null/empty - iv2 hidden (no fallback)");
                            binding.iv2.setVisibility(View.GONE);
                        }
                    }
                    
                    if (!dataModel.getBase64_3().isEmpty()) {
                        loadImage(dataModel.getBase64_3(), binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                        binding.iv3.setAdjustViewBounds(true);
                        binding.iv3.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        binding.iv3.requestLayout();
                        binding.iv3.bringToFront();
                        Log.d("EDIT_IMAGE_LOAD", "Loaded base64_3 (iv3) - Length: " + dataModel.getBase64_3().length());
                    }
                    if (!dataModel.getBase64_4().isEmpty()) {
                        loadImage(dataModel.getBase64_4(), binding.iv4);
                        binding.iv4.setVisibility(View.VISIBLE);
                        binding.iv4.setAdjustViewBounds(true);
                        binding.iv4.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        binding.iv4.requestLayout();
                        binding.iv4.bringToFront();
                        Log.d("EDIT_IMAGE_LOAD", "Loaded base64_4 (iv4) - Length: " + dataModel.getBase64_4().length());
                    }
                    if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                        loadImage(dataModel.getBase64_5(), binding.iv5);
                        binding.iv5.setVisibility(View.VISIBLE);
                        binding.iv5.setAdjustViewBounds(true);
                        binding.iv5.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        binding.iv5.requestLayout();
                        binding.iv5.bringToFront();
                        Log.d("EDIT_IMAGE_LOAD", "Loaded base64_5 (iv5) - Length: " + dataModel.getBase64_5().length());
                    }

                    // Load photoDraw (canvas drawing from list question) into Preview Jawaban section (ivDraw)
                    // This is a reference image for students when working on the question
                    if (dataModel.getPhotoDraw() != null && !dataModel.getPhotoDraw().isEmpty()) {
                        binding.ivDraw.setVisibility(View.VISIBLE);
                        // Show the last canvas drawing (most recent)
                        String canvasUrl = dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size() - 1);
                        Log.d("PHOTO_DRAW_LOAD", "Loading canvas drawing from list question into ivDraw (Preview Jawaban) - URL: " + canvasUrl);
                        loadImage(canvasUrl, binding.ivDraw);
                    } else {
                        Log.w("PHOTO_DRAW_LOAD", "No canvas drawing found in user list question");
                        binding.ivDraw.setVisibility(View.GONE);
                    }
                    
                    // Load photoAnswerUrl/photoAnswer from list question into "Gambar Soal" section
                    // This is a reference image for students when working on the question
                    // Priority: Load into first available image slot (iv if empty, then iv2, etc.)
                    String photoAnswerUrl = dataModel.getPhotoAnswerUrl();
                    String photoAnswerBase64 = null;
                    if (photoAnswerUrl == null || photoAnswerUrl.isEmpty()) {
                        // Fallback to photoAnswer (Base64)
                        photoAnswerBase64 = dataModel.getPhotoAnswer();
                        if (photoAnswerBase64 != null && !photoAnswerBase64.isEmpty()) {
                            Log.d("PHOTO_ANSWER_LOAD", "Found photoAnswer (Base64) from list question");
                        }
                    } else {
                        Log.d("PHOTO_ANSWER_LOAD", "Found photoAnswerUrl from list question - URL: " + photoAnswerUrl);
                    }
                    
                    // Load photoAnswerUrl/photoAnswer into first available image slot (Gambar Soal)
                    if (photoAnswerUrl != null && !photoAnswerUrl.isEmpty()) {
                        // Use photoAnswerUrl (Firebase Storage URL)
                        // Find first available slot (check if base64 fields are empty)
                        if (dataModel.getBase64() == null || dataModel.getBase64().isEmpty()) {
                            // Load into iv (Gambar Soal 1)
                            loadImage(photoAnswerUrl, binding.iv);
                            binding.iv.setVisibility(View.VISIBLE);
                            binding.iv.setAdjustViewBounds(true);
                            binding.iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv.requestLayout();
                            binding.iv.bringToFront();
                            Log.d("PHOTO_ANSWER_LOAD", "Loaded photoAnswerUrl into iv (Gambar Soal 1)");
                        } else if (dataModel.getBase64_2() == null || dataModel.getBase64_2().isEmpty()) {
                            // Load into iv2 (Gambar Soal 2) if available
                            loadImage(photoAnswerUrl, binding.iv2);
                            binding.iv2.setVisibility(View.VISIBLE);
                            binding.iv2.setAdjustViewBounds(true);
                            binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv2.requestLayout();
                            binding.iv2.bringToFront();
                            Log.d("PHOTO_ANSWER_LOAD", "Loaded photoAnswerUrl into iv2 (Gambar Soal 2)");
                        } else if (dataModel.getBase64_3() == null || dataModel.getBase64_3().isEmpty()) {
                            // Load into iv3 (Table 1) if available
                            loadImage(photoAnswerUrl, binding.iv3);
                            binding.iv3.setVisibility(View.VISIBLE);
                            binding.iv3.setAdjustViewBounds(true);
                            binding.iv3.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv3.requestLayout();
                            binding.iv3.bringToFront();
                            Log.d("PHOTO_ANSWER_LOAD", "Loaded photoAnswerUrl into iv3 (Table 1)");
                        }
                    } else if (photoAnswerBase64 != null && !photoAnswerBase64.isEmpty()) {
                        // Fallback to photoAnswer (Base64)
                        // Find first available slot (check if base64 fields are empty)
                        if (dataModel.getBase64() == null || dataModel.getBase64().isEmpty()) {
                            // Load into iv (Gambar Soal 1)
                            loadImage(photoAnswerBase64, binding.iv);
                            binding.iv.setVisibility(View.VISIBLE);
                            binding.iv.setAdjustViewBounds(true);
                            binding.iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv.requestLayout();
                            binding.iv.bringToFront();
                            Log.d("PHOTO_ANSWER_LOAD", "Loaded photoAnswer (Base64) into iv (Gambar Soal 1)");
                        } else if (dataModel.getBase64_2() == null || dataModel.getBase64_2().isEmpty()) {
                            // Load into iv2 (Gambar Soal 2) if available
                            loadImage(photoAnswerBase64, binding.iv2);
                            binding.iv2.setVisibility(View.VISIBLE);
                            binding.iv2.setAdjustViewBounds(true);
                            binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv2.requestLayout();
                            binding.iv2.bringToFront();
                            Log.d("PHOTO_ANSWER_LOAD", "Loaded photoAnswer (Base64) into iv2 (Gambar Soal 2)");
                        } else if (dataModel.getBase64_3() == null || dataModel.getBase64_3().isEmpty()) {
                            // Load into iv3 (Table 1) if available
                            loadImage(photoAnswerBase64, binding.iv3);
                            binding.iv3.setVisibility(View.VISIBLE);
                            binding.iv3.setAdjustViewBounds(true);
                            binding.iv3.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            binding.iv3.requestLayout();
                            binding.iv3.bringToFront();
                            Log.d("PHOTO_ANSWER_LOAD", "Loaded photoAnswer (Base64) into iv3 (Table 1)");
                        }
                    }
                    
                    // Hide llDocumentationPhoto for list questions
                    // This section is for student's own documentation photos (from "Ambil foto"), not reference from list question
                    if (binding.llDocumentationPhoto != null) {
                        binding.llDocumentationPhoto.setVisibility(View.GONE);
                    }
                    Log.d("LIST_QUESTION_LOAD", "Hidden llDocumentationPhoto - this is for student's own documentation photos, not list question reference");

                    // Log summary of all loaded images for List Question
                    Log.d("LIST_QUESTION_LOAD", "=== LIST QUESTION IMAGE LOADING SUMMARY ===");
                    Log.d("LIST_QUESTION_LOAD", "iv (base64): " + (dataModel.getBase64() != null ? dataModel.getBase64().length() : "null") + " chars");
                    Log.d("LIST_QUESTION_LOAD", "iv2 (base64_2): " + (dataModel.getBase64_2() != null ? dataModel.getBase64_2().length() : "null") + " chars");
                    Log.d("LIST_QUESTION_LOAD", "iv3 (base64_3): " + (dataModel.getBase64_3() != null ? dataModel.getBase64_3().length() : "null") + " chars");
                    Log.d("LIST_QUESTION_LOAD", "iv4 (base64_4): " + (dataModel.getBase64_4() != null ? dataModel.getBase64_4().length() : "null") + " chars");
                    Log.d("LIST_QUESTION_LOAD", "iv5 (base64_5): " + (dataModel.getBase64_5() != null ? dataModel.getBase64_5().length() : "null") + " chars");
                    Log.d("LIST_QUESTION_LOAD", "==========================================");

                    // Show table images container if there are table images
                    boolean hasTableImages = (!dataModel.getBase64_3().isEmpty() || !dataModel.getBase64_4().isEmpty() ||
                                           (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()));
                    if (hasTableImages) {
                        Log.d("LIST_QUESTION_LOAD", "Showing table images container for List Question");
                        binding.tableImagesContainer.setVisibility(View.VISIBLE);
                        Log.d("TABLE_CONTAINER_DEBUG", "Table images container set to VISIBLE - hasTableImages: " + hasTableImages);

                        // Log the final image assignment to prevent duplication
                        Log.d("LIST_QUESTION_LOAD", "=== FINAL IMAGE ASSIGNMENT ===");
                        Log.d("LIST_QUESTION_LOAD", "iv (Graph 1): base64");
                        if (dataModel.getBase64_2() != null && !dataModel.getBase64_2().isEmpty()) {
                            Log.d("LIST_QUESTION_LOAD", "iv2 (Graph 2): base64_2");
                        } else if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                            Log.d("LIST_QUESTION_LOAD", "iv2 (Graph 2): base64_5 (fallback)");
                        } else if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
                            Log.d("LIST_QUESTION_LOAD", "iv2 (Graph 2): base64_3 (fallback)");
                        } else {
                            Log.d("LIST_QUESTION_LOAD", "iv2 (Graph 2): hidden (no fallback)");
                        }
                        Log.d("LIST_QUESTION_LOAD", "iv3 (Table 1): base64_3");
                        Log.d("LIST_QUESTION_LOAD", "iv4 (Table 2): base64_4");
                        if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                            Log.d("LIST_QUESTION_LOAD", "iv5 (Table 3): base64_5");
                        } else {
                            Log.d("LIST_QUESTION_LOAD", "iv5 (Table 3): hidden");
                        }
                        Log.d("LIST_QUESTION_LOAD", "=============================");
                    } else {
                        Log.w("LIST_QUESTION_LOAD", "No table images found for List Question - hiding container");
                        binding.tableImagesContainer.setVisibility(View.GONE);
                        Log.d("TABLE_CONTAINER_DEBUG", "Table images container set to GONE - hasTableImages: " + hasTableImages);
                    }

                    // For "Lihat Detail" List Question, we want to show tables but prevent duplication
                    // Only hide table images if they would duplicate with Graph 2
                    if (hasTableImages) {
                        boolean wouldDuplicate = false;

                        // Check if base64_4 would duplicate with Graph 2 fallback
                        if (dataModel.getBase64_2() == null || dataModel.getBase64_2().isEmpty()) {
                            if (dataModel.getBase64_4() != null && !dataModel.getBase64_4().isEmpty()) {
                                // If Graph 2 uses base64_4 as fallback, hide table images to prevent duplication
                                if (dataModel.getBase64_5() == null || dataModel.getBase64_5().isEmpty()) {
                                    if (dataModel.getBase64_3() == null || dataModel.getBase64_3().isEmpty()) {
                                        // Graph 2 will use base64_4, so hide table images
                                        wouldDuplicate = true;
                                        Log.d("LIST_QUESTION_LOAD", "Hiding table images to prevent duplication: Graph 2 will use base64_4");
                                    }
                                }
                            }
                        }

                        if (wouldDuplicate) {
                            binding.tableImagesContainer.setVisibility(View.GONE);
                            Log.d("LIST_QUESTION_LOAD", "Table images hidden to prevent duplication with Graph 2");
                        } else {
                            Log.d("LIST_QUESTION_LOAD", "Table images visible - no duplication detected");
                        }

                        // Log final visibility status
                        Log.d("LIST_QUESTION_LOAD", "=== FINAL VISIBILITY STATUS ===");
                        Log.d("LIST_QUESTION_LOAD", "tableImagesContainer: " + (binding.tableImagesContainer.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                        Log.d("LIST_QUESTION_LOAD", "iv3 (Table 1): " + (binding.iv3.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                        Log.d("LIST_QUESTION_LOAD", "iv4 (Table 2): " + (binding.iv4.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                        Log.d("LIST_QUESTION_LOAD", "Duplication detected: " + wouldDuplicate);
                        Log.d("LIST_QUESTION_LOAD", "===============================");
                    }

                    binding.tvQuestion.setText(dataModel.getQuestion());
                    
                    // Display original typeData from list question as reference below the question (tvTypeData)
                    // This is separate from student's input typeData (tvType)
                    Log.d("LIST_QUESTION_LOAD", "=== TYPE DATA DISPLAY DEBUG ===");
                    Log.d("LIST_QUESTION_LOAD", "originalTypeDataFromList: " + originalTypeDataFromList);
                    Log.d("LIST_QUESTION_LOAD", "tvTypeData binding: " + (binding.tvTypeData != null ? "NOT NULL" : "NULL"));
                    
                    if (originalTypeDataFromList != null && !originalTypeDataFromList.isEmpty()) {
                        // Display in tvTypeData (TextView below question) as reference
                        if (binding.tvTypeData != null) {
                            binding.tvTypeData.setText("📋 " + originalTypeDataFromList);
                            binding.tvTypeData.setVisibility(View.VISIBLE);
                            Log.d("LIST_QUESTION_LOAD", "✓ Displaying original typeData from list question below question as reference: " + originalTypeDataFromList);
                            Log.d("LIST_QUESTION_LOAD", "✓ tvTypeData visibility set to VISIBLE");
                            Log.d("LIST_QUESTION_LOAD", "✓ tvTypeData text set to: " + binding.tvTypeData.getText());
                        } else {
                            Log.e("LIST_QUESTION_LOAD", "✗ tvTypeData is null! Cannot display typeData reference");
                        }
                    } else {
                        // Hide if no typeData from list
                        if (binding.tvTypeData != null) {
                            binding.tvTypeData.setVisibility(View.GONE);
                        }
                        Log.d("LIST_QUESTION_LOAD", "No typeData from list question to display as reference (empty or null)");
                    }
                    Log.d("LIST_QUESTION_LOAD", "===============================");
                    
                    // Hide input field (tvType) by default - will appear when student clicks "Ketik" button
                    // This is for student's own typeData input, separate from list question's typeData
                    binding.tvType.setVisibility(View.GONE);
                    binding.tvType.setText(""); // Clear the input field
                    Log.d("LIST_QUESTION_LOAD", "typeData input field (tvType) hidden - will appear when 'Ketik' button is clicked for student input");
                }

            } else {
                int totalEdit =dataModel.getTotalEdit()+1;
                dataModel.setTotalEdit(totalEdit);
                binding.tvType.setText(dataModel.getTypeData());
                binding.tvQuestion.setText(dataModel.getQuestion());

                // Load canvas drawing if available (for Edit mode)
                if(dataModel.getPhotoDraw() != null && !dataModel.getPhotoDraw().isEmpty()){
                    binding.ivDraw.setVisibility(View.VISIBLE);
                    loadImage(dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size()-1), binding.ivDraw);
                }

                // Handle Graph Images
                if (dataModel.getBase64() != null && !dataModel.getBase64().isEmpty()) {
                    loadImageWithGlide(base64ToDrawable(dataModel.getBase64(), GeneratesActivity.this), binding.iv);
                    binding.iv.setVisibility(View.VISIBLE);
                    binding.iv.setAdjustViewBounds(true);
                    binding.iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    binding.iv.requestLayout();
                    binding.iv.bringToFront();
                    Log.d("EDIT_IMAGE_LOAD", "Edit mode: Loaded base64 (iv) - Length: " + dataModel.getBase64().length());
                }
                if (dataModel.getBase64() != null && !dataModel.getBase64().isEmpty()) {
                    loadImage(dataModel.getBase64(), binding.iv);
                    binding.iv.setVisibility(View.VISIBLE);
                    binding.iv.setAdjustViewBounds(true);
                    binding.iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    binding.iv.requestLayout();
                    binding.iv.bringToFront();
                    Log.d("EDIT_IMAGE_LOAD", "Edit mode: Loaded base64 (iv) - Length: " + dataModel.getBase64().length());
                }
                if (dataModel.getBase64_2() != null && !dataModel.getBase64_2().isEmpty()) {
                    loadImage(dataModel.getBase64_2(), binding.iv2);
                    binding.iv2.setVisibility(View.VISIBLE);
                    binding.iv2.setAdjustViewBounds(true);
                    binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    binding.iv2.requestLayout();
                    binding.iv2.bringToFront();
                    Log.d("EDIT_IMAGE_LOAD", "Edit mode: Loaded base64_2 (iv2) - Length: " + dataModel.getBase64_2().length());
                } else {
                    boolean shown2 = false;
                    if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                        loadImage(dataModel.getBase64_5(), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                        binding.iv2.setAdjustViewBounds(true);
                        binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        binding.iv2.requestLayout();
                        binding.iv2.bringToFront();
                        shown2 = true;
                        android.util.Log.w("EDIT_IMAGE_LOAD", "Edit mode: Base64_2 empty, using Base64_5 as fallback for iv2");
                    } else if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
                        loadImage(dataModel.getBase64_3(), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                        binding.iv2.setAdjustViewBounds(true);
                        binding.iv2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        binding.iv2.requestLayout();
                        binding.iv2.bringToFront();
                        shown2 = true;
                        android.util.Log.w("EDIT_IMAGE_LOAD", "Edit mode: Base64_2 empty, using Base64_3 as fallback for iv2");
                    }
                    if (!shown2) {
                        android.util.Log.w("EDIT_IMAGE_LOAD", "Base64_2 is null/empty in edit mode - iv2 hidden (no fallback)");
                        binding.iv2.setVisibility(View.GONE);
                    }
                }
                // Check and show table images in edit mode
                boolean hasTableImagesEdit = false;

                boolean table3Shown = false;
                boolean table4Shown = false;
                if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) {
                    Log.d("EDIT_IMAGE_LOAD", "Loading Base64_3 (iv3) - Length: " + dataModel.getBase64_3().length());
                    loadImage(dataModel.getBase64_3(), binding.iv3);
                    binding.iv3.setVisibility(View.VISIBLE);
                    binding.iv3.setAdjustViewBounds(true);
                    binding.iv3.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    binding.iv3.requestLayout();
                    binding.iv3.bringToFront();
                    table3Shown = true;
                    hasTableImagesEdit = true;
                } else {
                    Log.w("EDIT_IMAGE_LOAD", "Base64_3 is empty - iv3 will not be shown");
                    binding.iv3.setVisibility(View.GONE);
                }
                if (dataModel.getBase64_4() != null && !dataModel.getBase64_4().isEmpty()) {
                    Log.d("EDIT_IMAGE_LOAD", "Loading Base64_4 (iv4) - Length: " + dataModel.getBase64_4().length());
                    loadImage(dataModel.getBase64_4(), binding.iv4);
                    binding.iv4.setVisibility(View.VISIBLE);
                    binding.iv4.setAdjustViewBounds(true);
                    binding.iv4.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    binding.iv4.requestLayout();
                    binding.iv4.bringToFront();
                    table4Shown = true;
                    hasTableImagesEdit = true;
                } else {
                    Log.w("EDIT_IMAGE_LOAD", "Base64_4 is empty - iv4 will not be shown");
                    binding.iv4.setVisibility(View.GONE);
                }
                // Reorder container to ensure iv3 is before iv4
                if (hasTableImagesEdit) {
                    try {
                        ViewGroup parent = (ViewGroup) binding.iv3.getParent();
                        if (parent != null) {
                            parent.removeView(binding.iv3);
                            parent.removeView(binding.iv4);
                            // Add in order: iv3 then iv4
                            if (table3Shown) {
                                parent.addView(binding.iv3);
                                Log.d("EDIT_IMAGE_LOAD", "Added iv3 to parent container");
                            }
                            if (table4Shown) {
                                parent.addView(binding.iv4);
                                Log.d("EDIT_IMAGE_LOAD", "Added iv4 to parent container");
                            }
                        }
                    } catch (Exception e) {
                        Log.w("EDIT_IMAGE_LOAD", "Failed to reorder iv3/iv4: " + e.getMessage());
                    }
                }

                // Show table images container if there are table images in edit mode
                if (hasTableImagesEdit) {
                    Log.d("EDIT_IMAGE_LOAD", "Showing table images container in edit mode");
                    binding.tableImagesContainer.setVisibility(View.VISIBLE);

                    // Log summary of all loaded images for Edit mode
                    Log.d("EDIT_IMAGE_LOAD", "=== EDIT MODE IMAGE LOADING SUMMARY ===");
                    Log.d("EDIT_IMAGE_LOAD", "iv (base64): " + (dataModel.getBase64() != null ? dataModel.getBase64().length() : "null") + " chars");
                    Log.d("EDIT_IMAGE_LOAD", "iv2 (base64_2): " + (dataModel.getBase64_2() != null ? dataModel.getBase64_2().length() : "null") + " chars");
                    Log.d("EDIT_IMAGE_LOAD", "iv3 (base64_3): " + (dataModel.getBase64_3() != null ? dataModel.getBase64_3().length() : "null") + " chars");
                    Log.d("EDIT_IMAGE_LOAD", "iv4 (base64_4): " + (dataModel.getBase64_4() != null ? dataModel.getBase64_4().length() : "null") + " chars");
                    Log.d("EDIT_IMAGE_LOAD", "iv5 (base64_5): " + (dataModel.getBase64_5() != null ? dataModel.getBase64_5().length() : "null") + " chars");
                    Log.d("EDIT_IMAGE_LOAD", "=====================================");
                } else {
                    Log.w("EDIT_IMAGE_LOAD", "No table images in edit mode - hiding container");
                    binding.tableImagesContainer.setVisibility(View.GONE);
                }
                if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                    loadImage(dataModel.getBase64_5(), binding.iv5);
                    binding.iv5.setVisibility(View.VISIBLE);
                }

                // Load documentation photo if exists
                if (dataModel.getPhotoAnswer() != null && !dataModel.getPhotoAnswer().isEmpty()) {
                    displayPhotoInDocumentationSection(dataModel.getPhotoAnswer());
                }

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
                Log.d("btnTakePhoto ", "permission graned");
                dispatchTakePictureIntent();
            }
        });
        // Tombol Save untuk menyimpan progress
        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.btnUpload.setEnabled(false);

                try {
                    // Always upload canvas drawing (for Preview Jawaban section)
                    byte[] canvasData = convertBitmapToBytes(getViewAsBitmap(binding.llDraw));

                    Log.d("CANVAS_UPLOAD", "Uploading CANVAS ONLY (size: " + canvasData.length + " bytes) - Documentation photo stays as Base64");

                    // Validate canvas size before upload
                    if (canvasData.length > 800000) { // ~800KB limit
                        Toast.makeText(GeneratesActivity.this,
                            "Gambar canvas terlalu besar. Silakan coba dengan gambar yang lebih sederhana.",
                            Toast.LENGTH_LONG).show();
                        binding.btnUpload.setEnabled(true);
                        return;
                    }

                    // Upload canvas drawing (this goes to photoDraw for Preview Jawaban)
                    uploadImageToFirestore(canvasData, "canvas_answer_" + System.currentTimeMillis());
                } catch (Exception e) {
                    Log.e("UPLOAD_ERROR", "Error preparing image for upload: " + e.getMessage());
                    Toast.makeText(GeneratesActivity.this,
                        "Terjadi kesalahan saat mempersiapkan gambar. Silakan coba lagi.",
                        Toast.LENGTH_LONG).show();
                    binding.btnUpload.setEnabled( true);
                }
            }
        });

        // Tombol Next yang mengarahkan ke FeedbackActivity
        binding.btnSave.setOnClickListener(v -> {
            // Check if data has been uploaded first
            if (dataModel.getPhotoDraw() == null || dataModel.getPhotoDraw().isEmpty()) {
                Toast.makeText(GeneratesActivity.this, "Klik Simpan terlebih dahulu untuk menyimpan gambar", Toast.LENGTH_LONG).show();
                return;
            }

            // Validate all base64 data before proceeding
            try {
                validateBase64Data();

                // If validation passes, proceed to next activity
                Intent intent = new Intent(GeneratesActivity.this, RecordPreviewActivity.class);
                startActivity(intent);
                finish();

            } catch (Exception e) {
                Log.e("NEXT_ERROR", "Error validating data: " + e.getMessage());
                Toast.makeText(GeneratesActivity.this,
                    "Terjadi kesalahan saat memvalidasi data. Silakan coba lagi.",
                    Toast.LENGTH_LONG).show();
            }
        });
        binding.btnType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.tvType.getVisibility() == View.VISIBLE) {
                    // Hide and clear the field
                    binding.tvType.setVisibility(View.GONE);
                    binding.tvType.setText("");
                    dataModel.setTypeData("");
                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(binding.tvType.getWindowToken(), 0);
                    }
                } else {
                    // Show the field and clear any existing data
                    binding.tvType.setVisibility(View.VISIBLE);
                    binding.tvType.setText("");
                    binding.tvType.requestFocus();
                    // Show keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(binding.tvType, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                if (getIntent().getBooleanExtra("isFromMainMenu", true)) {
                    // Simpan progress ke Firestore tanpa ProgressDialog (lightweight payload)
                    if (suppressBackSave) {
                        finish();
                        return;
                    }
                    String table = "record";
                    if (isCustomQuestionNew) {
                        dataModel.setQuestion(binding.etQuestion.getText().toString());
                        table = "questions";
                    }
                    // Use student's input typeData (from tvType), not originalTypeDataFromList
                    // This ensures student's typeData is separate from list question's typeData
                    String studentTypeData = binding.tvType.getText().toString();
                    if (studentTypeData == null || studentTypeData.trim().isEmpty()) {
                        // If student didn't input typeData, use empty string (not originalTypeDataFromList)
                        studentTypeData = "";
                    }
                    dataModel.setTypeData(studentTypeData);
                    Log.d("SAVE_TYPE_DATA", "Saving student's typeData: '" + studentTypeData + "' (original from list was: '" + originalTypeDataFromList + "')");
                    // Ensure worker identity for history filter
                    dataModel.setIdCustomer(SessionManager.getId(GeneratesActivity.this));
                    dataModel.setCustomerName(SessionManager.getName(GeneratesActivity.this));

                    // Set status based on context
                    if (isCustomQuestionNew) {
                        // For new custom questions: status "berlanjut" (false)
                        dataModel.setFinished(false);

                        // Set custom title for Out Class questions
                        if (dataModel.getTypeData() != null && (dataModel.getTypeData().contains("Out Class") || dataModel.getTypeData().contains("SA3"))) {
                            dataModel.setTypeData("Out - Class (By AI)");
                        }
                    } else {
                        // For completed experiments: status "selesai" (true)
                        dataModel.setFinished(true);
                    }

                    java.util.Map<String, Object> progressData = new java.util.HashMap<>();
                    progressData.put("id", dataModel.getId());
                    progressData.put("idCustomer", dataModel.getIdCustomer());
                    progressData.put("customerName", dataModel.getCustomerName());
                    progressData.put("typeData", dataModel.getTypeData());
                    progressData.put("topics", dataModel.getTopics());
                    progressData.put("typeQuestion", dataModel.getTypeQuestion());
                    progressData.put("question", dataModel.getQuestion());
                    progressData.put("desc", dataModel.getDesc());
                    progressData.put("photo", dataModel.getPhoto());
                    progressData.put("photoDraw", dataModel.getPhotoDraw());
                    progressData.put("latitude", dataModel.getLatitude());
                    progressData.put("longitude", dataModel.getLongitude());
                    progressData.put("locationName", dataModel.getLocationName());
                    progressData.put("isFinished", dataModel.getFinished());
                    progressData.put("dateTime", dataModel.getDateTime());
                    progressData.put("totalEdit", dataModel.getTotalEdit());
                    progressData.put("views", dataModel.getViews());

                    // Add photo documentation fields
                    progressData.put("photoDocumentation", dataModel.getPhotoDocumentation());
                    progressData.put("photoDocumentationPaths", dataModel.getPhotoDocumentationPaths());
                    progressData.put("notePhotos", dataModel.getNotePhotos());
                    progressData.put("notePhotoPaths", dataModel.getNotePhotoPaths());
                    progressData.put("experimentPhotoUrl", dataModel.getExperimentPhotoUrl());
                    progressData.put("experimentPhotoPath", dataModel.getExperimentPhotoPath());
                    progressData.put("documentationNotes", dataModel.getDocumentationNotes());
                    progressData.put("storageUserId", dataModel.getStorageUserId());
                    progressData.put("lastPhotoUpdate", dataModel.getLastPhotoUpdate());

                    // Upload photo if exists before saving to Firestore
                    // Check for local photo path or base64 data that needs to be uploaded
                    String localPhotoPath = dataModel.getLocalPhotoPath();
                    String localPhotoBase64 = dataModel.getLocalPhotoBase64();

                    if ((localPhotoPath != null && !localPhotoPath.isEmpty()) ||
                        (localPhotoBase64 != null && !localPhotoBase64.isEmpty())) {
                        // We have local photo data to upload
                        if (localPhotoPath != null && !localPhotoPath.isEmpty()) {
                            Log.d("PHOTO_UPLOAD", "Uploading local photo file: " + localPhotoPath);
                            uploadPhotoBeforeSave(dataModel, table, progressData);
                        } else if (localPhotoBase64 != null && !localPhotoBase64.isEmpty()) {
                            Log.d("PHOTO_UPLOAD", "Uploading base64 photo data");
                            uploadBase64PhotoBeforeSave(dataModel, table, progressData);
                        }
                    } else {
                        // No local photo to upload, save directly
                        Log.d("PHOTO_UPLOAD", "No local photo to upload, saving directly");
                        saveToFirestoreWithBackup(table, progressData);
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
        // Tombol Calculator
        binding.btnCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GeneratesActivity.this, CalculatorActivity.class);
                startActivity(intent);
            }
        });
        // Record visit start time
        visitStartTime = System.currentTimeMillis();

        // Setup color palette functionality
        setupColorPalette();
    }

    private void setupColorPalette() {
        // Setup color palette button click listener
        binding.btnColorPalette.setOnClickListener(v -> toggleColorPicker());

        // Setup color buttons click listeners
        setupColorButtons();
    }

    private void toggleColorPicker() {
        if (binding.colorPickerOverlay.getVisibility() == View.VISIBLE) {
            binding.colorPickerOverlay.setVisibility(View.GONE);
        } else {
            binding.colorPickerOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void setupColorButtons() {
        // Get all color views from the overlay
        ViewGroup colorOverlay = binding.colorPickerOverlay;
        for (int i = 0; i < colorOverlay.getChildCount(); i++) {
            View child = colorOverlay.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View colorView = row.getChildAt(j);
                    if (colorView.getTag() != null) {
                        colorView.setOnClickListener(v -> {
                            String colorHex = (String) v.getTag();
                            setCurrentColor(colorHex);
                            binding.colorPickerOverlay.setVisibility(View.GONE);
                        });
                    }
                }
            }
        }
    }

    private void setCurrentColor(String colorHex) {
        try {
            int color = Color.parseColor(colorHex);

            // Update current color indicator
            GradientDrawable indicator = new GradientDrawable();
            indicator.setShape(GradientDrawable.OVAL);
            indicator.setColor(color);
            indicator.setStroke(4, Color.WHITE);
            binding.currentColorIndicator.setBackground(indicator);

            // Update drawing view color
            if (drawView != null) {
                drawView.setColor(color);
            }

            Toast.makeText(this, "Warna dipilih: " + colorHex, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Terjadi kesalahan memilih warna", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Log.d("btnTakePhoto", "dispatchTakePictureIntent");

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Uncomment dan perbaiki package visibility check
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Tidak ada aplikasi kamera", Toast.LENGTH_SHORT).show();
            Log.d("btnTakePhoto", "NO camera app available");
            return;
        }

        Log.d("btnTakePhoto", "takePictureIntent");

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Gagal membuat file gambar", Toast.LENGTH_SHORT).show();
            return; // Tambahkan return untuk menghindari crash
        }

        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".exportProvider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                Log.e("Camera", "Failed to start camera: " + e.getMessage());
                Toast.makeText(this, "Gagal membuka kamera", Toast.LENGTH_SHORT).show();
            }
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
        Log.d("onactivityresult", "--> " + resultCode);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                Log.d("onactivityresult", "--> processing image");
                // Cek apakah file exists
                if (currentPhotoPath != null && new File(currentPhotoPath).exists()) {
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

                        // Combine with previous documentation photo (old on top, new below) if exists
                        // Only combine if existing photo is from student's work (not from questions collection)
                        // Check if photoAnswerUrl exists and is from questions collection - if so, don't combine
                        String existingDoc = dataModel.getPhotoAnswer();
                        String existingPhotoAnswerPath = dataModel.getPhotoAnswerPath();
                        boolean isFromQuestionsCollection = (existingPhotoAnswerPath != null && existingPhotoAnswerPath.contains("questions"));
                        
                        // Only combine if existing photo is NOT from questions collection (it's student's own work)
                        if (existingDoc != null && !existingDoc.trim().isEmpty() && !isFromQuestionsCollection) {
                            try {
                                Bitmap oldBmp = base64ToBitmap(existingDoc);
                                Bitmap newBmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                if (oldBmp != null && newBmp != null) {
                                    Bitmap stacked = stackBitmapsVertically(oldBmp, newBmp);
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    stacked.compress(Bitmap.CompressFormat.JPEG, 85, out);
                                    base64Image = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
                                    // Recycle temp bitmaps
                                    oldBmp.recycle();
                                    newBmp.recycle();
                                    stacked.recycle();
                                    Log.d("DOC_PHOTO_SAVE", "Combined with previous student's documentation photo");
                                }
                            } catch (Exception ignore) {}
                        } else if (isFromQuestionsCollection) {
                            Log.d("DOC_PHOTO_SAVE", "Ignoring photoAnswer from questions collection - using only student's new photo");
                        }

                        // Save combined/new documentation photo as Base64
                        // This is the student's own documentation photo, not from questions collection
                        App app = (App) getApplication();
                        dataModel.setPhotoAnswer(base64Image);
                        // Clear photoAnswerUrl and photoAnswerPath from questions collection (if any)
                        // This ensures we use the student's own photo, not the reference from questions
                        dataModel.setPhotoAnswerUrl(null);
                        dataModel.setPhotoAnswerPath(null);
                        // Keep Base64 fields intact for Edit mode display; size enforcement will happen at final save
                        app.setDataModel(dataModel);

                        Log.d("DOC_PHOTO_SAVE", "Student's documentation photo saved as Base64 (length: " + base64Image.length() + ") - Will be uploaded to Firebase Storage on save");

                        // Display photo in documentation section
                        displayPhotoInDocumentationSection(base64Image);

                        // Clean up if we created a new bitmap
                        if (optimizedBitmap != bitmap) {
                            optimizedBitmap.recycle();
                        }
                    } else {
                        Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "File gambar tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("ImageProcess", "Error processing image: " + e.getMessage());
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            String clean = base64String;
            if (clean.contains(",")) clean = clean.split(",")[1];
            clean = clean.replaceAll("\\s+", "");
            byte[] decoded = Base64.decode(clean, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap stackBitmapsVertically(Bitmap top, Bitmap bottom) {
        // Scale both to same width (max of both), keep aspect ratio
        int targetWidth = Math.max(top.getWidth(), bottom.getWidth());
        Bitmap scaledTop = (top.getWidth() == targetWidth)
                ? top
                : Bitmap.createScaledBitmap(top, targetWidth, Math.round((float) top.getHeight() * targetWidth / top.getWidth()), true);
        Bitmap scaledBottom = (bottom.getWidth() == targetWidth)
                ? bottom
                : Bitmap.createScaledBitmap(bottom, targetWidth, Math.round((float) bottom.getHeight() * targetWidth / bottom.getWidth()), true);

        int totalHeight = scaledTop.getHeight() + scaledBottom.getHeight();
        Bitmap combined = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(combined);
        canvas.drawBitmap(scaledTop, 0, 0, null);
        canvas.drawBitmap(scaledBottom, 0, scaledTop.getHeight(), null);

        // Recycle temporary scaled if they are different objects
        if (scaledTop != top) scaledTop.recycle();
        if (scaledBottom != bottom) scaledBottom.recycle();
        return combined;
    }

    // REMOVED: uploadImageToFirestoreAnswer method
    // Documentation photos should NOT be uploaded to Firebase Storage
    // They are saved as Base64 in dataModel.setPhotoAnswer() for local storage only
    // Canvas drawings are uploaded to Firebase and saved in dataModel.setPhotoDraw()


    public Bitmap getViewAsBitmap(View view) {
        try {
            Log.d("CANVAS_BITMAP", "Creating bitmap from view: " + view.getWidth() + "x" + view.getHeight());

            // Ensure view has valid dimensions
            if (view.getWidth() <= 0 || view.getHeight() <= 0) {
                Log.e("CANVAS_BITMAP", "Invalid view dimensions: " + view.getWidth() + "x" + view.getHeight());
                throw new IllegalArgumentException("View must have positive dimensions");
            }

        // Gunakan Canvas untuk Android 11+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);

                Log.d("CANVAS_BITMAP", "Canvas bitmap created successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            return bitmap;
        } else {
            // Fallback untuk versi lama
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

                Log.d("CANVAS_BITMAP", "Cache bitmap created successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            return bitmap;
            }
        } catch (Exception e) {
            Log.e("CANVAS_BITMAP", "Error creating bitmap from view: " + e.getMessage());
            throw e;
        }
    }

    public byte[] convertBitmapToBytes(Bitmap bitmap) {
        try {
            if (bitmap == null) {
                Log.e("CONVERT_BITMAP", "Bitmap is null");
                throw new IllegalArgumentException("Bitmap cannot be null");
            }

            Log.d("CONVERT_BITMAP", "Converting bitmap to bytes: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            // Optimize bitmap before converting to bytes
            Bitmap optimizedBitmap = optimizeBitmapForStorage(bitmap);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            boolean success = optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);

            if (!success) {
                Log.e("CONVERT_BITMAP", "Failed to compress bitmap to JPEG");
                throw new RuntimeException("Failed to compress bitmap");
            }

            byte[] result = byteArrayOutputStream.toByteArray();
            Log.d("CONVERT_BITMAP", "Bitmap converted successfully to " + result.length + " bytes");

            // Clean up if we created a new bitmap
            if (optimizedBitmap != bitmap) {
                optimizedBitmap.recycle();
            }

            return result;
        } catch (Exception e) {
            Log.e("CONVERT_BITMAP", "Error converting bitmap to bytes: " + e.getMessage());
            throw e;
        }
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
                // Image is large but Firebase Storage URLs may already be set as fallback.
                // Log a warning and continue rather than blocking navigation.
                Log.w("BASE64_VALIDATION", "Base64 field " + (i + 1) + " is too large: " + base64Data.length() + " bytes (will rely on Firebase Storage URL)");
            }
        }

        Log.d("BASE64_VALIDATION", "All base64 data validated successfully");
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
     * Display captured photo in the documentation section
     * @param base64Image Base64 string of the captured photo
     */
    private void displayPhotoInDocumentationSection(String base64Image) {
        try {
            Log.d("DOC_PHOTO", "=== DISPLAY PHOTO DOCUMENTATION SECTION START ===");
            Log.d("DOC_PHOTO", "Base64 image length: " + base64Image.length());
            Log.d("DOC_PHOTO", "Base64 image preview: " + base64Image.substring(0, Math.min(50, base64Image.length())) + "...");

            // Check if binding is available
            if (binding == null) {
                Log.e("DOC_PHOTO", "Binding is null!");
                return;
            }

            // Check if llDocumentationPhoto exists
            if (binding.llDocumentationPhoto == null) {
                Log.e("DOC_PHOTO", "llDocumentationPhoto is null!");
                return;
            }

            // Show the documentation photo container
            binding.llDocumentationPhoto.setVisibility(View.VISIBLE);
            Log.d("DOC_PHOTO", "llDocumentationPhoto visibility set to VISIBLE");

            // Convert base64 to drawable
            BitmapDrawable drawable = base64ToDrawable(base64Image, GeneratesActivity.this);
            if (drawable != null) {
                Log.d("DOC_PHOTO", "Successfully converted base64 to drawable");

                // Check if ivDocumentationPhoto exists
                if (binding.ivDocumentationPhoto == null) {
                    Log.e("DOC_PHOTO", "ivDocumentationPhoto is null!");
                    return;
                }

                // Load image with Glide for better performance
                loadImageWithGlide(drawable, binding.ivDocumentationPhoto);
                Log.d("DOC_PHOTO", "Image loaded with Glide");

                // Configure PhotoView for documentation photo
                if (binding.ivDocumentationPhoto instanceof com.github.chrisbanes.photoview.PhotoView) {
                    com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) binding.ivDocumentationPhoto;

                    try {
                        // Reset PhotoView state first
                        photoView.setScale(1.0f, true);

                        // Set zoom levels for documentation photo
                        photoView.setMaximumScale(4.0f);
                        photoView.setMediumScale(2.0f);
                        photoView.setMinimumScale(0.8f);
                        photoView.setZoomable(true);

                        Log.d("DOC_PHOTO", "Documentation photo displayed successfully with zoom capability");
                    } catch (Exception e) {
                        Log.e("DOC_PHOTO", "Error setting zoom for documentation photo: " + e.getMessage());
                        // Fallback to basic display
                        photoView.setZoomable(true);
                    }
                } else {
                    Log.w("DOC_PHOTO", "ivDocumentationPhoto is not a PhotoView instance");
                }

                // Show success message
                Toast.makeText(this, "📸 Foto dokumentasi berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                Log.d("DOC_PHOTO", "=== DISPLAY PHOTO DOCUMENTATION SECTION SUCCESS ===");

            } else {
                Log.e("DOC_PHOTO", "Failed to convert base64 to drawable for documentation photo");
                Toast.makeText(this, "Gagal menampilkan foto dokumentasi", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("DOC_PHOTO", "Error displaying documentation photo: " + e.getMessage());
            Log.e("DOC_PHOTO", "Stack trace: ", e);
            Toast.makeText(this, "Error menampilkan foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Display photo documentation from URL (Firebase Storage)
     */
    private void displayPhotoInDocumentationSectionFromUrl(String imageUrl) {
        try {
            Log.d("DOC_PHOTO", "=== DISPLAY PHOTO DOCUMENTATION FROM URL START ===");
            Log.d("DOC_PHOTO", "Image URL: " + imageUrl);

            // Check if binding is available
            if (binding == null) {
                Log.e("DOC_PHOTO", "Binding is null!");
                return;
            }

            // Check if llDocumentationPhoto exists
            if (binding.llDocumentationPhoto == null) {
                Log.e("DOC_PHOTO", "llDocumentationPhoto is null!");
                return;
            }

            // Show the documentation photo container
            binding.llDocumentationPhoto.setVisibility(View.VISIBLE);
            Log.d("DOC_PHOTO", "llDocumentationPhoto visibility set to VISIBLE");

            // Check if ivDocumentationPhoto exists
            if (binding.ivDocumentationPhoto == null) {
                Log.e("DOC_PHOTO", "ivDocumentationPhoto is null!");
                return;
            }

            // Load image directly from URL using loadImage method
            loadImage(imageUrl, binding.ivDocumentationPhoto);
            Log.d("DOC_PHOTO", "Image loaded from URL");

            // Configure PhotoView for documentation photo
            if (binding.ivDocumentationPhoto instanceof com.github.chrisbanes.photoview.PhotoView) {
                com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) binding.ivDocumentationPhoto;

                try {
                    // Reset PhotoView state first
                    photoView.setScale(1.0f, true);

                    // Set zoom levels for documentation photo
                    photoView.setMaximumScale(4.0f);
                    photoView.setMediumScale(2.0f);
                    photoView.setMinimumScale(0.8f);
                    photoView.setZoomable(true);

                    Log.d("DOC_PHOTO", "Documentation photo displayed successfully with zoom capability");
                } catch (Exception e) {
                    Log.e("DOC_PHOTO", "Error setting zoom for documentation photo: " + e.getMessage());
                    // Fallback to basic display
                    photoView.setZoomable(true);
                }
            } else {
                Log.w("DOC_PHOTO", "ivDocumentationPhoto is not a PhotoView instance");
            }

            Log.d("DOC_PHOTO", "=== DISPLAY PHOTO DOCUMENTATION FROM URL SUCCESS ===");

        } catch (Exception e) {
            Log.e("DOC_PHOTO", "Error displaying documentation photo from URL: " + e.getMessage());
            Log.e("DOC_PHOTO", "Stack trace: ", e);
            Toast.makeText(this, "Error menampilkan foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        final int FIRESTORE_LIMIT = 900000; // Leave more margin for safety

        if (base64String.length() > FIRESTORE_LIMIT) {
            Log.w("FIRESTORE_COMPRESS", "Base64 string too large (" + base64String.length() + " bytes), compressing...");

            try {
                // Extract base64 data without prefix
                String base64Data = base64String;
                if (base64String.contains(",")) {
                    base64Data = base64String.split(",")[1];
                }

                // Remove whitespace (API may return base64 with newlines)
                base64Data = base64Data.replaceAll("\\s+", "").trim();
                // Add padding if needed
                int padMod = base64Data.length() % 4;
                if (padMod != 0) {
                    StringBuilder padSb = new StringBuilder(base64Data);
                    for (int p = 0; p < 4 - padMod; p++) padSb.append('=');
                    base64Data = padSb.toString();
                }

                byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
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

            // If compression fails, return original so the image can still be displayed.
            // Firestore size enforcement in RecordPreviewActivity will handle trimming if needed.
            Log.e("FIRESTORE_COMPRESS", "Compression failed; returning original base64 to preserve display");
            return base64String;
        }

        return base64String;
    }

    public void uploadImageToFirestore(byte[] imageData, String fileName) {
        // Create and configure ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Mengunggah Gambar");
        progressDialog.setMessage("Mohon tunggu, gambar sedang diunggah...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Generate filename with userId and timestamp
        String userId = SessionManager.getId(this);
        String userName = SessionManager.getName(this);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String finalFileName = userId + "_canvas_" + timestamp + "_" + System.currentTimeMillis() + ".jpg";

        // Determine source based on question type
        boolean isCustomQuestionNew = getIntent().getBooleanExtra("isCustomQuestionNew", false);
        final String source; // Must be final for use in lambda
        if (isCustomQuestionNew) {
            source = "Buat Pertanyaan Sendiri";
        } else {
            boolean isCustomQuestion = getIntent().getBooleanExtra("isCustomQuestion", false);
            if (isCustomQuestion) {
                if (dataModel.getTypeData() != null && dataModel.getTypeData().contains("By AI")) {
                    source = "Buat Pertanyaan dengan AI";
                } else {
                    source = "Buat Pertanyaan Sendiri";
                }
            } else {
                source = "Experiment"; // Default for regular experiments
            }
        }

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
                ArrayList<String> photoDraws = dataModel.getPhotoDraw();
                photoDraws.add(downloadUrl);
                dataModel.setPhotoDraw(photoDraws);
                // Use student's input typeData (from tvType), not originalTypeDataFromList
                String studentTypeData = binding.tvType.getText().toString();
                if (studentTypeData == null || studentTypeData.trim().isEmpty()) {
                    studentTypeData = "";
                }
                dataModel.setTypeData(studentTypeData);
                Log.d("UPLOAD_TYPE_DATA", "Setting student's typeData: '" + studentTypeData + "' (original from list was: '" + originalTypeDataFromList + "')");
                app.setDataModel(dataModel);

                // Canvas sudah tersimpan ke Storage → aktifkan Lanjutkan
                binding.btnUpload.setEnabled(true);
                if (binding.btnSave != null) {
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setAlpha(1f);
                }

                // Log success
                Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);
                Log.d("Firebase", "Storage path: " + storagePath + ", Source: " + source);
                
                progressDialog.dismiss();
                
                // Save data to Firestore after successful upload
                ProgressDialog saveProgressDialog = new ProgressDialog(GeneratesActivity.this);
                saveProgressDialog.setTitle("Menyimpan Data");
                saveProgressDialog.setMessage("Mohon tunggu...");
                saveProgressDialog.setCancelable(false);
                saveProgressDialog.show();

                String documentId = dataModel.getId();
                if (documentId == null || documentId.isEmpty()) {
                    documentId = String.valueOf(System.currentTimeMillis());
                }
                dataModel.setId(documentId);
                
                // Capture original source info (from question list) - define early for use throughout callback
                String sourceQuestionId = dataModel.getId();
                String sourceCreatorName = dataModel.getCustomerName();
                
                // Continue with save logic
                String table = "record";
                boolean isCustomQuestion = getIntent().getBooleanExtra("isCustomQuestion", false);
                
                if (isCustomQuestionNew) {
                    dataModel.setQuestion(binding.etQuestion.getText().toString());
                    table = "questions";
                } else if (isCustomQuestion) {
                    // For questions from list: keep original owner; only set descriptive text with both users
                    String originalOwnerName = sourceCreatorName;
                    String workerName = SessionManager.getName(this);
                    if (originalOwnerName == null || originalOwnerName.trim().isEmpty()) {
                        originalOwnerName = "Unknown";
                    }
                    dataModel.setDesc(workerName + " Mengerjakan pertanyaan dari " + originalOwnerName + ")");
                }
                app.setDataModel(dataModel);
                
                try {
                    // Final guard before save from this screen as well
                    // Reuse RecordPreviewActivity's enforcement via preview step, but we ensure here too
                    // by validating base64 sizes so we don't pass oversized data forward
                    validateBase64Data();
                } catch (Exception ignore) {
                    // If invalid here, still proceed to preview where stronger enforcement runs
                }
                
                // Ensure worker identity set for history filtering
                dataModel.setIdCustomer(SessionManager.getId(this));
                dataModel.setCustomerName(SessionManager.getName(this));

                // Build progress payload (include base64 for questions collection so images appear when accessed from list)
                // Determine creator name (for question list context)
                String creatorNameForList = "";
                
                if (isCustomQuestion) {
                    // For custom questions, use current user as creator
                    if (isCustomQuestionNew) {
                        // New custom question: use current user name
                        creatorNameForList = SessionManager.getName(this);
                        if (creatorNameForList == null || creatorNameForList.trim().isEmpty()) {
                            creatorNameForList = dataModel.getCustomerName();
                        }
                    } else {
                        // Existing custom question from list: use source creator or current user
                        creatorNameForList = (sourceCreatorName != null && !sourceCreatorName.trim().isEmpty()) ? sourceCreatorName : dataModel.getCustomerName();
                    }
                    if (creatorNameForList == null || creatorNameForList.trim().isEmpty()) {
                        creatorNameForList = "Unknown";
                    }
                }
                
                java.util.Map<String, Object> progressData = new java.util.HashMap<>();
                progressData.put("id", documentId);
                if (isCustomQuestion && sourceQuestionId != null && !sourceQuestionId.isEmpty()) {
                    progressData.put("sourceQuestionId", sourceQuestionId);
                    dataModel.setSourceQuestionId(sourceQuestionId);
                }
                
                progressData.put("idCustomer", dataModel.getIdCustomer());
                progressData.put("customerName", dataModel.getCustomerName());
                
                // Persist creator info for admin (Dibuat Oleh) using multiple keys
                // For custom questions, always set creator fields
                if (isCustomQuestionNew) {
                    progressData.put("dibuatOleh", creatorNameForList);
                    progressData.put("Dibuat Oleh", creatorNameForList);
                    progressData.put("creatorName", creatorNameForList);
                    progressData.put("createdBy", creatorNameForList);
                    
                    // Set admin tracking fields in DataModel
                    dataModel.setCreatorName(creatorNameForList);
                    dataModel.setDibuatOleh(creatorNameForList);
                    dataModel.setCreatedBy(creatorNameForList);
                } else if (isCustomQuestion) {
                    // For existing custom questions, preserve existing creator or set if empty
                    progressData.put("dibuatOleh", creatorNameForList);
                    progressData.put("Dibuat Oleh", creatorNameForList);
                    progressData.put("creatorName", creatorNameForList);
                    progressData.put("createdBy", creatorNameForList);
                    
                    // Set admin tracking fields in DataModel
                    dataModel.setCreatorName(creatorNameForList);
                    dataModel.setDibuatOleh(creatorNameForList);
                    dataModel.setCreatedBy(creatorNameForList);
                }

                // Set status based on context
                if (isCustomQuestion) {
                    // For custom questions: status "berlanjut" (false)
                    dataModel.setFinished(false);

                    // Set custom title for Out Class questions
                    if (dataModel.getTypeData() != null && (dataModel.getTypeData().contains("Out Class") || dataModel.getTypeData().contains("SA3"))) {
                        dataModel.setTypeData("Out - Class (By AI)");
                    }
                    
                    // Remove location fields for custom questions
                    if (isCustomQuestionNew) {
                        dataModel.setLatitude(0.0);
                        dataModel.setLongitude(0.0);
                        dataModel.setLocationName(null);
                    }
                } else {
                    // For completed experiments: status "selesai" (true)
                    dataModel.setFinished(true);
                }

                progressData.put("typeData", dataModel.getTypeData());
                progressData.put("topics", dataModel.getTopics());
                progressData.put("typeQuestion", dataModel.getTypeQuestion());
                progressData.put("question", dataModel.getQuestion());
                progressData.put("desc", dataModel.getDesc());
                progressData.put("photo", dataModel.getPhoto());
                
                // Ensure photoDraw is not null (required for deserialization)
                ArrayList<String> photoDraw = dataModel.getPhotoDraw();
                if (photoDraw == null) {
                    photoDraw = new ArrayList<>();
                    dataModel.setPhotoDraw(photoDraw);
                }
                progressData.put("photoDraw", photoDraw);
                
                // For custom questions (Buat Pertanyaan Sendiri), set location fields to 0.0 (not null)
                // Firestore cannot deserialize null to primitive double, so use 0.0 instead
                if (!isCustomQuestionNew) {
                    progressData.put("latitude", dataModel.getLatitude());
                    progressData.put("longitude", dataModel.getLongitude());
                    progressData.put("locationName", dataModel.getLocationName());
                } else {
                    // Set location fields to 0.0 for custom questions (not null, to avoid deserialization errors)
                    progressData.put("latitude", 0.0);
                    progressData.put("longitude", 0.0);
                    progressData.put("locationName", null);
                    // Also update DataModel to ensure consistency
                    dataModel.setLatitude(0.0);
                    dataModel.setLongitude(0.0);
                }
                
                progressData.put("isFinished", dataModel.getFinished());
                
                // Ensure dateTime is set (required for sorting/display)
                String dateTime = dataModel.getDateTime();
                if (dateTime == null || dateTime.isEmpty()) {
                    dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    dataModel.setDateTime(dateTime);
                }
                progressData.put("dateTime", dateTime);
                
                // For custom questions, remove experiment-related fields
                if (isCustomQuestionNew) {
                    // Custom questions: set views and totalEdit to 0 (not null) for proper display
                    progressData.put("totalEdit", 0);
                    progressData.put("views", 0);
                    progressData.put("experimentPhotoUrl", null);
                    progressData.put("experimentPhotoPath", null);
                    progressData.put("documentationNotes", null);
                    progressData.put("photo", null); // Not needed for questions
                    // storageUserId can be kept for organization
                    if (dataModel.getStorageUserId() != null) {
                        progressData.put("storageUserId", dataModel.getStorageUserId());
                    }
                } else {
                    // For regular records/experiments, keep all fields
                    progressData.put("totalEdit", dataModel.getTotalEdit());
                    progressData.put("views", dataModel.getViews());
                    
                    // Add photo documentation fields
                    progressData.put("photoDocumentation", dataModel.getPhotoDocumentation());
                    progressData.put("photoDocumentationPaths", dataModel.getPhotoDocumentationPaths());
                    progressData.put("notePhotos", dataModel.getNotePhotos());
                    progressData.put("notePhotoPaths", dataModel.getNotePhotoPaths());
                    progressData.put("experimentPhotoUrl", dataModel.getExperimentPhotoUrl());
                    progressData.put("experimentPhotoPath", dataModel.getExperimentPhotoPath());
                    progressData.put("documentationNotes", dataModel.getDocumentationNotes());
                    progressData.put("storageUserId", dataModel.getStorageUserId());
                    progressData.put("lastPhotoUpdate", dataModel.getLastPhotoUpdate());
                    progressData.put("photo", dataModel.getPhoto());
                }
                
                final ProgressDialog finalSaveDialog = saveProgressDialog;
                final String tableForSave = table;
                final java.util.Map<String, Object> progressDataForSave = progressData;
                uploadGeneratedQuestionImagesThenContinue(finalSaveDialog, tableForSave, progressDataForSave, () -> {
                if ("questions".equals(tableForSave)) {
                    try {
                        if (dataModel.getBase64() != null && !dataModel.getBase64().isEmpty()) progressDataForSave.put("base64", compressBase64ForFirestore(dataModel.getBase64()));
                        if (dataModel.getBase64_2() != null && !dataModel.getBase64_2().isEmpty()) progressDataForSave.put("base64_2", compressBase64ForFirestore(dataModel.getBase64_2()));
                        if (dataModel.getBase64_3() != null && !dataModel.getBase64_3().isEmpty()) progressDataForSave.put("base64_3", compressBase64ForFirestore(dataModel.getBase64_3()));
                        if (dataModel.getBase64_4() != null && !dataModel.getBase64_4().isEmpty()) progressDataForSave.put("base64_4", compressBase64ForFirestore(dataModel.getBase64_4()));
                        if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) progressDataForSave.put("base64_5", compressBase64ForFirestore(dataModel.getBase64_5()));
                        
                        // For custom questions: upload photoAnswer to Storage and use URL/path instead of base64
                        if (dataModel.getPhotoAnswer() != null && !dataModel.getPhotoAnswer().isEmpty()) {
                            // Upload photoAnswer to Firebase Storage for all questions (custom and non-custom)
                            uploadPhotoAnswerToStorage(dataModel, tableForSave, progressDataForSave);
                            return; // Exit early, save will be called after upload
                        }
                        
                        if (dataModel.getPhotoAcceleration() != null && !dataModel.getPhotoAcceleration().isEmpty()) progressDataForSave.put("photoAcceleration", compressBase64ForFirestore(dataModel.getPhotoAcceleration()));
                    } catch (Exception ignore) {}
                }

                // Upload photo if exists before saving to Firestore
                // Check for local photo path or base64 data that needs to be uploaded
                String localPhotoPath = dataModel.getLocalPhotoPath();
                String localPhotoBase64 = dataModel.getLocalPhotoBase64();

                if ((localPhotoPath != null && !localPhotoPath.isEmpty()) ||
                    (localPhotoBase64 != null && !localPhotoBase64.isEmpty())) {
                    // We have local photo data to upload
                    if (localPhotoPath != null && !localPhotoPath.isEmpty()) {
                        Log.d("PHOTO_UPLOAD", "Uploading local photo file: " + localPhotoPath);
                        uploadPhotoBeforeSave(dataModel, tableForSave, progressDataForSave);
                    } else if (localPhotoBase64 != null && !localPhotoBase64.isEmpty()) {
                        Log.d("PHOTO_UPLOAD", "Uploading base64 photo data");
                        uploadBase64PhotoBeforeSave(dataModel, tableForSave, progressDataForSave);
                    }
                } else {
                    // No local photo to upload, save directly
                    Log.d("PHOTO_UPLOAD", "No local photo to upload, saving directly");
                    saveToFirestoreWithBackup(tableForSave, progressDataForSave);
                }
                });
            },
            e -> {
                // Handle upload failure
                Log.e("Firebase", "Image upload failed", e);
                progressDialog.dismiss();
                Toast.makeText(this, "Gagal mengunggah gambar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            },
            progress -> {
                // Update progress
                progressDialog.setMessage("Terunggah: " + (int) progress + "%");
            }
        );
    }

    private void fetchAdvancedQuestion(String language, String type) {
        ProgressDialog progressDialog = new ProgressDialog(this); // Replace 'this' with 'requireContext()' if inside a Fragment
        progressDialog.setTitle("");
        progressDialog.setMessage("Memuat...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        String d1Label = prefs.getString("device1_label", "Experiment 1");
        String d2Label = prefs.getString("device2_label", "Experiment 2");
        ApiRequest request = new ApiRequest(language, de.rwth_aachen.phyphox.Helper.SessionManager.getId(this), d1Label, d2Label);

        Callback<ApiResponse> callback = new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    App app = (App) getApplication();
                    dataModel.setQuestion(apiResponse.getQuestions());
                    dataModel.setIdCustomer(SessionManager.getId(GeneratesActivity.this));
                    binding.tvQuestion.setText(apiResponse.getQuestions());

                    // Reset all image views to GONE and clear any previous images
                    binding.iv.setVisibility(View.GONE);
                    binding.iv.setImageDrawable(null);
                    binding.iv2.setVisibility(View.GONE);
                    binding.iv2.setImageDrawable(null);
                    binding.iv3.setVisibility(View.GONE);
                    binding.iv3.setImageDrawable(null);
                    binding.iv4.setVisibility(View.GONE);
                    binding.iv4.setImageDrawable(null);
                    binding.iv5.setVisibility(View.GONE);
                    binding.iv5.setImageDrawable(null);

                    // Handle Graph Images (graphImages -> iv)
                    if (apiResponse.getGraphImages() != null && !apiResponse.getGraphImages().isEmpty()) {
                        try {
                        String originalBase64 = apiResponse.getGraphImages().get(0);
                            if (originalBase64 != null && !originalBase64.trim().isEmpty()) {
                        dataModel.setBase64(compressBase64ForFirestore(originalBase64));
                        Log.d("IMAGE_LOAD", "Loading GraphImage to iv: " + originalBase64.substring(0, Math.min(50, originalBase64.length())));

                                BitmapDrawable drawable = base64ToDrawable(originalBase64, GeneratesActivity.this);
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
                        dataModel.setBase64_2(compressBase64ForFirestore(originalBase64_2));
                        Log.d("IMAGE_LOAD", "Loading Image1 to iv2: " + originalBase64_2.substring(0, Math.min(50, originalBase64_2.length())));
                        loadImageWithGlide(base64ToDrawable(originalBase64_2, GeneratesActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }
                    // Handle Table Images
                    boolean hasTableImages = false;

                    // Handle Table 1 (table_img_base64_1 -> iv3)
                    if (apiResponse.getTable_img_base64_1() != null && !apiResponse.getTable_img_base64_1().isEmpty()) {
                        try {
                        String originalBase64_3 = apiResponse.getTable_img_base64_1();
                            if (originalBase64_3 != null && !originalBase64_3.trim().isEmpty()) {
                                dataModel.setBase64_3(compressBase64ForFirestore(originalBase64_3));
                                // Log base64 length for debugging
                                Log.d("TABLE_IMAGE_DEBUG", "Table 1 base64 length: " + originalBase64_3.length());
                                Log.d("TABLE_IMAGE_DEBUG", "Table 1 base64 preview: " + originalBase64_3.substring(0, Math.min(100, originalBase64_3.length())));

                        Log.d("getTable_img_base64_1 --&> ", "" + originalBase64_3.substring(0, Math.min(50, originalBase64_3.length())));

                                // Try to convert base64 to drawable with better error handling
                                BitmapDrawable drawable = null;
                                try {
                                    drawable = base64ToDrawable(originalBase64_3, GeneratesActivity.this);
                                    Log.d("TABLE_IMAGE_DEBUG", "Table 1 base64ToDrawable result: " + (drawable != null ? "SUCCESS" : "FAILED"));
                                } catch (Exception e) {
                                    Log.e("TABLE_IMAGE_DEBUG", "Error in base64ToDrawable for Table 1: " + e.getMessage());
                                    e.printStackTrace();
                                }

                                if (drawable != null) {
                                    try {
                                    loadImageWithGlide(drawable, binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                                    hasTableImages = true;
                                        Log.d("TABLE_IMAGE_DEBUG", "Table 1 image loaded successfully to iv3");
                                    } catch (Exception e) {
                                        Log.e("TABLE_IMAGE_DEBUG", "Error loading Table 1 image to iv3: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.e("IMAGE_LOAD", "Failed to convert table image 1 to drawable - drawable is null");
                                    // Try alternative loading method
                                    try {
                                        Log.d("TABLE_IMAGE_DEBUG", "Trying alternative loading method for Table 1");
                                        binding.iv3.setImageDrawable(null); // Clear any existing image
                                        binding.iv3.setVisibility(View.GONE); // Hide if failed
                                    } catch (Exception e2) {
                                        Log.e("TABLE_IMAGE_DEBUG", "Alternative loading also failed: " + e2.getMessage());
                                    }
                                }
                            } else {
                                Log.w("TABLE_IMAGE_DEBUG", "Table 1 base64 is empty or null");
                            }
                        } catch (Exception e) {
                            Log.e("IMAGE_LOAD", "Error loading table image 1: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("TABLE_IMAGE_DEBUG", "Table 1 base64 is null or empty in API response");
                    }

                    // Handle Table 2 (table_img_base64_2 -> iv4)
                    if (apiResponse.getTable_img_base64_2() != null && !apiResponse.getTable_img_base64_2().isEmpty()) {
                        try {
                        String originalBase64_4 = apiResponse.getTable_img_base64_2();
                            if (originalBase64_4 != null && !originalBase64_4.trim().isEmpty()) {
                                dataModel.setBase64_4(compressBase64ForFirestore(originalBase64_4));
                                // Log base64 length for debugging
                                Log.d("TABLE_IMAGE_DEBUG", "Table 2 base64 length: " + originalBase64_4.length());
                                Log.d("TABLE_IMAGE_DEBUG", "Table 2 base64 preview: " + originalBase64_4.substring(0, Math.min(100, originalBase64_4.length())));

                        Log.d("getTable_img_base64_2 --&> ", "" + originalBase64_4.substring(0, Math.min(50, originalBase64_4.length())));

                                // Try to convert base64 to drawable with better error handling
                                BitmapDrawable drawable = null;
                                try {
                                    drawable = base64ToDrawable(originalBase64_4, GeneratesActivity.this);
                                    Log.d("TABLE_IMAGE_DEBUG", "Table 2 base64ToDrawable result: " + (drawable != null ? "SUCCESS" : "FAILED"));
                                } catch (Exception e) {
                                    Log.e("TABLE_IMAGE_DEBUG", "Error in base64ToDrawable for Table 2: " + e.getMessage());
                                    e.printStackTrace();
                                }

                                if (drawable != null) {
                                    try {
                                    loadImageWithGlide(drawable, binding.iv4);
                        binding.iv4.setVisibility(View.VISIBLE);
                                    hasTableImages = true;
                                        Log.d("TABLE_IMAGE_DEBUG", "Table 2 image loaded successfully to iv4");
                                    } catch (Exception e) {
                                        Log.e("TABLE_IMAGE_DEBUG", "Error loading Table 2 image to iv4: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.e("IMAGE_LOAD", "Failed to convert table image 2 to drawable - drawable is null");
                                    // Try alternative loading method
                                    try {
                                        Log.d("TABLE_IMAGE_DEBUG", "Trying alternative loading method for Table 2");
                                        binding.iv4.setImageDrawable(null); // Clear any existing image
                                        binding.iv4.setVisibility(View.GONE); // Hide if failed
                                    } catch (Exception e2) {
                                        Log.e("TABLE_IMAGE_DEBUG", "Alternative loading also failed: " + e2.getMessage());
                                    }
                                }
                            } else {
                                Log.w("TABLE_IMAGE_DEBUG", "Table 2 base64 is empty or null");
                            }
                        } catch (Exception e) {
                            Log.e("IMAGE_LOAD", "Error loading table image 2: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("TABLE_IMAGE_DEBUG", "Table 2 base64 is null or empty in API response");
                    }

                    // Handle table_img_base64 (fallback jika table_img_base64_1 kosong)
                    if ((apiResponse.getTable_img_base64_1() == null || apiResponse.getTable_img_base64_1().isEmpty())
                        && apiResponse.getTable_img_base64() != null && !apiResponse.getTable_img_base64().isEmpty()) {
                        try {
                        String originalFallback3 = apiResponse.getTable_img_base64();
                        dataModel.setBase64_3(compressBase64ForFirestore(originalFallback3));
                            Log.d("TABLE_FALLBACK_DEBUG", "Using fallback table image for iv3");
                        Log.d("getTable_img_base64 fallback --&> ", "" + originalFallback3.substring(0, Math.min(50, originalFallback3.length())));

                            // Try to convert fallback base64 to drawable
                            BitmapDrawable fallbackDrawable = null;
                            try {
                                fallbackDrawable = base64ToDrawable(originalFallback3, GeneratesActivity.this);
                                Log.d("TABLE_FALLBACK_DEBUG", "Fallback base64ToDrawable result: " + (fallbackDrawable != null ? "SUCCESS" : "FAILED"));
                            } catch (Exception e) {
                                Log.e("TABLE_FALLBACK_DEBUG", "Error in fallback base64ToDrawable: " + e.getMessage());
                                e.printStackTrace();
                            }

                            if (fallbackDrawable != null) {
                                try {
                                    loadImageWithGlide(fallbackDrawable, binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                                    hasTableImages = true; // Update flag for fallback
                                    Log.d("TABLE_FALLBACK_DEBUG", "Fallback table image loaded successfully to iv3");
                                } catch (Exception e) {
                                    Log.e("TABLE_FALLBACK_DEBUG", "Error loading fallback table image to iv3: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                Log.e("TABLE_FALLBACK_DEBUG", "Failed to convert fallback table image to drawable");
                                binding.iv3.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {
                            Log.e("TABLE_FALLBACK_DEBUG", "Error in fallback table image loading: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("TABLE_FALLBACK_DEBUG", "No fallback table image needed or available");
                    }

                    // Handle local_image_base64 (fallback jika image1_base64 kosong)
                    if ((apiResponse.getImage1_base64() == null || apiResponse.getImage1_base64().isEmpty())
                        && apiResponse.getLocal_image_base64() != null && !apiResponse.getLocal_image_base64().isEmpty()) {
                        String originalFallback2 = apiResponse.getLocal_image_base64();
                        dataModel.setBase64_2(compressBase64ForFirestore(originalFallback2));
                        Log.d("getLocal_image_base64 fallback --&> ", "" + originalFallback2.substring(0, Math.min(50, originalFallback2.length())));
                        loadImageWithGlide(base64ToDrawable(originalFallback2, GeneratesActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }

                    // Handle image2_base64 (baru) -> iv5
                    if (apiResponse.getImage2_base64() != null && !apiResponse.getImage2_base64().isEmpty()) {
                        String originalBase64_5 = apiResponse.getImage2_base64();
                        dataModel.setBase64_5(compressBase64ForFirestore(originalBase64_5));
                        Log.d("getImage2_base64 --&> ", "" + originalBase64_5.substring(0, Math.min(50, originalBase64_5.length())));
                        loadImageWithGlide(base64ToDrawable(originalBase64_5, GeneratesActivity.this), binding.iv5);
                        binding.iv5.setVisibility(View.VISIBLE);
                    }

                    // Show table images container if there are table images - MOVED HERE after all image loading
                    if (hasTableImages) {
                        binding.tableImagesContainer.setVisibility(View.VISIBLE);
                        Log.d("TABLE_CONTAINER_DEBUG", "Table images container set to VISIBLE - hasTableImages: " + hasTableImages);
                    } else {
                        binding.tableImagesContainer.setVisibility(View.GONE);
                        Log.d("TABLE_CONTAINER_DEBUG", "Table images container set to GONE - hasTableImages: " + hasTableImages);
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
                Toast.makeText(GeneratesActivity.this, "Gagal terhubung", Toast.LENGTH_SHORT).show();
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
        try {
        Glide.with(this)
                .load(url)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            try {
                        imageView.setImageDrawable(resource); // Ini yang benar untuk PhotoView agar zoom aktif

                        // Optimize image view size for table images
                        if (imageView.getId() == R.id.iv3 || imageView.getId() == R.id.iv4) {
                            optimizeImageViewForTable(imageView, resource);
                        }

                        // Enhanced zoom for other PhotoView images (excluding table images iv3, iv4)
                        if (imageView instanceof com.github.chrisbanes.photoview.PhotoView) {
                            com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) imageView;

                            try {
                                // CRITICAL: Reset PhotoView state first to clear any existing zoom conflicts
                                photoView.setScale(1.0f, true);

                                // Configure zoom based on image type - set maximum first, then work downward
                                if (imageView.getId() == R.id.iv5) {
                                    // Additional image - moderate zoom
                                    photoView.setMaximumScale(4.0f);
                                    photoView.setMediumScale(2.5f);
                                    photoView.setMinimumScale(0.7f);
                                } else if (imageView.getId() == R.id.ivDraw) {
                                    // Drawing image - high zoom for detail
                                    photoView.setMaximumScale(6.0f);
                                    photoView.setMediumScale(3.0f);
                                    photoView.setMinimumScale(0.5f);
                                } else if (imageView.getId() != R.id.iv3 && imageView.getId() != R.id.iv4) {
                                    // Default zoom for other images, excluding table images (iv3, iv4)
                                    photoView.setMaximumScale(3.0f);
                                    photoView.setMediumScale(2.0f);
                                    photoView.setMinimumScale(0.8f);
                                }
                                // Note: iv3 and iv4 (table images) are handled by optimizeImageViewForTable

                                // Enable smooth zoom transitions
                                photoView.setZoomTransitionDuration(300);

                                // Enable double tap to zoom (PhotoView handles this automatically)
                                photoView.setZoomable(true);

                                Log.d("ZOOM_CONFIG", "LoadImageWithGlide zoom configured successfully for imageView ID " +
                                      imageView.getId() + ": min=" + photoView.getMinimumScale() +
                                      ", medium=" + photoView.getMediumScale() + ", max=" + photoView.getMaximumScale());

                            } catch (Exception e) {
                                Log.e("ZOOM_CONFIG", "Error setting zoom levels in loadImageWithGlide for imageView ID " +
                                      imageView.getId() + ": " + e.getMessage());
                                // Fallback: just enable basic zoom
                                try {
                                    photoView.setZoomable(true);
                                } catch (Exception fallbackError) {
                                    Log.e("ZOOM_CONFIG", "Even basic zoom failed: " + fallbackError.getMessage());
                                }
                            }
                                }

                                Log.d("IMAGE_LOAD", "Image loaded successfully to " + getImageViewName(imageView));

                            } catch (Exception e) {
                                Log.e("IMAGE_LOAD", "Error setting image drawable: " + e.getMessage());
                                // Try fallback method
                                loadImageFallback(url, imageView);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle case when the image is cleared
                    }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            Log.e("IMAGE_LOAD", "Glide load failed for " + getImageViewName(imageView));
                            // Try fallback method
                            loadImageFallback(url, imageView);
                        }
                    });
        } catch (Exception e) {
            Log.e("IMAGE_LOAD", "Error in loadImageWithGlide: " + e.getMessage());
            // Try fallback method
            loadImageFallback(url, imageView);
        }
    }

    /**
     * Fallback method for loading image if Glide fails
     */
    private void loadImageFallback(BitmapDrawable drawable, ImageView imageView) {
        try {
            Log.d("IMAGE_LOAD", "Using fallback method for " + getImageViewName(imageView));
            imageView.setImageDrawable(drawable);

            // Basic zoom configuration for PhotoView
            if (imageView instanceof com.github.chrisbanes.photoview.PhotoView) {
                com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) imageView;
                photoView.setZoomable(true);
                photoView.setMinimumScale(0.5f);
                photoView.setMaximumScale(3.0f);
                Log.d("IMAGE_LOAD", "Fallback zoom configured for " + getImageViewName(imageView));
            }

        } catch (Exception e) {
            Log.e("IMAGE_LOAD", "Fallback method also failed for " + getImageViewName(imageView) + ": " + e.getMessage());
            // Last resort: just show the image without zoom
            try {
                imageView.setImageDrawable(drawable);
            } catch (Exception e2) {
                Log.e("IMAGE_LOAD", "All loading methods failed for " + getImageViewName(imageView) + ": " + e2.getMessage());
            }
        }
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

    private void loadImage(String url, ImageView imageView) {
        String imageViewName = getImageViewName(imageView);
        Log.d("LOAD_IMAGE", "Loading image for " + imageViewName + " - URL length: " + (url != null ? url.length() : 0));

        if (url.contains("http")) {
            Log.d("LOAD_IMAGE", "Loading from URL for " + imageViewName);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                String base64 = imageUrlToBase64(url);

                handler.post(() -> {
                    if (!base64.isEmpty()) {
                        Log.d("LOAD_IMAGE", "URL converted to Base64 for " + imageViewName + " - Length: " + base64.length());
                        loadImageWithGlide(base64ToDrawable(base64, this), imageView);
                    } else {
                        Log.e("LOAD_IMAGE", "Failed to convert URL to base64 for " + imageViewName);
                    }
                });
            });

        } else {
            Log.d("LOAD_IMAGE", "Loading from Base64 for " + imageViewName + " - Length: " + url.length());
            BitmapDrawable drawable = base64ToDrawable(url, GeneratesActivity.this);
            if (drawable != null) {
                Log.d("LOAD_IMAGE", "Base64 converted to drawable successfully for " + imageViewName);
                loadImageWithGlide(drawable, imageView);
            } else {
                Log.e("LOAD_IMAGE", "Failed to convert Base64 to drawable for " + imageViewName);
            }
        }
    }

    private String getImageViewName(ImageView imageView) {
        if (imageView.getId() == R.id.iv) return "iv (Graph 1)";
        if (imageView.getId() == R.id.iv2) return "iv2 (Graph 2)";
        if (imageView.getId() == R.id.iv3) return "iv3 (Table 1)";
        if (imageView.getId() == R.id.iv4) return "iv4 (Table 2)";
        if (imageView.getId() == R.id.iv5) return "iv5 (Additional)";
        if (imageView.getId() == R.id.ivDraw) return "ivDraw (Canvas)";
        return "Unknown ImageView";
    }


    public BitmapDrawable base64ToDrawable(String base64String, Context context) {
        try {
            if (base64String == null || base64String.isEmpty()) {
                Log.e("Base64Error", "Base64 string is null or empty");
                return null;
            }

            Log.d("Base64Debug", "Processing base64 string, length: " + base64String.length());

            // Remove Base64 headers (if any)
            if (base64String.contains(",")) {
                base64String = base64String.split(",")[1];
                Log.d("Base64Debug", "Removed header, new length: " + base64String.length());
            }

            // Remove spaces & newlines
            String cleanBase64 = base64String.replaceAll("\\s+", "").trim();
            Log.d("Base64Debug", "Cleaned base64 length: " + cleanBase64.length());

            // Validate base64 string
            if (cleanBase64.length() == 0) {
                Log.e("Base64Error", "Base64 string is empty after cleaning");
                return null;
            }

            // Check if base64 string is valid (should be divisible by 4)
            if (cleanBase64.length() % 4 != 0) {
                Log.w("Base64Warning", "Base64 string length not divisible by 4, padding might be needed");
                // Try to pad the string
                while (cleanBase64.length() % 4 != 0) {
                    cleanBase64 += "=";
                }
                Log.d("Base64Debug", "Padded base64 length: " + cleanBase64.length());
            }

            // Decode Base64 safely
            byte[] decodedByte = Base64.decode(cleanBase64, Base64.NO_WRAP);
            Log.d("Base64Debug", "Decoded bytes length: " + decodedByte.length);

            // Convert to Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
            if (bitmap == null) {
                Log.e("Base64Error", "Failed to decode bitmap from byte array");
                return null;
            }

            Log.d("Base64Debug", "Bitmap created successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            // Return Drawable
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            Log.d("Base64Debug", "BitmapDrawable created successfully");
            return drawable;

        } catch (IllegalArgumentException e) {
            Log.e("Base64Error", "Invalid Base64 String: " + e.getMessage());
            Log.e("Base64Error", "Base64 string preview: " + (base64String != null ? base64String.substring(0, Math.min(100, base64String.length())) : "null"));
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            Log.e("Base64Error", "Out of memory while processing base64: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Log.e("Base64Error", "Unexpected error processing base64: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void showIntroductionDialog() {
        // Inflate the binding layout manually
        visitStartTime = System.currentTimeMillis();
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

    private void saveProgress() {
        // Method ini dihapus karena duplikat dengan logic save yang sudah ada
        // Photo documentation akan diintegrasikan ke method save yang existing
    }

    /**
     * Upload grafik/tabel generate (base64 di DataModel) ke Storage, isi questionImageUrl/Path, lalu lanjut simpan.
     */
    private void uploadGeneratedQuestionImagesThenContinue(
            final ProgressDialog saveProgressDialog,
            final String table,
            final java.util.Map<String, Object> progressData,
            final Runnable next) {
        final String documentId = dataModel.getId();
        if (documentId == null || documentId.isEmpty()) {
            App app = (App) getApplication();
            QuestionImageFirestoreSync.mergeQuestionImageFieldsIntoMap(dataModel, progressData);
            app.setDataModel(dataModel);
            next.run();
            return;
        }
        final boolean useRecordsPath = "record".equals(table);
        QuestionImageFirestoreSync.uploadQuestionImagesFromDataModel(
                this,
                dataModel,
                documentId,
                useRecordsPath,
                new QuestionImageFirestoreSync.BatchCallback() {
                    @Override
                    public void onComplete() {
                        App app = (App) getApplication();
                        app.setDataModel(dataModel);
                        QuestionImageFirestoreSync.mergeQuestionImageFieldsIntoMap(dataModel, progressData);
                        if (saveProgressDialog != null && saveProgressDialog.isShowing()) {
                            saveProgressDialog.dismiss();
                        }
                        next.run();
                    }

                    @Override
                    public void onFailure(String message) {
                        if (saveProgressDialog != null && saveProgressDialog.isShowing()) {
                            saveProgressDialog.dismiss();
                        }
                        Toast.makeText(GeneratesActivity.this,
                                "Gagal mengunggah gambar soal: " + message,
                                Toast.LENGTH_LONG).show();
                        if (binding.btnSave != null) {
                            binding.btnSave.setEnabled(true);
                            binding.btnSave.setAlpha(1f);
                        }
                        if (binding.btnUpload != null) {
                            binding.btnUpload.setEnabled(true);
                        }
                    }
                });
    }

    /**
     * Upload photo before saving to Firestore
     */
    private void uploadPhotoBeforeSave(DataModel dataModel, String table, java.util.Map<String, Object> progressData) {
        try {
            String photoPath = dataModel.getLocalPhotoPath(); // Use local photo path
            String experimentId = dataModel.getId();
            String experimentType = dataModel.getTypeData();
            
            // Determine source based on question type
            boolean isCustomQuestionNew = getIntent().getBooleanExtra("isCustomQuestionNew", false);
            final String source; // Must be final for use in lambda
            if (isCustomQuestionNew) {
                source = "Buat Pertanyaan Sendiri";
            } else {
                boolean isCustomQuestion = getIntent().getBooleanExtra("isCustomQuestion", false);
                if (isCustomQuestion) {
                    source = "Buat Pertanyaan dengan AI";
                } else {
                    source = "Experiment"; // Default
                }
            }

            StorageUtil.uploadExperimentPhotoWithSource(
                this,
                photoPath,
                experimentId,
                experimentType,
                source, // Source metadata
                (downloadUrl, storagePath) -> {
                    // Update progress data with photo URL and path
                    progressData.put("experimentPhotoUrl", downloadUrl);
                    progressData.put("experimentPhotoPath", storagePath);
                    Log.d("PHOTO_UPLOAD", "Experiment photo uploaded: " + storagePath + ", Source: " + source);

                    // Save to Firestore
                    saveToFirestoreWithBackup(table, progressData);
                },
                e -> {
                    Log.e("PHOTO_UPLOAD", "Failed to upload experiment photo: " + e.getMessage());
                    Toast.makeText(this, "Gagal mengunggah foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    // Save without photo if upload fails
                    saveToFirestoreWithBackup(table, progressData);
                }
            );

        } catch (Exception e) {
            Log.e("PHOTO_UPLOAD", "Error uploading experiment photo: " + e.getMessage());
            Toast.makeText(this, "Terjadi kesalahan mengunggah foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Save without photo if upload fails
            saveToFirestoreWithBackup(table, progressData);
        }
    }

    /**
     * Upload base64 photo before saving to Firestore
     */
    private void uploadBase64PhotoBeforeSave(DataModel dataModel, String table, java.util.Map<String, Object> progressData) {
        try {
            String base64Data = dataModel.getLocalPhotoBase64(); // Use local base64 data
            String experimentId = dataModel.getId();
            String experimentType = dataModel.getTypeData();

            // Convert base64 to bytes and upload
            byte[] imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);

            // Determine source based on question type
            String userId = SessionManager.getId(this);
            String userName = SessionManager.getName(this);
            boolean isCustomQuestionNew = getIntent().getBooleanExtra("isCustomQuestionNew", false);
            final String source; // Must be final for use in lambda
            if (isCustomQuestionNew) {
                source = "Buat Pertanyaan Sendiri";
            } else {
                boolean isCustomQuestion = getIntent().getBooleanExtra("isCustomQuestion", false);
                if (isCustomQuestion) {
                    source = "Buat Pertanyaan dengan AI";
                } else {
                    source = "Experiment"; // Default
                }
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = userId + "_exp_" + experimentId + "_" + experimentType + "_" + timestamp + ".jpg";
            
            StorageUtil.uploadBytesWithUserIdAndMetadata(
                this,
                imageBytes,
                StorageUtil.STORAGE_PATH_EXPERIMENTS,
                fileName,
                source, // Source metadata
                userName, // User name metadata
                (downloadUrl, storagePath) -> {
                    // Update progress data with photo URL and path
                    progressData.put("experimentPhotoUrl", downloadUrl);
                    progressData.put("experimentPhotoPath", storagePath);
                    Log.d("PHOTO_UPLOAD", "Experiment photo uploaded: " + storagePath + ", Source: " + source);

                    // Save to Firestore
                    saveToFirestoreWithBackup(table, progressData);
                },
                e -> {
                    Log.e("PHOTO_UPLOAD", "Failed to upload base64 experiment photo: " + e.getMessage());
                    Toast.makeText(this, "Gagal mengunggah foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    // Save without photo if upload fails
                    saveToFirestoreWithBackup(table, progressData);
                },
                null // OnUploadProgressListener - tidak diperlukan untuk base64 upload
            );

        } catch (Exception e) {
            Log.e("PHOTO_UPLOAD", "Error uploading base64 experiment photo: " + e.getMessage());
            Toast.makeText(this, "Terjadi kesalahan mengunggah foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Save without photo if upload fails
            saveToFirestoreWithBackup(table, progressData);
        }
    }

    /**
     * Upload photoAnswer to Firebase Storage for all questions (custom and non-custom)
     */
    private void uploadPhotoAnswerToStorage(DataModel dataModel, String table, java.util.Map<String, Object> progressData) {
        try {
            String photoAnswerBase64 = dataModel.getPhotoAnswer();
            if (photoAnswerBase64 == null || photoAnswerBase64.isEmpty()) {
                // No photoAnswer to upload, save directly
                saveToFirestoreWithBackup(table, progressData);
                return;
            }
            
            // Convert base64 to bytes
            String base64Data = photoAnswerBase64;
            if (photoAnswerBase64.contains(",")) {
                base64Data = photoAnswerBase64.split(",")[1];
            }
            byte[] imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
            
            // Generate filename with userId
            String userId = SessionManager.getId(this);
            String userName = SessionManager.getName(this);
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            String fileName = userId + "_photo_answer_" + dataModel.getId() + "_" + timestamp + ".jpg";
            
            // Determine source based on question type
            boolean isCustomQuestionNew = getIntent().getBooleanExtra("isCustomQuestionNew", false);
            boolean isCustomQuestion = getIntent().getBooleanExtra("isCustomQuestion", false);
            final String source; // Must be final for use in lambda
            if (isCustomQuestionNew) {
                source = "Buat Pertanyaan Sendiri"; // Default for custom questions
            } else if (isCustomQuestion) {
                source = "Buat Pertanyaan dengan AI"; // For questions from list
            } else {
                source = "Experiment"; // For regular experiments/records
            }
            
            // Upload to Firebase Storage with metadata (userId, source, userName)
            StorageUtil.uploadBytesWithUserIdAndMetadata(
                this,
                imageBytes,
                StorageUtil.STORAGE_PATH_DOCUMENTATION,
                fileName,
                source, // Source metadata
                userName, // User name metadata
                (downloadUrl, storagePath) -> {
                    Log.d("PHOTO_ANSWER_UPLOAD", "Photo answer uploaded successfully: " + downloadUrl);
                    Log.d("PHOTO_ANSWER_UPLOAD", "Storage path: " + storagePath + ", Source: " + source);
                    
                    // Set photoAnswerUrl and photoAnswerPath in progressData
                    progressData.put("photoAnswerUrl", downloadUrl);
                    progressData.put("photoAnswerPath", storagePath);
                    
                    // Remove photoAnswer (base64) from progressData
                    progressData.put("photoAnswer", null);
                    
                    // Also update DataModel
                    dataModel.setPhotoAnswerUrl(downloadUrl);
                    dataModel.setPhotoAnswerPath(storagePath);
                    dataModel.setPhotoAnswer(null); // Clear base64
                    
                    // Save to Firestore
                    saveToFirestoreWithBackup(table, progressData);
                },
                e -> {
                    Log.e("PHOTO_ANSWER_UPLOAD", "Failed to upload photo answer: " + e.getMessage());
                    Toast.makeText(this, "Gagal mengunggah foto jawaban: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Save without photoAnswer if upload fails
                    progressData.put("photoAnswer", null);
                    saveToFirestoreWithBackup(table, progressData);
                },
                null
            );
            
        } catch (Exception e) {
            Log.e("PHOTO_ANSWER_UPLOAD", "Error uploading photo answer: " + e.getMessage());
            Toast.makeText(this, "Error mengunggah foto jawaban: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Save without photoAnswer if error
            progressData.put("photoAnswer", null);
            saveToFirestoreWithBackup(table, progressData);
        }
    }

    /**
     * Save to Firestore (without backup)
     */
    private void saveToFirestoreWithBackup(String table, java.util.Map<String, Object> progressData) {
        // Don't override status - use the one already set in progressData
        // The status should already be correctly set based on context in the calling methods

        // Get documentId from progressData (more reliable than dataModel.getId())
        String documentId = (String) progressData.get("id");
        if (documentId == null || documentId.isEmpty()) {
            // Fallback to dataModel.getId() if not in progressData
            documentId = dataModel.getId();
            if (documentId == null || documentId.isEmpty()) {
                documentId = String.valueOf(System.currentTimeMillis());
                progressData.put("id", documentId);
                dataModel.setId(documentId);
            }
        } else {
            // Ensure dataModel.getId() matches progressData
            dataModel.setId(documentId);
        }

        // Make final for use in lambda
        final String finalTable = table;
        final String finalDocumentId = documentId;

        // Log for debugging
        Log.d("SAVE_TO_FIRESTORE", "Saving to collection: " + finalTable + ", documentId: " + finalDocumentId);
        Log.d("SAVE_TO_FIRESTORE", "ProgressData keys: " + progressData.keySet());
        Log.d("SAVE_TO_FIRESTORE", "ProgressData id: " + progressData.get("id"));
        Log.d("SAVE_TO_FIRESTORE", "ProgressData question: " + progressData.get("question"));
        Log.d("SAVE_TO_FIRESTORE", "ProgressData customerName: " + progressData.get("customerName"));

        // Save to Firestore without backup collection
        FirestoreUtil.addOrUpdateDocument(
            finalTable,
            finalDocumentId,
            progressData,
            () -> {
                // Success callback
                Log.d("SAVE_TO_FIRESTORE", "Successfully saved to " + finalTable + " with documentId: " + finalDocumentId);

                if (finalTable.equals("questions")) {
                    // Custom questions (Buat Pertanyaan Sendiri): langsung ke home
                    Toast.makeText(this, "Pertanyaan berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    try {
                        Intent homeIntent = new Intent(GeneratesActivity.this, MainActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(homeIntent);
                        finish();
                    } catch (Exception ex) {
                        Log.e("SAVE_SUCCESS", "Error navigating to home: " + ex.getMessage());
                        finish();
                    }
                } else {
                    // Regular records: aktifkan Lanjutkan, biarkan user klik sendiri
                    Toast.makeText(this, "✅ Tersimpan! Klik Lanjutkan untuk melanjutkan.", Toast.LENGTH_SHORT).show();
                    if (binding.btnSave != null) {
                        binding.btnSave.setEnabled(true);
                        binding.btnSave.setAlpha(1f);
                    }
                    if (binding.btnUpload != null) {
                        binding.btnUpload.setEnabled(true);
                    }
                }
            },
            e -> {
                // Failure callback
                Log.e("FIRESTORE_SAVE", "Failed to save progress: " + e.getMessage());
                Toast.makeText(this, "Gagal menyimpan progress: " + e.getMessage(), Toast.LENGTH_LONG).show();

                // Re-enable buttons if failed
                if (binding.btnSave != null) {
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setAlpha(1f);
                }
                if (binding.btnUpload != null) {
                    binding.btnUpload.setEnabled(true);
                }
            }
        );
    }
}
