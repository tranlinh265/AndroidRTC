package me.kevingleason.androidrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import me.kevingleason.androidrtc.util.Constants;

/**
 * Created by linhtm on 3/7/2018.
 */

public class HomeActivity extends Activity implements View.OnClickListener{
    private Switch swPickUser;
    private Button btnCall;
    private String username,otherUsername;

    private TextView tvUsername;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_activity_main);
        this.username = getString(R.string.user_2);
        this.otherUsername = getString(R.string.user_1);
        tvUsername = (TextView) findViewById(R.id.tv_username);
        tvUsername.setText(username);
        swPickUser = (Switch) findViewById(R.id.sw_pick_user);

        swPickUser.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    changeUsername(getString(R.string.user_1),getString(R.string.user_2));
                }else{
                    changeUsername(getString(R.string.user_2),getString(R.string.user_1));
                }
            }
        });
        btnCall = (Button)findViewById(R.id.btn_call);
        btnCall.setOnClickListener(this);
    }

    private void changeUsername(String username,String otherUsername){
        HomeActivity.this.username = username;
        HomeActivity.this.otherUsername = otherUsername;

        tvUsername.setText(HomeActivity.this.username);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_call:
                SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                edit.putString(Constants.USER_NAME, username);
                edit.putString(Constants.OTHER_USER_NAME,otherUsername);
                edit.apply();
                Intent intent = new Intent(this,ActivityMain.class);
                startActivity(intent);
                break;
        }
    }

}
