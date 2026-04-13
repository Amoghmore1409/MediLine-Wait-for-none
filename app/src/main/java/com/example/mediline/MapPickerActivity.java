package com.example.mediline;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double initialLat = 0;
    private double initialLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map_picker);

        initialLat = getIntent().getDoubleExtra("LATITUDE", 0);
        initialLng = getIntent().getDoubleExtra("LONGITUDE", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        FrameLayout confirmBtn = findViewById(R.id.btn_confirm_location);
        confirmBtn.setOnClickListener(v -> {
            if (mMap != null) {
                LatLng target = mMap.getCameraPosition().target;
                Intent resultIntent = new Intent();
                resultIntent.putExtra("LATITUDE", target.latitude);
                resultIntent.putExtra("LONGITUDE", target.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Default to a central location if none provided (e.g., New York)
        LatLng startLoc = new LatLng(40.7128, -74.0060);
        
        if (initialLat != 0 && initialLng != 0) {
            startLoc = new LatLng(initialLat, initialLng);
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLoc, 15f));
    }
}
