/**************************************************************************/
/*  RTNGodotView.java                                                     */
/**************************************************************************/
/* Copyright (c) 2024-2025 Slay GmbH                                      */
/*                                                                        */
/* Permission is hereby granted, free of charge, to any person obtaining  */
/* a copy of this software and associated documentation files (the        */
/* "Software"), to deal in the Software without restriction, including    */
/* without limitation the rights to use, copy, modify, merge, publish,    */
/* distribute, sublicense, and/or sell copies of the Software, and to     */
/* permit persons to whom the Software is furnished to do so, subject to  */
/* the following conditions:                                              */
/*                                                                        */
/* The above copyright notice and this permission notice shall be         */
/* included in all copies or substantial portions of the Software.        */
/*                                                                        */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,        */
/* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF     */
/* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. */
/* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY   */
/* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */
/* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE      */
/* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                 */
/**************************************************************************/

package net.somesoap.rtngodot;

import org.godotengine.godot.input.GodotInputHandler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.events.EventDispatcher;

public class RTNGodotView extends FrameLayout implements SurfaceHolder.Callback2, TextureView.SurfaceTextureListener {
	private static final String TAG = "RTNGodotView";

	private String windowName = "";
	// Start with TextureView because React applies props after constructing the
	// native view. Creating a temporary SurfaceView here can leave its already
	// destroyed surface registered as Godot's default render target.
	private boolean transparent = true;
	private boolean cancelTouchWhenOutside = false;
	private boolean touchCanceledOutside = false;
	@Nullable private SurfaceView surfaceView;
	@Nullable private TextureView textureView;
	@Nullable private Surface textureSurface;
	private boolean surfaceReadyEventSent = false;

	private GodotInputHandler mInputHandler;

	public RTNGodotView(Context context) {
		super(context);
		this.configureComponent();
	}

