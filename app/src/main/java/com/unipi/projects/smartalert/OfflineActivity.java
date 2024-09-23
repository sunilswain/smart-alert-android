package com.unipi.projects.smartalert;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Locale;

import io.reactivex.rxjava3.annotations.NonNull;

public class OfflineActivity extends AppCompatActivity {
    private boolean userEditedMessage = false;
    private LocationManager _locationManager;
    private LocationListener _locationListener;

    private Double _lat;
    private Double _long;

    private static final int PERMISSION_SEND_SMS = 123;
    private final String predefinedNumber = "+917537979019";
    private Button sendSmsButton;
    private EditText smsEditText;

    private CardView emergencyInfoCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_offline);
        emergencyInfoCard = findViewById(R.id.emergencyInfoCard);

        Button closeCardButton = findViewById(R.id.closeCardButton);
        closeCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideEmergencyInfo(v);  // Hide the emergency info card
            }
        });

        setContentView(R.layout.activity_offline); // Set content view first

        // Initialize views after setting the content view
        sendSmsButton = findViewById(R.id.sendSmsButton);
        smsEditText = findViewById(R.id.smsEditText);

        // Set a TextWatcher to detect user edits
        smsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userEditedMessage = true; // User has edited the message
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (isOnline()) {
            Log.i("TAG", "The user is online");
            Intent loginIntent = new Intent(OfflineActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        _locationListener = setLocationListener();
        _locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (_locationManager == null) {
            Log.e("LocationManagerError", "LocationManager is null.");
            return;
        }

        locationRequest();

        // Set up the SMS button's click listener
        sendSmsButton.setOnClickListener((view) -> {
            String messageToSend = smsEditText.getText().toString().trim();
            if (messageToSend.isEmpty()) {
                Toast.makeText(OfflineActivity.this, "SMS content cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send SMS only if permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                sendSMS(predefinedNumber, messageToSend);
            } else {
                Toast.makeText(this, "SMS permission is needed to send messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showEmergencyInfo(View view) {
        CardView emergencyInfoCard = findViewById(R.id.emergencyInfoCard);
        emergencyInfoCard.setVisibility(View.VISIBLE); // Make the card visible
    }

    public void hideEmergencyInfo(View view) {
        CardView emergencyInfoCard = findViewById(R.id.emergencyInfoCard);
        emergencyInfoCard.setVisibility(View.GONE); // Hide the card
    }



    // Request location updates if permission is granted
    private void locationRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, _locationListener); // 5 seconds interval
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_SEND_SMS);
        }
    }

    private LocationListener setLocationListener() {
        return new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                _lat = location.getLatitude();
                _long = location.getLongitude();

                // Update SMS EditText and show the button only when location is fetched
                if (_lat != null && _long != null) {
                    // Only update if the user hasn't edited the message
                    if (!userEditedMessage) {
                        String message = String.format("Here are my location coordinates. Lat = %.6f Long = %.6f \n Event: Fire", _lat, _long);
                        smsEditText.setText(message);
                        smsEditText.setSelection(message.length()); // Move cursor to the end
                    }
                    smsEditText.setVisibility(View.VISIBLE);
                    sendSmsButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFlushComplete(int requestCode) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationRequest(); // Retry requesting location updates
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to check SMS permission
    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
        }
    }

    // Method to send SMS
    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SMS sent successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("SMS Error", e.getMessage());
            Toast.makeText(this, "Failed to send SMS!", Toast.LENGTH_SHORT).show();
        }
    }


    // Optionally, override onResume to check network status again
    @Override
    protected void onResume() {
        super.onResume();
        if (isOnline()) {
            Intent loginIntent = new Intent(OfflineActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    }



    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(title)
                .setMessage(message)
                .show();
    }



    // Method to check network status
    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }


}
