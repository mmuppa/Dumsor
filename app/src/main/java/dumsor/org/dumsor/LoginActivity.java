package dumsor.org.dumsor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import org.dumsor.dumsor.R;

import io.fabric.sdk.android.Fabric;


public class LoginActivity extends FragmentActivity
{

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "Dz3swIdbMxuoVB0YgR11xbP0Y";
    private static final String TWITTER_SECRET = "M5Bhy9QypnTeqU5PiMBOE2rUCi27ePbvxuOLwH6HIwwgu86iSo";
    private TwitterLoginButton tw_login_button;

    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        Log.i("", "FacebookSDK Initialized.");

        TwitterAuthConfig authConfig = new TwitterAuthConfig("kPf9LvlSzjbpgBmghsyxeCoWh", "2f5I7LPFg59cOQ5uIyYYtfoAH4PgM6p3zbo1BmJX0lzOAPHSyl");
        Fabric.with(this, new TwitterCore(authConfig));

        setContentView(R.layout.activity_login);

        final SharedPreferences prefs = this.getSharedPreferences(getString(R.string.app_name),
                MODE_PRIVATE);
        if (prefs.contains("login") && prefs.contains("uid"))
        {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        LocationManager loc_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = loc_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!enabled) {
            toggleGPSDialogue();
        }

        final SharedPreferences.Editor prefs_editor = this.getSharedPreferences(getString(R.string
                .app_name), MODE_PRIVATE).edit();
        callbackManager = CallbackManager.Factory.create();

        LoginButton fb_login_button = (LoginButton) findViewById(R.id.fb_login_button);
        fb_login_button.registerCallback(callbackManager, new FacebookCallback<LoginResult>()
        {
            @Override
            public void onSuccess(LoginResult loginResult)
            {
                Toast.makeText(getApplicationContext(), "Facebook Login Successful.",
                        Toast.LENGTH_SHORT).show();
                prefs_editor.putString("login", "Facebook");
                prefs_editor.putString("uid", loginResult.getAccessToken().getUserId());
                prefs_editor.commit();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancel()
            {
                Toast.makeText(getApplicationContext(), "Facebook Login Cancelled.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception)
            {
                Toast.makeText(getApplicationContext(), "Facebook Login Failed.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        tw_login_button = (TwitterLoginButton) findViewById(
                R.id.tw_login_button);
        tw_login_button.setCallback(new Callback<TwitterSession>()
        {
            @Override
            public void success(Result<TwitterSession> result)
            {
                Log.i("Twitter Log", "Twitter login successful, changing activities...");
                Toast.makeText(getApplicationContext(), "Twitter Login Successful.",
                        Toast.LENGTH_SHORT).show();
                prefs_editor.putString("login", "Twitter");
                prefs_editor.putString("uid", Long.toString(result.data.getUserId()));
                prefs_editor.commit();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(getApplicationContext(), "Twitter Login Failed.", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        Button g_login_button = (Button) findViewById(R.id.g_login_button);
        g_login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs_editor.putString("login", "Guest");
                prefs_editor.putString("uid", "0");
                prefs_editor.commit();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        tw_login_button.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
}
