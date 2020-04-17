package com.auro.scholr.home.presentation.view.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;

import com.auro.scholr.core.application.AuroApp;
import com.auro.scholr.core.application.base_component.BaseActivity;
import com.auro.scholr.core.application.di.component.ViewModelFactory;
import com.auro.scholr.core.common.AppConstant;
import com.auro.scholr.core.common.OnItemClickListener;
import com.auro.scholr.core.database.AppPref;
import com.auro.scholr.core.database.PrefModel;
import com.auro.scholr.databinding.CameraFragmentLayoutBinding;
import com.auro.scholr.home.presentation.view.fragment.CameraFragment;
import com.auro.scholr.util.AppLogger;
import com.auro.scholr.util.camera.CameraOverlay;
import com.auro.scholr.util.camera.FaceOverlayGraphics;
import com.auro.scholr.util.permission.PermissionHandler;
import com.auro.scholr.util.permission.PermissionUtil;
import com.auro.scholr.util.permission.Permissions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.auro.scholr.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

public class CameraActivity extends BaseActivity implements View.OnClickListener {
    String TAG = "AppCompatActivity";
    CameraSource mCameraSource;
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    int cameraID = 0;
    Camera.Parameters params;
    Camera camera;
    boolean isFlash = false;
    CameraFragmentLayoutBinding binding;
    public static boolean status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        init();
    }

    @Override
    protected void init() {
        binding = DataBindingUtil.setContentView(this, getLayout());
        binding.setLifecycleOwner(this);
        HomeActivity.setListingActiveFragment(HomeActivity.DEMOGRAPHIC_FRAGMENT);
        if (hasFrontCamera()) {
            cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        setListener();
        askPermission();
        checkValueEverySecond();
    }

    private void checkValueEverySecond() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (status) {
                    binding.captureButtonSecondaryContainer.animate().alpha(1F).start();
                } else {
                    binding.captureButtonSecondaryContainer.animate().alpha(0F).start();
                }
                checkValueEverySecond();
            }
        }, 1000);

    }


    private void askPermission() {
        String rationale = "Please provide location permission so that you can ...";
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("Info")
                .setSettingsDialogTitle("Warning");
        Permissions.check(this, PermissionUtil.mCameraPermissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                createCameraSource(cameraID);
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                // permission denied, block the feature.
            }
        });
    }


    private void createCameraSource(int cameraID) {

        Context context = this.getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();


        detector.setProcessor(
                new MultiProcessor.Builder<>(new CameraActivity.GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {

            AppLogger.e(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setFacing(cameraID)
                .setAutoFocusEnabled(true)
                .setRequestedFps(30.0f)
                .build();

        startCameraSource();
    }


    private void startCameraSource() {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                this.getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                binding.preview.start(mCameraSource, binding.faceOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.switch_orientation) {
            changeCamera();
        } else if (v.getId() == R.id.flash_toggle) {
            flashIsAvailable();

        } else if (v.getId() == R.id.stillshot) {
            clickPicture();
        }
    }

    private void changeCamera() {
        if (cameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
            cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
            binding.flashToggle.setImageDrawable(this.getDrawable(R.drawable.ic_flash_off_black));
        } else {
            cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        binding.faceOverlay.clear();
        mCameraSource.release();
        createCameraSource(cameraID);
    }


    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new CameraActivity.GraphicFaceTracker(binding.faceOverlay);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private CameraOverlay mOverlay;
        private FaceOverlayGraphics faceOverlayGraphics;

        GraphicFaceTracker(CameraOverlay overlay) {
            mOverlay = overlay;
            faceOverlayGraphics = new FaceOverlayGraphics(overlay);
        }


        @Override
        public void onNewItem(int faceId, Face item) {

            faceOverlayGraphics.setId(faceId);
            status = true;

        }

        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(faceOverlayGraphics);
            faceOverlayGraphics.updateFace(face);
        }

        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {

            mOverlay.remove(faceOverlayGraphics);

        }

        @Override
        public void onDone() {
            mOverlay.remove(faceOverlayGraphics);
            status = false;

        }
    }

    private void clickPicture() {

        binding.loadingSpinner.setVisibility(View.VISIBLE);
        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes) {
                try {
                    // convert byte array into bitmap
                    Bitmap loadedImage = null;
                    loadedImage = BitmapFactory.decodeByteArray(bytes, 0,
                            bytes.length);
                    String path = saveToInternalStorage(loadedImage) + "/profile.jpg";
                    Intent intent = new Intent();
                    intent.putExtra(AppConstant.PROFILE_IMAGE_PATH, path);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                    //loadImageFromStorage(saveToInternalStorage(loadedImage));


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(AuroApp.getAppContext().getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("auroImageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }


    private void loadImageFromStorage(String path) {
        try {

            File f = new File(path, "profile.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            this.getSupportFragmentManager().popBackStack();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean hasFrontCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return true;
            }
        }
        return false;
    }


    private void flashIsAvailable() {
        boolean hasFlash = this.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (hasFlash && cameraID == 0) {
            changeFlashStatus();
        }
    }


    public void changeFlashStatus() {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    camera = (Camera) field.get(mCameraSource);
                    if (camera != null) {
                        params = camera.getParameters();
                        if (!isFlash) {
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            binding.flashToggle.setImageDrawable(this.getDrawable(R.drawable.ic_flash_on_black));
                            isFlash = true;
                        } else {
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            binding.flashToggle.setImageDrawable(this.getDrawable(R.drawable.ic_flash_off_black));
                            isFlash = false;
                        }
                        camera.setParameters(params);
                    }

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    @Override
    protected void setListener() {
        binding.stillshot.setOnClickListener(this);
        binding.switchOrientation.setOnClickListener(this);
        binding.flashToggle.setOnClickListener(this);
    }

    @Override
    protected int getLayout() {
        return R.layout.camera_fragment_layout;
    }

    private void releaseCamera() {
        binding.faceOverlay.clear();
        mCameraSource.release();
    }

}