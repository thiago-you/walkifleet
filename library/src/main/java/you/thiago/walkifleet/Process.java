package you.thiago.walkifleet;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;

public class Process {
    public static final int PING_INTERVAL = 5000;

    public String SelectedObjectId = "";
    public String SelectedObjectName = "";
    public String SelectedUserId = "";
    public String SelectedObjectType = "";
    public String ActiveCallId = "";
    public ArrayList<FleetObject> groupObjects = new ArrayList<FleetObject>();
    public ArrayList<FleetObject> userObjects = new ArrayList<FleetObject>();
    public Timer pingTimer;

    public void ProcessPTTControl(JSONObject jptt, String DeviceID, ProcessControl processControl)
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
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_ENTER:
                    ActiveCallId = callId;
                    SelectedObjectType = "private";
                    if (sourceId.equals(DeviceID)) {
                        SelectedObjectName = targetName;
                        SelectedObjectId = targetId;
                    } else {
                        SelectedObjectName = sourceName;
                        SelectedObjectId = sourceId;
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_PRESSED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_PRESSED:
                    if (ActiveCallId.equals(callId)) {
                        if (sourceId.equals(DeviceID)) {
                            VoIP.record.startRecording();
                            VoIP.tx = true;
                        } else {
                            VoIP.StartAnnonceTimer();
                            VoIP.track.play();
                            VoIP.rx = true;
                        }
                    }
                    break;
                case Protocol.PTT_CONTROL.VOICE_PRIVATE_RELEASED:
                case Protocol.PTT_CONTROL.VOICE_GROUP_RELEASED:
                    VoIP.StopAnnonceTimer();
                    if (ActiveCallId.equals(callId)) {
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
                    if (ActiveCallId.equals(callId)) {
                        ActiveCallId = "";
                        SelectedObjectId = "";
                        SelectedObjectName = "";
                        SelectedObjectType = "";
                    }
                    break;
            }

            processControl.processControl(jptt);
        }
        catch (Exception ex)
        {
            Log.e("PTT_CONTROL", ex.toString());
        }
    }

    public void ProcessDataxGroups(JSONArray grps, int op)
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

    public void ProcessDataxUsers(JSONArray usrs, int op, String DeviceID)
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

    public interface ProcessControl {
        void processControl(JSONObject jptt);
    }
}