	public RTNGodotView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		this.configureComponent();
	}

	public RTNGodotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.configureComponent();
	}

	private void configureComponent() {
		mInputHandler = RTNLibGodot.getInstance().getInputHandler();
		setRenderTarget(true);
	}

	/**
	 * Opaque views keep the faster SurfaceView path. Transparent SurfaceViews must
	 * be placed above the app window, which also puts them above RN overlays.
	 * TextureView is composited with normal React Native children instead.
	 */
	private void setRenderTarget(boolean useTextureView) {
		if ((useTextureView && textureView != null) || (!useTextureView && surfaceView != null)) {
			return;
		}

		removeAllViews();
		surfaceView = null;
		textureView = null;
		textureSurface = null;

		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		if (useTextureView) {
			TextureView target = new TextureView(getContext()) {
				@Override
				public boolean onTouchEvent(MotionEvent event) {
					return RTNGodotView.this.handleTouchEvent(event);
				}

				@Override
				public boolean onGenericMotionEvent(MotionEvent event) {
					return mInputHandler.onGenericMotionEvent(event);
				}
			};
			target.setOpaque(false);
			target.setSurfaceTextureListener(this);
			textureView = target;
			addView(target, layoutParams);
			return;
		}

		SurfaceView target = new SurfaceView(getContext()) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				return RTNGodotView.this.handleTouchEvent(event);
			}

			@Override
			public boolean onGenericMotionEvent(MotionEvent event) {
				return mInputHandler.onGenericMotionEvent(event);
			}
		};
		target.getHolder().setFormat(PixelFormat.OPAQUE);
		target.getHolder().addCallback(this);
		surfaceView = target;
		addView(target, layoutParams);
	}

	public void setWindowName(String newWindowName) {
		windowName = newWindowName;
	}

	public String getWindowName() {
		return windowName;
	}

	public void setTransparent(boolean newTransparent) {
		if (transparent == newTransparent) {
			return;
		}
		transparent = newTransparent;
		setRenderTarget(transparent);
		RTNLibGodot.getInstance().setWindowTransparent(windowName, transparent);
	}

	public void setCancelTouchWhenOutside(boolean newCancelTouchWhenOutside) {
		cancelTouchWhenOutside = newCancelTouchWhenOutside;
	}

	@Override
	public void surfaceRedrawNeeded(@NonNull SurfaceHolder surfaceHolder) {
		Log.i(TAG, String.format("surfaceRedrawNeeded: %s %s", windowName, surfaceHolder.getSurface().toString()));
	}

	@Override
	public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
		Log.i(TAG, String.format("surfaceCreated: %s %s", windowName, surfaceHolder.getSurface().toString()));
	}

	@Override
	public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
		Log.i(TAG, String.format("surfaceChanged: %s %s %d %d %d", windowName, surfaceHolder.getSurface().toString(), format, width, height));
		if (surfaceView != null) {
			RTNLibGodot.getInstance().updateWindow(windowName, surfaceView.getSurfaceControl(), surfaceHolder, format, width, height, transparent);
		}
	}

	@Override
	public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
		Log.i(TAG, String.format("surfaceRemoved: %s %s", windowName, surfaceHolder.getSurface().toString()));
		if ("".equals(windowName)) {
			RTNLibGodot.getInstance().restoreDefaultWindowSurface();
		} else {
			RTNLibGodot.getInstance().removeWindow(windowName);
		}
	}

	@Override
	public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
		textureSurface = new Surface(surfaceTexture);
		RTNLibGodot.getInstance().updateTextureWindow(windowName, textureSurface, width, height, transparent);
		post(this::emitSurfaceReady);
	}

	@Override
	public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
		if (textureSurface != null) {
			RTNLibGodot.getInstance().updateTextureWindow(windowName, textureSurface, width, height, transparent);
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
		// The default Godot window is persistent, while TextureView surfaces are
		// not. Restore the persistent surface before releasing this one so a new
		// game can never initialize against a destroyed TextureView surface.
		if ("".equals(windowName)) {
			RTNLibGodot.getInstance().restoreDefaultWindowSurface();
		} else {
			RTNLibGodot.getInstance().removeWindow(windowName);
		}
		// Returning true gives TextureView ownership of releasing this surface.
		// Releasing it here as well crashes Android's native Surface reference
		// counter during the screen-transition teardown.
		textureSurface = null;
		surfaceReadyEventSent = false;
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
	}

	private void emitSurfaceReady() {
		if (surfaceReadyEventSent || textureSurface == null || !(getContext() instanceof ThemedReactContext)) {
			return;
		}
		ThemedReactContext reactContext = (ThemedReactContext)getContext();
		EventDispatcher dispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, getId());
		if (dispatcher == null) {
			return;
		}
		int surfaceId = UIManagerHelper.getSurfaceId(reactContext);
		dispatcher.dispatchEvent(new SurfaceReadyEvent(surfaceId, getId()));
		surfaceReadyEventSent = true;
	}

	@SuppressLint("ClickableViewAccessibility")
	private boolean handleTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_DOWN) {
			touchCanceledOutside = false;
		}
		if (cancelTouchWhenOutside && !touchCanceledOutside && action == MotionEvent.ACTION_MOVE && isTouchOutsideView(event)) {
			MotionEvent cancelEvent = MotionEvent.obtain(event);
			cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
			mInputHandler.onTouchEvent(cancelEvent);
			cancelEvent.recycle();
			touchCanceledOutside = true;
			return true;
		}
		if (touchCanceledOutside) {
			if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				touchCanceledOutside = false;
			}
			return true;
		}
		return mInputHandler.onTouchEvent(event);
	}

	private boolean isTouchOutsideView(MotionEvent event) {
		for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
			float x = event.getX(pointerIndex);
			float y = event.getY(pointerIndex);
			if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		return mInputHandler.onGenericMotionEvent(event);
	}

	@Override
	public boolean onCapturedPointerEvent(MotionEvent event) {
		return mInputHandler.onGenericMotionEvent(event);
	}

	private boolean canCapturePointer() {
		return true;
	}

	@Override
	public void requestPointerCapture() {
		if (canCapturePointer()) {
			super.requestPointerCapture();
			mInputHandler.onPointerCaptureChange(true);
		}
	}

	@Override
	public void releasePointerCapture() {
		super.releasePointerCapture();
		mInputHandler.onPointerCaptureChange(false);
	}

	@Override
	public void onPointerCaptureChange(boolean hasCapture) {
		super.onPointerCaptureChange(hasCapture);
		mInputHandler.onPointerCaptureChange(hasCapture);
	}
}
