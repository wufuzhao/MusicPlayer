package com.wfz.musicplayer;

import android.app.AlertDialog;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.wfz.musicplayer.view.CircleVisualizerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PalyerActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView bg;
    private ImageButton playBtn;
    private Button openFile;
    //private Equalizer mEqualizer;
    private MediaPlayer mMediaPlayer;
    private Visualizer mVisualizer;
    private CircleVisualizerView visualizerView;
    private SeekBar seekBar;
    private Mp3Album mp3Album;

    Handler handler = new Handler();
    Runnable updateThread = new Runnable() {
        public void run() {
            // 获得歌曲现在播放位置并设置成播放进度条的值
            seekBar.setProgress(mMediaPlayer.getCurrentPosition());
            // 每次延迟200毫秒再启动线程
            handler.postDelayed(updateThread, 250);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palyer);
        bg = (ImageView) findViewById(R.id.bg);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        playBtn = (ImageButton) findViewById(R.id.playBtn);
        playBtn.setOnClickListener(this);
        openFile = (Button) findViewById(R.id.openFile);
        openFile.setOnClickListener(this);
        visualizerView = (CircleVisualizerView) findViewById(R.id.visualizerView);
        AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.one_more_time_one_more_chance);
        mp3Album = new Mp3Album();
        try {
            Bitmap bitmap = mp3Album.getMp3Album(afd.createInputStream());
            bg.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initMediaPlayer();
        setupVisualizerFxAndUI();
        //setBg(BitmapFactory.decodeResource(getResources(), R.drawable.re0));
    }

    private void initMediaPlayer() {
        mMediaPlayer = MediaPlayer.create(this, R.raw.one_more_time_one_more_chance);
        mMediaPlayer
                .setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        notPlaying();
                    }
                });

        seekBar.setMax(mMediaPlayer.getDuration());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // fromUser判断是用户改变的滑块的值
                if (fromUser == true) {
                    mMediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });
    }


    private void setupVisualizerFxAndUI() {
        final int maxCR = Visualizer.getMaxCaptureRate();
        final int rate = maxCR/4;
        Toast.makeText(this,"采样频率"+rate, Toast.LENGTH_SHORT).show();
        // 实例化Visualizer，参数SessionId可以通过MediaPlayer的对象获得
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        // 设置需要转换的音乐内容长度，专业的说这就是采样,该采样值一般为2的指数倍
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
        // 接下来就好理解了设置一个监听器来监听不断而来的所采集的数据。一共有4个参数，第一个是监听者，第二个单位是毫赫兹，表示的是采集的频率，第三个是是否采集波形，第四个是是否采集频率
        mVisualizer.setDataCaptureListener(
                // 这个回调应该采集的是波形数据
                new Visualizer.OnDataCaptureListener() {
                    byte[] wave;
                    byte[] fft;
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] wave, int samplingRate) {
                        this.wave = wave;
                        //System.out.println("Wave数-->"+wave.length);
                    }

                    // 这个回调应该采集的是快速傅里叶变换有关的数据
                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] fft, int samplingRate) {
                        this.fft = fft;
                        //System.out.println("Fft数-->"+fft.length);
                        //System.out.println("samplingRate-->"+samplingRate);
                        visualizerView.updateWithAmin(rate, this.fft, this.wave);
                    }
                }, rate, true, true);
    }

    private void setBg(Bitmap bitmap){
        long startMs = System.currentTimeMillis();
        float w = 50f;
        int imgW = bitmap.getWidth();
        float r = w/imgW;
        System.out.println("缩放比例"+r);
        Matrix matrix = new Matrix();
        matrix.postScale(r,r);
        Bitmap out = Bitmap.createBitmap(bitmap,0,0,imgW,bitmap.getHeight(), matrix, false);
        RenderScript rs = RenderScript.create(this);
        Allocation overlayAlloc = Allocation.createFromBitmap(
                rs, out);
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(
                rs, Element.U8_4(rs));
        blur.setInput(overlayAlloc);
        //blur.setRadius(5f);
        blur.forEach(overlayAlloc);
        overlayAlloc.copyTo(out);
        System.out.println("花费"+(System.currentTimeMillis() - startMs) + "ms");
        bg.setImageBitmap(out);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing() && mMediaPlayer != null) {
            handler.removeCallbacks(updateThread);
            mVisualizer.release();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playBtn:
                if(mMediaPlayer.isPlaying()){
                    pause();
                }else {
                    play();
                }
                break;
            case R.id.openFile:
                pause();
                getFile();
                break;
        }
    }

    private void pause() {
        mMediaPlayer.pause();
        notPlaying();
    }

    private void notPlaying() {
        mVisualizer.setEnabled(false);
        visualizerView.stop();
        handler.removeCallbacks(updateThread);
        setVolumeControlStream(AudioManager.STREAM_SYSTEM);
        playBtn.setImageResource(android.R.drawable.ic_media_play);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void play() {
        mMediaPlayer.start();
        visualizerView.start();
        mVisualizer.setEnabled(true);
        handler.post(updateThread);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        playBtn.setImageResource(android.R.drawable.ic_media_pause);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*visualizerView.start();
        byte[] wave = new byte[128];
        byte[] fft = new byte[128];
        for (int i=0;i<128;i++) {
            wave[i] = -1;
            fft[i] = 127;
        }
        visualizerView.updateWithAmin(100, fft, wave);*/
    }



    private ListView fileListView;
    private List<File> allFiles;
    private static String path = Environment.getExternalStorageDirectory().toString();
    private FileAdapter fileAdapter;
    private AlertDialog showFile;
    public void getFile() {
        fileListView = new ListView(this);
        allFiles = updatePath(path);
        if (allFiles.size() == 0) {
            allFiles = updatePath(Environment.getExternalStorageDirectory().toString());
        }
        fileAdapter = new FileAdapter(allFiles, this);
        fileListView.setAdapter(fileAdapter);
        fileListView.setPadding(5, 10, 0, 10);
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
                File file = (File) fileAdapter.getItem(position);
                if (file.isFile()) {
                    if (!file.getName().endsWith(".mp3")) {
                        Toast.makeText(PalyerActivity.this, "请选择正确的mp3文件", Toast.LENGTH_SHORT).show();
                    } else {
                        palyMp3(file);
                    }
                    showFile.dismiss();
                } else if (file.isDirectory()) {
                    allFiles.clear();
                    allFiles.addAll(updatePath(file.getPath()));
                    fileAdapter.notifyDataSetChanged();
                }

            }
        });
        showFile = new AlertDialog.Builder(this).setTitle("请选择mp3文件：").setView(fileListView).setNegativeButton("取消", null).create();
        showFile.show();
    }

    private void palyMp3(File file) {
        pause();
        mMediaPlayer.reset();
        try {
            Bitmap bitmap = mp3Album.getMp3Album(new FileInputStream(file));
            bg.setImageBitmap(bitmap);
            mMediaPlayer.setDataSource(file.getPath());
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        play();
    }

    private List<File> updatePath(String path) {
        File[] files = new File(path).listFiles();
        ArrayList<File> allFiles = new ArrayList<File>();
        if (files!=null&&files.length > 0) {
            ArrayList<File> isFiles = new ArrayList<File>();
            ArrayList<File> isFolder = new ArrayList<File>();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".mp3")) {
                    isFiles.add(file);
                } else if (file.isDirectory()) {
                    isFolder.add(file);
                }
            }
            allFiles.addAll(isFiles);
            allFiles.addAll(isFolder);
        }
        return allFiles;
    }
}
