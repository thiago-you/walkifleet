package you.thiago.walkifleet;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Protocol
{
    public class PTT_CONTROL
    {
        public static final byte VOICE_PRIVATE_BEGIN = 0;
        public static final byte VOICE_PRIVATE_PRESSED = 1;
        public static final byte VOICE_PRIVATE_RELEASED = 2;
        public static final byte VOICE_PRIVATE_END = 3;
        public static final byte VOICE_GROUP_BEGIN = 4;
        public static final byte VOICE_GROUP_PRESSED = 5;
        public static final byte VOICE_GROUP_RELEASED = 6;
        public static final byte VOICE_GROUP_END = 7;
        public static final byte VOICE_PRIVATE_ENTER = 9;
        public static final byte VOICE_GROUP_ENTER = 10;

        public static final byte VIDEO_PRIVATE_BEGIN = 11;
        public static final byte VIDEO_PRIVATE_PRESSED = 12;
        public static final byte VIDEO_PRIVATE_RELEASED = 13;
        public static final byte VIDEO_PRIVATE_END = 14;
        public static final byte VIDEO_GROUP_BEGIN = 15;
        public static final byte VIDEO_GROUP_PRESSED = 16;
        public static final byte VIDEO_GROUP_RELEASED = 17;
        public static final byte VIDEO_GROUP_END = 18;
        public static final byte VIDEO_PRIVATE_ENTER = 20;
        public static final byte VIDEO_GROUP_ENTER = 21;
    }

    public class PTT_REQUEST
    {
        public static final byte VOICE_PRIVATE_PRESS = 0;
        public static final byte VOICE_PRIVATE_RELEASE = 1;
        public static final byte VOICE_GROUP_PRESS = 2;
        public static final byte VOICE_GROUP_RELEASE = 3;

        public static final byte VIDEO_PRIVATE_PRESS = 4;
        public static final byte VIDEO_PRIVATE_RELEASE = 5;
        public static final byte VIDEO_GROUP_PRESS = 6;
        public static final byte VIDEO_GROUP_RELEASE = 7;
    }

    public class PTT_RESPONSE
    {
        public static final byte OK = 0;
        public static final byte DECLINE_BUSY = 1;
        public static final byte DECLINE_UNKNOWN = 2;
    }

    public static boolean isInitialLogin;
    public static boolean isConnected;

    public static int PingTimeOut;
    public static int PingTimeOutTicks;
    public static Timer pingTimer;
    public static Object wscLock = new Object();

    public static class PingTask extends TimerTask {
        public void run() {
            try {
                PingTimeOutTicks++;
                if(PingTimeOutTicks > 1)
                {
                    PingTimeOutTicks = 0;
                    wsc.closeConnection(1000, "");
                }
            }
            catch (Exception ex){
            }
        }
    }

    static WebSocketClient wsc;
    public static MSGListener msgListener;
    public static void connectWebSocket(String wsAddress) {
        URI uri;
        try {
            uri = new URI("wss://"+wsAddress);
            Log.i("Websocket", "adress " + uri.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        wsc = new WebSocketClient(uri, new Draft_6455(), new HashMap<>(), 1000) {
            @Override
            public void reconnect() {
                super.reconnect();
                Log.i("Websocket", "Recconecting");
            }

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                msgListener.OnOpen();
                Log.i("Websocket", "Opened");
            }

            @Override
            public void onMessage(String s) {
                try {
                    JSONArray msgs = new JSONArray(s);
                    for (int i = 0; i < msgs.length(); i++){
                         msgListener.OnMessage(new JSONObject(msgs.getString(i)));
                    }
                }
                catch (JSONException e){
                    Log.e("Websocket", e.toString());
                }
            }
            @Override
            public void onClose(int i, String s, boolean b) {
                VoIP.stopAudioProxy();
                isConnected = false;
                msgListener.OnClose();
                if(pingTimer != null) pingTimer.cancel();
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }

            @Override
            public void onClosing(int code, String reason, boolean remote) {
                super.onClosing(code, reason, remote);
                Log.i("Websocket", "Closing");
            }
        };
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {

                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {

                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {

                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            }, new SecureRandom());

            SSLSocketFactory factory = sslContext.getSocketFactory();

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            wsc.setSocketFactory(factory);

            wsc.setTcpNoDelay(true);
            wsc.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void sendMessage(JSONObject obj)
    {
        synchronized (wscLock)
        {
            JSONArray arr = new JSONArray();
            arr.put(obj);
            String s = arr.toString();
            wsc.send(arr.toString());
        }
    }
    public static byte[] uuidToBytes(UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
