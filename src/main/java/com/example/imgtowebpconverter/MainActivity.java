package com.example.imgtowebpconverter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private Uri selectedImageUri;

    boolean DEBUG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectImgButton = findViewById(R.id.select_img_button);
        selectImgButton.setOnClickListener(v -> openImageChooser());

        if (!checkPermission()) {
            requestPermission();
        }
    }

    private void openImageChooser() {
        if(DEBUG){Toast.makeText(this, "DEBUG: openImgChooser", Toast.LENGTH_SHORT).show();}

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "select picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            saveImageToDownloads();
        }
    }

    private void saveImageToDownloads() {
        try {
            if(DEBUG){Toast.makeText(this, "DEBUG: saveImg2Download func - try", Toast.LENGTH_SHORT).show();}

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            String[] filePathColumn = {MediaStore.Images.Media.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);
            String imageName = "converted_image";
            if (cursor != null && cursor.moveToFirst()) {
                if(DEBUG){Toast.makeText(this, "DEBUG: saveImg2Download - try - func", Toast.LENGTH_SHORT).show();}
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imageName = cursor.getString(columnIndex);
                imageName = imageName.substring(0, imageName.lastIndexOf('.')) + ".webp";
                cursor.close();
            }

            File imageFile = new File(downloadsFolder, imageName);

            // it shows scary message `Call requires API level 26` but not necessary, working well!
            try (OutputStream outputStream = Files.newOutputStream(imageFile.toPath())) {
                if(DEBUG){Toast.makeText(this, "DEBUG: saveImg2Download - try - try", Toast.LENGTH_SHORT).show();}
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream);
                outputStream.flush();
                Toast.makeText(this, "done!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkPermission() {
        if(DEBUG){Toast.makeText(this, "DEBUG: checkPermission", Toast.LENGTH_SHORT).show();}

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
