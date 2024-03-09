package com.japg.mastermoviles.opengl10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.japg.mastermoviles.opengl10.util.LoggerConfig;
import com.japg.mastermoviles.opengl10.util.Resource3DSReader;
import com.japg.mastermoviles.opengl10.util.ShaderHelper;
import com.japg.mastermoviles.opengl10.util.TextResourceReader;
import com.japg.mastermoviles.opengl10.util.TextureHelper;

import java.nio.Buffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_MAX_TEXTURE_IMAGE_UNITS;
import static android.opengl.GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.frustumM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;


public class OpenGLRenderer implements Renderer {
	private static final String TAG = "OpenGLRenderer";
	private static final int BYTES_PER_FLOAT = 4;
	private final Context context;
	private int program;
	// Uniform names
	private static final String U_MVPMATRIX 		= "u_MVPMatrix";
	private static final String U_MVMATRIX 			= "u_MVMatrix";
	private static final String U_COLOR 			= "u_Color";
	private static final String U_TEXTURE 			= "u_TextureUnit";
	// Attributes name
	private static final String A_POSITION = "a_Position";
	private static final String A_NORMAL   = "a_Normal";
	private static final String A_UV       = "a_UV";
	// Handles para los shaders
	private int uMVPMatrixLocation;
	private int uMVMatrixLocation;
	private int uColorLocation;
	private int uTextureUnitLocation;
	private int aPositionLocation;
	private int aNormalLocation;
	private int aUVLocation;
	// Rotation around axis
	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int NORMAL_COMPONENT_COUNT = 3;
	private static final int UV_COMPONENT_COUNT = 2;
	// Date --> 3 + 3 + 2
	private static final int STRIDE = (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT + UV_COMPONENT_COUNT) * BYTES_PER_FLOAT; // (8 * 4) => 32
	// Matrices de proyección y de vista
	private final float[] projectionMatrix = new float[16];
	private final float[] modelMatrix = new float[16];
	private final float[] modelMatrixBody = new float[16];
	private final float[] MVP = new float[16];

	//------------------------
	//   Model definition
	Resource3DSReader obj3DShead;
	Resource3DSReader obj3DSbody;
	//------------------------
	//Texture definition
	private int	textureHead;
	private int textureBody;
	//Perspectives and frustum
	void frustum(float[] m, int offset, float l, float r, float b, float t, float n, float f)
	{
		frustumM(m, offset, l, r, b, t, n, f);
		// Corrección del bug de Android
		m[8] /= 2;
	}
    void perspective(float[] m, int offset, float fovy, float aspect, float n, float f)
    {	final float d = f-n;
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
	//MODELS LOADING
	//------------------------
	public OpenGLRenderer(Context context) {
		this.context = context;
		// model head
		obj3DShead = new Resource3DSReader();
		obj3DShead.read3DSFromResource(context, R.raw.cabeza_mario_6);

		// model body
		obj3DSbody = new Resource3DSReader();
		obj3DSbody.read3DSFromResource(context, R.raw.cuerpo_mario_6);
	}
	//------------------------
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		String vertexShaderSource;
		String fragmentShaderSource;
			
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		int[]	maxVertexTextureImageUnits = new int[1];
		int[]	maxTextureImageUnits       = new int[1];

		// Check if vertex shader support texture
		glGetIntegerv(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, maxVertexTextureImageUnits, 0);
		if (LoggerConfig.ON) {
			Log.w(TAG, "Max. Vertex Texture Image Units: "+maxVertexTextureImageUnits[0]);
		}
		// Check if fragment shader support texture
		glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureImageUnits, 0);
		if (LoggerConfig.ON) {
			Log.w(TAG, "Max. Texture Image Units: "+maxTextureImageUnits[0]);
		}

		//--------------------------------------
		//--------------------------------------
		//TEXTURE LOADING
		textureHead = TextureHelper.loadTexture(context, R.drawable.cara_2);
		textureBody = TextureHelper.loadTexture(context, R.drawable.cuerpo_2);

