package dumsor.org.dumsor;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import org.dumsor.dumsor.R;

import java.net.MalformedURLException;
import java.util.ArrayList;

public class HeatMapActivity extends FragmentActivity {

    private GoogleMap m_map;
    private HeatmapTileProvider m_hmtp;
    private TileOverlay m_overlay;
    private ArrayList<LatLng> m_unweight_LatLng = new ArrayList<LatLng>();
    private MobileServiceClient m_client;
    private MobileServiceTable<DataPointItem> m_dpi_table;
    private MobileServiceList<DataPointItem> m_results;
    private SharedPreferences m_prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heat_map);

        m_prefs = this.getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        try
        {
            m_client = new MobileServiceClient(
                    "https://dumsor.azure-mobile.net/",
                    "CqPNJENHdzMyPQMFrxWjrLONzBUGzn70",
                    this
            );
            m_dpi_table = m_client.getTable("dumsortable", DataPointItem.class);
            Log.i("", "Client retrieved table from Mobile Service");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        setUpMapIfNeeded();

        init_unweight_heatmap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #m_map} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (m_map == null) {
            // Try to obtain the map from the SupportMapFragment.
            m_map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (m_map != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa. Additionally, if the zoom goes closer than 11.0 f, it is zoomed out.
     * <p/>
     * This should only be called once and when we are sure that {@link #m_map} is not null.
     */
    private void setUpMap() {
        m_map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(8.1, -1.2), 7.0f));
        m_map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (cameraPosition.zoom > 9.0f)
                {
                    m_map.moveCamera(CameraUpdateFactory.zoomTo(9.0f));
                }
            }
        });
    }

    private void init_unweight_heatmap() {
        m_unweight_LatLng = new ArrayList<LatLng>();
        getUnweightEntities(m_prefs.getLong("start_time", 0), m_prefs.getLong("end_time", 0));
    }

    public void getUnweightEntities(final long start, final long end)
    {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    m_results = m_client.getTable("dumsortable", DataPointItem.class).select()
                            .field("timestamp").le(end).and().field("timestamp").ge(start)
                            .execute().get();
                    Log.i("", "DATA QUERY COMPLETE: " + m_results.getTotalCount());
                    if (m_results == null)
                        Log.i("", "THERE IS NO DATA IN RESULTS");
                    else
                    {
                        for (DataPointItem dpi : m_results) {
                            m_unweight_LatLng.add(new LatLng(dpi.get_lat(), dpi.get_lon()));
                        }

                        int[] colors = {
                                Color.rgb(102, 225, 0), // green
                                Color.rgb(255, 0, 0)    // red
                        };

                        float[] startPoints = {
                                0.2f, 1f
                        };

                        Gradient gradient = new Gradient(colors, startPoints);

                        m_hmtp = new HeatmapTileProvider.Builder()
                                .data(m_unweight_LatLng)
                                .radius(50)
                                .opacity(0.7)
                                .gradient(gradient)
                                .build();

                        Log.i("", m_unweight_LatLng.size() + "CONTENTS IN DATASTORE");

                        runOnUiThread(new Runnable() {
                            public void run() {
                                m_overlay = m_map.addTileOverlay(new TileOverlayOptions().tileProvider(m_hmtp));
                                m_overlay.clearTileCache();
                            }
                        });
                    }
                } catch (Exception e) {
                    createAndShowDialog(e.toString(), "Error");
                }
                return null;
            }
        };
        task.execute();
    }

    private void createAndShowDialog(String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }
}
