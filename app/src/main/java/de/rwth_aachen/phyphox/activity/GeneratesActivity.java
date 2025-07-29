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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.se.omapi.Session;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.FirestoreUtil;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.NetworkConnection.ApiRequest;
import de.rwth_aachen.phyphox.NetworkConnection.ApiResponse;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.databinding.ActivityGeneratesBinding;
import de.rwth_aachen.phyphox.databinding.DialogIntroductionBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        boolean isCustomQuestion = getIntent().getBooleanExtra("isCustomQuestion", false);
        boolean isCustomQuestionNew = getIntent().getBooleanExtra("isCustomQuestionNew", false);
        if (isFromMainMenu) {
            dataModel.setDesc(SessionManager.getName(this) + " Mengerjakan pertanyaan dari Sistem");
            fetchAdvancedQuestion("indonesia", dataModel.getTypeQuestion());
        } else {
            if (isCustomQuestion) {
                dataModel.setCustomerName(SessionManager.getName(this));
                if (isCustomQuestionNew) {
                    binding.etQuestion.setVisibility(View.VISIBLE);
                    binding.tvQuestion.setVisibility(View.GONE);
                    if (!dataModel.getTopics().equals("In Class")) {
                        fetchAdvancedQuestion("indonesia", dataModel.getTypeQuestion());
                    }
                } else {

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    if (!dataModel.getIdCustomer().isEmpty()) {
                        db.collection("user").document(dataModel.getIdCustomer()).get().addOnSuccessListener(userDoc -> {
                            String userName = userDoc.getString("name");
                            dataModel.setDesc(SessionManager.getName(this) + " Mengerjakan pertanyaan dari " + userName);
                        }).addOnFailureListener(e -> {

                        });
                    }


                    if (!dataModel.getBase64().isEmpty()) {
                        loadImage(dataModel.getBase64(), binding.iv);
                        binding.iv.setVisibility(View.VISIBLE);
                    }
                    if (!dataModel.getBase64_2().isEmpty()) {
                        loadImage(dataModel.getBase64_2(), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }
                    if (!dataModel.getBase64_3().isEmpty()) {
                        loadImage(dataModel.getBase64_3(), binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                    }
                    if (!dataModel.getBase64_4().isEmpty()) {
                        loadImage(dataModel.getBase64_4(), binding.iv4);
                        binding.iv4.setVisibility(View.VISIBLE);
                    }
                    if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                        loadImage(dataModel.getBase64_5(), binding.iv5);
                        binding.iv5.setVisibility(View.VISIBLE);
                    }
                    if (!dataModel.getPhoto().isEmpty()) {
                        loadImage(dataModel.getPhoto(), binding.ivPhoto);
                        binding.ivPhoto.setVisibility(View.VISIBLE);
                    }

                    binding.tvQuestion.setText(dataModel.getQuestion());
                    binding.tvType.setText(dataModel.getTypeData());
                }

            } else {
                int totalEdit =dataModel.getTotalEdit()+1;
                dataModel.setTotalEdit(totalEdit);
                binding.tvType.setText(dataModel.getTypeData());
                binding.tvQuestion.setText(dataModel.getQuestion());
                if(!dataModel.getPhotoDraw().isEmpty()){
                    binding.ivDraw.setVisibility(View.VISIBLE);
                    loadImage(dataModel.getPhotoDraw().get(dataModel.getPhotoDraw().size()-1), binding.ivDraw);
                }
                // Handle Graph Images
                if (!dataModel.getBase64().isEmpty()) {
                    loadImageWithGlide(base64ToDrawable(dataModel.getBase64(), GeneratesActivity.this), binding.iv);
//                binding.iv.setBackground(base64ToDrawable(dataModel.getBase64(), GeneratesActivity.this));
                    binding.iv.setVisibility(View.VISIBLE);
                }
                if (!dataModel.getBase64().isEmpty()) {
                    loadImage(dataModel.getBase64(), binding.iv);
                    binding.iv.setVisibility(View.VISIBLE);
                }
                if (!dataModel.getBase64_2().isEmpty()) {
                    loadImage(dataModel.getBase64_2(), binding.iv2);
                    binding.iv2.setVisibility(View.VISIBLE);
                }
                if (!dataModel.getBase64_3().isEmpty()) {
                    loadImage(dataModel.getBase64_3(), binding.iv3);
                    binding.iv3.setVisibility(View.VISIBLE);
                }
                if (!dataModel.getBase64_4().isEmpty()) {
                    loadImage(dataModel.getBase64_4(), binding.iv4);
                    binding.iv4.setVisibility(View.VISIBLE);
                }
                if (dataModel.getBase64_5() != null && !dataModel.getBase64_5().isEmpty()) {
                    loadImage(dataModel.getBase64_5(), binding.iv5);
                    binding.iv5.setVisibility(View.VISIBLE);
                }
                if (!dataModel.getPhoto().isEmpty()) {
                    loadImage(dataModel.getPhoto(), binding.ivPhoto);
                    binding.ivPhoto.setVisibility(View.VISIBLE);
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
        // Tombol Submit (fungsionalitas belum diimplementasikan)
        // Tombol Next yang mengarahkan ke FeedbackActivity
        binding.btnSave.setOnClickListener(v -> {
            binding.btnSave.setEnabled(false);

            // Upload image terlebih dahulu (dari drawView atau imageBytes)
            if (imageBytes == null) {
                uploadImageToFirestore(convertBitmapToBytes(getViewAsBitmap(binding.llDraw)), "answer_image_" + System.currentTimeMillis());
            } else {
                uploadImageToFirestoreAnswer(imageBytes, "answer_image_" + System.currentTimeMillis());
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
                if (getIntent().getBooleanExtra("isFromMainMenu", true)) {
                    // Simpan progress ke Firestore tanpa ProgressDialog
                    String table = "record";
                    if (isCustomQuestionNew) {
                        dataModel.setQuestion(binding.etQuestion.getText().toString());
                        table = "questions";
                    }
                    dataModel.setTypeData(binding.tvType.getText().toString());
                    FirestoreUtil.addOrUpdateDocument(table, dataModel.getId(), dataModel,
                            () -> {
                                // Setelah simpan, langsung ke homepage
                                Intent homeIntent = new Intent(GeneratesActivity.this, MainActivity.class);
                                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(homeIntent);
                                finish();
                            },
                            e -> {
                                // Jika gagal, tetap kembali ke homepage (atau bisa tampilkan Toast jika mau)
                                Intent homeIntent = new Intent(GeneratesActivity.this, MainActivity.class);
                                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(homeIntent);
                                finish();
                            }
                    );
                } else {
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

        // Uncomment dan perbaiki package visibility check
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            Log.d("btnTakePhoto", "NO camera app available");
            return;
        }

        Log.d("btnTakePhoto", "takePictureIntent");

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
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
                binding.ivPhoto.setVisibility(View.VISIBLE);

                // Cek apakah file exists
                if (currentPhotoPath != null && new File(currentPhotoPath).exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    if (bitmap != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        imageBytes = baos.toByteArray();
                        String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                        BitmapDrawable drawable = base64ToDrawable(base64Image, GeneratesActivity.this);
                        if (drawable != null) {
                            loadImageWithGlide(drawable, binding.ivPhoto);
                            App app = (App) getApplication();
                            dataModel.setPhotoAnswer(base64Image);
                            app.setDataModel(dataModel);
                        }
                    } else {
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("ImageProcess", "Error processing image: " + e.getMessage());
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void uploadImageToFirestoreAnswer(byte[] imageData, String fileName) {
        // Create and configure ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this); // Replace 'this' with 'requireContext()' if inside a Fragment
        progressDialog.setTitle("Uploading Image Camera");
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
                        dataModel.setPhotoAnswer(downloadUrl);
                        dataModel.setTypeData(binding.tvType.getText().toString());
                        app.setDataModel(dataModel);
                        progressDialog.dismiss();
                        uploadImageToFirestore(convertBitmapToBytes(getViewAsBitmap(binding.llDraw)), "answer_image_" + System.currentTimeMillis());

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


    public Bitmap getViewAsBitmap(View view) {
        // Gunakan Canvas untuk Android 11+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        } else {
            // Fallback untuk versi lama
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            return bitmap;
        }
    }

    public byte[] convertBitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); // Reduce quality
        return byteArrayOutputStream.toByteArray();
    }

    public void uploadImageToFirestore(byte[] imageData, String fileName) {
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
                        ArrayList<String> photoDraws= dataModel.getPhotoDraw();
                        photoDraws.add(downloadUrl);
                        dataModel.setPhotoDraw(photoDraws);
                        dataModel.setTypeData(binding.tvType.getText().toString());
                        app.setDataModel(dataModel);

                        // Enable the save button
                        binding.btnSave.setEnabled(true);

                        // Log success
                        Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);
                        boolean isCustomQuestionNew = getIntent().getBooleanExtra("isCustomQuestionNew", false);
                        // Save data to Firestore after successful upload
                        ProgressDialog saveProgressDialog = new ProgressDialog(GeneratesActivity.this);
                        saveProgressDialog.setTitle("Save data to Server");
                        saveProgressDialog.setMessage("Please wait...");
                        saveProgressDialog.setCancelable(false);
                        saveProgressDialog.show();

                        String documentId = dataModel.getId();
                        if (documentId == null || documentId.isEmpty()) {
                            documentId = String.valueOf(System.currentTimeMillis());
                        }
                        dataModel.setId(documentId);
                        String table = "record";
                        if (isCustomQuestionNew) {
                            dataModel.setQuestion(binding.etQuestion.getText().toString());
                            table = "questions";
                        }
                        app.setDataModel(dataModel);
                        FirestoreUtil.addOrUpdateDocument(table, documentId, dataModel,
                                () -> {
                                    saveProgressDialog.dismiss();
                                    progressDialog.dismiss();
                                    if (isCustomQuestionNew) {
                                        Intent intent = new Intent(GeneratesActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        startActivity(new Intent(GeneratesActivity.this, MapsActivity.class));
                                        finish();
                                    }

                                },
                                e -> {
                                    saveProgressDialog.dismiss();
                                    progressDialog.dismiss();
                                    Toast.makeText(GeneratesActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    binding.btnSave.setEnabled(true); // Re-enable button if failed
                                });

                        // Dismiss the progress dialog
                        progressDialog.dismiss();
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
                    dataModel.setQuestion(apiResponse.getQuestions());
                    dataModel.setIdCustomer(SessionManager.getId(GeneratesActivity.this));
                    binding.tvQuestion.setText(apiResponse.getQuestions());

                    // Reset all image views to GONE at the start
                    binding.iv.setVisibility(View.GONE);
                    binding.iv2.setVisibility(View.GONE);
                    binding.iv3.setVisibility(View.GONE);
                    binding.iv4.setVisibility(View.GONE);
                    binding.iv5.setVisibility(View.GONE);

                    // Handle Graph Images (graphImages -> iv)
                    if (apiResponse.getGraphImages() != null && !apiResponse.getGraphImages().isEmpty()) {
                        dataModel.setBase64(apiResponse.getGraphImages().get(0));
                        Log.d("getGraphImages --&> ", "" + apiResponse.getGraphImages().get(0));
                        loadImageWithGlide(base64ToDrawable(apiResponse.getGraphImages().get(0), GeneratesActivity.this), binding.iv);
                        binding.iv.setVisibility(View.VISIBLE);
                    }
                    // Handle Image (image1_base64 -> iv2)
                    if (apiResponse.getImage1_base64() != null && !apiResponse.getImage1_base64().isEmpty()) {
                        dataModel.setBase64_2(apiResponse.getImage1_base64());
                        Log.d("getImage1_base64 --&> ", "" + apiResponse.getImage1_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getImage1_base64(), GeneratesActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }
                    // Handle Table 1 (table_img_base64_1 -> iv3)
                    if (apiResponse.getTable_img_base64_1() != null && !apiResponse.getTable_img_base64_1().isEmpty()) {
                        dataModel.setBase64_3(apiResponse.getTable_img_base64_1());
                        Log.d("getTable_img_base64_1 --&> ", "" + apiResponse.getTable_img_base64_1());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getTable_img_base64_1(), GeneratesActivity.this), binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                    }
                    // Handle Table 2 (table_img_base64_2 -> iv4)
                    if (apiResponse.getTable_img_base64_2() != null && !apiResponse.getTable_img_base64_2().isEmpty()) {
                        dataModel.setBase64_4(apiResponse.getTable_img_base64_2());
                        Log.d("getTable_img_base64_2 --&> ", "" + apiResponse.getTable_img_base64_2());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getTable_img_base64_2(), GeneratesActivity.this), binding.iv4);
                        binding.iv4.setVisibility(View.VISIBLE);
                    }

                    // (Optional) Handle table_img_base64 (lama) jika masih dipakai untuk iv3
                    if (apiResponse.getTable_img_base64() != null && !apiResponse.getTable_img_base64().isEmpty()) {
                        dataModel.setBase64_3(apiResponse.getTable_img_base64());
                        Log.d("getTable_img_base64 --&> ", "" + apiResponse.getTable_img_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getTable_img_base64(), GeneratesActivity.this), binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                    }

                    // (Optional) Handle local_image_base64 jika ingin tetap tampilkan di iv2
                    if (apiResponse.getLocal_image_base64() != null && !apiResponse.getLocal_image_base64().isEmpty()) {
                        dataModel.setBase64_2(apiResponse.getLocal_image_base64());
                        Log.d("getLocal_image_base64 --&> ", "" + apiResponse.getLocal_image_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getLocal_image_base64(), GeneratesActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }

                    // Handle image2_base64 (baru) -> iv5
                    if (apiResponse.getImage2_base64() != null && !apiResponse.getImage2_base64().isEmpty()) {
                        dataModel.setBase64_5(apiResponse.getImage2_base64());
                        Log.d("getImage2_base64 --&> ", "" + apiResponse.getImage2_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getImage2_base64(), GeneratesActivity.this), binding.iv5);
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
                Toast.makeText(GeneratesActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
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
                        imageView.setImageDrawable(resource); // Ini yang benar untuk PhotoView agar zoom aktif
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle case when the image is cleared
                    }
                });
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
        if (url.contains("http")) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                String base64 = imageUrlToBase64(url);

                handler.post(() -> {
                    if (!base64.isEmpty()) {
                        loadImageWithGlide(base64ToDrawable(base64, this), imageView);
                    } else {
                        Log.e("loadImage", "Failed to convert image to base64");
                    }
                });
            });

        } else {
            loadImageWithGlide(base64ToDrawable(url, GeneratesActivity.this), imageView);
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
}
