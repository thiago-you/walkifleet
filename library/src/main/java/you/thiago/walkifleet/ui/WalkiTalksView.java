package you.thiago.walkifleet.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import you.thiago.walkifleet.R;

public class WalkiTalksView extends FrameLayout {
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
    }
}

