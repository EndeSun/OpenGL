package com.japg.mastermoviles.opengl10;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_MAX_TEXTURE_IMAGE_UNITS;
import static android.opengl.GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

import android.content.Context;
import android.util.Log;

import com.japg.mastermoviles.opengl10.util.LoggerConfig;
import com.japg.mastermoviles.opengl10.util.Resource3DSReader;
import com.japg.mastermoviles.opengl10.util.ShaderHelper;
import com.japg.mastermoviles.opengl10.util.TextResourceReader;
import com.japg.mastermoviles.opengl10.util.TextureHelper;

import java.nio.Buffer;

public class ModelObject {
    private Context context;
    private int uMVPMatrixLocation;
    private int uMVMatrixLocation;
    private int uColorLocation;
    private int uTextureUnitLocation;
    private int aPositionLocation;
    private int aNormalLocation;
    private int aUVLocation;
    private int program;

    private final float[] MVP = new float[16];
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int NORMAL_COMPONENT_COUNT = 3;
    private static final int UV_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT + UV_COMPONENT_COUNT) * BYTES_PER_FLOAT; // (8 * 4) => 32

    private Resource3DSReader object;
    private int texture;
    private int textureResource;

    private float rX;
    private float rY;
    private float rZ;

    private float dstrX;
    private float dstrY;
    private float dstrZ;

    private final float[] modelMatrix = new float[16];


    public ModelObject(Context context, int objectResource, int textureResource, float rXinit, float rYinit, float rZinit){
        this.context = context;
        this.textureResource = textureResource;
        this.object = new Resource3DSReader();
        this.object.read3DSFromResource(context, objectResource);

        this.rX = rXinit;
        this.rY = rYinit;
        this.rZ = rZinit;
    }

    public void setdst(float dstrX, float dstrY){
        this.dstrX = dstrX;
        this.dstrY = dstrY;
    }

    public void updatePosition(float speed){
        float diffX = this.dstrX - this.rX;
        float diffY = this.dstrY - this.rY;

        this.rX += diffX * speed;
        this.rY += diffY * speed;
    }

    public void zoom(float rZ){
        this.rZ += rZ;
    }

    public void loadTexture(){
        String vertexShaderSource;
        String fragmentShaderSource;

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        int[]	maxVertexTextureImageUnits = new int[1];
        int[]	maxTextureImageUnits       = new int[1];

        // Check if vertex shader support texture
        glGetIntegerv(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, maxVertexTextureImageUnits, 0);
        if (LoggerConfig.ON) {
            Log.w("OpenGLRenderer", "Max. Vertex Texture Image Units: "+maxVertexTextureImageUnits[0]);
        }
        // Check if fragment shader support texture
        glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, maxTextureImageUnits, 0);
        if (LoggerConfig.ON) {
            Log.w("OpenGLRenderer", "Max. Texture Image Units: "+maxTextureImageUnits[0]);
        }
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

        // validate openGL program
        if (LoggerConfig.ON) { ShaderHelper.validateProgram(program); }

        // OpenGL activation
        glUseProgram(program);

        // Capture uniforms
        uMVPMatrixLocation = glGetUniformLocation(program, "u_MVPMatrix");
        uMVMatrixLocation = glGetUniformLocation(program, "u_MVMatrix");
        uColorLocation = glGetUniformLocation(program, "u_Color");
        uTextureUnitLocation = glGetUniformLocation(program, "u_TextureUnit");

        // Capture attributes
        aPositionLocation = glGetAttribLocation(program, "a_Position");
        glEnableVertexAttribArray(aPositionLocation);
        aNormalLocation = glGetAttribLocation(program, "a_Normal");
        glEnableVertexAttribArray(aNormalLocation);
        aUVLocation = glGetAttribLocation(program, "a_UV");
        glEnableVertexAttribArray(aUVLocation);

        this.texture = TextureHelper.loadTexture(context, this.textureResource);
    }
    
    public void drawObject(float[] projectionMatrix) {
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, 0f, 0.0f, rZ);
        rotateM(modelMatrix, 0, rY, 0f, 1f, 0f);
        rotateM(modelMatrix, 0, rX, 1f, 0f, 0f);

        // las matrices se premultiplican para que funcionen.
        multiplyMM(MVP, 0, projectionMatrix, 0, modelMatrix, 0);
        // Envía la matriz de proyección multiplicada por modelMatrix al shader
        glUniformMatrix4fv(uMVPMatrixLocation, 1, false, MVP, 0);
        // Envía la matriz modelMatrix al shader
        glUniformMatrix4fv(uMVMatrixLocation, 1, false, modelMatrix, 0);
        // Update color
        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);

        glBindTexture(GL_TEXTURE_2D, texture);
        glUniform1f(this.uTextureUnitLocation, 0);

        for (int i=0; i<this.object.numMeshes; i++) {
            // Asociando vértices con su attribute
            final Buffer position = this.object.dataBuffer[i].position(0);
            glVertexAttribPointer(this.aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, object.dataBuffer[i]);
            // Asociamos el vector de normales
            this.object.dataBuffer[i].position(POSITION_COMPONENT_COUNT);
            glVertexAttribPointer(aNormalLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, object.dataBuffer[i]);
            // Asociamos el vector de UVs
            this.object.dataBuffer[i].position(POSITION_COMPONENT_COUNT+ NORMAL_COMPONENT_COUNT);
            glVertexAttribPointer(aUVLocation, NORMAL_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, object.dataBuffer[i]);
            glDrawArrays(GL_TRIANGLES, 0, this.object.numVertices[i]);
        }
    }
}
