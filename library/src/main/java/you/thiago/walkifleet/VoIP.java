package you.thiago.walkifleet;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class VoIP
{
    public static String login = "";
    public static String password = "";
    public static String serverAddress = "";
    public static String DestinationIp = "";
    public static int DestinationPort;
    public static int PayloadType = 106;
    public static int SampleRate;
    public static int FrameSize;
    public static Opus Codec;

    public static AudioRecord record =null;
    public static AudioTrack track =null;

    public static int sequenceNumber = 0;
    public static long timestamp = 0;

    public static InetAddress rtpSocketAddress;
    public static DatagramSocket audioSocket;
    public static RtpSocket rtpAudioSocket;
    public static RtpPacket rtpAudioPacket;
    public static byte[] voiceData;
    public static int SSRC;
    public static Thread audioThread;
    static boolean isClosing = false;
    public static boolean rx = false;
    public static boolean tx = false;

    private static byte[] announceRtpPacket;
    private static Boolean annonceTimerStarted;
    public static Timer annonceTimer;

    static class AnnounceTask extends TimerTask {
        public void run() {
            try {
               rtpAudioSocket.send(announceRtpPacket);
            }
            catch (Exception ex){
                Log.e("VoIP", ex.toString());
            }
        }
    }

    public static void initAudio(Activity activity)
    {
        try {
            activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
            int min = AudioRecord.getMinBufferSize(SampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            record = new AudioRecord(MediaRecorder.AudioSource.MIC, SampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, min);

            int maxJitter = AudioTrack.getMinBufferSize(SampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxJitter, AudioTrack.MODE_STREAM);
        }catch(Exception ex)
        {
            Log.e("ERROR", ex.getMessage());
        }
    }
    public static void StartAnnonceTimer()
    {
        annonceTimer = new Timer();
        annonceTimer.scheduleAtFixedRate(new AnnounceTask(),0, FrameSize);
        annonceTimerStarted = true;
    }

    public static void StopAnnonceTimer()
    {
        try {
            if (annonceTimerStarted && annonceTimer != null) {
                annonceTimer.cancel();
                annonceTimerStarted = false;
            }
        }catch(Exception ex){
            Log.e("StopAnnonceTimer", ex.toString());
        }
    }

    public static void startAudioProxy()
    {
        try {
            isClosing = false;
            rtpSocketAddress = InetAddress.getByName(DestinationIp);

            audioSocket = new DatagramSocket(null);
            audioSocket.setSoTimeout(250);
            audioSocket.setReceiveBufferSize(8192);
            audioSocket.setSendBufferSize(SampleRate * 2 * FrameSize / 1000);
            audioSocket.setReuseAddress(true);
            audioSocket.bind(new InetSocketAddress(20000));

            rtpAudioSocket = new RtpSocket(audioSocket, rtpSocketAddress, DestinationPort);
            voiceData = new byte[8192];
            rtpAudioPacket = new RtpPacket(voiceData, 8192);
            rtpAudioPacket.setSscr(SSRC);
            rtpAudioPacket.setVersion(2);
            rtpAudioPacket.setPayloadType(PayloadType);
            rtpAudioPacket.setSequenceNumber(0);
            rtpAudioPacket.setTimestamp(0);

            //Announce rtp packet is sent when incoming voice is expected to arrive.
            RtpPacket arp = new RtpPacket(new byte[12], 12);
            arp.setSscr(SSRC);
            arp.setVersion(2);
            arp.setPayloadType(PayloadType);
            arp.setSequenceNumber(0);
            arp.setTimestamp(0);
            arp.setPayload(new byte[0], 0);
            announceRtpPacket = arp.getPacket();

            audioThread = new Thread(
                    new Runnable() {
                        public void run() {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                            while (!isClosing) {
                                if (rx && !tx) {
                                    try {
                                        RtpPacket packet = new RtpPacket(voiceData, 8192);
                                        rtpAudioSocket.receive(packet);
                                        byte[] encoded = packet.getPayload();

                                        if (encoded != null) {
                                            StopAnnonceTimer();
                                            short[] decoded = VoIP.Codec.Decode(encoded);
                                            VoIP.track.write(decoded, 0, decoded.length);
                                        }
                                    }
                                    catch (Exception ex)
                                    {
                                        String msg = ex.getLocalizedMessage();
                                        if (msg != null)
                                            Log.e("RTP", ex.getMessage());
                                    }
                                }
                                while (tx) {
                                    try {
                                        short[] bufferPCM = new short[SampleRate * FrameSize / 1000];
                                        int readCount = VoIP.record.read(bufferPCM,0, bufferPCM.length);
                                        if (readCount > 0)
                                        {
                                            byte[] encoded = VoIP.Codec.Encode(bufferPCM);

                                            sequenceNumber++;
                                            timestamp += 480;
                                            rtpAudioPacket.setSequenceNumber(sequenceNumber);
                                            rtpAudioPacket.setTimestamp(timestamp);
                                            rtpAudioPacket.setSscr(SSRC);
                                            rtpAudioPacket.setPayload(encoded, encoded.length);
                                            rtpAudioSocket.send(rtpAudioPacket);
                                        }
                                    } catch (Exception ex) {
                                        Log.e("VoIP", ex.toString());
                                    }
                                }
                            }
                        }
                    }
            );
            audioThread.start();
        } catch (Exception e) {
            Log.e("VoIP", e.toString());
        }
    }

    public static void stopAudioProxy()
    {
        isClosing = true;
        if (rtpAudioSocket != null) {
            rtpAudioSocket.close();
            rtpAudioSocket = null;
            audioSocket = null;
        }
        if(audioThread != null) audioThread = null;
    }
}
