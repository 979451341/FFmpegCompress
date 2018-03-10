package org.voiddog.ffmpeg;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.voiddog.ffmpeg.FFmpegNativeBridge;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 100;
    private EditText et_input,et_output;
    private Button btn_compress;
    private TextView tv_result;
    private Handler handler;

    private static final int MSG_COMPRESS_END = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FFmpegNativeBridge.setDebug(true);

        btn_compress = (Button)findViewById(R.id.btn_compress);
        et_input = (EditText)findViewById(R.id.et_input);
        et_output = (EditText)findViewById(R.id.et_output);
        tv_result = (TextView)findViewById(R.id.tv_result);

        et_input.setText("/storage/emulated/0/pauseRecordDemo/video/video.mp4");
        et_output.setText("/storage/emulated/0/pauseRecordDemo/video/compress2.mp4");

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case MSG_COMPRESS_END:
                        btn_compress.setClickable(true);
                        Bundle bundle = msg.getData();
                        long time = bundle.getLong("time",0);
                        int result = bundle.getInt("result");
                        if(result == 1){
                            tv_result.setText("压缩失败！");
                        }else {
                            tv_result.setText("压缩成功，压缩使用时间"+time+"秒");
                        }

                        break;
                }
            }
        };

    }

    public void doCompress(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_PERMISSION);
        } else {
            tv_result.setText("开始压缩视频文件");
            btn_compress.setClickable(false);
            // test compress time
            // you need replace to your source
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    int ret = FFmpegNativeBridge.runCommand(new String[]{"ffmpeg",
                            "-i", et_input.getText().toString(),
                            "-y",
                            "-c:v", "libx264",
                            "-c:a", "aac",
                            "-vf", "scale=-2:640",
                            "-preset", "ultrafast",
                            "-b:v", "450k",
                            "-b:a", "96k",
                            et_output.getText().toString()});
                    System.out.println("ret: " + ret + ", time: " + (System.currentTimeMillis() - startTime));

                    Message message = handler.obtainMessage();
                    message.what = MSG_COMPRESS_END;
                    Bundle bundle = new Bundle();
                    bundle.putInt("result",ret);
                    bundle.putLong("time",(System.currentTimeMillis() - startTime)/1000);
                    message.setData(bundle);

                    handler.sendMessage(message);
                }
            }).start();



        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION){
            doCompress(null);
        }
    }
}
