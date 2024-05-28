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

import android.view.MenuInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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

        CheckBox advancedOptionsCheckBox = findViewById(R.id.advancedOptionsCheckBox);
        advancedOptionsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> advancedOptions());

        advancedOptionsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> advancedOptions());

        if (!checkPermission()) {
            requestPermission();
        }
    }

    private void imageSize(){
        EditText imgSizeWidthEditText = findViewById(R.id.imgSizeWidthEditText);
        EditText imgSizeHeightEditText = findViewById(R.id.imgSizeHeightEditText);

        if((imgSizeWidthEditText == null) || (imgSizeHeightEditText == null)) {
            Toast.makeText(this, "width & height is null!", Toast.LENGTH_SHORT).show();
        } else {
            int imgWidth = Integer.parseInt(imgSizeWidthEditText.getText().toString());
            int imgHeight = Integer.parseInt(imgSizeHeightEditText.getText().toString());

            if(DEBUG){Toast.makeText(this, "img width: "+imgWidth+"   img height: "+imgHeight, Toast.LENGTH_SHORT).show();}
        }
    }

    // output folder funcl

    private void advancedOptions(){
        LinearLayout advancedOptionsLinearLayout = findViewById(R.id.advancedOptionsLinearLayout);
        CheckBox advancedOptionsCheckBox = findViewById(R.id.advancedOptionsCheckBox);

        if(advancedOptionsCheckBox.isChecked()){
            advancedOptionsLinearLayout.setVisibility(View.VISIBLE);
        } else {
            advancedOptionsLinearLayout.setVisibility(View.GONE);
        }
    }

    private Integer getImgQuality(){
        EditText qualityEditText = findViewById(R.id.qualityEditText);
        Integer quality = Integer.valueOf(qualityEditText.getText().toString());
        return quality;
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

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webview);
        if (webView.getVisibility() == View.VISIBLE) {
            webView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    public void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.more_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.githubLink) {
                WebView webView = findViewById(R.id.webview);
                webView.setWebViewClient(new WebViewClient());
                webView.loadUrl("https://github.com/ga111o/android-img2webp-converter");
                webView.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });
        popup.show();
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
                bitmap.compress(Bitmap.CompressFormat.WEBP, getImgQuality(), outputStream);
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
