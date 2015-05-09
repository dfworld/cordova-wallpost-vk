package ru.advrocket.wallpostvk;

import org.json.JSONException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;
import android.os.AsyncTask;
import android.app.AlertDialog;
import android.app.Activity;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKCaptchaDialog;
import com.vk.sdk.dialogs.VKShareDialog;
import com.vk.sdk.VKScope;
import com.vk.sdk.api.photo.VKUploadImage;
import com.vk.sdk.api.photo.VKImageParameters;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

public class WallpostVk extends CordovaPlugin {
	private static final String TAG = "WallpostVk";
	private static final String ACTION_INIT = "initWallpostVk";
	private static final String ACTION_SHARE = "share";
	private static final String ACTION_FRIENDS = "getFriends";
	private static final String ACTION_POSTWALL = "postWall";
	private CallbackContext _callbackContext;

	private String savedUrl = null;
	private String savedComment = null;
	private String savedImageUrl = null;

	/**
	 * Gets the application context from cordova's main activity.
	 * @return the application context
	 */
	private Context getApplicationContext() {
		return this.webView.getContext();
	}

	private Activity getActivity() {
		return (Activity)this.webView.getContext();
	}
	
	@Override
	public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
		this._callbackContext = callbackContext;
		Log.i(TAG, "Action: "+action);
		if(ACTION_INIT.equals(action)) {
			return init(args.getString(0));
		} else if (ACTION_SHARE.equals(action)) {
			return shareOrLogin(args.getString(0), args.getString(1), args.getString(2));
		} else if (ACTION_FRIENDS.equals(action)) {
			return getFriends();
		} else if (ACTION_POSTWALL.equals(action)) {
			return postWall(args.getString(0), args.getString(1), args.getString(2));
		} else {
			Log.i(TAG, "Unknown action: "+action);
		}
		return true;
	}

	private boolean init(String appId)
	{
		this.cordova.setActivityResultCallback(this);
		final String sTokenKey = "VK_ACCESS_TOKEN";
		VKSdkListener sdkListener = new VKSdkListener() {
			private void success() {
				Log.i(TAG, "VK token success");
				_callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
				_callbackContext.success();
			}
			private void fail() {
				Log.w(TAG, "VK token fail");
				_callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
				_callbackContext.error("Error");
			}
			
			@Override
			public void onCaptchaError(VKError captchaError) {
				new VKCaptchaDialog(captchaError).show();
				fail();
			}
			
			@Override
			public void onTokenExpired(VKAccessToken expiredToken) {
				//VKSdk.authorize(scope);
				Log.w(TAG, "VK token expired");
				success();
			}
			
			@Override
			public void onAccessDenied(VKError authorizationError) {
				new AlertDialog.Builder(webView.getContext())
					.setMessage(authorizationError.errorMessage)
					.show();
				Log.w(TAG, "VK Access denied!");
				fail();
			}
			
			@Override
			public void onReceiveNewToken(VKAccessToken newToken) {
				Log.i(TAG, "VK new token: "+newToken.accessToken);
				newToken.saveTokenToSharedPreferences(webView.getContext(), sTokenKey);
				success();
				share(savedUrl, savedComment, savedImageUrl);
			}
			
			@Override
			public void onAcceptUserToken(VKAccessToken token) {
				Log.i(TAG, "VK accept token: "+token.accessToken);
				success();
				share(savedUrl, savedComment, savedImageUrl);
			}
			
			@Override
			public void onRenewAccessToken(VKAccessToken newToken) {
				Log.i(TAG, "VK Renew token: "+newToken.accessToken);
				newToken.saveTokenToSharedPreferences(webView.getContext(), sTokenKey);
				success();
				share(savedUrl, savedComment, savedImageUrl);
			}
		};
		
		Log.i(TAG, "VK initialize");
		Log.i(TAG, sTokenKey);
		Log.i(TAG, appId);

		VKSdk.initialize(sdkListener, appId, VKAccessToken.tokenFromSharedPreferences(webView.getContext(), sTokenKey));
		VKUIHelper.onCreate(getActivity());

		return true;
	}

	private boolean shareOrLogin(final String url, final String comment, final String imageUrl)
	{
	 this.cordova.setActivityResultCallback(this);
		final String[] scope = new String[]{VKScope.WALL, VKScope.FRIENDS};
		if(!VKSdk.isLoggedIn()) {
			savedUrl = url;
			savedComment = comment;
			savedImageUrl = imageUrl;
			VKSdk.authorize(scope, false, true);
		} else {
			share(url, comment, imageUrl);
		}
		return true;
	}

	private boolean getFriends()
	{
		this.cordova.setActivityResultCallback(this);
		final String[] scope = new String[]{VKScope.WALL, VKScope.FRIENDS};
		if(!VKSdk.isLoggedIn()) {
			VKSdk.authorize(scope, false, true);
		} else {
		VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS, "first_name, last_name, sex, bdate, city, country, home_town, photo_50, photo_100, photo_200, online, domain, site, education, universities, schools, followers_count, occupation, relatives, relation, personal, activities, interests")).executeWithListener(new VKRequest.VKRequestListener() {
			@Override
			public void onComplete(VKResponse response) {
//				super.onComplete(response); 
				_callbackContext.success(response.json);
			}
		});
		}

		return true;
	}


	private boolean postWall(final String friendId, final String url, final String comment){
		this.cordova.setActivityResultCallback(this);
		final String[] scope = new String[]{VKScope.WALL, VKScope.FRIENDS};
		if(!VKSdk.isLoggedIn()) {
			VKSdk.authorize(scope, false, true);
		} else {
		VKApi.wall().post(VKParameters.from(VKApiConst.OWNER_ID, friendId, VKApiConst.MESSAGE, comment, VKApiConst.ATTACHMENTS, url)).executeWithListener(new VKRequest.VKRequestListener() {
			@Override 
			public void onComplete(VKResponse response) { 
//				super.onComplete(response); 
				_callbackContext.success(response.json);
//				Log.d("Result", response.responseString);
			} 
			@Override 
			public void onError(VKError error) { 
				Log.d("Errror COde", error.errorMessage);
			} 
		});
		}

		return true;
	}


	private boolean share(final String url, final String comment, final String imageUrl)
	{
		if(url == null || comment == null) return false;
		
		new AsyncTask<String, Void, String>() {
			private Bitmap image = null;
			@Override protected String doInBackground(String... args) {
				if(imageUrl != null)
					image = getBitmapFromURL(imageUrl);
				return "";
			}
			@Override protected void onPostExecute(String result) {
				VKShareDialog vsh = new VKShareDialog()
					.setText(comment)
					.setAttachmentLink("", url)
					.setShareDialogListener(new VKShareDialog.VKShareDialogListener() {
							public void onVkShareComplete(int postId) {
								Log.i(TAG, "VK sharing complete");
							}
							public void onVkShareCancel() {
								Log.i(TAG, "VK sharing cancelled");
							}
						});
				if(image != null) {
					vsh.setAttachmentImages(new VKUploadImage[]{
							new VKUploadImage(image, VKImageParameters.pngImage())
						});
				}
				vsh.show(getActivity().getFragmentManager().beginTransaction(), "VK_SHARE_DIALOG");
			}
		}.execute();

		savedUrl = null;
		savedComment = null;
		savedImageUrl = null;
		return true;
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.i(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data + ')');
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode != 0)
			VKUIHelper.onActivityResult(getActivity(), requestCode, resultCode, data);
	}
	
	public static Bitmap getBitmapFromURL(String src) {
		try {
			URL url = new URL(src);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			if(myBitmap == null) {
				Log.e(TAG, "Can't load image from "+src);
			}
			return myBitmap;
		} catch (IOException e) {
			// Log exception
			Log.e(TAG, "Can't fetch image from url "+src+": "+e);
			return null;
		}
	}
}
