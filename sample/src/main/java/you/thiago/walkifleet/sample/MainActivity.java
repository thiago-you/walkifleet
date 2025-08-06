package you.thiago.walkifleet.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import you.thiago.walkifleet.Process;
import you.thiago.walkifleet.Protocol;
import you.thiago.walkifleet.VoIP;
import you.thiago.walkifleet.composition.CommunicationProcess;
import you.thiago.walkifleet.composition.VoipCommunicationProtocol;
import you.thiago.walkifleet.ui.WalkiTalksView;

public class MainActivity extends AppCompatActivity
{
    private WalkiTalksView walkiTalksView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Protocol.msgListener = new MainProtocol(this);

        walkiTalksView = findViewById(R.id.walki_talks);
    }

    private class MainProtocol extends VoipCommunicationProtocol {

        public MainProtocol(Context context) {
            super(context);
        }

        protected Context getProtocolContext() {
            return MainActivity.this;
        }

        @Override
        protected Process.ProcessControl getProcessControl() {
            return findViewById(R.id.walki_talks);
        }

        @Override
        protected void initAudioVoip(Activity activity) {
            VoIP.initAudio(activity);
        }

        @Override
        protected void onUpdateDataEx(int dataType) {
            runOnUiThread(() -> {
                switch (dataType)
                {
                    case 12: //GROUPS
                        walkiTalksView.updateGroupData();
                        break;
                    case 10: //DEVICES
                        walkiTalksView.updateUserData();
                        break;
                }
            });
        }

        @Override
        protected void onPptResponse(int response) {
            runOnUiThread(() -> {
                if(response == 1) // Busy
                    Toast.makeText(MainActivity.this, "CHANNEL BUSY!", Toast.LENGTH_SHORT).show();;
            });
        }

        @Override
        protected void configServerResponseNack(String reason) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show());
        }

        @Override
        public void OnClose() {
            runOnUiThread(() -> {
                walkiTalksView.setSelectedViewMessage("Reconnecting ...");
                walkiTalksView.updateGroupData();

                CommunicationProcess.Process.groupObjects.clear();
                CommunicationProcess.Process.userObjects.clear();

                walkiTalksView.updateGroupData();
                walkiTalksView.updateUserData();

                // reconnect again
                Protocol.connectWebSocket(VoIP.serverAddress);
            });
        }

        @Override
        public void OnOpen() {
            runOnUiThread(() -> walkiTalksView.setSelectedViewMessage(""));
        }
    }
}
