/*
 * Copyright (C) 2012 ZXing authors
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

package org.dync.zxinglibrary.zxing.camera.open;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;

public class OpenCameraInterface {

	static final String TAG = OpenCameraInterface.class.getName();
	private static OpenCameraInterface mInstance;
	private int mFrontCameraId = -1;
	private int mBackCameraId = -1;
	private Camera mCamera;
	private int mCameraId = -1;

	public static OpenCameraInterface getInstance() {
		if(mInstance == null) {
			synchronized (OpenCameraInterface.class) {
				if(mInstance == null) {
					mInstance = new OpenCameraInterface();
				}
			}
		}
		return mInstance;
	}

	/**
	 * 初始化摄像头信息。
	 */
	public void initCameraInfo() {
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
	public int switchCameraId() {
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
	 * 获取要开启的相机 ID，优先开启后置。
	 */
	private int getAutoCameraId() {
		if (hasBackCamera()) {
			return mBackCameraId;
		} else if (hasFrontCamera()) {
			return mFrontCameraId;
		} else {
			throw new RuntimeException("No available camera id found.");
		}
	}

	/**
	 * 返回当前的摄像头id
	 */
	public int getCameraId() {
		return mCameraId;
	}

	/**
	 * Opens the requested camera with {@link Camera#open(int)}, if one exists.
	 * 
	 * @param cameraId
	 *            camera ID of the camera to use. A negative value means
	 *            "no preference"
	 * @return handle to {@link Camera} that was opened
	 */
	public Camera open(int cameraId) {
		int numCameras = Camera.getNumberOfCameras();
		if (numCameras == 0 || (!hasBackCamera() && !hasFrontCamera())) {
			Log.w(TAG, "No cameras!");
			return null;
		}

		mCamera = Camera.open(cameraId);
		mCameraId = cameraId;
		Log.d(TAG, "Camera[" + cameraId + "] has been opened.");

		return mCamera;
	}

	/**
	 * Opens a rear-facing camera with {@link Camera#open(int)}, if one exists,
	 * or opens camera 0.
	 * 
	 * @return handle to {@link Camera} that was opened
	 */
	public Camera open() {
		return open(getAutoCameraId());
	}

}
