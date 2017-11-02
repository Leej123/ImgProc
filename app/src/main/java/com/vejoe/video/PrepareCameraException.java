package com.vejoe.video;

import android.util.Log;

/**
 * <p>@author: Leej
 * <p>Company: VEJOE
 * <p>Comment: 在准备录制时抛出的异常。
 * <p>Date: 2017/10/28 0028-上午 10:52
 */
public class PrepareCameraException extends Exception {

	private static final String	LOG_PREFIX			= "Unable to unlock camera - ";
	private static final String	MESSAGE				= "Unable to use camera for recording";

	private static final long	serialVersionUID	= 6305923762266448674L;

	@Override
	public String getMessage() {
		Log.e(getClass().getSimpleName(), LOG_PREFIX + MESSAGE);
		return MESSAGE;
	}
}
