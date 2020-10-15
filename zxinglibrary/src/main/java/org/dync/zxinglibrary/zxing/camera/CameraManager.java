/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dync.zxinglibrary.zxing.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.zxing.PlanarYUVLuminanceSource;

import org.dync.zxinglibrary.zxing.camera.open.OpenCameraInterface;

import java.io.IOException;
import java.util.List;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
@SuppressWarnings("deprecation") // camera APIs
public final class CameraManager {

	private static final String TAG = CameraManager.class.getSimpleName();

	private static final int MIN_FRAME_WIDTH = 240;
	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 675;
	private static final int MAX_FRAME_HEIGHT = 675;

	private final Context mContext;
	private SurfaceView surfaceView;
	private int mFrontCameraId = -1;
	private int mBackCameraId = -1;
	private Camera mCamera;
	private AutoFocusManager autoFocusManager;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean initialized;
	private boolean previewing;
	private int mCameraId = -1;
	private int requestedFramingRectWidth;
	private int requestedFramingRectHeight;
	// 屏幕分辨率
	Point screenResolution;
	// 相机分辨率
	Point cameraResolution;

	private int orientation = -1;
	/**
	 * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
	 * clear the handler so it will only receive one message.
	 */
	private PreviewCallback previewCallback;

	public CameraManager(Context context) {
		this.mContext = context;
		initCameraInfo();
		mCameraId = getCameraId();
	}

