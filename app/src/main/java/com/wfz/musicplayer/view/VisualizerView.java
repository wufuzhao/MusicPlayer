package com.wfz.musicplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Wufuzhao on 2016/9/22.
 */

public class VisualizerView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    SurfaceHolder mSurfaceHolder = null;
    private int width, height;
    private int startX;
    private int startY;
    private byte[] lastWave, nowWave, toWave, drawWave;
    private byte[] lastFFT, nowFFT, toFFT, drawFFT;
    private int lineW;
    private int jiange;
    private Paint fttPaint, wavePaint, cleanPaint;
    private Object synObject = new Object();

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        fttPaint = new Paint();
        fttPaint.setColor(Color.YELLOW);
        fttPaint.setStyle(Paint.Style.FILL);

        wavePaint = new Paint();
        wavePaint.setColor(Color.RED);
        wavePaint.setStyle(Paint.Style.FILL);

        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        setZOrderOnTop(true);//设置画布  背景透明
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        int p = w / 128;
        lineW = (int) (p * (2.0 / 3));
        jiange = p - lineW;
        startX = (width-128*lineW-127*jiange)/2;
        startY = height/2;
        fttPaint.setStrokeWidth(lineW);
        wavePaint.setStrokeWidth(lineW);
    }

    private void setToFft(byte[] fft) {
        if (toFFT == null) {
            nowFFT = new byte[fft.length];
            drawFFT = new byte[fft.length];
        }
        toFFT = fft;
        lastFFT = nowFFT.clone();
    }

    /*public void updateFFT(byte[] newFFT){
        setToFft(newFFT);
        update();
    }*/

    private void setToWave(byte[] waveform) {
        if (toWave == null) {
            toWave = new byte[waveform.length];
            nowWave = new byte[waveform.length];
            drawWave = new byte[waveform.length];
        }
        for (int i = 0; i < waveform.length; i++) {
            toWave[i] = (byte) (waveform[i] + 128);
        }
        lastWave = nowWave.clone();
    }

    private int buZhenShu;
    private int zhen;
    private static final int MAX_FPS = 60;
    private int fps = 60;
    private int perFrameTime = 1000 / fps;

    public void setFps(int fps) {
        this.fps = fps;
        this.perFrameTime = 1000 / fps;
    }

    public void updateWithAmin(int rate, byte[] fft, byte[] waveform) {
        synchronized (synObject){
            setToFft(fft);
            setToWave(waveform);
            this.buZhenShu = fps * 1000 / rate;
            starDrawTime = System.currentTimeMillis();
        }
    }

    public void drawWaveAndFFT(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (drawFFT != null) {
            int x = startX;
            for (byte b : drawFFT) {
                /*if(i%10==0)
                    System.out.println("nowFFT-->"+nowFFT[i]);*/
                int stopY = startY + Math.abs(b) * startY / 128;
                canvas.drawLine(x, startY, x, stopY, fttPaint);

                /*if (b > 0) {
                    float stopY = startY + b * startY / 128.0f;
                    canvas.drawLine(x, startY, x, stopY, fttPaint);
                }*/
                x += (lineW + jiange);
            }
            //Log.d("drawThread","drawFFT[0] height-->" + (int)(Math.abs(drawFFT[0]) * startY / 128));
        }
        if (drawWave != null) {
            int x = startX;
            for (byte b : drawWave) {
                /*if(i%10==0)
                    System.out.println("nowWave-->"+nowWave[i]);*/
                int stopY = startY - Math.abs(b) * startY / 128;
                canvas.drawLine(x, startY, x, stopY, wavePaint);
                /*if (b > 0) {
                    float stopY = startY - b * startY / 128.0f;
                    canvas.drawLine(x, startY, x, stopY, wavePaint);
                }*/
                x += (lineW + jiange);
            }
        }
        //Log.d("drawWaveAndFFT","Draw zhen-->" + zhen + "/" + buZhenShu);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        /*timerRunFlag = true;
        new MyTimeThread().start();*/
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        runFlag = false;
        //timerRunFlag = false;
    }

    public void draw() {
        Canvas canvas;
        if (mSurfaceHolder == null || (canvas = mSurfaceHolder.lockCanvas()) == null) {
            return;
        }
        drawWaveAndFFT(canvas);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    public void stop(){
        runFlag = false;
    }

    public void start(){
        if(runFlag == false){
            if(drawThread!=null&&drawThread.isAlive()){
                try {
                    drawThread.join(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runFlag = true;
            drawThread = new Thread(this);
            drawThread.start();
            System.out.println("drawThread start");
            //setFps(30);
        }
    }

    private boolean runFlag;
    private Thread drawThread;
    private long starDrawTime;
    private int lastZhen;

    private void calNowDataAndDraw(){
        boolean isDraw = true;
        synchronized (synObject){
            zhen = (int) ((System.currentTimeMillis()-starDrawTime)/ perFrameTime)+1;
            zhen = Math.min(zhen, buZhenShu);
            if(lastZhen==zhen) {
                //Log.d("drawThread","lastZhen-->" + lastZhen + ";zhen-->" + zhen);
                return;
            }
            Log.d("drawThread","zhen-->" + zhen + "/" + buZhenShu);
            if (zhen < buZhenShu) {
                float bl = (float) zhen / buZhenShu;
                if (toFFT != null) {
                    for (int i = 0; i < toFFT.length; i++) {
                        nowFFT[i] = (byte) (lastFFT[i] + ((toFFT[i] - lastFFT[i]) * bl));
                    }
                    //Log.d("drawThread","nowFFT[0]/toFFT[0]-->" + nowFFT[0] + "/" + toFFT[0]);
                    drawFFT = nowFFT.clone();
                }
                if (toWave != null) {
                    for (int i = 0; i < toWave.length; i++) {
                        nowWave[i] = (byte) (lastWave[i] + ((toWave[i] - lastWave[i]) * bl));
                    }
                    //Log.d("drawThread","nowWave[0]/toWave[0]-->" + nowWave[0] + "/" + toWave[0]);
                    drawWave = nowWave.clone();
                }
            } else if (zhen == buZhenShu) {
                nowFFT = toFFT.clone();
                nowWave = toWave.clone();
                drawFFT = nowFFT.clone();
                drawWave = nowWave.clone();
            }else {
                Log.d("drawThread", "帧数画完");
                isDraw = false;
            }
        }
        if (isDraw)
            draw();
        lastZhen = zhen;
    }

    @Override
    public void run() {
        while (runFlag) {
            if (toFFT != null || toWave != null) {
                calNowDataAndDraw();
            }
            try {
                Thread.sleep(perFrameTime/4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(toFFT!=null)
            drawFFT = new byte[toFFT.length];
        if(toWave!=null)
            drawWave = new byte[toWave.length];
        draw();
        System.out.println("drawThread stop");
    }

    /*private long creat2NowMs;
    private boolean timerRunFlag;
    class MyTimeThread extends Thread{
        @Override
        public void run() {
            while (timerRunFlag){
                try {
                    sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                creat2NowMs = System.currentTimeMillis();
            }
        }
    }*/
}
