package me.kevingleason.androidrtc;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import me.kevingleason.androidrtc.servers.XirSysRequest;
import me.kevingleason.androidrtc.util.Constants;
import me.kevingleason.androidrtc.util.LogRTCListener;
import me.kevingleason.pnwebrtc.PnPeer;
import me.kevingleason.pnwebrtc.PnRTCClient;
import me.kevingleason.pnwebrtc.PnSignalingParams;

/**
 * Created by linhtm on 3/7/2018.
 */

public class ActivityVideo extends Activity implements View.OnClickListener{
    public static final String VIDEO_TRACK_ID = "videoPN";
    public static final String AUDIO_TRACK_ID = "audioPN";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";

    private PnRTCClient pnRTCClient;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;

    private ImageButton ibtnEndCall;
    private GLSurfaceView videoView;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ibtnEndCall = (ImageButton)findViewById(R.id.ibtn_end_call);
        ibtnEndCall.setOnClickListener(this);

        Bundle extras = getIntent().getExtras();

        this.username = extras.getString(Constants.USER_NAME, "");

        PeerConnectionFactory.initializeAndroidGlobals(this,
                false, //Audio enabled
                true,// video enabled
                true,// hardware acceleration enable
                null );
        PeerConnectionFactory pcFactory = new PeerConnectionFactory();
        this.pnRTCClient = new PnRTCClient(Constants.PUB_KEY, Constants.SUB_KEY, this.username);

        List<PeerConnection.IceServer> servers = getxirSysIceServer();

        if(!servers.isEmpty()){
            this.pnRTCClient.setSignalParams(new PnSignalingParams());
        }

        String fronFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice();

        VideoCapturer capturer = VideoCapturerAndroid.create(fronFacingCam);

        localVideoSource = pcFactory.createVideoSource(capturer, this.pnRTCClient.videoConstraints());
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);

        AudioSource audioSource = pcFactory.createAudioSource(this.pnRTCClient.audioConstraints());
        AudioTrack localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        this.videoView = (GLSurfaceView)findViewById(R.id.gl_surface);

        VideoRendererGui.setView(videoView, null);

        remoteRender = VideoRendererGui.create(0,0,100,100,VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        localRender = VideoRendererGui.create(0,0,100,100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,true);

        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);

        mediaStream.addTrack(localAudioTrack);
        mediaStream.addTrack(localVideoTrack);

        this.pnRTCClient.attachRTCListener(new MyLogRTCListener());

        this.pnRTCClient.attachLocalMediaStream(mediaStream);

        this.pnRTCClient.listenOn(this.username);
        this.pnRTCClient.setMaxConnections(1);

        String callUser = extras.getString(Constants.CALL_USER, "");
//        connectToUser(callUser, extras.getBoolean("dialed"));
    }

    private void connectToUser(String user, boolean dialed){
        this.pnRTCClient.connect(user,dialed);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.videoView.onPause();
        this.localVideoSource.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.videoView.onResume();
        this.localVideoSource.restart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(this.localVideoSource != null){
            this.localVideoSource.stop();
        }
        if(this.pnRTCClient != null){
            this.pnRTCClient.onDestroy();
        }
    }

    private List<PeerConnection.IceServer> getxirSysIceServer(){
        List<PeerConnection.IceServer> servers = new ArrayList<>();
        try{
            servers = new XirSysRequest().execute().get();
        }catch (InterruptedException e){
            e.printStackTrace();
        }catch (ExecutionException e){
            e.printStackTrace();
        }
        return servers;
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ibtn_end_call:
                break;
        }
    }

    private class MyLogRTCListener extends LogRTCListener{
        @Override
        public void onLocalStream(final MediaStream localStream) {
            super.onLocalStream(localStream);
            ActivityVideo.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(localStream.videoTracks.size() == 0)return;
                    localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                }
            });
        }

        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer) {
            super.onAddRemoteStream(remoteStream, peer);
            ActivityVideo.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ActivityVideo.this,"Connected to " + peer.getId(),Toast.LENGTH_LONG).show();
                    try{
                        if(remoteStream.videoTracks.size() == 0) return;
//                        if(remoteStream.audioTracks.size() == 0 || remoteStream.videoTracks.size() == 0) return;
                        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                        VideoRendererGui.update(remoteRender, 0,0,100,100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,false);
                        VideoRendererGui.update(localRender,72,65,25,25,VideoRendererGui.ScalingType.SCALE_ASPECT_FIT,true);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onPeerConnectionClosed(PnPeer peer) {
            super.onPeerConnectionClosed(peer);
            Intent intent = new Intent(ActivityVideo.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
