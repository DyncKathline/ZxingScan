package org.dync.zxinglibrary.decod;

import android.os.Handler;
import android.os.Message;

import com.google.zxing.Result;

import org.dync.zxinglibrary.ScanManager;

public class PhotoScanHandler extends Handler {
	public final static int PHOTODECODEERROR = 0;
	public final static int PHOTODECODEOK = 1;
  	ScanManager scanManager;
	public PhotoScanHandler(ScanManager scanManager) {
		this.scanManager = scanManager;
	}
	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case PHOTODECODEERROR:
      		scanManager.handleDecodeError((Exception)message.obj);
			break;
		case PHOTODECODEOK:
      		scanManager.handleDecode((Result) message.obj, null, 1.0f);
			break;
		default:
			break;
		}
	}

}
