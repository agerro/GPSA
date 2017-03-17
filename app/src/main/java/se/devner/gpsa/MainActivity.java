package se.devner.gpsa;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
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

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap map;
    MarkerOptions clickedMarker;
    LatLng currentLocation;
    CountDownTimer cdt;
    boolean alarmActive;
    boolean GPSActivated;
    boolean notifying;
    double selectedRange;
    SeekBar sb;
    ToggleButton tb;
    TextView tv;
    Circle circle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        cdt = null;
        clickedMarker = null;
        notifying = false;
        circle = null;
        currentLocation = null;
        alarmActive = false;
        selectedRange = 0;
        GPSActivated = false;

        SmartLocation.with(this).location()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        GPSActivated = true;
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    }
                });

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
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Alarm deactivated", Toast.LENGTH_SHORT).show();
                    stopCheck();
                }
            }
        });

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
    }

    private void startCheck() {
        cdt = new CountDownTimer(30000000, 2000) {
            public void onTick(long millisUntilFinished) {
                if (currentLocation != null) {
                    if (userWithinRange(clickedMarker, currentLocation)) {
                        //Notify
                        if(!notifying) {
                            startNotification();
                        }
                    }
                }
            }

            public void onFinish() {
                stopCheck();
            }
        }.start();
    }

    private void startNotification() {
        stopCheck();
        notifying = true;
        clickedMarker = null;
        map.clear();
        tb.setChecked(false);

        new AlertDialog.Builder(this)
                .setTitle("Alarm")
                .setMessage("Du är framme!")
                .setPositiveButton("Stäng alarm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        notifying = false;
                    }
                })

                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void stopCheck() {
        cdt.cancel();
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
        map.setMyLocationEnabled(true);
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
        if(meterDistanceBetweenPoints((float)sel.getPosition().latitude, (float)sel.getPosition().longitude, (float)cur.latitude, (float)cur.longitude) < selectedRange){
            return true;
        }else{
            return false;
        }
    }

    private double meterDistanceBetweenPoints(float lat_a, float lng_a, float lat_b, float lng_b) {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        Float f = new Float(distance * meterConversion).floatValue();

        Log.d("Meters between:", String.valueOf(f));
        Log.d("SelectedRange:", String.valueOf(selectedRange));

        return f;

    }
}