		// Read the shaders for vertex shader
		if (maxVertexTextureImageUnits[0]>0) {
			// texture support
			vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_vertex_shader);
			fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_fragment_shader);
		} else {
			// texture no support
			vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_vertex_shader2);
			fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.specular_fragment_shader2);
		}
		
		// Compile shaders
		int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
		int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
		
		// Link OpenGL program
		program = ShaderHelper.linkProgram(vertexShader, fragmentShader);
		
		// En depuración validamos el programa OpenGL
		if (LoggerConfig.ON) {
			ShaderHelper.validateProgram(program);
		}
		
		// OpenGL activation
		glUseProgram(program);
		
		// Capture uniforms
		uMVPMatrixLocation = glGetUniformLocation(program, U_MVPMATRIX);
		uMVMatrixLocation = glGetUniformLocation(program, U_MVMATRIX);
		uColorLocation = glGetUniformLocation(program, U_COLOR);
		uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE);
		
		// Capture attributes
		aPositionLocation = glGetAttribLocation(program, A_POSITION);
		glEnableVertexAttribArray(aPositionLocation);
		aNormalLocation = glGetAttribLocation(program, A_NORMAL);
		glEnableVertexAttribArray(aNormalLocation);
		aUVLocation = glGetAttribLocation(program, A_UV);
		glEnableVertexAttribArray(aUVLocation);	
	}
	//------------------------------------------
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
	//------------------------------------------
	@Override
	public void onDrawFrame(GL10 glUnused) {
		// Clear the rendering surface.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glLineWidth(2.0f);

		//MODEL MATRIX CADA OBJETO TIENE QUE TENER EL SUYO
		setIdentityM(modelMatrix, 0);
		// alejamos 5 de profundidad
		translateM(modelMatrix, 0, 0f, 0.0f, -5.0f);
		rotateM(modelMatrix, 0, rY, 0f, 1f, 0f);
		rotateM(modelMatrix, 0, rX, 1f, 0f, 0f);

		//^ las matrices se premultiplican para que funcionen.
		multiplyMM(MVP, 0, projectionMatrix, 0, modelMatrix, 0);
		// Envía la matriz de proyección multiplicada por modelMatrix al shader
		glUniformMatrix4fv(uMVPMatrixLocation, 1, false, MVP, 0);
		// Envía la matriz modelMatrix al shader
		glUniformMatrix4fv(uMVMatrixLocation, 1, false, modelMatrix, 0);	
		// Update color
		glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);


			
		// TEXTURE ACTIVATION
		glActiveTexture(GL_TEXTURE0);
		//Head print
		textureAssociation(textureHead, obj3DShead);
		//Body print
		textureAssociation(textureBody,obj3DSbody );
		updateRotation();
	}
	//------------------------------------------
	public void drawObject(Resource3DSReader obj){
		// Drawing
		for (int i=0; i<obj.numMeshes; i++) {
			// Asociando vértices con su attribute
			final Buffer position = obj.dataBuffer[i].position(0);
			glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, obj.dataBuffer[i]);
			// Asociamos el vector de normales
			obj.dataBuffer[i].position(POSITION_COMPONENT_COUNT);
			glVertexAttribPointer(aNormalLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, obj.dataBuffer[i]);
			// Asociamos el vector de UVs
			obj.dataBuffer[i].position(POSITION_COMPONENT_COUNT+NORMAL_COMPONENT_COUNT);
			glVertexAttribPointer(aUVLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, obj.dataBuffer[i]);
			glDrawArrays(GL_TRIANGLES, 0, obj.numVertices[i]);
		}
	}
	//------------------------------------------
	public void textureAssociation(int textureObject, Resource3DSReader obj){
		glBindTexture(GL_TEXTURE_2D, textureObject);
		glUniform1f(uTextureUnitLocation, 0);
		drawObject(obj);
	}
	//   			HANDLERS
	//------------------------------------------
	//------------------------------------------
	private float rotationSpeed = 0.1f; // Puedes ajustar esta velocidad según sea necesario
	private float destRX = 0f;
	private float destRY = 0f;
	private float rX = 0f;
	private float rY = 0f;
	public void handleTouchPress(float normalizedX, float normalizedY) {
		// Calcular la rotación necesaria para mover gradualmente el cuerpo hacia el punto de clic
		destRX = -normalizedY * 180f;
		destRY = normalizedX * 180f;
	}

	public void handleTouchDrag(float normalizedX, float normalizedY) {
		// Calcular la rotación necesaria para mover gradualmente el cuerpo hacia el punto de clic
		destRX = -normalizedY * 180f;
		destRY = normalizedX * 180f;
	}

	public void updateRotation() {
		// Calcular la rotación incremental para cada fotograma
		float diffX = destRX - rX;
		float diffY = destRY - rY;

		// Aplicar rotación incremental
		rX += diffX * rotationSpeed;
		rY += diffY * rotationSpeed;
	}
}