package se.devner.gpsa;

import android.Manifest;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
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

        //DEBUG METHODS
        //SharedPreferences settings = getSharedPreferences("FAVORITES", 0);
        //settings.edit().putString("favorites", "").commit();

        //Init variables
        clickedMarker = null;
        notifying = false;
        circle = null;
        alarmActive = false;
        selectedRange = 0;
        GPSActivated = false;
        alarm = false;

        dsb = (DiscreteSeekBar) findViewById(R.id.dsb);
        dsb.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                if(!alarmActive){
                    selectedRange = (double) value;
                }
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
        getSupportActionBar().setIcon(R.drawable.logobar);
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
                        startLocationService();
                        dsb.setEnabled(false);
                    } else {
                        tb.setChecked(false);
                        showSnackbar(3, 0);
                    }
                } else {
                    dsb.setEnabled(true);
                    stopLocationService();
                }
            }
        });
        tb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showFavoriteList();
                return false;
            }
        });
    }

    private void showFavoriteList() {
        SharedPreferences settings = getSharedPreferences("FAVORITES", 0);
        String temp = settings.getString("favorites", null);
        final String[] stringList;
        if(temp != null) {
            stringList = temp.split(";");
        }else{
            stringList = new String[]{"empty"};
        }
        final int[] selected = {0};
        // custom dialog
        final AlertDialog favoritesDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.favorite_list_dialog_title)
                .setView(R.layout.select_favorite_layout)
                .setPositiveButton(R.string.favorite_list_dialog_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(stringList.length >= 2) {
                            stopLocationService();

                            Dialog f = (Dialog) dialog;
                            RadioGroup tempRG = (RadioGroup) f.findViewById(R.id.radioGroup);
                            int selectedId = tempRG.getCheckedRadioButtonId();
                            RadioButton tempRB = (RadioButton) tempRG.findViewById(selectedId);
                            int index = tempRG.indexOfChild(tempRB);
                            LatLng favLL = new LatLng(Double.valueOf(stringList[(index * 4) + 2]), Double.valueOf(stringList[(index * 4) + 3]));
                            clickedMarker = new MarkerOptions().position(favLL);
                            selectedRange = Double.valueOf(stringList[(index * 4) + 1]);
                            map.clear();
                            map.addMarker(clickedMarker);
                            drawCircle(clickedMarker, selectedRange);
                            dsb.setProgress((int) selectedRange);
                            tb.setChecked(true);

                            startLocationService();
                        }else{
                            showSnackbar(12, 0);
                        }
                    }
                })
                .setNegativeButton(R.string.favorite_list_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();

        // here is list
        final RadioGroup rg  = (RadioGroup) favoritesDialog.findViewById(R.id.radioGroup);
        final TextView status = (TextView) favoritesDialog.findViewById(R.id.favStatus);
        final Button clearButton = (Button) favoritesDialog.findViewById(R.id.clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFavorites();
                favoritesDialog.dismiss();
            }
        });

        final Button removeButton = (Button) favoritesDialog.findViewById(R.id.remove);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioGroup tempRG = (RadioGroup) favoritesDialog.findViewById(R.id.radioGroup);
                int selectedId = tempRG.getCheckedRadioButtonId();
                RadioButton tempRB = (RadioButton) tempRG.findViewById(selectedId);
                removeFavorite(tempRB.getText().toString());
                favoritesDialog.dismiss();
            }
        });
        if (stringList.length <= 2) {
            status.setText(getResources().getText(R.string.no_favorite_status));
        } else {
            clearButton.setVisibility(View.VISIBLE);
            removeButton.setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < stringList.length - 1; i += 4) {
            final RadioButton rb = new RadioButton(this); // dynamically creating RadioButton and adding to RadioGroup.
            rb.setText(stringList[i]);
            rb.setId(i);
            rb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selected[0] = rb.getId();
                }
            });
            if (i == 0) {
                rb.setChecked(true);
                selected[0] = rb.getId();
            }
            rg.addView(rb);
        }
    }

    private void removeFavorite(final String s) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_favorite_dialog_title)
                .setMessage(s.toString())
                .setPositiveButton(R.string.remove_favorite_dialog_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences settings = getSharedPreferences("FAVORITES", 0);
                        String tempString = settings.getString("favorites", null);
                        String[] tempStringArray = tempString.split(";");
                        int index = 0;
                        String newString = "";
                        for (String ss : tempStringArray) {
                            Log.d("ss", ss);
                            if (s.equals(ss)) {
                                index = 4;
                            }
                            if (index <= 0) {
                                newString += ss + ";";
                            }
                            index--;
                        }
                        settings.edit().putString("favorites", newString).commit();
                        showSnackbar(11, 0);

                    }
                })
                .setNegativeButton(R.string.remove_favorite_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void clearFavorites() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_favorites_dialog_title)
                .setPositiveButton(R.string.remove_favorite_dialog_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences settings = getSharedPreferences("FAVORITES", 0);
                        settings.edit().putString("favorites", "").commit();
                        stopLocationService();
                        showSnackbar(13, 0);
                    }
                })
                .setNegativeButton(R.string.remove_favorite_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showAddFavoriteDialog() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_favorite_dialog_title)
                .setView(R.layout.add_favorite_layout)
                .setPositiveButton(R.string.add_favorite_dialog_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Dialog f = (Dialog) dialog;
                        //This is the input I can't get text from
                        EditText inputTemp = (EditText) f.findViewById(R.id.editText);

                        if (inputTemp.getText().toString().length() > 0) {
                            //Spara och stäng
                            addFavorite(inputTemp.getText().toString());
                        } else {
                            showSnackbar(8, 0);
                            //Shake the input feild
                        }
                    }
                })
                .setNegativeButton(R.string.add_favorite_dialog_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private void addFavorite(String s) {
        // Restore preferences
        SharedPreferences settings = getSharedPreferences("FAVORITES", 0);
        String temp = settings.getString("favorites", null);
        if (temp != null) {
            if (temp.split(";").length <= 16) { //4 x 4 information in sharedpref, then adds the 5th (which is max)0123 4567 891011 12131415
                if (!temp.contains(s)) {
                    temp += (s + ";" + String.valueOf(selectedRange) + ";" + String.valueOf(clickedMarker.getPosition().latitude) + ";" + String.valueOf(clickedMarker.getPosition().longitude) + ";");
                    showSnackbar(4, 0);
                } else {
                    temp += "";
                    showSnackbar(7, 0);
                    //showAddFavoriteDialog();
                }
            } else {
                temp += "";
                showSnackbar(6, 0);
            }
        } else {
            temp = (s + ";" + String.valueOf(selectedRange) + ";" + String.valueOf(clickedMarker.getPosition().latitude) + ";" + String.valueOf(clickedMarker.getPosition().longitude) + ";");
            showSnackbar(4, 0);
        }
        settings.edit().putString("favorites", temp).commit();
    }
    //End of onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.guide:
                Intent howtoIntent = new Intent(this, GuideActivity.class);
                startActivity(howtoIntent);
                return true;
            case R.id.about:
                Intent aboutIntent = new Intent(this, AboutActivty.class);
                startActivity(aboutIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Finished methods
    @Override
    public void onMapReady(GoogleMap m) {
        map = m;
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                if (!alarmActive) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.draggable(true);
                    markerOptions.title(getResources().getText(R.string.map_your_destination).toString());
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
                    .setTitle(R.string.location_permission_dialog_title)
                    .setMessage(R.string.location_permission_dialog_message)
                    .setPositiveButton(R.string.location_persmission_dialog_positive, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent();
                            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            i.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        }
                    })
                    .setNegativeButton(R.string.location_permission_dialog_negative, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        map.setMyLocationEnabled(true);
        checkIfAlarmActive();

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                showAddFavoriteDialog();
            }
        });
    }

    private void checkIfAlarmActive() {
        SharedPreferences settings = getSharedPreferences("ACTIVEALARM", 0);
        String temp = settings.getString("activealarm", null);

        if (temp != null) {
            selectedRange = Double.parseDouble(temp.split(";")[0]);
            dsb.setProgress((int) selectedRange);

            LatLng ll = new LatLng(Double.parseDouble(temp.split(";")[1]), Double.parseDouble(temp.split(";")[2]));
            MarkerOptions tempMarker = new MarkerOptions();
            tempMarker.position(ll);
            clickedMarker = tempMarker;
            map.addMarker(clickedMarker);
            drawCircle(clickedMarker, selectedRange);

            tb.setChecked(true);

            toggleNotification(true);
        }
    }

    private void toggleNotification(boolean show) {
        if (show) {
            Notification.Builder builder = new Notification.Builder(this);

            builder.setSmallIcon(R.drawable.ic_stat_name)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setContentTitle(getResources().getText(R.string.notification_title));
            Notification n = builder.build();
            n.flags |= Notification.FLAG_NO_CLEAR;
            nm.notify(1, n);
        } else {
            nm.cancel(1);
        }
    }

    public void showSnackbar(int id, double range) {
        final View snackbarLayout = findViewById(R.id.cLayout);
        switch (id) {
            case 1:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_1).toString() + range + " meter", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 2:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_2).toString() + range + " meter", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 3:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_3).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 4:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_4).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 5:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_5).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 6:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_6).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 7:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_7).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 8:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_8).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 9:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_9).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 10:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_10).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 11:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_11).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 12:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_12).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case 13:
                Snackbar.make(snackbarLayout, getResources().getText(R.string.snackbar_id_13).toString(), Snackbar.LENGTH_SHORT)
                        .show();
                break;
        }
    }

    private void drawCircle(MarkerOptions clickedMarker, double selectedRange) {
        if (circle != null) {
            circle.remove();
        }
        circle = map.addCircle(new CircleOptions()
                .center(new LatLng(clickedMarker.getPosition().latitude, clickedMarker.getPosition().longitude))
                .radius(selectedRange)
                .strokeColor(Color.argb(100, 0, 0, 0))
                .fillColor(Color.argb(100, 100, 150, 200)));
    }

    public void startLocationService() {
        SharedPreferences settings = getSharedPreferences("ACTIVEALARM", 0);
        settings.edit().putString("activealarm", selectedRange + ";" + clickedMarker.getPosition().latitude + ";" + clickedMarker.getPosition().longitude).commit();

        showSnackbar(9, 0);
        toggleNotification(true);
        alarmActive = true;
        startService(new Intent(getBaseContext(), LocationCheckerService.class));
    }

    // Method to stop the service
    public void stopLocationService() {
        SharedPreferences settings = getSharedPreferences("ACTIVEALARM", 0);
        settings.edit().putString("activealarm", null).commit();

        showSnackbar(10, 0);
        alarmActive = false;
        clickedMarker = null;
        map.clear();
        dsb.setProgress(0);
        toggleNotification(false);
        stopService(new Intent(getBaseContext(), LocationCheckerService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationService();
    }

    public static double getSelectedRange() {
        return selectedRange;
    }

    public static MarkerOptions getClickedMarker() {
        return clickedMarker;
    }
}