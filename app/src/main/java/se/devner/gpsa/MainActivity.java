package se.devner.gpsa;

import android.Manifest;
import android.app.job.JobScheduler;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
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
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    static GoogleMap map;
    static MarkerOptions clickedMarker;
    static LatLng currentLocation;
    boolean alarmActive;
    boolean GPSActivated;
    static boolean alarm;
    static boolean notifying;
    static double selectedRange;
    DiscreteSeekBar dsb;
    static ToggleButton tb;
    Circle circle;
    static NotificationCompat.Builder mBuilder;
    //JobInfo.Builder builder;
    static JobScheduler mJobScheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mBuilder = new NotificationCompat.Builder(this);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logosmall);
        getSupportActionBar().setTitle("");
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //cdt = null;
        clickedMarker = null;
        notifying = false;
        circle = null;
        currentLocation = null;
        alarmActive = false;
        selectedRange = 0;
        GPSActivated = false;
/*
        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );
*/
        SmartLocation.with(this).location()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        GPSActivated = true;
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    }
                });

        //tv = (TextView) findViewById(R.id.textView);
        tb = (ToggleButton) findViewById(R.id.toggleButton2);
        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (tb.isChecked()) {
                if (GPSActivated) {
                    if (clickedMarker != null) {
                        //Toast.makeText(MainActivity.this, "Alarm activated", Toast.LENGTH_SHORT).show();
                        startCheck();
                    } else {
                        tb.setChecked(false);
                        Toast.makeText(MainActivity.this, "Please choose a place for your alarm", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                //Toast.makeText(MainActivity.this, "Alarm deactivated", Toast.LENGTH_SHORT).show();
                stopCheck();
            }
            }
        });

        dsb = (DiscreteSeekBar)  findViewById(R.id.dsb);
        dsb.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                selectedRange = (double) value;
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if (clickedMarker != null) {
                    drawCircle(clickedMarker, selectedRange);
                }
            }
        });
/*
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
        */
    }

    public void startCheck(){

       /*
        //Toast.makeText(this, "Stqrt", Toast.LENGTH_SHORT).show();
        builder = new JobInfo.Builder( 1,
                new ComponentName( getPackageName(),
                        JobSchedulerService.class.getName() ) );
        builder.setPeriodic(2000);
        int status = mJobScheduler.schedule(builder.build());

        if( status <= 0 ) {
            //If something goes wrong
            Toast.makeText(this, "WRONG", Toast.LENGTH_SHORT).show();
        }*/
    }

    public static void noti(){
        /*new AlertDialog.Builder(this)
                .setTitle("Alarm")
                .setMessage("Du är framme!")
                .setPositiveButton("Stäng alarm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        notifying = false;
                    }
                })

                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
                */

        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("Notification Alert, Click Me!");
        mBuilder.setContentText("Hi, This is Android Notification Detail!");
        mBuilder.build();

    }

    public static void stopCheck(){
        notifying = true;
        clickedMarker = null;
        map.clear();
        tb.setChecked(false);
        //mJobScheduler.cancelAll();
        if(alarm) {
            noti();
        }
    }

/*
    private void startCheck() {
        cdt = new CountDownTimer(30000000, 2000) {
            public void onTick(long millisUntilFinished) {
                if (currentLocation != null) {
                    if (userWithinRange()) {
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

    private void stopCheck() {
        cdt.cancel();
        cdt = null;
    }
    *//*
public static void startNotification() {
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
    */

    @Override
    public void onMapReady(GoogleMap m) {
        map = m;

        // Setting a click event handler for the map
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(latLng.latitude + " : " + latLng.longitude);
                map.clear();
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                clickedMarker = markerOptions;
                drawCircle(clickedMarker, selectedRange);
                map.addMarker(markerOptions);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("This application needs to access your location in order to work. Please give this application permission to use location in your phones settings. REMEMBER to restart the app after you have given it permission to use the phones location.")
                    .setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent();
                            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            i.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        }
                    })
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            return;
        }
        Toast.makeText(MainActivity.this, "MapReady", Toast.LENGTH_SHORT).show();
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

    public static boolean userWithinRange(){
        if(meterDistanceBetweenPoints((float)clickedMarker.getPosition().latitude, (float)clickedMarker.getPosition().longitude, (float)currentLocation.latitude, (float)currentLocation.longitude) < selectedRange){
            return true;
        }else{
            return false;
        }
    }

    private static double meterDistanceBetweenPoints(float lat_a, float lng_a, float lat_b, float lng_b) {
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
