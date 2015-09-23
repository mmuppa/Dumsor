package dumsor.org.dumsor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.parse.ParseObject;

import org.dumsor.dumsor.R;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends Activity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{

    private static final String TAG = "MainActivity";
    private static final long INTERVAL = 100; // Milliseconds

    private FusedLocationProviderApi m_fused_location_provider;
    private GoogleApiClient m_google_api_client;
    private LocationRequest m_location_request;
    private Long m_prev_time;
    protected static MobileServiceClient m_client;
    protected static MobileServiceTable<DataPointItem> m_dpi_table;
    private SharedPreferences prefs;

    protected void createLocationRequest()
    {
        m_location_request = LocationRequest.create();
        m_location_request.setInterval(INTERVAL);
        m_location_request.setFastestInterval(INTERVAL);
        m_location_request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        m_location_request.setSmallestDisplacement(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (!isGooglePlayServicesAvailable())
        {
            finish();
        }

        // Enable Local Datastore.
        //Parse.enableLocalDatastore(this);

        //Parse.initialize(this, "6jR7eMMTMhIcOb03rZiBlICHicstB6zhvqG00KSa", "sychAhNg4my8kPsoG5L78vHxAWzk8IWMKj4bqB2V");


//        try
//        {
//            m_client = new MobileServiceClient(
//                    "https://dumsor.azure-mobile.net/",
//                    "CqPNJENHdzMyPQMFrxWjrLONzBUGzn70",
//                    this
//            );
//            m_dpi_table = m_client.getTable("dumsortable", DataPointItem.class);
//        } catch (MalformedURLException e)
//        {
//            e.printStackTrace();
//        }

        m_google_api_client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        m_fused_location_provider = LocationServices.FusedLocationApi;

        createLocationRequest();

        setContentView(R.layout.activity_main);

        prefs = this.getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        LocationManager loc_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = loc_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!enabled) {
            toggleGPSDialogue();
        }

        ImageView imgView = (ImageView) findViewById(R.id.imageView);
        imgView.setImageResource(R.drawable.dumsor_launcher);

        final ToggleButton tb = (ToggleButton) findViewById(R.id.powerToggleButton);
        tb.setChecked(true);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                RelativeLayout rl = (RelativeLayout) findViewById(R.id.layout_screen);
                tb.setEnabled(false);


                Timer buttonTimer = new Timer();
                buttonTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                tb.setEnabled(true);
                            }
                        });
                    }
                }, 5000); //was set at 60000 - 5 sec for testing

                if (isChecked) {
                    rl.setBackgroundColor(Color.WHITE);
                    Toast.makeText(getApplicationContext(), getString(R.string
                            .main_power_on_message), LENGTH_LONG).show();
                    recordLocationOn();
                } else {
                    rl.setBackgroundColor(Color.BLACK);
                    Toast.makeText(getApplicationContext(), getString(R.string
                            .main_power_off_message), LENGTH_LONG).show();
                    recordLocationOff();
                }
            }
        });

        final Button map_button = (Button) findViewById(R.id.map_button);
        map_button.setEnabled(false);
        map_button.setVisibility(View.INVISIBLE);
