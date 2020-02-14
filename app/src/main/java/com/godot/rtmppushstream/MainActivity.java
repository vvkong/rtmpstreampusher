package com.godot.rtmppushstream;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.godot.rtmppushstream.live.LivePusher;
import com.godot.rtmppushstream.live.widgets.AutoFitSurfaceView;

public class MainActivity extends AppCompatActivity {

    private LivePusher livePusher;
    private AutoFitSurfaceView surfaceView;
    private Button startBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface_view);
        findViewById(R.id.switch_camera).setOnClickListener( (v) -> {
            livePusher.switchCamera();
        });
        startBtn = findViewById(R.id.start_live);
        startBtn.setOnClickListener(v -> {
            if(PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) &&
                    PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                doStart();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
            }
        });
        livePusher = new LivePusher(this, 800, 480, 8000000, 24);
        livePusher.setPreviewDisplay(surfaceView.getHolder());
        livePusher.setOnSizeChangeListener(new LivePusher.OnSizeChangeListener() {
            @Override
            public void onChanged(int width, int height) {
                if( surfaceView != null ) {
                    surfaceView.setAspectRatio(width, height);
                }
            }
        });
    }


    private void doStart() {
        if( "start live".equals(startBtn.getText().toString()) ) {
            startBtn.setText("stop live");
            livePusher.startLive("rtmp://192.168.0.105/live360p/godot");
        } else {
            startBtn.setText("start live");
            livePusher.stopLive();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if( grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
            doStart();
        } else {
            Toast.makeText(this, "需要相机权限", Toast.LENGTH_LONG).show();
        }
    }
}
