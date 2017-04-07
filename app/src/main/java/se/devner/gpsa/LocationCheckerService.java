package se.devner.gpsa;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.Calendar;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;


/**
 * Created by TutorialsPoint7 on 8/23/2016.
 */

public class LocationCheckerService extends Service {

    boolean GPSActivated, alarming;
    LatLng currentLocation;
    MarkerOptions clickedMarker;
    double selectedRange;
    CountDownTimer cdt;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        currentLocation = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        clickedMarker = MainActivity.getClickedMarker();
        selectedRange = MainActivity.getSelectedRange();
        alarming = false;

        // Let it continue running until it is stopped.
        SmartLocation.with(this).location()
            .config(LocationParams.NAVIGATION)
            .start(new OnLocationUpdatedListener() {
                @Override
                public void onLocationUpdated(Location location) {
                    GPSActivated = true;
                    if(location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    }
                }
            });

        cdt = new CountDownTimer(86400000, 1000) {
            public void onTick(long millisUntilFinished) {
                if (currentLocation != null) {
                    if(userWithinRange()){
                        if(!alarming) {
                            activateAlarm();
                        }
                    }
                }
            }
            public void onFinish() {

            }
        }.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        alarming = false;
        SmartLocation.with(this).location().stop();
        cdt.cancel();
        cdt = null;
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
        return f;
    }

    private void activateAlarm() {
        alarming = true;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 1);
        Intent intent = new Intent(this, AlarmReceiverActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 12345, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
    }
}