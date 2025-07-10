package you.thiago.walkifleet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.UUID;

public class LoginActivity extends AppCompatActivity implements MSGListener
{
    // UI references.
    private EditText mLoginView;
    private EditText mPasswordView;
    private EditText mServerAddressView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mLoginView = (EditText) findViewById(R.id.login);
        mPasswordView = (EditText) findViewById(R.id.password);
        mServerAddressView = (EditText) findViewById(R.id.serveraddress);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        Protocol.isInitialLogin = true;

        mLoginView.setText("USER" + String.valueOf(Random.nextInt(19) + 1));
        mServerAddressView.setText("200.98.129.21:8000");
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mLoginView.setError(null);
        mPasswordView.setError(null);
        mServerAddressView.setError(null);

        // Store values at the time of the login attempt.
        String login = mLoginView.getText().toString();
        String serveraddress = mServerAddressView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(login)) {
            mLoginView.setError(getString(R.string.error_field_required));
            focusView = mLoginView;
            cancel = true;
        }
        else if (TextUtils.isEmpty(serveraddress)) {
            mServerAddressView.setError(getString(R.string.error_field_required));
            focusView = mServerAddressView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and
            // perform the user login attempt.
            Protocol.msgListener = this;
            VoIP.DestinationIp = serveraddress.split(":")[0];
            VoIP.serverAddress = serveraddress;
            Protocol.connectWebSocket(serveraddress);
            showProgress(true);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void OnMessage(JSONObject msg)
    {
        try
        {
            String message = msg.getString("MessageID");
            switch (message)
            {
                case "SERVER_CONFIG":
                    VoIP.DestinationPort = msg.getInt("VoipPort");
                    VoIP.SampleRate = msg.getInt("AudioSampleRate");
                    VoIP.FrameSize = msg.getInt("AudioFrameSize");
                    VoIP.SSRC = Random.nextRandomPositiveInt();
                    VoIP.login = mLoginView.getText().toString();
                    VoIP.password = mPasswordView.getText().toString();

                    VoIP.Codec = new Opus(VoIP.SampleRate, VoIP.FrameSize);

                    JSONObject devconf = new JSONObject();
                    devconf.put("MessageID","DEVICE_CONFIG");
                    devconf.put("Ssrc", VoIP.SSRC);
                    devconf.put("AppName", "APIClientAndroid");
                    devconf.put("VersionName", "5.5");
                    devconf.put("VersionCode", 1);
                    devconf.put("AudioCodec", 0); // Opus
                    devconf.put("Password", VoIP.password);

                    JSONObject deviceData = new JSONObject();
                    deviceData.put("SessionID", Base64.encodeToString(Protocol.uuidToBytes(UUID.randomUUID()), Base64.NO_WRAP));
                    deviceData.put("ID", Base64.encodeToString(Protocol.uuidToBytes(Device.GetDeviceID(this)), Base64.NO_WRAP));
                    deviceData.put("StatusID", Base64.encodeToString(Protocol.uuidToBytes(new UUID(0L, 0L)), Base64.NO_WRAP));
                    String deviceDescription = String.format("MANUFACTURER=%s; MODEL=%s; SERIAL=%s; OSVERSION=%s", android.os.Build.MANUFACTURER, android.os.Build.MODEL, android.os.Build.SERIAL, android.os.Build.VERSION.RELEASE);
                    deviceData.put("DeviceDescription", deviceDescription);
                    deviceData.put("Login", VoIP.login);
                    deviceData.put("AvatarHash", "");
                    devconf.put("DeviceData", deviceData);
                    Protocol.sendMessage(devconf);
                    break;
                case "CONFIG_SERVER_RESPONSE_NACK":
                    final String reason = msg.getString("Reason");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                             showProgress(false);
                             Toast.makeText(LoginActivity.this, reason, Toast.LENGTH_LONG).show();
                        }
                    });
                   break;
                case "CONFIG_SERVER_RESPONSE_ACK":
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(false);
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                    break;
            }
        }
        catch(Exception e){
            Log.e("LoginActivity", e.toString());
        }
    }

    @Override
    public void OnClose() {
        Log.i("Websocket", "Closed");
    }

    @Override
    public void OnOpen() {
        Log.i("Websocket", "Opened");
    }
}

