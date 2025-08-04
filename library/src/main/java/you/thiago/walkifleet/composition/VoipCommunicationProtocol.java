package you.thiago.walkifleet.composition;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import you.thiago.walkifleet.MSGListener;
import you.thiago.walkifleet.Process;
import you.thiago.walkifleet.Protocol;
import you.thiago.walkifleet.Random;
import you.thiago.walkifleet.VoIP;
import you.thiago.walkifleet.helpers.Util;

abstract public class VoipCommunicationProtocol implements MSGListener
{
    public VoipCommunicationProtocol(Activity activity)
    {
        CommunicationProcess.DeviceId = Util.getDeviceId(activity);

        VoIP.initAudio(activity);

        try {
            if (Protocol.isInitialLogin) {
                Protocol.isInitialLogin = false;
                JSONObject login = new JSONObject();
                login.put("MessageID", "LOGIN");
                Protocol.sendMessage(login);
            }
        }
        catch(Exception ignore) {}
    }

    protected abstract Context getProtocolContext();

    protected abstract Process.ProcessControl getProcessControl();

    protected abstract void onUpdateDataEx(int dataType);

    protected abstract void onPptResponse(int response);

    protected abstract void configServerResponseNack(String reason);

    protected void configServerResponseAck() {
        try{
            JSONObject login = new JSONObject();
            login.put("MessageID", "LOGIN");
            Protocol.sendMessage(login);
        } catch(Exception ignore) {}
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

                    JSONObject devConf = getDevConfigJson();
                    JSONObject deviceData = getDevConfigData();

                    devConf.put("DeviceData", deviceData);
                    Protocol.sendMessage(devConf);
                    break;
                case "CONFIG_SERVER_RESPONSE_NACK":
                    final String reason = msg.getString("Reason");
                    configServerResponseNack(reason);
                    break;
                case "CONFIG_SERVER_RESPONSE_ACK":
                    configServerResponseAck();
                    break;
                case "LOGIN_RESPONSE":
                    Protocol.isConnected = true;
                    CommunicationProcess.UserId = msg.getString("UserID");
                    VoIP.startAudioProxy();
                    break;
                case "DEVICE_CONTEXT":
                    SetPingTimer();
                    Protocol.PingTimeOut = msg.getInt("PingTimeout");
                    CommunicationProcess.Process.pingTimer = new Timer();
                    CommunicationProcess.Process.pingTimer.scheduleAtFixedRate(new Protocol.PingTask(), Protocol.PingTimeOut, Protocol.PingTimeOut);
                    break;
                case "PING":
                    Protocol.PingTimeOutTicks = 0;
                    break;
                case "DATAEX":
                    final int dataType = msg.getInt("DataType");
                    final int opType = msg.getInt("Operation");
                    final JSONArray objects = msg.getJSONArray("DataObjects");

                    switch (dataType)
                    {
                        case 12: //GROUPS
                            CommunicationProcess.Process.ProcessDataxGroups(objects, opType);
                            break;
                        case 10: //DEVICES
                            CommunicationProcess.Process.ProcessDataxUsers(objects, opType, CommunicationProcess.DeviceId);
                            break;
                    }

                    this.onUpdateDataEx(dataType);
                    break;
                case "PTT_RESPONSE":
                    final int response = msg.getInt("Response");
                    onPptResponse(response);
                    break;
                case "PTT_CONTROL":
                    final JSONObject pttmsg = msg;
                    new Handler(Looper.getMainLooper()).post(() -> CommunicationProcess.Process.ProcessPTTControl(pttmsg, CommunicationProcess.DeviceId, getProcessControl()));
                    break;
            }
        }
        catch(Exception e) {
            Log.e("VoipComProtocol", e.toString());
        }
    }

    private JSONObject getDevConfigJson() throws JSONException {
        JSONObject devconf = new JSONObject();
        devconf.put("MessageID","DEVICE_CONFIG");
        devconf.put("Ssrc", VoIP.SSRC);
        devconf.put("AppName", "APIClientAndroid");
        devconf.put("VersionName", "5.5");
        devconf.put("VersionCode", 1);
        devconf.put("AudioCodec", 0); // Opus
        devconf.put("Password", VoIP.password);
        return devconf;
    }

    private JSONObject getDevConfigData() throws JSONException {
        String deviceDescription = String.format("MANUFACTURER=%s; MODEL=%s; SERIAL=%s; OSVERSION=%s", android.os.Build.MANUFACTURER, android.os.Build.MODEL, android.os.Build.SERIAL, android.os.Build.VERSION.RELEASE);

        JSONObject deviceData = new JSONObject();
        deviceData.put("SessionID", Base64.encodeToString(Protocol.uuidToBytes(UUID.randomUUID()), Base64.NO_WRAP));
        deviceData.put("ID", Util.getDeviceId(getProtocolContext()));
        deviceData.put("StatusID", Base64.encodeToString(Protocol.uuidToBytes(new UUID(0L, 0L)), Base64.NO_WRAP));
        deviceData.put("DeviceDescription", deviceDescription);
        deviceData.put("Login", VoIP.login);
        deviceData.put("AvatarHash", "");

        return deviceData;
    }

    @Override
    public void OnClose() {
        Log.i("Websocket", "Closed");
    }

    @Override
    public void OnOpen() {
        Log.i("Websocket", "Opened");
    }

    private void SetPingTimer()
    {
        if (CommunicationProcess.Process.pingTimer != null) {
            CommunicationProcess.Process.pingTimer.cancel();
        }

        CommunicationProcess.Process.pingTimer = new Timer();

        CommunicationProcess.Process.pingTimer.scheduleAtFixedRate(new TimerTask()
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
}

