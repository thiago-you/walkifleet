package you.thiago.walkifleet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import android.content.Context;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class Device
{
	public static UUID GetDeviceID(Context context)
	{
		String devID = GetHardwareID(context);
		if ((devID == null) || devID.isEmpty())
			devID = GetSoftID();
		
		return UUID.fromString(devID);
	}
	
	public static String GetHardwareID(Context context)
	{
		String hwID = "";
		String device_id = null;
		try {
			TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			device_id = tm.getDeviceId();
		}catch(Exception e){};

		String android_Id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		if ("9774d56d682e549c".equals(android_Id))
			android_Id = null;
		
		String bt_mac = GetPersistentParameter("ril.bt_macaddr");
		String wf_mac = GetPersistentParameter("ril.wifi_macaddr");
		String imei = GetPersistentParameter("ril.IMEI");
		String serial1 = GetPersistentParameter("ril.serialnumber");
		String serial2 = GetPersistentParameter("ro.serialno");
		String serial3 = Build.SERIAL;
		
		if (device_id != null && !device_id.isEmpty())
			hwID += device_id;
		
		if (android_Id != null &&  !android_Id.isEmpty())
			hwID += android_Id;

		if (bt_mac != null &&  !bt_mac.isEmpty())
			hwID += bt_mac;

		if (wf_mac != null &&  !wf_mac.isEmpty())
			hwID += wf_mac;

		if (imei != null &&  !imei.isEmpty())
			hwID += imei;

		if (serial1 != null &&  !serial1.isEmpty())
			hwID += serial1;

		if (serial2 != null &&  !serial2.isEmpty())
			hwID += serial2;

		if (serial3 != null &&  !serial3.isEmpty())
			hwID += serial3;

		if (hwID != null && !hwID.isEmpty())
		{
			MessageDigest md = null;
			try
			{
				md = MessageDigest.getInstance("MD5");
				hwID = byteArrayToHex(md.digest(hwID.getBytes()));
				hwID = hwID.substring(0, 8) + "-" + hwID.substring(8, 12) + "-" + hwID.substring(12, 16) + "-" + hwID.substring(16, 20) + "-" + hwID.substring(20);
			}
			catch (NoSuchAlgorithmException e)
			{
				hwID = null;
			}
		}
		
		return hwID;
	}
	public static final String byteArrayToHex(byte[] data)
	{
		StringBuilder sb = new StringBuilder();

		for (byte b : data)
			sb.append(String.format("%02x", b));

		return sb.toString();
	}

	public static String GetPersistentParameter(String name)
	{
		String result = "";
		try
		{
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class);
			result = (String)get.invoke(c, name);
		} catch (Exception ignored) {	}

		return result;
	}
	
	public static String GetSoftID()
	{
		String devID = null;
		byte[] data = null;
		File fdir = new File("sdcard/backups/apps/fleet");
		
    	if (!fdir.exists())
    		fdir.mkdirs();
		
		File file = new File(fdir.getPath(), "fid");
	    try
	    {
        	FileInputStream fis = null;
			try
			{
				fis = new FileInputStream(file);
	        	data = new byte[(int)file.length()];
            	fis.read(data);
            	fis.close();
            	devID = new String(data, 0, data.length);
			}
			catch (Exception e)
			{
				if (fis != null)
				{
					try
					{
						fis.close();
					}
					catch (IOException e1)
					{
					}
				}
			}
	    }
	    finally {}
	    if (devID == null)
	    {
	    	UUID fid = UUID.randomUUID();
	    	devID = fid.toString();
			FileOutputStream fos = null;
			try
			{
				fos = new FileOutputStream(file);
		    	byte[] newdata = devID.getBytes();
		    	fos.write(newdata);
		    	fos.close();
			}
		    catch (Exception ex)
		    {
				if (fos != null)
				{
					try
					{
						fos.close();
					}
					catch (IOException e)
					{
					}
				}
				
				if (file.exists())
					file.delete();
		    }
	    }
	    
	    return devID;
	}

}
