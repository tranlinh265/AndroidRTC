package me.kevingleason.androidrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import me.kevingleason.androidrtc.util.Constants;

/**
 * Created by linhtm on 3/7/2018.
 */

public class MainActivity extends Activity implements View.OnClickListener{
    private Button btnCall;
    private String username,otherUsername;
    private String stdByChannel;
    private SharedPreferences sharedPreferences;
    private Pubnub pubnub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCall = (Button) findViewById(R.id.btn_call);
        btnCall.setOnClickListener(this);
        this.sharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        this.username = this.sharedPreferences.getString(Constants.USER_NAME,"");
        this.otherUsername = this.sharedPreferences.getString(Constants.OTHER_USER_NAME, "");
        Log.d("name", "onCreate: username: "+ username + "othername: " + otherUsername );
        this.stdByChannel = this.username + Constants.STDBY_SUFFIX;
        initPubNub();
    }

    private void initPubNub(){
        this.pubnub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        this.pubnub.setUUID(this.username);
        subscribeStdBy();
    }
    private void subscribeStdBy(){
        try{
            this.pubnub.subscribe(this.stdByChannel, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("CA-iPN", "MESSAGE: "+message.toString());
                    if(!(message instanceof JSONObject))return;
                    JSONObject jsonMsg = (JSONObject)message;
                    try{
                        if(!jsonMsg.has(Constants.JSON_CALL_USER))return;
                        String user = jsonMsg.getString(Constants.JSON_CALL_USER);
                        Log.d("JSON_CALL_USER", "User: " +user);
                        dispatchIncomingCall(user);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                }

                @Override
                public void connectCallback(String channel, Object message) {
                }
            });
        }catch (PubnubException e){
            e.printStackTrace();
        }
    }
    private void dispatchCall(final String callNum){
        final String callNumStdBy = callNum + Constants.STDBY_SUFFIX;
        this.pubnub.hereNow(callNumStdBy, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Log.d("CA-dC","HERE_NOW: "+" CH - "+callNumStdBy + " " +message.toString());
                try{
                    int occupancy = ((JSONObject)message).getInt(Constants.JSON_OCCUPANCY);
                    if(occupancy == 0){
                        showToast("User is not online!");
                        return;
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Constants.JSON_CALL_USER,username);
                    jsonObject.put(Constants.JSON_CALL_TIME,System.currentTimeMillis());
                    pubnub.publish(callNumStdBy, jsonObject, new Callback() {
                        @Override
                        public void successCallback(String channel, Object message) {
                            Log.d("CA-dC", "SUCCESS: " + message.toString() );
                            Intent intent = new Intent(MainActivity.this, VideoCallActivity.class);
                            intent.putExtra(Constants.USER_NAME , username);
                            intent.putExtra(Constants.CALL_USER,callNum);
                            intent.putExtra("dialed",true);
                            startActivity(intent);
                        }
                    });
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void dispatchIncomingCall(String userId){
        showToast("Call from");
        Intent intent = new Intent(MainActivity.this,IncomingCallActivity.class);
        intent.putExtra(Constants.USER_NAME, username);
        intent.putExtra(Constants.CALL_USER,userId);
        startActivity(intent);
    }

    private void showToast(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message,Toast.LENGTH_LONG).show();
            }
        });
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_call:
                dispatchCall(otherUsername);
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(this.pubnub != null){
            this.pubnub.unsubscribeAll();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(this.pubnub==null){
            initPubNub();
        }else{
            subscribeStdBy();
        }
    }
}