//        map_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, FilterActivity.class);
//                startActivity(intent);
//            }
//        });

        RelativeLayout rl = (RelativeLayout) findViewById(R.id.layout_screen);
        rl.setBackgroundColor(Color.WHITE);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.d(TAG, "Starting.");
        createLocationRequest();
        m_google_api_client.connect();
        if (m_google_api_client.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (m_google_api_client.isConnected()) {
            stopLocationUpdates();
            m_google_api_client.disconnect();
        }
    }

    private boolean isGooglePlayServicesAvailable()
    {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status)
        {
            return true;
        } else
        {
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "Connected to Network" + m_google_api_client.isConnected());
        startLocationUpdates();
    }

    protected void startLocationUpdates()
    {
        m_fused_location_provider.requestLocationUpdates(
                m_google_api_client, m_location_request, this);
        Log.d(TAG, "Location update started");
    }

    @Override
    public void onConnectionSuspended(int i)
    {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.i(TAG, "its not working.");
    }

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    protected void onPause()
    {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates()
    {
        m_fused_location_provider.removeLocationUpdates(
                m_google_api_client, this);
        Log.d(TAG, "Location update stopped");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        LocationManager loc_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = loc_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!enabled) {
            toggleGPSDialogue();
        }
        if (m_google_api_client.isConnected()) {
            startLocationUpdates();
            Log.d(TAG, "Location update resumed");
        }
    }

    private void recordLocationOn()
    {
        LocationManager loc_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = loc_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!enabled) {
            toggleGPSDialogue();
        }

        if (isConnected() && m_fused_location_provider.getLastLocation
                (m_google_api_client) != null)
        {
            long time = System.currentTimeMillis();
            if (m_prev_time == null) m_prev_time = time;
            Location temp_loc = m_fused_location_provider.getLastLocation(m_google_api_client);
            insert_dpi(prefs.getString("login", ""), prefs.getString("uid", ""),
                    temp_loc.getLatitude(), temp_loc.getLongitude(), time, m_prev_time, 1);
            m_prev_time = time;
        }
    }

    private void recordLocationOff()
    {
        LocationManager loc_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = loc_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!enabled) {
            toggleGPSDialogue();
        }

        if (isConnected() && LocationServices.FusedLocationApi.getLastLocation
                (m_google_api_client) != null)
        {
            long time = System.currentTimeMillis();
            if (m_prev_time == null) m_prev_time = time;
            Location temp_loc = m_fused_location_provider.getLastLocation(m_google_api_client);
            insert_dpi(prefs.getString("login", ""), prefs.getString("uid", ""),
                    temp_loc.getLatitude(), temp_loc.getLongitude(), time, m_prev_time, 0);
            m_prev_time = time;
        }
    }

    private boolean isConnected()
    {
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    private void toggleGPSDialogue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Enable GPS");
        builder.setMessage("Please enable GPS");
        builder.setInverseBackgroundForced(true);
        builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startActivity(
                        new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
                System.exit(0);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void insert_dpi(final String auth, final String source, final double lat,
                           final double lon, final long timestamp, final long previous,
                           final int power)
    {
        final DataPointItem dpi = new DataPointItem(auth, source, lat, lon, timestamp, previous,
                power);

        //Toast.makeText(MainActivity.this, "Insert In Progress", Toast.LENGTH_SHORT).show();

        ParseObject powerRecord = new ParseObject("Power");
        powerRecord.put("auth", auth);
        powerRecord.put("source", source);
        powerRecord.put("lat", lat);
        powerRecord.put("long", lon);
        powerRecord.put("timestamp", timestamp);
        powerRecord.put("previous", previous);
        powerRecord.put("power", power);
        powerRecord.saveInBackground();


//        new AsyncTask<Void, Void, Void>() {
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                ParseObject powerRecord = new ParseObject("power");
//                powerRecord.put("auth", auth);
//                powerRecord.put("source", source);
//                powerRecord.put("lat", lat);
//                powerRecord.put("long", lon);
//                powerRecord.put("timestamp", timestamp);
//                powerRecord.put("previous", previous);
//                powerRecord.put("power", power);
//                powerRecord.saveInBackground();
//
//                try {
//                    m_dpi_table.insert(dpi).get();
//                } catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//        }.execute();
    }

    public void insert_generated_data()
    {
        Random rand = new Random();
        for (int i = 0; i < 500; i++)
        {
            long temp = System.currentTimeMillis() - 604800000;
            insert_dpi("NULL", "0", 47 + rand.nextDouble(), 122 + rand.nextDouble(), temp + rand.nextLong(),
                    temp + rand.nextLong(), 0);
        }
    }
}
