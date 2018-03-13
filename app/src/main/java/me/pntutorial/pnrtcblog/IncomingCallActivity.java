package me.pntutorial.pnrtcblog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;

import org.json.JSONObject;

import me.kevingleason.pnwebrtc.PnPeerConnectionClient;
import me.pntutorial.pnrtcblog.util.Constants;

/**
 * Created by linhtm on 3/13/2018.
 */

public class IncomingCallActivity extends Activity implements View.OnClickListener {
    private String username, callUser;
    private Pubnub pubnub;
    private SharedPreferences sp;
    private ImageButton ibtnCall, ibtnEndCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ibtnCall = (ImageButton)findViewById(R.id.ibtn_call);
        ibtnEndCall = (ImageButton)findViewById(R.id.ibtn_end_call);

        ibtnCall.setOnClickListener(this);
        ibtnEndCall.setOnClickListener(this);

        this.sp = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);

        this.username = this.sp.getString(Constants.USER_NAME, "");

        Bundle extras = getIntent().getExtras();
        this.callUser = extras.getString(Constants.CALL_USER, "");

        this.pubnub = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        this.pubnub.setUUID(this.username);
    }

    private void acceptCall(){
        Intent intent = new Intent(IncomingCallActivity.this,VideoChatActivity.class);
        intent.putExtra(Constants.USER_NAME, this.username);
        intent.putExtra(Constants.CALL_USER, this.callUser);
        intent.putExtra("dialed", false);
        startActivity(intent);
    }

    private void rejectCall(){
        JSONObject hangupMSg = PnPeerConnectionClient.generateHangupPacket(this.username);
        this.pubnub.publish(this.callUser, hangupMSg, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Intent intent = new Intent(IncomingCallActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(this.pubnub !=null){
            this.pubnub.unsubscribeAll();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ibtn_call:
                acceptCall();
                break;
            case R.id.ibtn_end_call:
                rejectCall();
                break;
        }
    }
}
