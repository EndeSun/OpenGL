package com.japg.mastermoviles.opengl10;

import static android.widget.Toast.*;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
public class OpenGLActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;
    private float dstX = 1f;
    private float dstY = 1f;
    private ImageButton leftButton;
    private ImageButton rightButton;
    private OpenGLRenderer openGLRenderer;
    private View buttonsLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buttonsLayout = LayoutInflater.from(this).inflate(R.layout.openglmainlayout, null);
        initUI();
        leftButton.setOnClickListener(v -> glSurfaceView.queueEvent(() -> openGLRenderer.rotateHeadLeft()));
        rightButton.setOnClickListener(v -> glSurfaceView.queueEvent(() -> openGLRenderer.rotateHeadRight()));
    }



    private void initUI(){
        //Surface View
        glSurfaceView = new GLSurfaceView(this);
        openGLRenderer = new OpenGLRenderer(this);
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

        // Version check
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86");

        if (supportsEs2) {
            // Request OpenGL 2.0 compatible context.
            glSurfaceView.setEGLContextClientVersion(2);
            // Emulator configuration
            glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            // Asigna nuestro renderer.
            // -----------------------
            glSurfaceView.setRenderer(openGLRenderer);
            // -----------------------
            rendererSet = true;
            makeText(this, "OpenGL ES 2.0 supported", LENGTH_LONG).show();
        } else {
            makeText(this, "OpenGL ES 2.0 not supported", LENGTH_LONG).show();
            return;
        }

        glSurfaceView.setOnTouchListener((v, event) -> {
            if (event != null) {
                // Android's Y coordinates are inverted --> normalization
                final float normalizedX = (event.getX() / (float) v.getWidth()) * 2 - 1;
                final float normalizedY = -((event.getY() / (float) v.getHeight()) * 2 - 1);

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    glSurfaceView.queueEvent(() -> openGLRenderer.handleTouchPress(normalizedX, normalizedY));
                    //old distances
                    dstX = normalizedX;
                    dstY = normalizedY;

                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    glSurfaceView.queueEvent(() -> openGLRenderer.handleTouchDrag(normalizedX, normalizedY));
                    //Flinch event detected
                    if (event.getPointerCount() == 2) {
                        float secondFingerX = (event.getX(1) / (float) v.getWidth()) * 2 - 1;
                        float secondFingerY = -((event.getY(1) / (float) v.getHeight()) * 2 - 1);
                        float distanceNew = calculateDistance(normalizedX, normalizedY, secondFingerX, secondFingerY);
                        float distanceOld = calculateDistance(dstX, dstY, secondFingerX, secondFingerY);
                        // Check for zoom out (absolute value of old distance is less than new distance)
                        if (Math.abs(distanceOld) < Math.abs(distanceNew)) {
                            float zoomFactor = (distanceNew - distanceOld) * 10; // Adjust sensitivity as needed
                            glSurfaceView.queueEvent(() -> openGLRenderer.handleZoomOut(zoomFactor));
                        }

                        if (Math.abs(distanceOld) > Math.abs(distanceNew)) {
                            float zoomFactor = (distanceOld - distanceNew) * 10; // Adjust sensitivity as needed
                            glSurfaceView.queueEvent(() -> openGLRenderer.handleZoomIn(zoomFactor));
                        }
                        dstX = normalizedX;
                        dstY = normalizedY;

                    }
                }
                return true;

            } else {
                return false;
            }
        });

        setContentView(glSurfaceView);


        // Add the buttons layout to the GLSurfaceView's parent view at the bottom
        FrameLayout parent = (FrameLayout) glSurfaceView.getParent();
        parent.addView(buttonsLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        leftButton = findViewById(R.id.left_button);
        rightButton = findViewById(R.id.right_button);


    }








    //Distance for the zoom in and the zoom out
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    //Activities lifecycle
    @Override
    protected void onPause() {
        super.onPause();
        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }



    // Menu view
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    // Menu option
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
