package com.example.cameratest;

import android.view.SurfaceView;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.content.Context;
import android.view.SurfaceHolder;
import java.io.IOException;
import java.util.List;

import android.hardware.Camera.Parameters;
import android.util.Log;
import android.widget.FrameLayout;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Context mContext;
    private int cameraID=0;
    private int mCameraMode=1;
    private String mLogPath = "";
    private static final String TAG = "CameraPreview";

    public CameraPreview(Context context, Camera camera, SurfaceView sv, int camID, int cameraMode) {
        super(context);
        cameraID = camID;
        mCameraMode = cameraMode;
        mCamera = camera;
        mContext=context;

        mSurfaceView = sv;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = sv.getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
    	Log.v(TAG,"surfaceCreated");
        /*
        try {

             mCamera.setPreviewDisplay(mHolder);
             mCamera.startPreview();

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
        */
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    	Log.v(TAG,"surfaceDestroyed");
        /*
        if(mCamera!=null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        */
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
    	Log.e(TAG,"surfaceChanged");
    	
        if (mHolder.getSurface() == null || mCamera == null){
            Log.e(TAG,"preview surface does not exist!");
            return;
        }

        // stop preview before making changes
        /*
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }*/

        // set preview size and make any resize, rotate or
        setMaxPreviewAndPictureSize(mCamera);
        
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            Log.e(TAG,"[ok] start preview : "+cameraID);
            wlog("call startPreview ok");
        } catch (Exception e){
            Log.e(TAG,"[fail] start preview : "+cameraID);
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void setMaxPreviewAndPictureSize(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
        	Parameters parameters=mCamera.getParameters();
        	int lw = 0, lh = 0;
            float picScale= (float) 0.0;
            float preScale= (float) 0.0;
            float diff = (float) 0.0;

            //set picture size
            List<Size> mSupportedPictureSizes = parameters.getSupportedPictureSizes();
            for (int i = 0; i < mSupportedPictureSizes.size(); ++i) {
                Size size = mSupportedPictureSizes.get(i);
                Log.i(TAG, "mSupportedPictureSizes:" + size.width + "x" + size.height );
                if (size.width > lw || size.height > lh) {
                    lw = size.width;
                    lh = size.height;
                }
            }
            parameters.setPictureSize(lw, lh);
            picScale = (float)lw/(float)lh;
            Log.v(TAG, "picScale = " + picScale);
            Log.v(TAG, "set picturesize:" + lw + "x" + lh );

            lw = 0;
            lh = 0;
            //set preview size
            List<Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            for (int i = 0; i < mSupportedPreviewSizes.size(); ++i) {
                Size size = mSupportedPreviewSizes.get(i);
                Log.i(TAG, "mSupportedPreviewSizes:" + size.width + "x" + size.height );
                if (size.width > lw || size.height > lh) {
                    preScale = (float)size.width/(float)size.height;
                    diff = Math.abs(preScale - picScale);
                    Log.v(TAG, "Scale diff = " + diff); //keep preview FOV same as picture
                    if(Math.abs(diff) < 0.1 ){
                        lw = size.width;
                        lh = size.height;
                    }
                }
            }
            parameters.setPreviewSize(lw, lh);
            Log.v(TAG, "set previewsize:" + lw + "x" + lh );

            //reset display size if not dual camera mode
            if(mCameraMode != 0){
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
                lp.height = lw;
                lp.width = lh;
                mSurfaceView.setLayoutParams(lp);
            }

			mCamera.setParameters(parameters);
        }   
    }
    
	public  void wlog(String msg)
	{
		if(msg!=null){
			Log.v(TAG, msg);
		}
		WriteLog.appendLog(mContext,msg, mLogPath);
	}
	
    public void setlogPath(String path)
    {
    	mLogPath=path;
    }

    public void setExp(int exp)
    {
        float mExp;
        Parameters parameters=mCamera.getParameters();
        mExp = (float)exp;

        parameters.set("set_shutter_speed",mExp+"");
        mCamera.setParameters(parameters);
        Log.e("set_shutter_speed","exp:"+mExp);
        wlog("set_iso_exp done");
    }

    public void setiso(int iso)
    {
        Parameters parameters=mCamera.getParameters();
        if(iso == 0){
            parameters.set("iso","auto");
            Log.e(TAG, "iso auto");
        }else {
            parameters.set("iso",iso);
            Log.e(TAG, "iso"+iso);
        }
        mCamera.setParameters(parameters);
        wlog("set_iso_exp done");
    }
}



