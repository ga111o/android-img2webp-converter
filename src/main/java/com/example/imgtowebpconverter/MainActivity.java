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
import androidx.documentfile.provider.DocumentFile;

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
import java.io.IOException;
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

        if (!checkPermission()) {
            requestPermission();
        }

        Button outputFolderButton = findViewById(R.id.outputFolderButton);
        outputFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, 1);
            }
        });
    }

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
        if(DEBUG){Toast.makeText(this, "DEBUG: onActivityResult ftf", Toast.LENGTH_SHORT).show();}

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imageSize();
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
            if(DEBUG){Toast.makeText(this, "DEBUG: why its not working......", Toast.LENGTH_SHORT).show();}
            selectedFolderUri = data.getData();
            if (selectedFolderUri != null) {
                getContentResolver().takePersistableUriPermission(selectedFolderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                Toast.makeText(this, "folder selected", Toast.LENGTH_SHORT).show();
            }
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

    private Uri selectedFolderUri;

    private Bitmap resizeImage(Bitmap originalImage, float scalePercentage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        int newWidth = (int) (width * scalePercentage / 100);
        int newHeight = (int) (height * scalePercentage / 100);

        return Bitmap.createScaledBitmap(originalImage, newWidth, newHeight, true);
    }

    private void imageSize() {
        EditText imgSizeEditText = findViewById(R.id.imgSizeEditText);
        float scalePercentage;
        try {
            scalePercentage = Float.parseFloat(imgSizeEditText.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
            Bitmap resizedBitmap = resizeImage(bitmap, scalePercentage);
            saveImageToDownloads(resizedBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImageToDownloads(Bitmap bitmap) {
        try {
            if (DEBUG) {Toast.makeText(this, "DEBUG: saveImg2Download func - try", Toast.LENGTH_SHORT).show();}

            if (selectedFolderUri == null) {
                selectedFolderUri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
                Toast.makeText(this, "saved at Download folder", Toast.LENGTH_SHORT).show();
            }

            String[] filePathColumn = {MediaStore.Images.Media.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);
            String imageName = "converted_image";
            if (cursor != null && cursor.moveToFirst()) {
                if (DEBUG) {Toast.makeText(this, "DEBUG: saveImg2Download - try - func", Toast.LENGTH_SHORT).show();}
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imageName = cursor.getString(columnIndex);
                imageName = imageName.substring(0, imageName.lastIndexOf('.')) + ".webp";
                cursor.close();
            }

            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, selectedFolderUri);
            DocumentFile newFile = pickedDir.createFile("image/webp", imageName);

            try (OutputStream outputStream = getContentResolver().openOutputStream(newFile.getUri())) {
                if (DEBUG) {Toast.makeText(this, "DEBUG: saveImg2Download - try - try", Toast.LENGTH_SHORT).show();}
                bitmap.compress(Bitmap.CompressFormat.WEBP, getImgQuality(), outputStream);
                outputStream.flush();
                Toast.makeText(this, "saved at " + newFile.getUri().getPath(), Toast.LENGTH_SHORT).show();
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
