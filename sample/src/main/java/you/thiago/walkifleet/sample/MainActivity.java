package you.thiago.walkifleet.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import you.thiago.walkifleet.Device;
import you.thiago.walkifleet.FleetObject;
import you.thiago.walkifleet.MSGListener;
import you.thiago.walkifleet.Process;
import you.thiago.walkifleet.Protocol;
import you.thiago.walkifleet.Random;
import you.thiago.walkifleet.VoIP;

public class MainActivity extends AppCompatActivity implements MSGListener, Process.ProcessControl
{
    private static final you.thiago.walkifleet.Process Process = new Process();

    String UserID, DeviceID;
    ListView groupView, userView;
    TextView selectedView;
    Button pttBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Protocol.msgListener = this;
        DeviceID = Base64.encodeToString(Protocol.uuidToBytes(Device.GetDeviceID(this)), Base64.NO_WRAP);
        VoIP.initAudio(this);
        groupView = (ListView)findViewById(R.id.groupView);
        userView = (ListView)findViewById(R.id.userView);
        selectedView = (TextView)findViewById(R.id.selectedObjectView);
        pttBtn = (Button) findViewById(R.id.pttBtn);
        ObjectListAdapter groupAdapter = new ObjectListAdapter(this, R.layout.rowlayout, Process.groupObjects);
        groupView.setAdapter(groupAdapter);
        ObjectListAdapter userAdapter = new ObjectListAdapter(this, R.layout.rowlayout, Process.userObjects);
        userView.setAdapter(userAdapter);
        groupView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                groupView.setSelector(R.color.colorSelector);
                groupView.setSelected(true);
                FleetObject fo = Process.groupObjects.get(position);
                Process.SelectedObjectName = fo.objectName;
                Process.SelectedObjectId = fo.objectId;
                Process.SelectedUserId = fo.userId;
                Process.SelectedObjectType = "group";
                selectedView.setText(Process.SelectedObjectName);
                userView.setSelected(false);
                userView.setSelector(android.R.color.transparent);
            }
        });
        userView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                userView.setSelector(R.color.colorSelector);
                userView.setSelected(true);
                FleetObject fo = Process.userObjects.get(position);
                Process.SelectedObjectName = fo.objectName;
                Process.SelectedObjectId = fo.objectId;
                Process.SelectedUserId = fo.userId;
                Process.SelectedObjectType = "private";
                selectedView.setText(Process.SelectedObjectName);
                groupView.setSelected(false);
                groupView.setSelector(android.R.color.transparent);
            }
        });

        pttBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                try
                {
                    int action = arg1.getAction();
                    if(action == MotionEvent.ACTION_DOWN) {
                        if (Process.SelectedObjectId.equals("")) return false;
                        JSONObject pttrequest = new JSONObject();
                        pttrequest.put("MessageID", "PTT_REQUEST");
                        pttrequest.put("Destination", Process.SelectedObjectId);
                        pttrequest.put("Type", Process.SelectedObjectType.equals("group") ? Protocol.PTT_REQUEST.VOICE_GROUP_PRESS : Protocol.PTT_REQUEST.VOICE_PRIVATE_PRESS);
                        Protocol.sendMessage(pttrequest);
                        return true;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                        if (Process.SelectedObjectId.equals("")) return false;
                        JSONObject pttrequest = new JSONObject();
                        pttrequest.put("MessageID", "PTT_REQUEST");
                        pttrequest.put("Destination", Process.SelectedObjectId);
                        pttrequest.put("Type", Process.SelectedObjectType.equals("group") ? Protocol.PTT_REQUEST.VOICE_GROUP_RELEASE : Protocol.PTT_REQUEST.VOICE_PRIVATE_RELEASE);
                        Protocol.sendMessage(pttrequest);
                        return true;
                    }
                    return false;
                }
                catch (Exception e){ return  false; }
            }
        });

        try {
            if(Protocol.isInitialLogin) {
                Protocol.isInitialLogin = false;
                JSONObject login = new JSONObject();
                login.put("MessageID", "LOGIN");
                Protocol.sendMessage(login);
            }
        }
        catch(Exception e){}
    }
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
                            Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                case "CONFIG_SERVER_RESPONSE_ACK":
                    try{
                        JSONObject login = new JSONObject();
                        login.put("MessageID", "LOGIN");
                        Protocol.sendMessage(login);
                    }catch(Exception ex){}
                    break;
                case "LOGIN_RESPONSE":
                    Protocol.isConnected = true;
                    UserID = msg.getString("UserID");
                    VoIP.startAudioProxy();
                    break;
                case "DEVICE_CONTEXT":
                    SetPingTimer();
                    Protocol.PingTimeOut = msg.getInt("PingTimeout");
                    Process.pingTimer = new Timer();
                    Process.pingTimer.scheduleAtFixedRate(new Protocol.PingTask(),Protocol.PingTimeOut,Protocol.PingTimeOut);
                    break;
                case "PING":
                    Protocol.PingTimeOutTicks = 0;
                    break;
                case "DATAEX":
                    final int dataType = msg.getInt("DataType");
                    final int opType = msg.getInt("Operation");
                    final JSONArray objects = msg.getJSONArray("DataObjects");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (dataType)
                            {
                                case 12: //GROUPS
                                    Process.ProcessDataxGroups(objects, opType);
                                    ((ObjectListAdapter)groupView.getAdapter()).notifyDataSetChanged();
                                    break;
                                case 10: //DEVICES
                                    Process.ProcessDataxUsers(objects, opType, DeviceID);
                                    ((ObjectListAdapter)userView.getAdapter()).notifyDataSetChanged();
                                    break;
                            }
                        }
                    });
                    break;
                case "PTT_RESPONSE":
                    final int response = msg.getInt("Response");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(response == 1) //Busy
                                Toast.makeText(MainActivity.this, "CHANNEL BUSY!", Toast.LENGTH_SHORT).show();;
                        }
                    });
                    break;
                case "PTT_CONTROL":
                    final JSONObject pttmsg = msg;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Process.ProcessPTTControl(pttmsg, DeviceID, MainActivity.this);
                        }
                    });
                    break;
            }
        }
        catch(Exception e){}
    }

    @Override
    public void processControl(JSONObject jptt)
    {
        try
        {
            int control = jptt.getInt("Control");
            String sourceId = jptt.getString("SourceID");
            String callId = jptt.getString("CallID");

            switch (control) {
                case Protocol.PTT_CONTROL.VOICE_GROUP_ENTER:
                    selectedView.setText(Process.SelectedObjectName);
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_ENTER:
                    selectedView.setText(Process.SelectedObjectName);
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_PRESSED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_PRESSED:
                    if (Process.ActiveCallId.equals(callId)) {
                        if (sourceId.equals(DeviceID)) {
                            pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                        } else {
                            pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                        }
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_RELEASED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_RELEASED:
                    if (Process.ActiveCallId.equals(callId)) {
                        pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_END:
                case Protocol.PTT_CONTROL.VOICE_GROUP_END:
                    if (Process.ActiveCallId.equals(callId)) {
                        pttBtn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

                        if (groupView.isSelected()) {
                            FleetObject fo = Process.groupObjects.get(groupView.getCheckedItemPosition());
                            Process.SelectedObjectId = fo.objectId;
                            Process.SelectedObjectName = fo.objectName;
                            Process.SelectedUserId = "";
                            Process.SelectedObjectType = "group";
                        } else if (userView.isSelected()) {
                            FleetObject fo = Process.userObjects.get(userView.getCheckedItemPosition());
                            Process.SelectedObjectId = fo.objectId;
                            Process.SelectedObjectName = fo.objectName;
                            Process.SelectedUserId = fo.userId;
                            Process.SelectedObjectType = "private";
                        }

                        selectedView.setText(Process.SelectedObjectName);
                    }
                    break;
            }
        }
        catch (Exception ex)
        {
            Log.e("PTT_CONTROL", ex.toString());
        }
    }

    private void SetPingTimer()
    {
        if (Process.pingTimer != null)
            Process.pingTimer.cancel();

        Process.pingTimer = new Timer();
        Process.pingTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    JSONObject ping = new JSONObject();
                    ping.put("MessageID", "PING");
                    Protocol.sendMessage(ping);
                }
                catch (Exception e)
                {
                }
            }
        }, Process.PING_INTERVAL, Process.PING_INTERVAL);
    }

    @Override
    public void OnClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectedView.setText("Reconnecting ...");
                Process.groupObjects.clear();
                Process.userObjects.clear();
                ((ObjectListAdapter)groupView.getAdapter()).notifyDataSetChanged();
                ((ObjectListAdapter)userView.getAdapter()).notifyDataSetChanged();
                //reconnect again
                Protocol.connectWebSocket(VoIP.serverAddress);
            }
        });
    }

    @Override
    public void OnOpen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectedView.setText("");
            }
        });
    }
}
