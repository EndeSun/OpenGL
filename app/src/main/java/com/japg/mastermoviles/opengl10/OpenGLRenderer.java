package com.japg.mastermoviles.opengl10;
import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;
import android.widget.Toast;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glViewport;

public class OpenGLRenderer implements Renderer {
	private Context context;
	private final float[] projectionMatrix = new float[16];
	private final ModelObject headModel;
	private final ModelObject bodyModel;
	//------------------------------------------
	public OpenGLRenderer(Context context) {
		this.context = context;
		headModel = new ModelObject(context, R.raw.cabeza_mario_3, R.drawable.cara_2, 0, 0, -5);
		bodyModel = new ModelObject(context, R.raw.cuerpo_mario_6, R.drawable.cuerpo_2, 0, 0, -5);
	}
	//------------------------------------------
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		headModel.loadTexture();
		bodyModel.loadTexture();
	}
	//------------------------------------------
	@Override
	public void onDrawFrame(GL10 glUnused) {
		// Clear the rendering surface.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glLineWidth(2.0f);
		// TEXTURE ACTIVATION
		glActiveTexture(GL_TEXTURE0);

		headModel.drawObject(projectionMatrix);
		bodyModel.drawObject(projectionMatrix);

		headModel.updatePosition(0.06f);
		bodyModel.updatePosition(0.06f);
	}








	//-------------------HANDLERS-----------------------
	public void handleTouchPress(float normalizedX, float normalizedY) {
		headModel.setdst(-normalizedY * 20f, normalizedX * 20f);
		//bodyModel.setdst(-normalizedY, normalizedX);
	}

	public void handleTouchDrag(float normalizedX, float normalizedY) {
		headModel.setdst(-normalizedY * 180f, normalizedX * 180f);
		bodyModel.setdst(-normalizedY * 180f, normalizedX * 180f);
	}

	public void handleZoomIn(float normalizedZ) {
		headModel.zoom(-normalizedZ);
		bodyModel.zoom(-normalizedZ);
	}

	public void handleZoomOut(float normalizedZ){
		headModel.zoom(normalizedZ);
		bodyModel.zoom(normalizedZ);
	}


	public void rotateHeadLeft(){
		headModel.rotateY(-20);
	}

	public void rotateHeadRight(){
		headModel.rotateY(20);
	}










	//------------------------------------------
	//Perspectives
	void perspective(float[] m, int offset, float fovy, float aspect, float n, float f){
		final float d = f-n;
		final float angleInRadians = (float) (fovy * Math.PI / 180.0);
		final float a = (float) (1.0 / Math.tan(angleInRadians / 2.0));

		m[0] = a/aspect;
		m[1] = 0f;
		m[2] = 0f;
		m[3] = 0f;

		m[4] = 0f;
		m[5] = a;
		m[6] = 0f;
		m[7] = 0f;

		m[8] = 0;
		m[9] = 0;
		m[10] = (n - f) / d;
		m[11] = -1f;

		m[12] = 0f;
		m[13] = 0f;
		m[14] = -2*f*n/d;
		m[15] = 0f;
	}
	//To adopt different orientation
	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Stablish viewport for all screen
		glViewport(0, 0, width, height);
		final float aspectRatio = width > height ?
				(float) width / (float) height :
				(float) height / (float) width;
		if (width > height) {
			//Landscape
			perspective(projectionMatrix, 0, 45f, aspectRatio, 0.01f, 1000f);
		} else {
			// Portrait or square
			perspective(projectionMatrix, 0, 45f, 1f/aspectRatio, 0.01f, 1000f);
		}
	}
}