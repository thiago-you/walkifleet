package you.thiago.walkifleet.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONObject;

import you.thiago.walkifleet.FleetObject;
import you.thiago.walkifleet.Process;
import you.thiago.walkifleet.Protocol;
import you.thiago.walkifleet.R;
import you.thiago.walkifleet.composition.CommunicationProcess;

public class WalkiTalksView extends FrameLayout implements Process.ProcessControl {
    private ListView groupView, userView;
    private TextView selectedView;
    private Button pttBtn;

    public WalkiTalksView(Context context) {
        super(context);
        init(context);
    }

    public WalkiTalksView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WalkiTalksView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.walki_talks_view, this, true);
        setup();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setup() {
        groupView = findViewById(R.id.groupView);
        userView = findViewById(R.id.userView);
        selectedView = findViewById(R.id.selectedObjectView);
        pttBtn = findViewById(R.id.pttBtn);

        ObjectListAdapter groupAdapter = new ObjectListAdapter(getRootView().getContext(), R.layout.rowlayout, CommunicationProcess.Process.groupObjects);
        groupView.setAdapter(groupAdapter);

        ObjectListAdapter userAdapter = new ObjectListAdapter(getRootView().getContext(), R.layout.rowlayout, CommunicationProcess.Process.userObjects);
        userView.setAdapter(userAdapter);

        groupView.setOnItemClickListener((parent, itemClicked, position, id) -> {
            groupView.setSelector(R.color.walkifleet_colorSelector);
            groupView.setSelected(true);

            FleetObject fo = CommunicationProcess.Process.groupObjects.get(position);

            CommunicationProcess.Process.SelectedObjectName = fo.objectName;
            CommunicationProcess.Process.SelectedObjectId = fo.objectId;
            CommunicationProcess.Process.SelectedUserId = fo.userId;
            CommunicationProcess.Process.SelectedObjectType = "group";

            selectedView.setText(CommunicationProcess.Process.SelectedObjectName);
            userView.setSelected(false);
            userView.setSelector(android.R.color.transparent);
        });

        userView.setOnItemClickListener((parent, itemClicked, position, id) -> {
            userView.setSelector(R.color.walkifleet_colorSelector);
            userView.setSelected(true);

            FleetObject fo = CommunicationProcess.Process.userObjects.get(position);

            CommunicationProcess.Process.SelectedObjectName = fo.objectName;
            CommunicationProcess.Process.SelectedObjectId = fo.objectId;
            CommunicationProcess.Process.SelectedUserId = fo.userId;
            CommunicationProcess.Process.SelectedObjectType = "private";

            selectedView.setText(CommunicationProcess.Process.SelectedObjectName);
            groupView.setSelected(false);
            groupView.setSelector(android.R.color.transparent);
        });

        pttBtn.setOnTouchListener((arg0, arg1) -> {
            try
            {
                int action = arg1.getAction();

                if (action == MotionEvent.ACTION_DOWN) {
                    if (CommunicationProcess.Process.SelectedObjectId.isEmpty()) {
                        return false;
                    }

                    JSONObject pttRequest = new JSONObject();
                    pttRequest.put("MessageID", "PTT_REQUEST");
                    pttRequest.put("Destination", CommunicationProcess.Process.SelectedObjectId);
                    pttRequest.put("Type", CommunicationProcess.Process.SelectedObjectType.equals("group") ? Protocol.PTT_REQUEST.VOICE_GROUP_PRESS : Protocol.PTT_REQUEST.VOICE_PRIVATE_PRESS);

                    Protocol.sendMessage(pttRequest);

                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                    if (CommunicationProcess.Process.SelectedObjectId.isEmpty()) {
                        return false;
                    }

                    JSONObject pttRequest = new JSONObject();
                    pttRequest.put("MessageID", "PTT_REQUEST");
                    pttRequest.put("Destination", CommunicationProcess.Process.SelectedObjectId);
                    pttRequest.put("Type", CommunicationProcess.Process.SelectedObjectType.equals("group") ? Protocol.PTT_REQUEST.VOICE_GROUP_RELEASE : Protocol.PTT_REQUEST.VOICE_PRIVATE_RELEASE);

                    Protocol.sendMessage(pttRequest);

                    return true;
                }

                return false;
            } catch (Exception e) {
                return  false;
            }
        });
    }

    public void updateGroupData() {
        if (groupView != null) {
            ((ObjectListAdapter) groupView.getAdapter()).notifyDataSetChanged();
        }
    }

    public void updateUserData() {
        if (userView != null) {
            ((ObjectListAdapter) userView.getAdapter()).notifyDataSetChanged();
        }
    }

    public void setSelectedViewMessage(String message) {
        if (selectedView != null) {
            selectedView.setText(message);
        }
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
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_ENTER:
                    selectedView.setText(CommunicationProcess.Process.SelectedObjectName);
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_PRESSED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_PRESSED:
                    if (CommunicationProcess.Process.ActiveCallId.equals(callId)) {
                        if (sourceId.equals(CommunicationProcess.DeviceId)) {
                            pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                        } else {
                            pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                        }
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_RELEASED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_RELEASED:
                    if (CommunicationProcess.Process.ActiveCallId.equals(callId)) {
                        pttBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_END:
                case Protocol.PTT_CONTROL.VOICE_GROUP_END:
                    if (CommunicationProcess.Process.ActiveCallId.equals(callId)) {
                        pttBtn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

                        if (groupView.isSelected()) {
                            FleetObject fo = CommunicationProcess.Process.groupObjects.get(groupView.getCheckedItemPosition());
                            CommunicationProcess.Process.SelectedObjectId = fo.objectId;
                            CommunicationProcess.Process.SelectedObjectName = fo.objectName;
                            CommunicationProcess.Process.SelectedUserId = "";
                            CommunicationProcess.Process.SelectedObjectType = "group";
                        } else if (userView.isSelected()) {
                            FleetObject fo = CommunicationProcess.Process.userObjects.get(userView.getCheckedItemPosition());
                            CommunicationProcess.Process.SelectedObjectId = fo.objectId;
                            CommunicationProcess.Process.SelectedObjectName = fo.objectName;
                            CommunicationProcess.Process.SelectedUserId = fo.userId;
                            CommunicationProcess.Process.SelectedObjectType = "private";
                        }

                        selectedView.setText(CommunicationProcess.Process.SelectedObjectName);
                    }
                    break;
            }
        }
        catch (Exception ex)
        {
            Log.e("PTT_CONTROL", ex.toString());
        }
    }
}

