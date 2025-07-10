package you.thiago.walkifleet;

public class FleetObject {
    public String objectId;
    public String objectName;
    public String userId;

    public FleetObject(String _objectid, String _objectname, String _userid)
    {
      objectId = _objectid;
      objectName = _objectname;
      userId = _userid;
    }
}
