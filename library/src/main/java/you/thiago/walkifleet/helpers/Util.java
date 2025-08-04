package you.thiago.walkifleet.helpers;

import android.content.Context;
import android.util.Base64;

import you.thiago.walkifleet.Device;
import you.thiago.walkifleet.Protocol;
import you.thiago.walkifleet.Random;

public final class Util {
    private Util() {
        // Prevent instantiation
    }

    public static String getRandomUser() {
        return "USER" + (Random.nextInt(19) + 1);
    }

    public static String getDeviceId(Context context) {
        return Base64.encodeToString(Protocol.uuidToBytes(Device.GetDeviceID(context)), Base64.NO_WRAP);
    }
}
