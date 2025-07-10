package you.thiago.walkifleet;

import org.json.JSONObject;

public interface MSGListener
{
    public void OnMessage(JSONObject msg);
    public void OnClose();
    public void OnOpen();
}
