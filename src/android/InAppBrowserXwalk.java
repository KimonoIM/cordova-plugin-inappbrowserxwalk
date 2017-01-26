package com.example.plugin.InAppBrowserXwalk;

import com.example.plugin.InAppBrowserXwalk.BrowserDialog;

import android.content.res.Resources;
import org.apache.cordova.*;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkCookieManager;

import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.widget.Toast;

import android.webkit.WebResourceResponse;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class InAppBrowserXwalk extends CordovaPlugin {

    private BrowserDialog dialog;
    private XWalkView xWalkWebView;
    private XWalkCookieManager mCookieManager;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if(action.equals("open")) {
            this.callbackContext = callbackContext;
            this.openBrowser(data);
        }

        if(action.equals("close")) {
            this.closeBrowser();
        }

        if(action.equals("show")) {
            this.showBrowser();
        }

        if(action.equals("hide")) {
            this.hideBrowser();
        }

        return true;
    }

    class MyResourceClient extends XWalkResourceClient {
        MyResourceClient(XWalkView view) { super(view); }

        @Override
        public void onReceivedLoadError (XWalkView view, int errorCode, String description, String failingUrl) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", "loaderror");
                obj.put("code", errorCode);
                obj.put("status", "failed");
                obj.put("description", description);
                obj.put("url", failingUrl);
                PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
            } catch (JSONException ex) {}
        }
    }

    class MyClientUI extends XWalkUIClient {
           MyClientUI(XWalkView view) {
               super(view);
           }

           @Override
           public void onPageLoadStarted (XWalkView view, String url) {
               try {
                   JSONObject obj = new JSONObject();
                   obj.put("type", "loadstart");
                   obj.put("url", url);
                   PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                   result.setKeepCallback(true);
                   callbackContext.sendPluginResult(result);
               } catch (JSONException ex) {}
           }

           @Override
           public void onPageLoadStopped (XWalkView view, String url, LoadStatus status) {
               try {
                   String code = (status == LoadStatus.FINISHED) ? "loadstop" : "loaderror";

                   JSONObject obj = new JSONObject();
                   obj.put("type", code);
                   obj.put("url", url);

                   if (status != LoadStatus.FINISHED) {
                       obj.put("status", status == LoadStatus.FAILED ? "failed": "cancelled");
                   }

                   PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                   result.setKeepCallback(true);
                   callbackContext.sendPluginResult(result);
               } catch (JSONException ex) {}
           }
   }

   public List<String> toListOfCookieValue(JSONArray array) {
    if(array==null)
        return null;

    List<String> arr=new ArrayList<String>();
    for(int i=0; i<array.length(); i++) {
        arr.add(array.optString(i));
    }
    return arr;
}

    private void openBrowser(final JSONArray data) throws JSONException {
        final String url = data.getString(0);
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String toolbarColor = "#FFFFFF";
                int toolbarHeight = 80;
                String closeButtonText = "< Close";
                int closeButtonSize = 25;
                String closeButtonColor = "#000000";
                boolean openHidden = false;
                boolean clearcache = false;
                boolean clearsessioncache = false;
                String cookie = "";
                List<String> urls = new ArrayList<String>();

                if(data != null && data.length() > 1) {
                    try {
                            JSONObject options = new JSONObject(data.getString(1));

                            if(!options.isNull("toolbarColor")) {
                                toolbarColor = options.getString("toolbarColor");
                            }
                            if(!options.isNull("toolbarHeight")) {
                                toolbarHeight = options.getInt("toolbarHeight");
                            }
                            if(!options.isNull("closeButtonText")) {
                                closeButtonText = options.getString("closeButtonText");
                            }
                            if(!options.isNull("closeButtonSize")) {
                                closeButtonSize = options.getInt("closeButtonSize");
                            }
                            if(!options.isNull("closeButtonColor")) {
                                closeButtonColor = options.getString("closeButtonColor");
                            }
                            if(!options.isNull("openHidden")) {
                                openHidden = options.getBoolean("openHidden");
                            }
                            if(!options.isNull("clearcache")) {
                                clearcache = options.getBoolean("clearcache");
                            }
                            if(!options.isNull("clearsessioncache")) {
                                clearcache = options.getBoolean("clearsessioncache");
                            }
                            if(!options.isNull("cookie")) {
                                cookie = options.getString("cookie");
                                Log.d("lalala", "Got cookie: " + cookie);
                            } else {
                                Log.d("lalala", "No cookie: " + cookie);
                            }
                            if (!options.isNull("url")) {
                                JSONArray urlsArray = options.getJSONArray("url");
                                urls = toListOfCookieValue(urlsArray);
                            }
                        }
                    catch (JSONException ex) {}
                }

                dialog = new BrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                xWalkWebView = new XWalkView(cordova.getActivity(), cordova.getActivity());

                mCookieManager = new XWalkCookieManager();
                mCookieManager.setAcceptCookie(true);
                mCookieManager.setAcceptFileSchemeCookies(true);

                if(clearcache) {
                    mCookieManager.removeAllCookie();
                } else if (clearsessioncache) {
                    mCookieManager.removeSessionCookie();
                }

                //mCookieManager.setCookie(url, cookie);

                if (urls.size() > 0) {
                    for(int i=0; i<urls.size(); i++) {
                                String urlItem = urls.get(i);
                                mCookieManager.setCookie(urlItem, cookie);
                    }
                } else {
                    mCookieManager.setCookie(url, cookie);
                }
               

                xWalkWebView.setUIClient(new MyClientUI(xWalkWebView));
                xWalkWebView.setResourceClient(new MyResourceClient(xWalkWebView));
                xWalkWebView.load(url, "");

                LinearLayout main = new LinearLayout(cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);

                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                toolbar.setBackgroundColor(android.graphics.Color.parseColor(toolbarColor));
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolbarHeight));
                toolbar.setPadding(5, 5, 5, 5);

                TextView closeButton = new TextView(cordova.getActivity());
                closeButton.setText(closeButtonText);
                closeButton.setTextSize(closeButtonSize);
                closeButton.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
                closeButton.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                toolbar.addView(closeButton);

                closeButton.setOnClickListener(new View.OnClickListener() {
                     public void onClick(View v) {
                         closeBrowser();
                     }
                 });

                main.addView(toolbar);
                main.addView(xWalkWebView);

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                dialog.setCancelable(true);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
                dialog.addContentView(main, layoutParams);
                if(!openHidden) {
                    dialog.show();
                }
            }
        });
    }

    public void hideBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dialog != null) {
                    dialog.hide();
                }
            }
        });
    }

    public void showBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dialog != null) {
                    dialog.show();
                }
            }
        });
    }

    public void closeBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                xWalkWebView.onDestroy();
                dialog.dismiss();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("type", "exit");
                    PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                } catch (JSONException ex) {}
            }
        });
    }
}
