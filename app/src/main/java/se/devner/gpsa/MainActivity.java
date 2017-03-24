package se.devner.gpsa;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap map;
    static MarkerOptions clickedMarker;
    boolean alarmActive;
    boolean GPSActivated;
    boolean alarm;
    boolean notifying;
    static double selectedRange;
    double maxRange;
    Button increase, decrease;
    DiscreteSeekBar dsb;
    ToggleButton tb;
    Circle circle;
    NotificationManager nm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init variables
        clickedMarker = null;
        notifying = false;
        circle = null;
        alarmActive = false;
        selectedRange = 0;
        GPSActivated = false;
        alarm = false;

        //Init slider
        dsb = (DiscreteSeekBar) findViewById(R.id.dsb);
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

        //Init on slider range buttons
        maxRange = 1000;
        dsb.setMax((int) maxRange);

        decrease = (Button) findViewById(R.id.decrease);
        decrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (maxRange <= 1000) {
                    maxRange = 1000;
                    showSnackbar(2, maxRange);
                } else {
                    maxRange -= 1000;
                    showSnackbar(1, maxRange);
                }
                dsb.setMax((int) maxRange);
            }
        });

        increase = (Button) findViewById(R.id.increase);
        increase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxRange += 1000;
                showSnackbar(1, maxRange);
                dsb.setMax((int) maxRange);
            }
        });

        //Init logo instead off text in toolbar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logosmall);
        getSupportActionBar().setTitle("");

        //Init map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Init notification manager
        nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        //Init togglebutton
        tb = (ToggleButton) findViewById(R.id.toggleButton2);
        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tb.isChecked()) {
                    if (clickedMarker != null) {
                        //startCheck();
                        startLocationService();
                    } else {
                        tb.setChecked(false);
                        showSnackbar(3, selectedRange);
                    }
                } else {
                    //stopCheck();
                    resetApp();
                    stopLocationService();
                }
            }
        });
    }

    private void resetApp() {
    }

    //End of onCreate
/*
    public void stopCheck(){
        notifying = true;
        alarmActive = false;
        clickedMarker = null;
        map.clear();
        tb.setChecked(false);
        toggleNotification(false);
        stopLocationService();

        if(alarm) {
            alarm = false;
        }
    }

    private void startCheck() {
        alarmActive = true;
        startLocationService();
        toggleNotification(true);
    }
*/
    //Finished methods
    @Override
    public void onMapReady(GoogleMap m) {
        map = m;
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                if(!alarmActive) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(latLng.latitude + " : " + latLng.longitude);
                    map.clear();
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    clickedMarker = markerOptions;
                    drawCircle(clickedMarker, selectedRange);
                    map.addMarker(markerOptions);
                }
            }
        });

        //Code to check if the user have given permission to use location services
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
        map.setMyLocationEnabled(true);
    }

    private void toggleNotification(boolean show) {
        if(show){
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Bundle bundle = new Bundle();
            bundle.putString("buzz", "buzz");
            notificationIntent.putExtras(bundle);

            PendingIntent contentIntent = PendingIntent.getActivity(this,
                    1, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            Notification.Builder builder = new Notification.Builder(this);

            builder.setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setContentTitle("Alarm is on");
            //.setContentText("Click to turn off alarm");
            Notification n = builder.build();

            nm.notify(1, n);
        }else{
            nm.cancel(1);
        }
    }

    public void showSnackbar(int id, double range){
        final View snackbarLayout = findViewById(R.id.cLayout);
        switch (id) {
            case 1:
                Snackbar.make(snackbarLayout, "Maximum range set to " + range + " meters", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 2:
                Snackbar.make(snackbarLayout, "Can't be lower then " + range + " meters", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 3:
                Snackbar.make(snackbarLayout, "Please choose a place for your alarm", Snackbar.LENGTH_SHORT)
                        .show();
                break;
        }
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

    public void startLocationService() {
        toggleNotification(true);
        alarmActive = true;
        startService(new Intent(getBaseContext(), LocationCheckerService.class));
    }

    // Method to stop the service
    public void stopLocationService() {
        alarmActive = false;
        clickedMarker = null;
        map.clear();
        toggleNotification(false);
        stopService(new Intent(getBaseContext(), LocationCheckerService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationService();
    }

    public static double getSelectedRange(){
        return selectedRange;
    }

    public static MarkerOptions getClickedMarker(){
        return clickedMarker;
    }
}