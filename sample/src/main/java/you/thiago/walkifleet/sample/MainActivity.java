package you.thiago.walkifleet;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements MSGListener
{
    private static final int PING_INTERVAL = 5000;

    String UserID, DeviceID;
    ListView groupView, userView;
    TextView selectedView;
    Button pttBtn;
    String SelectedObjectId = "";
    String SelectedObjectName = "";
    String SelectedUserId = "";
    String SelectedObjectType = "";
    String ActiveCallId = "";
    public ArrayList<FleetObject> groupObjects = new ArrayList<FleetObject>();
    public ArrayList<FleetObject> userObjects = new ArrayList<FleetObject>();
    Timer pingTimer;

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
        ObjectListAdapter groupAdapter = new ObjectListAdapter(this, R.layout.rowlayout, groupObjects);
        groupView.setAdapter(groupAdapter);
        ObjectListAdapter userAdapter = new ObjectListAdapter(this, R.layout.rowlayout, userObjects);
        userView.setAdapter(userAdapter);
        groupView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                groupView.setSelector(R.color.colorSelector);
                groupView.setSelected(true);
                FleetObject fo = groupObjects.get(position);
                SelectedObjectName = fo.objectName;
                SelectedObjectId = fo.objectId;
                SelectedUserId = fo.userId;
                SelectedObjectType = "group";
                selectedView.setText(SelectedObjectName);
                userView.setSelected(false);
                userView.setSelector(android.R.color.transparent);
            }
        });
        userView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                userView.setSelector(R.color.colorSelector);
                userView.setSelected(true);
                FleetObject fo = userObjects.get(position);
                SelectedObjectName = fo.objectName;
                SelectedObjectId = fo.objectId;
                SelectedUserId = fo.userId;
                SelectedObjectType = "private";
                selectedView.setText(SelectedObjectName);
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
                        if (SelectedObjectId.equals("")) return false;
                        JSONObject pttrequest = new JSONObject();
                        pttrequest.put("MessageID", "PTT_REQUEST");
                        pttrequest.put("Destination", SelectedObjectId);
                        pttrequest.put("Type", SelectedObjectType.equals("group") ? Protocol.PTT_REQUEST.VOICE_GROUP_PRESS : Protocol.PTT_REQUEST.VOICE_PRIVATE_PRESS);
                        Protocol.sendMessage(pttrequest);
                        return true;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                        if (SelectedObjectId.equals("")) return false;
                        JSONObject pttrequest = new JSONObject();
                        pttrequest.put("MessageID", "PTT_REQUEST");
                        pttrequest.put("Destination", SelectedObjectId);
                        pttrequest.put("Type", SelectedObjectType.equals("group") ? Protocol.PTT_REQUEST.VOICE_GROUP_RELEASE : Protocol.PTT_REQUEST.VOICE_PRIVATE_RELEASE);
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
                    pingTimer = new Timer();
                    pingTimer.scheduleAtFixedRate(new Protocol.PingTask(),Protocol.PingTimeOut,Protocol.PingTimeOut);
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
                                ProcessDataxGroups(objects, opType);
                                ((ObjectListAdapter)groupView.getAdapter()).notifyDataSetChanged();
                                break;
                            case 10: //DEVICES
                                ProcessDataxUsers(objects, opType);
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
                            ProcessPTTControl(pttmsg);
                        }
                    });
                    break;
            }
        }
        catch(Exception e){}
    }

    private void SetPingTimer()
    {
        if (pingTimer != null)
            pingTimer.cancel();

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask()
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
        }, PING_INTERVAL, PING_INTERVAL);
    }

    @Override
    public void OnClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectedView.setText("Reconnecting ...");
                groupObjects.clear();
                userObjects.clear();
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

    private void ProcessPTTControl(JSONObject jptt)
    {
        try
        {
            int control = jptt.getInt("Control");
            String sourceId = jptt.getString("SourceID");
            String sourceName = jptt.getString("SourceName");
            String targetId = jptt.getString("TargetID");
            String targetName = jptt.getString("TargetName");
            String callId = jptt.getString("CallID");

            switch (control)
            {
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_BEGIN:
                    JSONObject pttConfirm = new JSONObject();
                    pttConfirm.put("MessageID", "PTT_RESPONSE");
                    pttConfirm.put("Destination", sourceId);
                    pttConfirm.put("Response", 0);
                    pttConfirm.put("Type", (int) Protocol.PTT_REQUEST.VOICE_PRIVATE_PRESS);
                    Protocol.sendMessage(pttConfirm);
                    break;
                case Protocol.PTT_CONTROL.VOICE_GROUP_ENTER:
                    ActiveCallId = callId;
                    SelectedObjectType = "group";
                    SelectedObjectName = targetName;
                    SelectedObjectId = targetId;
                    selectedView.setText(SelectedObjectName);
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_ENTER:
                    ActiveCallId = callId;
                    SelectedObjectType = "private";
                    if (sourceId.equals(DeviceID))
                    {
                        SelectedObjectName = targetName;
                        SelectedObjectId = targetId;
                        selectedView.setText(SelectedObjectName);
                    }
                    else
                    {
                        SelectedObjectName = sourceName;
                        SelectedObjectId = sourceId;
                        selectedView.setText(SelectedObjectName);
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_PRESSED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_PRESSED:
                    if (ActiveCallId.equals(callId))
                    {
                        if (sourceId.equals(DeviceID)) //TX
                        {
                            pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                            VoIP.record.startRecording();
                            VoIP.tx = true;
                        }
                        else
                        {
                            VoIP.StartAnnonceTimer();
                            VoIP.track.play();
                            pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                            VoIP.rx = true;
                        }
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_RELEASED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_RELEASED:
                    VoIP.StopAnnonceTimer();
                    if (ActiveCallId.equals(callId))
                    {
                        pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                        if(VoIP.rx) VoIP.track.stop();
                        if(VoIP.tx) VoIP.record.stop();
                        VoIP.tx = false;
                        VoIP.rx = false;
                        VoIP.sequenceNumber = 0;
                        VoIP.timestamp = 0;
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_END:
                case Protocol.PTT_CONTROL.VOICE_GROUP_END:
                    if (ActiveCallId.equals(callId))
                    {
                        pttBtn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        ActiveCallId = "";
                        SelectedObjectId = "";
                        SelectedObjectName = "";
                        SelectedObjectType = "";
                        selectedView.setText(SelectedObjectName);

                        if (groupView.isSelected())
                        {
                            FleetObject fo = groupObjects.get(groupView.getCheckedItemPosition());
                            SelectedObjectId = fo.objectId;
                            SelectedObjectName = fo.objectName;
                            SelectedUserId = "";
                            SelectedObjectType = "group";
                            selectedView.setText(SelectedObjectName);
                        }
                        else if (userView.isSelected())
                        {
                            FleetObject fo = userObjects.get(userView.getCheckedItemPosition());
                            SelectedObjectId = fo.objectId;
                            SelectedObjectName = fo.objectName;
                            SelectedUserId = fo.userId;
                            SelectedObjectType = "private";
                            selectedView.setText(SelectedObjectName);
                        }
                    }
                    break;
            }
        }
        catch (Exception ex)
        {
            Log.e("PTT_CONTROL", ex.toString());
        }
    }
    private void ProcessDataxGroups(JSONArray grps, int op)
    {
        try
        {
            FleetObject grp;
            if (op == 0) groupObjects.clear();
            for (int i = 0; i < grps.length(); i++)
            {
                JSONObject jo = grps.getJSONObject(i);
                switch (op)
                {
                    case 0: //INITIALIZE
                    case 1: //ADD
                        grp = new FleetObject(jo.getString("ID"), jo.getString("Name"), "");
                        groupObjects.add(grp);
                        break;
                    case 3: //CHANGE
                        grp = new FleetObject(jo.getString("ID"), jo.getString("Name"), "");
                        for(int j = 0; j < groupObjects.size(); j++)
                        {
                            if(groupObjects.get(j).objectId.equals(grp.objectId))
                            {
                                groupObjects.get(j).objectName = grp.objectName;
                                break;
                            }
                        }
                        break;
                    case 2: //REMOVE
                        String gid = jo.getString("ID");
                        for(int j = 0; j < groupObjects.size(); j++)
                        {
                            if(groupObjects.get(j).objectId.equals(gid))
                            {
                                groupObjects.remove(j);
                                break;
                            }
                        }
                        break;
                }
            }
        }
        catch(Exception e) { }
    }
    private void ProcessDataxUsers(JSONArray usrs, int op)
    {
        try
        {
            FleetObject usr;
            if (op == 0) userObjects.clear();
            for (int i = 0; i < usrs.length(); i++)
            {
                JSONObject jo = usrs.getJSONObject(i);
                switch (op)
                {
                    case 0: //INITIALIZE
                    case 1: //ADD
                        usr = new FleetObject(jo.getString("ID"), jo.getString("UserName"), jo.getString("UserID"));
                        if(!usr.objectId.equals(DeviceID)) userObjects.add(usr);
                        break;
                    case 3: //CHANGE
                        usr = new FleetObject(jo.getString("ID"), jo.getString("UserName"), jo.getString("UserID"));
                        for(int j = 0; j < userObjects.size(); j++)
                        {
                            if(userObjects.get(j).objectId.equals(usr.objectId))
                            {
                                userObjects.get(j).objectName = usr.objectName;
                                break;
                            }
                        }
                        break;
                    case 2: //REMOVE
                        String uid = jo.getString("ID");
                        for(int j = 0; j < userObjects.size(); j++)
                        {
                            if(userObjects.get(j).objectId.equals(uid))
                            {
                                userObjects.remove(j);
                                break;
                            }
                        }
                        break;
                }
            }
        }
        catch(Exception e) { }
    }
}
