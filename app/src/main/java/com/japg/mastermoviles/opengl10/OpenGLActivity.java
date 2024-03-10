package com.japg.mastermoviles.opengl10;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
public class OpenGLActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // View
        glSurfaceView = new GLSurfaceView(this);
        // OpenGLRenderer
        final OpenGLRenderer openGLRenderer = new OpenGLRenderer(this);
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
            Toast.makeText(this, "OpenGL ES 2.0 supported", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "OpenGL ES 2.0 not supported", Toast.LENGTH_LONG).show();
            return;
        }

        glSurfaceView.setOnTouchListener((v, event) -> {
            if (event != null) {
                // Android's Y coordinates are inverted --> normalization
                final float normalizedX = (event.getX() / (float) v.getWidth()) * 2 - 1;
                final float normalizedY = -((event.getY() / (float) v.getHeight()) * 2 - 1);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    glSurfaceView.queueEvent(() -> openGLRenderer.handleTouchPress(normalizedX, normalizedY));
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    glSurfaceView.queueEvent(() -> openGLRenderer.handleTouchDrag(normalizedX, normalizedY));
                }
                return true;
            } else {
                return false;
            }
        });
        setContentView(glSurfaceView);
    }

    private float calculatePinchDistance(MotionEvent event) {
        float x1 = event.getX(0);
        float y1 = event.getY(0);
        float x2 = event.getX(1);
        float y2 = event.getY(1);
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
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
