package de.rwth_aachen.phyphox.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import de.rwth_aachen.phyphox.NetworkConnection.ApiRequest;
import de.rwth_aachen.phyphox.NetworkConnection.ApiResponse;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.databinding.ActivityUserGeneratesNewBinding;
import de.rwth_aachen.phyphox.databinding.DialogIntroductionBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import de.rwth_aachen.phyphox.model.QuestionModel;
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
        // Tombol Submit (fungsionalitas belum diimplementasikan)
        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.btnNext.setEnabled(false);
                if (imageBytes.length == 0) {
                    uploadImageToFirestore(convertBitmapToBytes(getViewAsBitmap(binding.drawView)), "answer_image_" + System.currentTimeMillis());
                } else {
                    uploadImageCameraToFirestore(imageBytes, "answer_image_" + System.currentTimeMillis());
                }
                // Implementasi fungsi Submit bisa ditambahkan di sini
            }
        });

        // Tombol Next yang mengarahkan ke FeedbackActivity
        binding.btnNext.setOnClickListener(v -> {
            dataModel.setQuestion(binding.tvQuestion.getText().toString());
            dataModel.setCustomerName(SessionManager.getName(this));
                Log.d("btnsave","--> "+new Gson().toJson(dataModel));
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("Save data to Server");
                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.show();
                FirestoreUtil.addOrUpdateDocument("questions", dataModel.getId(), dataModel,
                        () -> {
                            progressDialog.dismiss();
                            Intent intent =new Intent(UserGeneratesQuestionActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        },
                        e -> {
                            progressDialog.dismiss();
                            Toast.makeText(UserGeneratesQuestionActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        });
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                    finish();
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode== RESULT_OK) {
            binding.ivPhoto.setVisibility(View.VISIBLE);
            // Optional: Konversi foto ke base64 dan simpan ke dataModel
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            loadImageWithGlide(base64ToDrawable(base64Image, UserGeneratesQuestionActivity.this), binding.ivPhoto);

            App app = (App) getApplication();
            DataModel dataModel = app.getDataModel();
//            dataModel.setPhotoAnswer(base64Image);
            app.setDataModel(dataModel);
        }
    }

    public void uploadImageCameraToFirestore(byte[] imageData, String fileName) {
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
                        DataModel dataModel = app.getDataModel();
                        dataModel.setPhoto(downloadUrl);
                        dataModel.setType(binding.tvType.getText().toString());
                        app.setDataModel(dataModel);
                        progressDialog.dismiss();
                        uploadImageToFirestore(convertBitmapToBytes(getViewAsBitmap(binding.drawView)), "answer_image_" + System.currentTimeMillis());

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
                        DataModel dataModel = app.getDataModel();
                        dataModel.setBase64(downloadUrl);
                        app.setDataModel(dataModel);

                        // Enable the save button
                        binding.btnNext.setEnabled(true);
                        binding.btnNext.performClick();
                        // Log success
                        Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);

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
                        dataModel.setBase64(apiResponse.getGraphImages().get(0));
                        Log.d("getGraphImages --&> ", "" + apiResponse.getGraphImages().get(0));
                        loadImageWithGlide(base64ToDrawable(apiResponse.getGraphImages().get(0), UserGeneratesQuestionActivity.this), binding.iv);
                        binding.iv.setVisibility(View.VISIBLE);
                    }
                    // Handle Image (image1_base64 -> iv2)
                    if (apiResponse.getImage1_base64() != null && !apiResponse.getImage1_base64().isEmpty()) {
                        dataModel.setBase64_2(apiResponse.getImage1_base64());
                        Log.d("getImage1_base64 --&> ", "" + apiResponse.getImage1_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getImage1_base64(), UserGeneratesQuestionActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }
                    // Handle Table 1 (table_img_base64_1 -> iv3)
                    if (apiResponse.getTable_img_base64_1() != null && !apiResponse.getTable_img_base64_1().isEmpty()) {
                        dataModel.setBase64_3(apiResponse.getTable_img_base64_1());
                        Log.d("getTable_img_base64_1 --&> ", "" + apiResponse.getTable_img_base64_1());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getTable_img_base64_1(), UserGeneratesQuestionActivity.this), binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                    }
                    // Handle Table 2 (table_img_base64_2 -> iv4)
                    if (apiResponse.getTable_img_base64_2() != null && !apiResponse.getTable_img_base64_2().isEmpty()) {
                        dataModel.setBase64_4(apiResponse.getTable_img_base64_2());
                        Log.d("getTable_img_base64_2 --&> ", "" + apiResponse.getTable_img_base64_2());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getTable_img_base64_2(), UserGeneratesQuestionActivity.this), binding.iv4);
                        binding.iv4.setVisibility(View.VISIBLE);
                    }

                    // (Optional) Handle table_img_base64 (lama) jika masih dipakai untuk iv3
                    if (apiResponse.getTable_img_base64() != null && !apiResponse.getTable_img_base64().isEmpty()) {
                        dataModel.setBase64_3(apiResponse.getTable_img_base64());
                        Log.d("getTable_img_base64 --&> ", "" + apiResponse.getTable_img_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getTable_img_base64(), UserGeneratesQuestionActivity.this), binding.iv3);
                        binding.iv3.setVisibility(View.VISIBLE);
                    }

                    // (Optional) Handle local_image_base64 jika ingin tetap tampilkan di iv2
                    if (apiResponse.getLocal_image_base64() != null && !apiResponse.getLocal_image_base64().isEmpty()) {
                        dataModel.setBase64_2(apiResponse.getLocal_image_base64());
                        Log.d("getLocal_image_base64 --&> ", "" + apiResponse.getLocal_image_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getLocal_image_base64(), UserGeneratesQuestionActivity.this), binding.iv2);
                        binding.iv2.setVisibility(View.VISIBLE);
                    }

                    // Handle image2_base64 (baru) -> iv5
                    if (apiResponse.getImage2_base64() != null && !apiResponse.getImage2_base64().isEmpty()) {
                        dataModel.setBase64_5(apiResponse.getImage2_base64());
                        Log.d("getImage2_base64 --&> ", "" + apiResponse.getImage2_base64());
                        loadImageWithGlide(base64ToDrawable(apiResponse.getImage2_base64(), UserGeneratesQuestionActivity.this), binding.iv5);
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
                        imageView.setBackground(resource); // Set as background
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

}
