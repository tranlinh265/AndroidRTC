package me.pntutorial.pnrtcblog;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.w3c.dom.Text;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.List;

import me.kevingleason.pnwebrtc.PnPeer;
import me.kevingleason.pnwebrtc.PnRTCClient;
import me.kevingleason.pnwebrtc.PnRTCListener;
import me.pntutorial.pnrtcblog.util.Constants;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends Activity implements EasyPermissions.PermissionCallbacks {

    public static final String VIDEO_TRACK_ID = "videoPN";
//    public static final String AUDIO_TRACK_ID = "audioPN";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";
    private static final int RC_CAMERA_AND_RECORD_AUDIO = 0x1;

    private PnRTCClient pnRTCClient;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private GLSurfaceView mVideoView;

    private RelativeLayout rlToastBar;
    private TextView tvToast;

    private String username;
    private boolean webRtcInitialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        rlToastBar = (RelativeLayout)findViewById(R.id.rl_toast_bar);
        tvToast = (TextView) findViewById(R.id.tv_toast);

        Log.i(Constants.LOG_TAG, "onCreate");

        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(Constants.USER_NAME)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Need to pass username to VideoChatActivity in intent extras (Constants.USER_NAME).",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        this.username  = extras.getString(Constants.USER_NAME, "");
        this.mVideoView = findViewById(R.id.gl_surface);

        // Then we set that view, and pass a Runnable to run once the surface is ready
        VideoRendererGui.setView(mVideoView, null);

        webRtcInitialized = false;
        preInitWebRTCResource();
    }

    private void showToast(String text){
        tvToast.setText(text);
        rlToastBar.setVisibility(View.VISIBLE);
        rlToastBar.animate().setStartDelay(500).translationY(-(int) getResources().getDimension(R.dimen.height_bottom_bar)).setDuration(400)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        rlToastBar.animate().translationY(0).setStartDelay(400).setDuration(400).start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
    }
    @AfterPermissionGranted(RC_CAMERA_AND_RECORD_AUDIO)
    private void preInitWebRTCResource() {
        String[] perms = { android.Manifest.permission.CAMERA };
        if (EasyPermissions.hasPermissions(this, perms)) {
            Bundle extras = getIntent().getExtras();
            initPeerConnectionFactory(extras);
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.camera_and_record_audio_rationale),
                    RC_CAMERA_AND_RECORD_AUDIO, perms);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.webRtcInitialized) {
            this.mVideoView.onPause();
            this.localVideoSource.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.webRtcInitialized) {
            this.mVideoView.onResume();
            this.localVideoSource.restart();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.webRtcInitialized) {
            this.localVideoSource.stop();
            this.pnRTCClient.onDestroy();
        }
    }

    private void initPeerConnectionFactory(Bundle extras) {
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true);  // Hardware Acceleration Enabled

        PeerConnectionFactory pcFactory = new PeerConnectionFactory();
        this.pnRTCClient = new PnRTCClient(Constants.PUB_KEY, Constants.SUB_KEY, this.username);

        // Returns the number of cams & front/back face device name
//        int camNumber = VideoCapturerAndroid.getDeviceCount();
        String frontFacingCam = CameraEnumerationAndroid.getNameOfFrontFacingDevice();
        String backFacingCam  = CameraEnumerationAndroid.getNameOfBackFacingDevice();
        boolean frontCamera = true;

        if (Constants.CAMERA_MODE_BACK.equals(extras.getString(Constants.CAMERA_MODE, ""))) {
            frontCamera = false;
            Log.i(Constants.LOG_TAG, "initializeAndroidGlobals: using back camera");
        }

        // Creates a VideoCapturerAndroid instance for the device name
        VideoCapturerAndroid capturer = (VideoCapturerAndroid) VideoCapturerAndroid.create(
                frontCamera ? frontFacingCam : backFacingCam
        );

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer, this.pnRTCClient.videoConstraints());
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);

        // First we create an AudioSource then we can create our AudioTrack
//        AudioSource audioSource = pcFactory.createAudioSource(this.pnRTCClient.audioConstraints());
//        AudioTrack localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        // Since we have our resources now, we can create our MediaStream
        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);

        // Now we can add our tracks.
        mediaStream.addTrack(localVideoTrack);
//        mediaStream.addTrack(localAudioTrack);

        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom
        remoteRender = VideoRendererGui.create(0, 0, 100, 100,
                RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
        localRender = VideoRendererGui.create(0, 0, 100, 100,
                RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);

        // First attach the RTC Listener so that callback events will be triggered
        this.pnRTCClient.attachRTCListener(new MyRTCListener());
        this.pnRTCClient.attachLocalMediaStream(mediaStream);

        // Listen on a channel. This is your "phone number," also set the max chat users.
        this.pnRTCClient.listenOn(this.username);
        this.pnRTCClient.setMaxConnections(1);

        webRtcInitialized = true;

        // If Constants.CALL_USER is in the intent extras, auto-connect them.
        boolean dialed = extras.getBoolean("dialed", false);

        if (extras.containsKey(Constants.CALL_USER) && !dialed) {
            String callUser = extras.getString(Constants.CALL_USER, "");
            connectToUser(callUser);
        }
    }

    public void connectToUser(String user) {
        this.pnRTCClient.connect(user);
    }

    public void hangup(View view) {
        this.pnRTCClient.closeAllConnections();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        preInitWebRTCResource();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }


    // VCA Code
    private class MyRTCListener extends PnRTCListener {
        @Override
        public void onLocalStream(final MediaStream localStream) {
            Log.i(Constants.LOG_TAG, "onLocalStream");
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (localStream.videoTracks.size() == 0) return;
                    Log.i(Constants.LOG_TAG, "onLocalStream:: addRenderer");
                    localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                }
            });
        }

        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer) {
            Log.i(Constants.LOG_TAG, "onAddRemoteStream");
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(VideoChatActivity.this,"Connected to " + peer.getId(), Toast.LENGTH_SHORT).show();
                    showToast(String.valueOf("Connected to " + peer.getId()));
                    try {
                        if (remoteStream.videoTracks.size() == 0) return;
//                        if(remoteStream.audioTracks.size() == 0 || remoteStream.videoTracks.size() == 0) return;

                        Log.i(Constants.LOG_TAG, "onAddRemoteStream:: addRenderer");
                        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                        VideoRendererGui.update(remoteRender, 0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
                        VideoRendererGui.update(localRender, 72, 72, 25, 25, RendererCommon.ScalingType.SCALE_ASPECT_FIT, true);
                    }
                    catch (Exception e){ e.printStackTrace(); }
                }
            });
        }

        @Override
        public void onCallReady(String callId) {
            super.onCallReady(callId);
            Log.i(Constants.LOG_TAG, "onCallReady: callId=" + callId);
        }

        @Override
        public void onConnected(String userId) {
            super.onConnected(userId);
            Log.i(Constants.LOG_TAG, "onConnected: userId=" + userId);
        }

        @Override
        public void onPeerConnectionClosed(PnPeer peer) {
            Log.i(Constants.LOG_TAG, "closed");
            Intent intent = new Intent(VideoChatActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        }


    }
}
