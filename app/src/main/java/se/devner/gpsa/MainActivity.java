package se.devner.gpsa;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.evernote.android.job.JobManager;
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

    GoogleMap map;
    MarkerOptions clickedMarker;
    LatLng currentLocation;
    boolean alarmActive;
    boolean GPSActivated;
    boolean alarm;
    boolean notifying;
    double selectedRange, maxRange;
    Button increase, decrease;
    DiscreteSeekBar dsb;
    ToggleButton tb;
    Ringtone r;
    Circle circle;
    CountDownTimer cdt;
    NotificationCompat.Builder mBuilder;
    JobScheduler mJobScheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getIntent().getExtras()!=null){
            if(r != null) {
                r.stop();
            }
        }

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

        maxRange = 1000;
        dsb.setMax((int)maxRange);

        decrease = (Button) findViewById(R.id.decrease);
        decrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(maxRange <= 1000){
                    maxRange = 1000;
                    showSnackbar(2, maxRange);
                }else{
                    maxRange -= 1000;
                    showSnackbar(1, maxRange);
                }
                dsb.setMax((int)maxRange);
            }
        });

        increase = (Button) findViewById(R.id.increase);
        increase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxRange += 1000;
                showSnackbar(1, maxRange);
                dsb.setMax((int)maxRange);
            }
        });

        mBuilder = new NotificationCompat.Builder(this);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.logosmall);
        getSupportActionBar().setTitle("");

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
        alarm = true;

        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );

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
                        showSnackbar(3, selectedRange);
                        //Toast.makeText(MainActivity.this, "Please choose a place for your alarm", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                //Toast.makeText(MainActivity.this, "Alarm deactivated", Toast.LENGTH_SHORT).show();
                stopCheck();
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
/*
    public void startCheck(){

        //JobManager.create(this).addJobCreator(new DemoJobCreator());
        Toast.makeText(this, "Stqrt", Toast.LENGTH_SHORT).show();
        /*builder = new JobInfo.Builder( 1,
                new ComponentName( getPackageName(),
                        JobSchedulerService.class.getName() ) );
        builder.setPeriodic(2000);

        mJobScheduler.schedule(builder.build());
*//*
        if( status <= 0 ) {
            //If something goes wrong
            Toast.makeText(this, "WRONG", Toast.LENGTH_SHORT).show();
        }
        */
    //}

    public void showSnackbar(int id, double range){
        switch (id) {
            case 1:
                Snackbar.make(findViewById(android.R.id.content), "Maximum range set to " + range + " meters", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 2:
                Snackbar.make(findViewById(android.R.id.content), "Minimum max range is " + range + " meters", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 3:
                Snackbar.make(findViewById(android.R.id.content), "Please choose a place for your alarm", Snackbar.LENGTH_SHORT)
                        .show();
                break;
        }
    }

    public void noti(){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Bundle bundle = new Bundle();
        bundle.putString("buzz", "buzz");
        notificationIntent.putExtras(bundle);

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                1, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager nm = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Resources res = this.getResources();
        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle("You have arrived")
                .setContentText("Click to turn off alarm");
        Notification n = builder.build();

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();

        nm.notify(1, n);
        notifying = false;

    }

    public void stopCheck(){
        notifying = true;
        cdt.cancel();
        cdt = null;
        clickedMarker = null;
        map.clear();
        tb.setChecked(false);

        if(alarm) {
            noti();
        }
    }


    private void startCheck() {
        cdt = new CountDownTimer(30000000, 2000) {
            public void onTick(long millisUntilFinished) {
                if (currentLocation != null) {
                    if (userWithinRange()) {
                        //Notify
                        if(!notifying) {
                            stopCheck();
                        }
                    }
                }
            }

            public void onFinish() {
                stopCheck();
            }
        }.start();
    }
/*
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
        //Toast.makeText(MainActivity.this, "MapReady", Toast.LENGTH_SHORT).show();
        map.setMyLocationEnabled(true);
        if(currentLocation != null){
            map.moveCamera( CameraUpdateFactory.newLatLngZoom(currentLocation , 14.0f) );
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

    public boolean userWithinRange(){
        if(meterDistanceBetweenPoints((float)clickedMarker.getPosition().latitude, (float)clickedMarker.getPosition().longitude, (float)currentLocation.latitude, (float)currentLocation.longitude) < selectedRange){
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
