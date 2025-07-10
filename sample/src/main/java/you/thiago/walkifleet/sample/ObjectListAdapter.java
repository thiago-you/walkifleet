package you.thiago.walkifleet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ObjectListAdapter extends ArrayAdapter<FleetObject>
{
    public List<FleetObject> fleetObjects;
    Context context;
    public ObjectListAdapter(Context context, int resource, List<FleetObject> objects)
    {
        super(context, resource, objects);
        this.fleetObjects = objects;
        this.context = context;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
        TextView labelView = (TextView) rowView.findViewById(R.id.label);
        labelView.setText(fleetObjects.get(position).objectName);
        TextView idView = (TextView) rowView.findViewById(R.id.objectid);
        idView.setText(fleetObjects.get(position).objectId);
        TextView useridView = (TextView) rowView.findViewById(R.id.userid);
        useridView.setText(fleetObjects.get(position).userId);

        return rowView;
    }

}
