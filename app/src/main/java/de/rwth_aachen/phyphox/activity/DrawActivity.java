package de.rwth_aachen.phyphox.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;

import de.rwth_aachen.phyphox.App;
import de.rwth_aachen.phyphox.Helper.DrawingView;
import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.model.DataModel;

public class DrawActivity extends AppCompatActivity {

    private DrawingView mDrawingView;
    private FloatingActionButton fab_next;
    private EditText inputValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        // Initialize DrawingView
        mDrawingView = new DrawingView(this);

        // Add DrawingView to the layout
        LinearLayout imageScreenshot = findViewById(R.id.iv_screenshot);
        LinearLayout mDrawingPad = findViewById(R.id.view_drawing_pad);
        FloatingActionButton fab_next = findViewById(R.id.fab_next);
        RelativeLayout rl = findViewById(R.id.rlView);
        inputValue = findViewById(R.id.input_value);
        mDrawingPad.addView(mDrawingView);

        // Get the image from the intent and set it as background
        Intent intent = getIntent();
        String uri = intent.getStringExtra("Uri Image");
        Log.d("DRAW ACTIVITY", "onCreate: " + uri);
        File file = new File(getRealPathFromURI(Uri.parse(uri)));
        Drawable d = Drawable.createFromPath(file.getAbsolutePath());
        imageScreenshot.setBackground(d);

        // Set up buttons for undo, redo, clear, calculator, and next
        FloatingActionButton btnUndo = findViewById(R.id.fab_undo);
        FloatingActionButton btnRedo = findViewById(R.id.fab_redo);
        FloatingActionButton btnClear = findViewById(R.id.fab_clear);
        FloatingActionButton btnCalculator = findViewById(R.id.fab_calculator);
        FloatingActionButton btnNext = findViewById(R.id.fab_next);

        // Set click listeners for buttons
        btnUndo.setOnClickListener(v -> mDrawingView.undo());
        btnRedo.setOnClickListener(v -> mDrawingView.redo());
        btnClear.setOnClickListener(v -> mDrawingView.clear());

        // Handle Calculator button click - Launch custom calculator
        btnCalculator.setOnClickListener(v -> {
            Intent calculatorIntent = new Intent(DrawActivity.this, CalculatorActivity.class);
            startActivity(calculatorIntent);
        });

        // Navigate to the next activity on Next button click
        btnNext.setOnClickListener(v -> startActivity(new Intent(this, QuestionActivity.class)));
        fab_next.setOnClickListener(v -> {
//            startActivity(new Intent(this, QuestionActivity.class));
//
            if (!inputValue.getText().toString().isEmpty()) {
                uploadImageToFirestore(convertBitmapToBytes(getViewAsBitmap(imageScreenshot)), "question_image_" + System.currentTimeMillis());
            } else {
                Toast.makeText(this, "Please Input value", Toast.LENGTH_SHORT).show();
            }
        });

        // Tambahkan logic untuk icon info (introduction)
        ImageView ivInfo = findViewById(R.id.ivSign);
        ivInfo.setOnClickListener(v -> showIntroductionDialog());
    }

    private void showIntroductionDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_introduction, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        // Tombol tutup
        dialogView.findViewById(R.id.tvClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Helper method to get the real file path from a URI
    private String getRealPathFromURI(Uri contentURI) {
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // Compress as PNG or JPEG
        return baos.toByteArray();
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
                        dataModel.setPhotoAcceleration(downloadUrl);
                        dataModel.setValueAcceleration(inputValue.getText().toString());
                        app.setDataModel(dataModel);
                        startActivity(new Intent(this, QuestionActivity.class));
                        // Enable the save button

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

}




