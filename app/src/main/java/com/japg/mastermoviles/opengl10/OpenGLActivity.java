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
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class OpenGLActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /*vista*/
        glSurfaceView = new GLSurfaceView(this);

        /*renderizado clase OpenGLRenderer*/
        final OpenGLRenderer openGLRenderer = new OpenGLRenderer(this);

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        //final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        /*Comprobación de las versiones*/
        final boolean supportsEs2 =
                configurationInfo.reqGlEsVersion >= 0x20000
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        && (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86")));
        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            glSurfaceView.setEGLContextClientVersion(2);
            // Para que funcione en el emulador
            glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            // Asigna nuestro renderer.
            glSurfaceView.setRenderer(openGLRenderer);
            rendererSet = true;
            Toast.makeText(this, "OpenGL ES 2.0 soportado",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Este dispositivo no soporta OpenGL ES 2.0",
                    Toast.LENGTH_LONG).show();
            return;
        }

        glSurfaceView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null) {
                    // Convert touch coordinates into normalized device
                    // coordinates, keeping in mind that Android's Y
                    // coordinates are inverted.
                    final float normalizedX = (event.getX() / (float) v.getWidth()) * 2 - 1;
                    final float normalizedY = -((event.getY() / (float) v.getHeight()) * 2 - 1);
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                openGLRenderer.handleTouchPress(normalizedX, normalizedY);
                            }
                        });
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                openGLRenderer.handleTouchDrag(normalizedX, normalizedY);
                            }
                        });    //falta añadir un evento que es del gesto de pelllzico
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });
        setContentView(glSurfaceView);
    }



    //Activities lifecycle
    //--------------------------------------------
    //--------------------------------------------
    //--------------------------------------------
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
