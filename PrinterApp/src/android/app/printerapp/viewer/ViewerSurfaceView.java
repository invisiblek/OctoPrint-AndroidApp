package android.app.printerapp.viewer;

import android.app.printerapp.viewer.Geometry.Vector;
import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ViewerSurfaceView extends GLSurfaceView{
	//View Modes
	public static final int NORMAL = 0;
	public static final int XRAY = 1;
	public static final int TRANSPARENT = 2;
	public static final int LAYERS = 3;
	
	ViewerRenderer mRenderer;
	
	//Touch
	private final float TOUCH_SCALE_FACTOR_ROTATION = 90.0f / 320;  //180.0f / 320;
	private float mPreviousX;
	private float mPreviousY;
	
   // zoom rate (larger > 1.0f > smaller)
	private float pinchScale = 1.0f;

	private PointF pinchStartPoint = new PointF();
	private float pinchStartY = 0.0f;
	private float pinchStartZ = 0.0f;
	private float pinchStartDistance = 0.0f;
	private float pinchStartFactorX = 0.0f;
	private float pinchStartFactorY = 0.0f;
	private float pinchStartFactorZ = 0.0f;

	// for touch event handling
	private static final int TOUCH_NONE = 0;
	private static final int TOUCH_DRAG = 1;
	private static final int TOUCH_ZOOM = 2;
	private int touchMode = TOUCH_NONE;
		
	//Viewer modes
	public static final int ROTATION_MODE =0;
	public static final int TRANSLATION_MODE = 1;
	public static final int LIGHT_MODE = 2;
	
	private int mMovementMode;
	
	//Edition mode
	private boolean mEdition = false;
	private int mEditionMode;
	
	//Edition modes
	public static final int NONE_EDITION_MODE = 0;
	public static final int MOVE_EDITION_MODE = 1;
	public static final int ROTATION_EDITION_MODE =2;
	public static final int SCALED_EDITION_MODE = 3;
	public static final int MIRROR_EDITION_MODE = 4;


	public static final int ROTATE_X = 0;
	public static final int ROTATE_Y = 1;
	public static final int ROTATE_Z = 2;
	
	public ViewerSurfaceView(Context context) {
	    super(context);
	}
	public ViewerSurfaceView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	}
	
	public ViewerSurfaceView(Context context, DataStorage data, int state, boolean doSnapshot, boolean stl) {
		super(context);
		
		// Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
      
		mRenderer = new ViewerRenderer (data, context, state, doSnapshot, stl);
		setRenderer(mRenderer);
				
		// Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	public void configViewMode (int state) {
		switch (state) {
		case (ViewerSurfaceView.NORMAL):
			setXray(false);
			setTransparent(false);
			break;			
		case (ViewerSurfaceView.XRAY):
			setXray(true);
			setTransparent(false);
			break;
		case (ViewerSurfaceView.TRANSPARENT):
			setXray(false);
			setTransparent(true);
			break;
		}
		
		requestRender();
	}
	
	
	public void showBackWitboxFace () {
		if (mRenderer.getShowBackWitboxFace()) mRenderer.showBackWitboxFace(false);
		else mRenderer.showBackWitboxFace(true);	
		requestRender();		
	}
	
	public void showRightWitboxFace () {
		if (mRenderer.getShowRightWitboxFace()) mRenderer.showRightWitboxFace(false);
		else mRenderer.showRightWitboxFace(true);
		requestRender();		
	}
	
	public void showLeftWitboxFace () {
		if (mRenderer.getShowLeftWitboxFace()) mRenderer.showLeftWitboxFace(false);
		else mRenderer.showLeftWitboxFace(true);
		requestRender();		
	}
	
	public void showDownWitboxFace () {
		if (mRenderer.getShowDownWitboxFace()) mRenderer.showDownWitboxFace(false);
		else mRenderer.showDownWitboxFace(true);
		requestRender();		
	}
	
	public void setTransparent (boolean trans) {
		mRenderer.setTransparent(trans);
	}
	
	public void setXray (boolean xray) {
		mRenderer.setXray(xray);
	}
	
	public void setEditionMode (int mode) {
		mEditionMode = mode;
	}
	
	public void deleteObject() {
		mRenderer.deleteObject();
		requestRender();
	}
	
	public void setRotationVector (int mode) {
		switch (mode) {
		case ROTATE_X:
			mRenderer.setRotationVector(new Vector (1,0,0));
			break;
		case ROTATE_Y:
			mRenderer.setRotationVector(new Vector (0,1,0));
			break;
		case ROTATE_Z:
			mRenderer.setRotationVector(new Vector (0,0,1));
			break;
		}
		mRenderer.resetTotalAngle();
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
        float y = event.getY();
        
        float normalizedX = (event.getX() / (float) mRenderer.getWidthScreen()) * 2 - 1;
		float normalizedY = -((event.getY() / (float) mRenderer.getHeightScreen()) * 2 - 1);
				
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			// starts pinch
			case MotionEvent.ACTION_POINTER_DOWN:
				if (event.getPointerCount() >= 2) {
					pinchStartDistance = getPinchDistance(event);
					pinchStartY = mRenderer.getCameraPosY();
					pinchStartZ = mRenderer.getCameraPosZ();
					pinchStartFactorX = mRenderer.getFactorScaleX();
					pinchStartFactorY = mRenderer.getFactorScaleY();
					pinchStartFactorZ = mRenderer.getFactorScaleZ();

					if (pinchStartDistance > 50f) {
						getPinchCenterPoint(event, pinchStartPoint);
						mPreviousX = pinchStartPoint.x;
						mPreviousY = pinchStartPoint.y;
						touchMode = TOUCH_ZOOM;
					}
				}
				break;				
			case MotionEvent.ACTION_DOWN:
				if (touchMode == TOUCH_NONE && event.getPointerCount() == 1) {
					if (!mEdition && mRenderer.touchPoint(normalizedX, normalizedY)) {
						mEdition = true;
						ViewerMain.setEditionMenuVisibility(View.VISIBLE);
					}
					
					touchMode = TOUCH_DRAG;
					mPreviousX = event.getX();
					mPreviousY = event.getY();
				}												
				break;			
			case MotionEvent.ACTION_MOVE:	
					float dx = x - mPreviousX;
					float dy = y - mPreviousY;
										
					if (touchMode == TOUCH_ZOOM && pinchStartDistance > 0) {
						// on pinch
						PointF pt = new PointF();
						getPinchCenterPoint(event, pt);
						
						mPreviousX = pt.x;
						mPreviousY = pt.y;
										
						pinchScale = getPinchDistance(event) / pinchStartDistance;
						
														
						if (mEdition && mEditionMode == SCALED_EDITION_MODE) {
							float fx = pinchStartFactorX*pinchScale;
							float fy = pinchStartFactorY*pinchScale;
							float fz = pinchStartFactorZ*pinchScale;

							mRenderer.scaleObject(fx,fy,fz);
						} else {
							mRenderer.setCameraPosY(pinchStartY / pinchScale);
							mRenderer.setCameraPosZ(pinchStartZ / pinchScale);						
						}

						requestRender();

						
					}else if (touchMode == TOUCH_DRAG) {
						mPreviousX = x;
					    mPreviousY = y;
					    
					    if (mEdition && (mEditionMode == MOVE_EDITION_MODE || mEditionMode == ROTATION_EDITION_MODE)) {
				    		editionDrag (normalizedX, normalizedY, dx);
					    } else {
					    	dragAccordingToMode (x,y,dx,dy);
					    }				    
					} 
									
					requestRender();								    
	                break;
			
			// end pinch
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:				
				if (touchMode == TOUCH_ZOOM) {
					pinchScale = 1.0f;
					pinchStartPoint.x = 0.0f;
					pinchStartPoint.y = 0.0f;
				}
				
				if(mEdition && mEditionMode==ROTATION_EDITION_MODE)	mRenderer.refreshRotatedObjectCoordinates();					
													
				requestRender();					

				touchMode = TOUCH_NONE;
				break;				
		}
		return true;
	}
	
	public void exitEditionMode () {
		mEdition = false;
		mEditionMode = NONE_EDITION_MODE;
		mRenderer.exitEditionMode();
    	ViewerMain.setEditionMenuVisibility(View.INVISIBLE);
    	
    	requestRender();
	}
	
	
	private void editionDrag (float x, float y, float dx) {			
		switch (mEditionMode) {
		case ROTATION_EDITION_MODE:
			mRenderer.setAngleRotationObject(dx*TOUCH_SCALE_FACTOR_ROTATION);
			break;
		case MOVE_EDITION_MODE:
			mRenderer.dragObject(x, y);
			break;			
		}
	}
	
	private void dragAccordingToMode (float x, float y, float dx, float dy) {
		switch (mMovementMode) {
		case ROTATION_MODE:
			doRotation (dx,dy);
			break;
		case TRANSLATION_MODE:
			doTranslation (dx,dy);
			break;
		case LIGHT_MODE:
            if (y < getHeight() * 3/4) {
                dy = 1;
            } else {
            	dy = -1;
            }

            if (x < getWidth() / 2) {
            	dx = -1;
            } else {
            	dx = 1;
            }
			doLight (dx, dy);
			break;
		}
		
	}
	
	public void doMirror () {
		float fx = mRenderer.getFactorScaleX();
		float fy = mRenderer.getFactorScaleY();
		float fz = mRenderer.getFactorScaleZ();
		
		mRenderer.scaleObject(-1*fx, fy, fz);
		requestRender();
	}
	
	private void doRotation (float dx, float dy) {              
        mRenderer.setSceneAngleX(dx*TOUCH_SCALE_FACTOR_ROTATION);
        mRenderer.setSceneAngleY(dy*TOUCH_SCALE_FACTOR_ROTATION);		
	} 
	
	private void doTranslation(float dx, float dy) {
		mRenderer.setCenterX(-dx);
		mRenderer.setCenterZ(dy); //
	}
	
	private void doLight (float dx, float dy) {              
        mRenderer.setLightVector(dx, dy);		
	} 
	
	public void setMovementMode (int mode) {
		mMovementMode = mode;
	}
	
	private float getPinchDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	
	/**
	 * 
	 * @param event
	 * @param pt pinched point
	 */
	private void getPinchCenterPoint(MotionEvent event, PointF pt) {
		pt.x = (event.getX(0) + event.getX(1)) * 0.5f;
		pt.y = (event.getY(0) + event.getY(1)) * 0.5f;
	}
}

