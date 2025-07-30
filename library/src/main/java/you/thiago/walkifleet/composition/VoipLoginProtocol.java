package you.thiago.walkifleet.composition;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import you.thiago.walkifleet.Device;
import you.thiago.walkifleet.MSGListener;
import you.thiago.walkifleet.Opus;
import you.thiago.walkifleet.Protocol;
import you.thiago.walkifleet.Random;
import you.thiago.walkifleet.VoIP;

abstract public class VoipLoginProtocol implements MSGListener
{
    abstract Context getProtocolContext();

    abstract void configServerResponseNack(String reason);

    abstract void configServerResponseAck();

    abstract String getVoipLogin();

    abstract String getVoipPassword();

    @Override
    public void OnMessage(JSONObject msg)
    {
        try {
            String message = msg.getString("MessageID");

            switch (message) {
                case "SERVER_CONFIG":
                    configVoip(msg);

                    JSONObject devconf = getDevConfigJson();
                    JSONObject deviceData = getDevConfigData();

                    devconf.put("DeviceData", deviceData);
                    Protocol.sendMessage(devconf);
                    break;
                case "CONFIG_SERVER_RESPONSE_NACK":
                    final String reason = msg.getString("Reason");
                    configServerResponseNack(reason);
                   break;
                case "CONFIG_SERVER_RESPONSE_ACK":
                    configServerResponseAck();
                    break;
            }
        } catch (Exception e) {
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

    protected void configVoip(JSONObject msg) throws JSONException {
        VoIP.DestinationPort = msg.getInt("VoipPort");
        VoIP.SampleRate = msg.getInt("AudioSampleRate");
        VoIP.FrameSize = msg.getInt("AudioFrameSize");

        VoIP.SSRC = Random.nextRandomPositiveInt();

        VoIP.login = getVoipLogin();
        VoIP.password = getVoipPassword();

        VoIP.Codec = new Opus(VoIP.SampleRate, VoIP.FrameSize);
    }

    protected JSONObject getDevConfigJson() throws JSONException {
        JSONObject devConf = new JSONObject();

        devConf.put("MessageID","DEVICE_CONFIG");
        devConf.put("Ssrc", VoIP.SSRC);
        devConf.put("AppName", "APIClientAndroid");
        devConf.put("VersionName", "5.5");
        devConf.put("VersionCode", 1);
        devConf.put("AudioCodec", 0); // Opus
        devConf.put("Password", VoIP.password);

        return devConf;
    }

    protected JSONObject getDevConfigData() throws JSONException {
        String deviceDescription = String.format("MANUFACTURER=%s; MODEL=%s; SERIAL=%s; OSVERSION=%s", Build.MANUFACTURER, Build.MODEL, Build.SERIAL, Build.VERSION.RELEASE);

        JSONObject deviceData = new JSONObject();

        deviceData.put("SessionID", Base64.encodeToString(Protocol.uuidToBytes(UUID.randomUUID()), Base64.NO_WRAP));
        deviceData.put("ID", Base64.encodeToString(Protocol.uuidToBytes(Device.GetDeviceID(getProtocolContext())), Base64.NO_WRAP));
        deviceData.put("StatusID", Base64.encodeToString(Protocol.uuidToBytes(new UUID(0L, 0L)), Base64.NO_WRAP));
        deviceData.put("DeviceDescription", deviceDescription);
        deviceData.put("Login", VoIP.login);
        deviceData.put("AvatarHash", "");

        return deviceData;
    }
}

