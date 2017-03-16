package se.devner.gpsa;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    GoogleMap map;
    MarkerOptions clickedMarker;
    LatLng currentLocation;
    CountDownTimer cdt;
    boolean alarmActive;
    boolean GPSActivated;
    double selectedRange;
    SeekBar sb;
    ToggleButton tb;
    TextView tv;
    Circle circle;
    LocationManager service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        cdt = null;
        GPSActivated = false;

        tv = (TextView) findViewById(R.id.textView);
        tb = (ToggleButton) findViewById(R.id.toggleButton2);
        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tb.isChecked()) {
                    if (GPSActivated) {
                        if (clickedMarker != null) {
                            Toast.makeText(MainActivity.this, "Alarm activated", Toast.LENGTH_SHORT).show();
                            startCheck();
                        } else {
                            tb.setChecked(false);
                            Toast.makeText(MainActivity.this, "Please choose a place for your alarm", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        checkGPS();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Alarm deactivated", Toast.LENGTH_SHORT).show();
                    stopCheck();
                }
            }
        });
        clickedMarker = null;
        circle = null;
        currentLocation = null;
        alarmActive = false;
        selectedRange = 0;
        sb = (SeekBar) findViewById(R.id.seekBar2);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedRange = (double) progress;
                tv.setText(String.valueOf(progress) + " meters");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (clickedMarker != null) {
                    drawCircle(clickedMarker, selectedRange);
                }
            }
        });

        checkGPS();
    }

    public void checkGPS() {
        String provider;

        service = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the locatioin provider -> use
        // default
        Criteria criteria = new Criteria();
        provider = service.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = service.getLastKnownLocation(provider);

        // Initialize the location fields
        if (location != null) {
            onLocationChanged(location);
            GPSActivated = true;
        } else {
            GPSActivated = false;
        }

        service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }else{
        }
    }

    private void startCheck() {
        new CountDownTimer(30000000, 2000) {

            public void onTick(long millisUntilFinished) {
                //currentLocation = new LatLng(service.getLastKnownLocation().getLatitude(), gpsTracker.getLongitude());
                Log.d("currLoc:", currentLocation.latitude + " " + currentLocation.longitude);
                if(currentLocation != null) {
                    Toast.makeText(MainActivity.this, "LocationNotNull", Toast.LENGTH_SHORT).show();
                    if (userWithinRange(clickedMarker, currentLocation)) {
                        //Notify
                        Toast.makeText(MainActivity.this, "WITHIN RANGE", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            public void onFinish() {
                stopCheck();
            }
        }.start();
    }

    private void stopCheck() {
        cdt = null;
    }

    @Override
    public void onMapReady(GoogleMap m) {
        map = m;

        // Setting a click event handler for the map
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                // This will be displayed on taping the marker
                markerOptions.title(latLng.latitude + " : " + latLng.longitude);

                // Clears the previously touched position
                map.clear();

                // Animating to the touched position
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                // Placing a marker on the touched position
                clickedMarker = markerOptions;
                drawCircle(clickedMarker, selectedRange);
                map.addMarker(markerOptions);
            }
        });
    }

    private void drawCircle(MarkerOptions clickedMarker, double selectedRange) {

        if(circle != null){
            circle.remove();
        }
        circle = map.addCircle(new CircleOptions()
            .center(new LatLng(clickedMarker.getPosition().latitude, clickedMarker.getPosition().longitude))
            .radius(selectedRange/2)
            .strokeColor(Color.argb(100,0,0,0))
            .fillColor(Color.argb(100, 100, 150, 200)));
    }

    public boolean userWithinRange(MarkerOptions sel, LatLng cur){
        Log.d("SelectedLocation:", String.valueOf(sel.getPosition().latitude) + String.valueOf(sel.getPosition().longitude));
        Log.d("CurrentLocation:", String.valueOf(cur.latitude) + String.valueOf(cur.longitude));
        if(meterDistanceBetweenPoints((float)sel.getPosition().latitude, (float)sel.getPosition().longitude, (float)cur.latitude, (float)cur.longitude) < selectedRange){
            return true;
        }else{
            return false;
        }
    }

    private double meterDistanceBetweenPoints(float lat_a, float lng_a, float lat_b, float lng_b) {
        float pk = (float) (180.f/Math.PI);

        float a1 = lat_a / pk;
        float a2 = lng_a / pk;
        float b1 = lat_b / pk;
        float b2 = lng_b / pk;

        float t1 = (float)(Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2));
        float t2 = (float)(Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2));
        float t3 = (float)(Math.sin(a1)*Math.sin(b1));
        double tt = Math.acos(t1 + t2 + t3);

        Log.d("SelectedRange:", String.valueOf(selectedRange));
        Log.d("Meters between:", String.valueOf(6366000*tt));
        return 6366000*tt;
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
