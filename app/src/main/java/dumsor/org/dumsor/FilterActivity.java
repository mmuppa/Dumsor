package dumsor.org.dumsor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import org.dumsor.dumsor.R;


public class FilterActivity extends Activity {

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        final DatePicker start_picker = (DatePicker) findViewById(R.id.start_picker);
        final DatePicker end_picker = (DatePicker) findViewById(R.id.end_picker);
        Button go_button = (Button) findViewById(R.id.go_button);
        Button live_button = (Button) findViewById(R.id.live_button);

        prefs = this.getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        /*
            Takes the two dates selected and displays all of the points between the two.
         */
        go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (start_picker.getYear() <= end_picker.getYear())
                {
                    if ((start_picker.getMonth() == end_picker.getMonth() &&
                            start_picker.getDayOfMonth() < end_picker.getDayOfMonth()) ||
                            start_picker.getMonth() < end_picker.getMonth())
                    {
                        long start_timestamp = (long) (start_picker.getYear() - 1) * 31556952000L +
                                (start_picker.getMonth() - 1) * 2629746000L +
                                (start_picker.getDayOfMonth() - 1) * 86400000L;
                        long end_timestamp = (long) (end_picker.getYear() - 1) * 31556952000L +
                                (end_picker.getMonth() - 1) * 2629746000L +
                                (end_picker.getDayOfMonth()) * 86400000L;

                        SharedPreferences.Editor edit_prefs = prefs.edit();
                        edit_prefs.putLong("start_time", start_timestamp);
                        edit_prefs.putLong("end_time", end_timestamp);
                        edit_prefs.apply();

                        Intent intent = new Intent(FilterActivity.this, HeatMapActivity.class);
                        startActivity(intent);
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Invalid date range!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*
            Creates a button that shows all activity over the last hour.
         */
        live_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit_prefs = prefs.edit();
                edit_prefs.putLong("start_time", System.currentTimeMillis() - 3600000L);
                edit_prefs.putLong("end_time", System.currentTimeMillis());
                edit_prefs.apply();

                Intent intent = new Intent(FilterActivity.this, HeatMapActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
