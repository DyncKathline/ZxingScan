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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * 邮箱: 1076559197@qq.com | tauchen1990@gmail.com
 * 
 * 作者: 陈涛
 * 
 * 日期: 2014年8月20日
 * 
 * 描述: 该类主要负责设置相机的参数信息，获取最佳的预览界面
 * 
 */
public final class CameraConfigurationManager {

	static final String TAG = "CameraConfiguration";

	static final int MIN_PREVIEW_PIXELS = 480 * 320;
	static final double MAX_ASPECT_DISTORTION = 0.15;

	final Context context;

	// 屏幕分辨率
	Point screenResolution;
	// 相机分辨率
	Point cameraResolution;
	private Point bestPreviewSize;
	private Point previewSizeOnScreen;

	public CameraConfigurationManager(Context context) {
		this.context = context;
	}

	public void initFromCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();

		int displayRotation = display.getRotation();
		int cwRotationFromNaturalToDisplay;
		switch (displayRotation) {
			case Surface.ROTATION_0:
				cwRotationFromNaturalToDisplay = 0;
				break;
			case Surface.ROTATION_90:
				cwRotationFromNaturalToDisplay = 90;
				break;
			case Surface.ROTATION_180:
				cwRotationFromNaturalToDisplay = 180;
				break;
			case Surface.ROTATION_270:
				cwRotationFromNaturalToDisplay = 270;
				break;
			default:
				// Have seen this return incorrect values like -90
				if (displayRotation % 90 == 0) {
					cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
				} else {
					throw new IllegalArgumentException("Bad rotation: " + displayRotation);
				}
		}
		Log.i(TAG, "Display at: " + cwRotationFromNaturalToDisplay);

		Point theScreenResolution = new Point();
		display.getSize(theScreenResolution);
		screenResolution = theScreenResolution;

		Log.i(TAG, ">>>>>>>Screen resolution in current orientation: " + screenResolution);
		cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution);
		Log.i(TAG, ">>>>>>>Camera resolution: " + cameraResolution);
		bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution);
		Log.i(TAG, ">>>>>>>Best available preview size: " + bestPreviewSize);

		boolean isScreenPortrait = screenResolution.x < screenResolution.y;
		boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

		if (isScreenPortrait == isPreviewSizePortrait) {
			previewSizeOnScreen = bestPreviewSize;
		} else {
			previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
		}
		Log.i(TAG, ">>>>>>>Preview size on screen: " + previewSizeOnScreen);
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
    Point getDisplaySize(final Display display) {
		final Point point = new Point();
		try {
			display.getSize(point);
		} catch (NoSuchMethodError ignore) {
			point.x = display.getWidth();
			point.y = display.getHeight();
		}
		return point;
	}

	public void setDesiredCameraParameters(Camera camera, boolean safeMode, int rotation) {
		Camera.Parameters parameters = camera.getParameters();

		if (parameters == null) {
			Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
			return;
		}

		Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

		if (safeMode) {
			Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
		}

		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
		camera.setParameters(parameters);

		Camera.Parameters afterParameters = camera.getParameters();
		Camera.Size afterSize = afterParameters.getPreviewSize();
		if (afterSize != null && (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize.height)) {
			Log.w(TAG, "Camera said it supported preview size " + cameraResolution.x + 'x' + cameraResolution.y + ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
			cameraResolution.x = afterSize.width;
			cameraResolution.y = afterSize.height;
		}

		// 1.获取屏幕切换角度值。
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}
		// 2.获取摄像头方向。
		int index = 0;
		int number = Camera.getNumberOfCameras();
		while (index < number) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(index, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				break;
			}
			index++;
		}
		android.hardware.Camera.CameraInfo info =
				new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(index, info);
		// 3.设置相机显示方向。
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}

	public Point getCameraResolution() {
		return cameraResolution;
	}

	public Point getScreenResolution() {
		return screenResolution;
	}

	/**
	 * 从相机支持的分辨率中计算出最适合的预览界面尺寸
	 * 
	 * @param parameters
	 * @param screenResolution
	 * @return
	 */
	Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {
		List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
		if (rawSupportedSizes == null) {
			Log.w(TAG, "Device returned no supported preview sizes; using default");
			Camera.Size defaultSize = parameters.getPreviewSize();
			return new Point(defaultSize.width, defaultSize.height);
		}

		// Sort by size, descending
		List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(rawSupportedSizes);
		Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
			@Override
			public int compare(Camera.Size a, Camera.Size b) {
				int aPixels = a.height * a.width;
				int bPixels = b.height * b.width;
				if (bPixels < aPixels) {
					return -1;
				}
				if (bPixels > aPixels) {
					return 1;
				}
				return 0;
			}
		});

		if (Log.isLoggable(TAG, Log.INFO)) {
			StringBuilder previewSizesString = new StringBuilder();
			for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
				previewSizesString.append(supportedPreviewSize.width).append('x').append(supportedPreviewSize.height).append(' ');
			}
			Log.i(TAG, "Supported preview sizes: " + previewSizesString);
		}

		double screenAspectRatio = (double) screenResolution.x / (double) screenResolution.y;

		// Remove sizes that are unsuitable
		Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
		while (it.hasNext()) {
			Camera.Size supportedPreviewSize = it.next();
			int realWidth = supportedPreviewSize.width;
			int realHeight = supportedPreviewSize.height;
			if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
				it.remove();
				continue;
			}

			boolean isCandidatePortrait = realWidth < realHeight;
			int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
			int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;

			double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
			double distortion = Math.abs(aspectRatio - screenAspectRatio);
			if (distortion > MAX_ASPECT_DISTORTION) {
				it.remove();
				continue;
			}

			if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
				Point exactPoint = new Point(realWidth, realHeight);
				Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
				return exactPoint;
			}
		}

		// If no exact match, use largest preview size. This was not a great
		// idea on older devices because
		// of the additional computation needed. We're likely to get here on
		// newer Android 4+ devices, where
		// the CPU is much more powerful.
		if (!supportedPreviewSizes.isEmpty()) {
			Camera.Size largestPreview = supportedPreviewSizes.get(0);
			Point largestSize = new Point(largestPreview.width, largestPreview.height);
			Log.i(TAG, "Using largest suitable preview size: " + largestSize);
			return largestSize;
		}

		// If there is nothing at shouquan_ic_all suitable, return current preview size
		Camera.Size defaultPreview = parameters.getPreviewSize();
		Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
		Log.i(TAG, "No suitable preview sizes, using default: " + defaultSize);

		return defaultSize;
	}
}
