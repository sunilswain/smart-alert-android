package com.unipi.projects.smartalert;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.location.Location;
import android.location.LocationManager;
import android.util.Base64;
import android.location.LocationListener;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.unipi.projects.smartalert.Services.Events.EventResult;
import com.unipi.projects.smartalert.Services.Events.EventService;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private LocationManager _locationManager;
    private LocationListener _locationListener;

    private Double _lat;
    private Double _long;

    private static final int PICK_IMAGE_REQUEST = 1;
    private String base64Image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup for selecting an image
        Button selectImageButton = findViewById(R.id.selectImageButton);
        selectImageButton.setOnClickListener(v -> openImageChooser());

        // Getting data from intent
        String email = getIntent().getStringExtra("email");
        String userId = getIntent().getStringExtra("userId");

        setupSpinnerLangSelection();
        onSpinnerChanged();
        setupSpinnerSelection();
        locationRequest();

        _locationListener = setLocationListener();
        _locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        EditText editTextMultiLine = findViewById(R.id.editTextMultiLine);
        Button sendEventButton = findViewById(R.id.sendEventButton);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            _locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, _locationListener);
            Log.i("MOT", "ENVOKED locationManager");
        }

        sendEventButton.setOnClickListener(view -> {
            Spinner spinner = findViewById(R.id.selectTypeSpinner);
            String selectedType = spinner.getSelectedItem().toString();

            if (_lat == null || _long == null) {
                Log.e("LocationError", "Latitude or Longitude is null.");
                Toast.makeText(getApplicationContext(), "Location not available.", Toast.LENGTH_SHORT).show();
                return; // Exit early
            }

            Single<EventResult> singleEventResult;
            EventService eventService = new EventService();

            if (editTextMultiLine.getText().toString().isEmpty()) {
                singleEventResult = eventService.SendEvent(selectedType, _lat.toString(), _long.toString(), userId);
            } else {
                String comment = editTextMultiLine.getText().toString();
                singleEventResult = eventService.SendEvent(selectedType, _lat.toString(), _long.toString(), comment, userId);
            }

            singleEventResultSubscription(singleEventResult);
            Toast.makeText(getApplicationContext(), R.string.location_sent_successfully, Toast.LENGTH_SHORT).show();
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                base64Image = encodeImage(bitmap); // Convert to Base64
                // Optionally display the image in an ImageView
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void setupSpinnerSelection() {
        Spinner spinner = findViewById(R.id.selectTypeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void locationRequest() {
        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            Button sendEventButton = findViewById(R.id.sendEventButton);
            sendEventButton.setClickable(isGranted);
            sendEventButton.setLongClickable(isGranted);

            if (!isGranted) {
                String title = getResources().getString(R.string.declined_location_request_title);
                String message = getResources().getString(R.string.declined_location_request_message);
                showMessage(title, message);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(title)
                .setMessage(message)
                .show();
    }

    private LocationListener setLocationListener() {
        return new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                _lat = location.getLatitude();
                _long = location.getLongitude();
            }

            @Override
            public void onFlushComplete(int requestCode) {}
            @Override
            public void onProviderEnabled(@NonNull String provider) {}
            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        };
    }

    private void singleEventResultSubscription(@NonNull Single<EventResult> singleEventResult) {
        singleEventResult.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<EventResult>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {}

                    @Override
                    public void onSuccess(EventResult eventResult) {
                        if (eventResult != null) {
                            Log.i("TAG", "Event sent successfully: " + eventResult.toString());
                        } else {
                            Log.i("TAG", "EventResult is null");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e("BRO", e.getMessage());
                    }
                });
    }

    private void setupSpinnerLangSelection() {
        Spinner spinner = findViewById(R.id.selectLangSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.lang_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void onSpinnerChanged() {
        Spinner spinner = findViewById(R.id.selectLangSpinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (adapterView.getSelectedItemPosition() == 1) {
                    changeLocale("en");
                } else if (adapterView.getSelectedItemPosition() == 2) {
                    changeLocale("el");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void changeLocale(String language) {
        Locale locale = new Locale(language);
        Resources resources = this.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        Locale.setDefault(locale);

        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, displayMetrics);
    }
}