	public synchronized void setConfiguration(int orientation) {
		this.orientation = orientation;
	}

	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 *
	 * @param surfaceView The surface object which the camera will draw preview frames into.
	 * @throws IOException Indicates the camera driver failed to open.
	 */
	public synchronized void openDriver(SurfaceView surfaceView) throws IOException {
		this.surfaceView = surfaceView;
		if (mCamera == null) {
			openCamera(mCameraId);

			if (mCamera == null) {
				throw new IOException("Camera.open() failed to return object from driver");
			}
			if(orientation == -1) {
				int degrees = getCameraDisplayOrientation(mContext, mCameraId);
				mCamera.setDisplayOrientation(degrees);
//				if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//					mCamera.setDisplayOrientation(90);
//				} else {//如果是横屏
//					mCamera.setDisplayOrientation(0);
//				}
			}else {
				mCamera.setDisplayOrientation(orientation);
			}
		}

		if (!initialized) {
			initialized = true;

			Camera.Parameters parameters = mCamera.getParameters();
			WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			Display display = manager.getDefaultDisplay();

			Point theScreenResolution = new Point();
			display.getSize(theScreenResolution);
			screenResolution = theScreenResolution;

			//initFromCameraParameters
			Log.i(TAG, ">>>>>>>Screen resolution in current orientation: " + screenResolution);
			cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);
			Log.i(TAG, ">>>>>>>Camera resolution: " + cameraResolution);
			Log.i(TAG, ">>>>>>>setPreviewSize Preview size on screen: " + cameraResolution);
			previewCallback = new PreviewCallback(cameraResolution);

			if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
				setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
				requestedFramingRectWidth = 0;
				requestedFramingRectHeight = 0;
			}
		}

		Camera.Parameters parameters = mCamera.getParameters();
		Log.i(TAG, ">>>>>>>setPreviewSize Preview size on screen: " + cameraResolution);
		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
		mCamera.setParameters(parameters);

		mCamera.setPreviewDisplay(surfaceView.getHolder());
	}

	private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
	private static final double MAX_ASPECT_DISTORTION = 0.05;
	public Point findBestPreviewSizeValue(Camera.Parameters parameters,final Point screenResolution) {

		List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
		if (rawSupportedSizes == null) {
			Log.w(TAG, "Device returned no supported preview sizes; using default");
			Camera.Size defaultSize = parameters.getPreviewSize();
			if (defaultSize == null) {
				throw new IllegalStateException("Parameters contained no preview size!");
			}
			return new Point(defaultSize.width, defaultSize.height);
		}

		if (Log.isLoggable(TAG, Log.INFO)) {
			StringBuilder previewSizesString = new StringBuilder();
			for (Camera.Size size : rawSupportedSizes) {
				previewSizesString.append(size.width).append('x').append(size.height).append(' ');
			}
			Log.i(TAG, "Supported preview sizes: " + previewSizesString);
		}

		double screenAspectRatio;
		if(screenResolution.x < screenResolution.y){
			screenAspectRatio = screenResolution.x / (double) screenResolution.y;
		}else{
			screenAspectRatio = screenResolution.y / (double) screenResolution.x;
		}
		Log.i(TAG, "screenAspectRatio: " + screenAspectRatio);
		// Find a suitable size, with max resolution
		int maxResolution = 0;

		Camera.Size maxResPreviewSize = null;
		for (Camera.Size size : rawSupportedSizes) {
			int realWidth = size.width;
			int realHeight = size.height;
			int resolution = realWidth * realHeight;
			if (resolution < MIN_PREVIEW_PIXELS) {
				continue;
			}

			boolean isCandidatePortrait = realWidth < realHeight;
			int maybeFlippedWidth = isCandidatePortrait ? realWidth: realHeight ;
			int maybeFlippedHeight = isCandidatePortrait ? realHeight : realWidth;
			Log.i(TAG, String.format("maybeFlipped:%d * %d",maybeFlippedWidth,maybeFlippedHeight));

			double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
			Log.i(TAG, "aspectRatio: " + aspectRatio);
			double distortion = Math.abs(aspectRatio - screenAspectRatio);
			Log.i(TAG, "distortion: " + distortion);
			if (distortion > MAX_ASPECT_DISTORTION) {
				continue;
			}

			if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
				Point exactPoint = new Point(realWidth, realHeight);
				Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
				return exactPoint;
			}

			// Resolution is suitable; record the one with max resolution
			if (resolution > maxResolution) {
				maxResolution = resolution;
				maxResPreviewSize = size;
			}
		}

		// If no exact match, use largest preview size. This was not a great idea on older devices because
		// of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
		// the CPU is much more powerful.
		if (maxResPreviewSize != null) {
			Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
			Log.i(TAG, "Using largest suitable preview size: " + largestSize);
			return largestSize;
		}

		// If there is nothing at all suitable, return current preview size
		Camera.Size defaultPreview = parameters.getPreviewSize();
		if (defaultPreview == null) {
			throw new IllegalStateException("Parameters contained no preview size!");
		}
		Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
		Log.i(TAG, "No suitable preview sizes, using default: " + defaultSize);
		return defaultSize;
	}

	/**
	 * 初始化摄像头信息。
	 */
	private void initCameraInfo() {
		int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
		for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				// 后置摄像头信息
				mBackCameraId = cameraId;
			} else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				// 前置摄像头信息
				mFrontCameraId = cameraId;
			}
		}
	}

	/**
	 * 切换前后置时切换ID
	 */
	private int switchCameraId() {
		if (mCameraId == mFrontCameraId && hasBackCamera()) {
			return mBackCameraId;
		} else if (mCameraId == mBackCameraId && hasFrontCamera()) {
			return mFrontCameraId;
		} else {
			throw new RuntimeException("No available camera id to switch.");
		}
	}

	/**
	 * 判断是否有后置摄像头。
	 *
	 * @return true 代表有后置摄像头
	 */
	private boolean hasBackCamera() {
		return mBackCameraId != -1;
	}

	/**
	 * 判断是否有前置摄像头。
	 *
	 * @return true 代表有前置摄像头
	 */
	private boolean hasFrontCamera() {
		return mFrontCameraId != -1;
	}

	/**
	 * 开启指定摄像头
	 */
	private Camera openCamera(int cameraId) {
		Camera camera = mCamera;
		if (camera != null) {
			throw new RuntimeException("You must close previous camera before open a new one.");
		}
		if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			mCamera = Camera.open(cameraId);
			mCameraId = cameraId;
			Log.d(TAG, "Camera[" + cameraId + "] has been opened.");
		}
		return mCamera;
	}

	/**
	 * 获取要开启的相机 ID，优先开启后置。
	 */
	private int getCameraId() {
		if (hasBackCamera()) {
			return mBackCameraId;
		} else if (hasFrontCamera()) {
			return mFrontCameraId;
		} else {
			throw new RuntimeException("No available camera id found.");
		}
	}

	public int getCameraDisplayOrientation(Context activity, int cameraId) {
		android.hardware.Camera.CameraInfo info =
				new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
		int rotation = wm.getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			if(rotation == Surface.ROTATION_90) {
				result = (result + 270)%360;
			}else if(rotation == Surface.ROTATION_270) {
				result = (result + 90)%360;
			}
		}else {
			if(rotation == Surface.ROTATION_0) {
				result = (result + 90)%360;
			}else if(rotation == Surface.ROTATION_180) {
				result = (result + 270)%360;
			}
		}
		String msg = "PORTRAIT is " + (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) + ", Camera[" + cameraId + "] rotation is " + info.orientation + ", device rotation is " + degrees + ", result is " + result;
		Log.d(TAG, msg);
		return result;
	}

	public boolean isZoomSupported() {
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			if (parameters != null) {
				return parameters.isZoomSupported();
			}
		}
		return false;
	}

	public void setZoom(int value) {
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			if (parameters != null) {
				if (parameters.isZoomSupported()) {
					int maxZoom = parameters.getMaxZoom();
					int currentValue = value * maxZoom / 100;
					parameters.setZoom(currentValue);
					//刷新
					mCamera.setParameters(parameters);
				}
			}
		}
	}

	public synchronized boolean isOpen() {
		return mCamera != null;
	}

	/**
	 * Closes the camera driver if still in use.
	 */
	public synchronized void closeDriver() {
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
			// Make sure to clear these each time we close the camera, so that any scanning rect
			// requested by intent is forgotten.
			framingRect = null;
			framingRectInPreview = null;
		}
	}

	/**
	 * Asks the camera hardware to begin drawing preview frames to the screen.
	 */
	public synchronized void startPreview() {
		Camera theCamera = mCamera;
		if (theCamera != null && !previewing) {
			theCamera.startPreview();
			previewing = true;
			autoFocusManager = new AutoFocusManager(mContext, theCamera);
		}
	}

	/**
	 * Tells the camera to stop drawing preview frames.
	 */
	public synchronized void stopPreview() {
		if (autoFocusManager != null) {
			autoFocusManager.stop();
			autoFocusManager = null;
		}
		if (mCamera != null && previewing) {
			mCamera.stopPreview();
			previewCallback.setHandler(null, 0);
			previewing = false;
		}
	}

	public void switchCamera() {
		int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
		if (numberOfCameras == 1) {
			return;
		}
		mCameraId = switchCameraId();// 切换摄像头 ID
		stopPreview();// 停止预览
		closeDriver();// 关闭当前的摄像头
		try {
			openDriver(surfaceView);// 开启新的摄像头
		} catch (IOException e) {
			e.printStackTrace();
		}
		startPreview();// 开启预览
	}

	public boolean hasLight() {
		return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
	}

	/**
	 * 打开闪光灯
	 */
	public void openLight() {
		if (mCamera != null) {
			Camera.Parameters parameter = mCamera.getParameters();
			parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			mCamera.setParameters(parameter);
		}
	}

	/**
	 * 关闭闪光灯
	 */
	public void offLight() {
		if (mCamera != null) {
			Camera.Parameters parameter = mCamera.getParameters();
			parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(parameter);
		}
	}

	/**
	 * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
	 * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
	 * respectively.
	 *
	 * @param handler The handler to send the message to.
	 * @param message The what field of the message to be sent.
	 */
	public synchronized void requestPreviewFrame(Handler handler, int message) {
		Camera theCamera = mCamera;
		if (theCamera != null && previewing) {
			previewCallback.setHandler(handler, message);
			theCamera.setOneShotPreviewCallback(previewCallback);
		}
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user where to place the
	 * barcode. This target helps with alignment as well as forces the user to hold the device
	 * far enough away to ensure the image will be in focus.
	 *
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public synchronized Rect getFramingRect() {
		if (framingRect == null) {
			if (mCamera == null) {
				return null;
			}
			if (screenResolution == null) {
				// Called early, before init even finished
				return null;
			}

			int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
			int height = width;

            int leftOffset;
            int topOffset;
			leftOffset = (screenResolution.x - width) / 2;
			topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
			Log.d(TAG, "Calculated framing rect: " + framingRect);
		}
		return framingRect;
	}

	private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
		int dim = 5 * resolution / 8; // Target 5/8 of each dimension
		if (dim < hardMin) {
			return hardMin;
		}
		if (dim > hardMax) {
			return hardMax;
		}
		return dim;
	}

	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
	 * not UI / screen.
	 *
	 * @return {@link Rect} expressing barcode scan area in terms of the preview size
	 */
	public synchronized Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect framingRect = getFramingRect();
			if (framingRect == null) {
				return null;
			}
			Rect rect = new Rect(framingRect);
			if (cameraResolution == null || screenResolution == null) {
				// Called early, before init even finished
				return null;
			}
			//2017.11.13 添加竖屏代码处理
			if (screenResolution.x < screenResolution.y) {
				// portrait
				rect.left = rect.left * cameraResolution.y / screenResolution.x;
				rect.right = rect.right * cameraResolution.y / screenResolution.x;
				rect.top = rect.top * cameraResolution.x / screenResolution.y;
				rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
			} else {
				// landscape
				rect.left = rect.left * cameraResolution.x / screenResolution.x;
				rect.right = rect.right * cameraResolution.x / screenResolution.x;
				rect.top = rect.top * cameraResolution.y / screenResolution.y;
				rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
			}
			framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}


	/**
	 * Allows third party apps to specify the camera ID, rather than determine
	 * it automatically based on available cameras and their orientation.
	 *
	 * @param cameraId camera ID of the camera to use. A negative value means "no preference".
	 */
	public synchronized void setManualCameraId(int cameraId) {
		mCameraId = cameraId;
	}

	/**
	 * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
	 * them automatically based on screen resolution.
	 *
	 * @param width  The width in pixels to scan.
	 * @param height The height in pixels to scan.
	 */
	public synchronized void setManualFramingRect(int width, int height) {
		if (initialized) {
			if (width > screenResolution.x) {
				width = screenResolution.x;
			}
			if (height > screenResolution.y) {
				height = screenResolution.y;
			}
			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
			Log.d(TAG, "Calculated manual framing rect: " + framingRect);
			framingRectInPreview = null;
		} else {
			requestedFramingRectWidth = width;
			requestedFramingRectHeight = height;
		}
	}

	/**
	 * A factory method to build the appropriate LuminanceSource object based on the format
	 * of the preview buffers, as described by Camera.Parameters.
	 *
	 * @param data   A preview frame.
	 * @param width  The width of the image.
	 * @param height The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
		Rect rect = getFramingRectInPreview();
		if (rect == null) {
			return null;
		}
		// Go ahead and assume it's YUV rather than die.
		return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
				rect.width(), rect.height(), false);
	}

}