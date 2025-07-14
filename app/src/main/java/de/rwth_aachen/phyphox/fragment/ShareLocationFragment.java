package de.rwth_aachen.phyphox.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Camera;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.ExperimentList;
import de.rwth_aachen.phyphox.Helper.SessionManager;
import de.rwth_aachen.phyphox.NetworkConnection.ApiResponse;
import de.rwth_aachen.phyphox.NetworkConnection.ApiService;
import de.rwth_aachen.phyphox.NetworkConnection.RetrofitClient;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.activity.MapsActivity;
import de.rwth_aachen.phyphox.databinding.FragmentShareLocationBinding;
import de.rwth_aachen.phyphox.model.DataModel;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import org.json.JSONObject;


public class ShareLocationFragment extends Fragment {

    private static final String TAG = "ShareLocationFragment";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private File photoFile;
    private FragmentShareLocationBinding binding;
    private ImageCapture imageCapture;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Bitmap bitmap;
    String downloadUrl = "";
    ProgressDialog progressDialog;
    private SensorManager sensorManager;
    private int deviceRotation = 0;
    private SensorEventListener sensorEventListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShareLocationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressDialog = new ProgressDialog(requireContext());
        binding.btnSave.setOnClickListener(v -> {
            App app = (App) requireActivity().getApplication();
            DataModel dataModel = app.getDataModel();
            if (!dataModel.getPhoto().isEmpty()) {
                startActivity(new Intent(getActivity(), ExperimentList.class));
            } else {
                Toast.makeText(getActivity(), "Upload Photo First", Toast.LENGTH_LONG).show();
            }
        });
        binding.btnUpload.setOnClickListener(v -> {
            binding.btnUpload.setEnabled(false);
            progressDialog.setTitle("Uploading Image");
            progressDialog.setMessage("Please wait while the image is being uploaded...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            binding.btnSave.setEnabled(false);
            uploadImageToFirestore(convertBitmapToBytes(bitmap), "capture_image_" + System.currentTimeMillis());

        });
        // Initialize the PreviewView
        previewView = binding.previewView;

        // Set up the click listener for the capture button
        binding.captureButton.setOnClickListener(v -> takePhoto());

        // Check and request camera permission
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        // Hide the captured image view initially
        binding.capturedImageView.setVisibility(View.VISIBLE);
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorEventListener = new SensorEventListener() {
            float[] gravity;
            float[] geomagnetic;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    gravity = event.values;
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = event.values;
                }

                if (gravity != null && geomagnetic != null) {
                    float[] R = new float[9];
                    float[] I = new float[9];
                    if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        float[] orientation = new float[3];
                        SensorManager.getOrientation(R, orientation);

                        float pitch = (float) Math.toDegrees(orientation[1]); // Up/Down
                        float roll = (float) Math.toDegrees(orientation[2]); // Left/Right

                        if (Math.abs(pitch) < 45) { // Jika miring ke samping
                            if (roll > 45) {
                                deviceRotation = 90;  // Rotasi ke kanan
                            } else if (roll < -45) {
                                deviceRotation = 270; // Rotasi ke kiri
                            } else {
                                deviceRotation = 0;   // Normal portrait
                            }
                        } else if (pitch > 45) {
                            deviceRotation = 180; // Terbalik
                        }
                    }
                    Log.d("onsensorchanged ","--> "+deviceRotation);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

// Daftarkan sensor listener
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);

    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void startCamera() {
        // Show the PreviewView and capture button
        previewView.setVisibility(View.VISIBLE);
        binding.captureButton.setVisibility(View.VISIBLE);

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreviewAndImageCapture(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreviewAndImageCapture(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        imageCapture = new ImageCapture.Builder().build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        // Unbind use cases before rebinding
        cameraProvider.unbindAll();

        try {
            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                new File(requireContext().getFilesDir(), "temp_photo.jpg")
        ).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        photoFile = new File(requireContext().getFilesDir(), "temp_photo.jpg");
//                        bitmap = rotateImageIfRequired(BitmapFactory.decodeFile(photoFile.getAbsolutePath()), photoFile);
                        bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                        // Putar gambar sesuai dengan orientasi perangkat
                        bitmap = rotateImage(bitmap, deviceRotation);

                        requireActivity().runOnUiThread(() -> {
                            binding.previewView.setVisibility(View.GONE);
                            binding.capturedImageView.setVisibility(View.VISIBLE);
                            binding.captureButton.setVisibility(View.GONE);
//                            binding.capturedImageView.setImageBitmap(bitmap);
                            binding.capturedImageView.setBackground(new BitmapDrawable(getResources(), bitmap));

                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                    }
                }
        );
    }


    // Add this helper method
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showPermissionDeniedMessage("Camera");
            }
        }
    }

    private void showPermissionDeniedMessage(String permission) {
        new AlertDialog.Builder(requireContext()).setTitle(permission + " Permission Denied").setMessage("This feature requires " + permission + " permission to function. Please grant it in settings.").setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
    }

    public byte[] convertBitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // Compress as PNG or JPEG
        return baos.toByteArray();
    }

    public void uploadImageToFirestore(byte[] imageData, String fileName) {
        // Create and configure ProgressDialog


        // Get Firebase Storage instance
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to the image
        StorageReference imageRef = storageRef.child("images/" + fileName);

        // Upload the image
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnProgressListener(snapshot -> {
            // Update the ProgressDialog with the upload progress
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressDialog.setMessage("Uploaded: " + (int) progress + "%");
        }).addOnSuccessListener(taskSnapshot -> {
            // Get the download URL
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Hide ProgressDialog
                downloadUrl = uri.toString();


                // Save download URL or perform other tasks
                App app = (App) requireActivity().getApplication();
                DataModel dataModel = app.getDataModel();
                dataModel.setIdCustomer(SessionManager.getId(requireActivity()));
                dataModel.setPhoto(downloadUrl);
                Log.d("dataModel", "Image uploaded  " + dataModel.getPhotoDraw());
                Log.d("dataModel", "Image uploaded  " + SessionManager.getId(requireActivity()));
                uploadImage(photoFile);
                Log.d("Firebase", "Image uploaded successfully: " + downloadUrl);
            });
        }).addOnFailureListener(e -> {
            // Handle upload failure
            progressDialog.dismiss();
            Log.e("Firebase", "Image upload failed", e);
            Toast.makeText(requireContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    private void uploadImage(File file) {
        // Create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        // Initialize your API service using your Retrofit client
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), SessionManager.getId(requireContext()));
        // Enqueue the call asynchronously
        apiService.uploadImage(body, userIdBody).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    Log.d(TAG, "Upload successful: " + apiResponse.getMessage());
                    Toast.makeText(requireContext(), "Upload successful: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        // Ambil error body dan parse jadi JSON
                        String errorBody = response.errorBody().string();
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String errorMessage = jsonObject.optString("error", "Unknown error");

                        Toast.makeText(requireContext(), "Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Upload failed: " + errorMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Upload failed: Unknown error", Toast.LENGTH_LONG).show();
                    }
                }
                binding.btnSave.setEnabled(true);
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                binding.btnSave.setEnabled(true);
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Image upload API failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Upload error: " + t.getMessage());
            }
        });
    }
    private void loadImageWithGlide(String url) {
        Glide.with(this)
                .load(url)
                .into(binding.capturedImageView);
    }
    private Bitmap rotateImageIfRequired(Bitmap img, File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath()); // Compatible with API 21+
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d("rotateImageIfRequired","--> "+orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading Exif data", e);
            return img;
        }
    }
    private Bitmap fixImageOrientation(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotationDegrees = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    break;
            }

            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } else {
                return bitmap;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fixing image orientation", e);
            return BitmapFactory.decodeFile(imagePath);
        }
    }
//    private Bitmap rotateImage(Bitmap img, int degree) {
//        Matrix matrix = new Matrix();
//        matrix.postRotate(degree);
//        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
//    }
    private Bitmap rotateImage(Bitmap img, int degree) {
        if (degree == 0) return img; // Tidak perlu rotasi jika 0 derajat

        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }
}
