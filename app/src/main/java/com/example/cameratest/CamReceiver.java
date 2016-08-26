package com.example.cameratest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class CamReceiver extends BroadcastReceiver {
	
    public static final String TAG = "CamReceiver";

    public static final String CLOSE_CAMERA = "asus.camera.release";
    public static final String SET_PARAMETER= "asus.camera.setparameter";
    public static final String TAKE_PIC = "asus.camera.takepicture";
    public static final String SET_SAVE_PATH = "asus.camera.setsavepath";
    public static final String SET_EXP_TIME_ACTION = "asus.camera.setexptime";
    public static final String SET_ISO_ACTION = "asus.camera.iso";

    public static Handler mCameraHandle;
    private Message msg=new Message();
    private Bundle bundle = new Bundle();
    
    public void setHandler(Handler handler){
    	mCameraHandle=handler;
    	Log.v(TAG, "mCamReceiver="+mCameraHandle);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, "onReceive action="+action);
       
        if(mCameraHandle==null){
            Log.e(TAG, "camera handle empty reminder, return.");
        	return;
        }

        if(action.equals(CLOSE_CAMERA)){
            msg.what=HandleMsg.MSG_CLOSE_CAMERA;
            Log.v(TAG, "close camera :");
            mCameraHandle.sendMessage(msg);
        }

        if(action.equals(SET_SAVE_PATH)){
        	msg.what=HandleMsg.MSG_SET_SAVE_PATH;
        	String savaPath = intent.getStringExtra("savepath");
			Log.v(TAG, "savaPath="+savaPath);
        	msg.obj=(Object)savaPath;
        	mCameraHandle.sendMessage(msg);
        }

        if(action.equals(TAKE_PIC))
        {
        	msg.what=HandleMsg.MSG_TAKE_PIC;
        	int focusneed=-1,rawneed=-1;
        	focusneed = intent.getIntExtra("focusneed",-1);
        	rawneed = intent.getIntExtra("rawneed",-1);
        	msg.arg1=focusneed;
        	msg.arg2=rawneed;
        	mCameraHandle.sendMessage(msg);
        }

        if(action.equals(SET_PARAMETER))
        {
        	msg.what=HandleMsg.MSG_SET_PARAMETER;
        	int flashmode=-1,focusmode=-1;//,zoom=-1;
        	flashmode = intent.getIntExtra("flashmode",-1);
        	focusmode = intent.getIntExtra("focusmode",-1);
        	
        	bundle.putInt("flashmode", flashmode);
        	bundle.putInt("focusmode", focusmode);
        	
        	msg.setData(bundle);
        	mCameraHandle.sendMessage(msg);
        }

        if(action.equals(SET_EXP_TIME_ACTION))
        {
            Log.v(TAG, "setExposureTime");
            int exp = intent.getIntExtra("exposuretime", 0);
            int cameraId = intent.getIntExtra("cameraId", -1);
            msg.what=HandleMsg.SET_EXP;
            msg.arg1=exp;
            msg.arg2=cameraId;
            mCameraHandle.sendMessage(msg);
        }

        if(action.equals(SET_ISO_ACTION))
        {
            Log.v(TAG, "setiso");
            int iso = intent.getIntExtra("iso", 0);
            int cameraId = intent.getIntExtra("cameraId", -1);
            msg.what=HandleMsg.SET_ISO;
            msg.arg1=iso;
            msg.arg2=cameraId;
            mCameraHandle.sendMessage(msg);
        }
    }
}
