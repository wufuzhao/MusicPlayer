package com.wfz.musicplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Wufuzhao on 2016/10/13.
 */

public class CircleVisualizerView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private SurfaceHolder mSurfaceHolder = null;
    private int width, height, radius;
    private int hr;//半径一般
    private int cx, cy;
    private int startX;
    private int startY;
    private int defaultLength = 128;
    private int dataLength;
    private byte[] lastWave, nowWave, toWave, drawWave;
    private int[] lastFFT, nowFFT, toFFT, drawFFT;
    private int lineW;
    //private int jiange;
    private Paint fftPaint, wavePaint, cleanPaint, gradientPaint;
    private Object synObject = new Object();
    private int step = 1;

    public CircleVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dataLength = defaultLength;
        drawWave = new byte[dataLength];
        drawFFT = new int[dataLength/2+1];

        fftPaint = new Paint();
        fftPaint.setColor(Color.rgb(242, 130, 244));
        fftPaint.setStyle(Paint.Style.FILL);
        //fftPaint.setAntiAlias(true);

        wavePaint = new Paint();
        wavePaint.setColor(Color.rgb(55, 190, 252));
        wavePaint.setStyle(Paint.Style.FILL);
        //wavePaint.setAntiAlias(true);

        cleanPaint = new Paint();
        PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        cleanPaint.setXfermode(xfermode);

        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        if (!isInEditMode())
            setZOrderOnTop(true);//设置画布  背景透明
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpeMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpeSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSpeSize = MeasureSpec.getSize(widthMeasureSpec);
        if (heightSpeMode == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(widthSpeSize, widthSpeSize);
        } else if (heightSpeMode == MeasureSpec.AT_MOST && widthSpeSize < heightSpeSize) {
            setMeasuredDimension(widthSpeSize, widthSpeSize);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        radius = Math.min(w, h) / 2;
        hr = radius / 2;
        cx = w / 2;
        cy = h / 2;

        lineW = (int) (2 * Math.PI * hr / (2 * dataLength - 2)/2 );
        if (lineW < 1)
            lineW = 1;
        e = lineW;
        fftPaint.setStrokeWidth(lineW*2);
        wavePaint.setStrokeWidth(lineW);
    }

    private void setToFft(byte[] fft) {
        int l = fft.length/2+1;
        if (toFFT == null) {
            toFFT = new int[l];
            nowFFT = new int[l];
            drawFFT = new int[l];
            step = fft.length/dataLength;
        }
        toFFT[0] = Math.abs(fft[0]);
        toFFT[l-1] = Math.abs(fft[1]);
        for (int i = 2,j = 1; i < fft.length; i+=2,j++) {
            Log.d(this.getClass().getName(), "Fft："+fft[i]+","+fft[i+1]);
            toFFT[j] = (int) Math.hypot(fft[i],fft[i+1]);
        }
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
            step = waveform.length/dataLength;
        }
        for (int i = 0; i < waveform.length; i++) {
            toWave[i] = (byte) (waveform[i] + 128);
            Log.d(this.getClass().getName(), "wave："+waveform[i]+"->"+toWave[i]);
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
        synchronized (synObject) {
            setToFft(fft);
            setToWave(waveform);
            this.buZhenShu = fps * 1000 / rate;
            starDrawTime = System.currentTimeMillis();
            zhen = 1;
        }
    }

    private int lastIndex;
    private double du;
    private int bottomIndex;
    private double[] sinWave;
    private double[] cosWave;

    public void drawWaveAndFFT(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.translate(cx, cy);
        float b;
        float c;
        /*用三角函数计算坐标*/
        if (du == 0) {
            lastIndex = dataLength - 1;
            du = Math.PI / (lastIndex);
            bottomIndex = dataLength / 2;   //这个index开始线在下面部分
            sinWave = new double[bottomIndex];
            cosWave = new double[bottomIndex];
            double d = 0;
            for (int i = 1; i < bottomIndex; i++) {
                d += du;
                sinWave[i] = Math.sin(d);
                cosWave[i] = Math.cos(d);
            }
        }
        //中间线
        b = Math.abs(drawWave[0]);
        //setPaintColor(wavePaint, b);
        canvas.drawLine(0, -hr, 0, -calWaveLineLength(b), wavePaint);
        b = Math.abs(drawWave[lastIndex * step]);
        //setPaintColor(wavePaint, b);
        canvas.drawLine(0, hr, 0, calWaveLineLength(b), wavePaint);

        //FFT
        b = Math.abs(drawFFT[0]);
        c = calFftLineLength(b);
        canvas.drawLine(0, c, 0, c-e, fftPaint);
        b = Math.abs(drawFFT[drawFFT.length-1]);
        c = calFftLineLength(b);
        canvas.drawLine(0, -c, 0, -(c-e), fftPaint);

        float x, y, startX, startY;
        for (int i = 1; i < bottomIndex; i++) {
            b = Math.abs(drawWave[i * step]);
            c = calWaveLineLength(b);
            startX = (float) (sinWave[i] * hr);
            startY = (float) -(cosWave[i] * hr);
            x = (float) (sinWave[i] * c);
            y = (float) -(cosWave[i] * c);
            //setPaintColor(wavePaint, b);
            canvas.drawLine(startX, startY, x, y, wavePaint);
            canvas.drawLine(-startX, startY, -x, y, wavePaint);//垂直对称

            //水平对称，sin和cos可以重用
            b = Math.abs(drawWave[(lastIndex - i) * step]);
            c = calWaveLineLength(b);
            x = (float) (sinWave[i] * c);
            y = (float) (cosWave[i] * c);
            //setPaintColor(wavePaint, b);
            canvas.drawLine(startX, -startY, x, y, wavePaint);
            canvas.drawLine(-startX, -startY, -x, y, wavePaint);//垂直对称

           /* =====================================================
              ========================  FFT  ======================
              =====================================================*/
            if(i%2==0){
                int index = (i / 2) * step;
                c = calFftLineLength(drawFFT[index]);
                startX = (float) (sinWave[i] * c);
                startY = (float) (cosWave[i] * c);
                c = c-e;
                x = (float) (sinWave[i] * c);
                y = (float) (cosWave[i] * c);
                //setPaintColor(wavePaint, b);
                canvas.drawLine(startX, startY, x, y, fftPaint);
                canvas.drawLine(-startX, startY, -x, y, fftPaint);//垂直对称

                //水平对称，sin和cos可以重用
                index = drawFFT.length - 1 - index;
                c = calFftLineLength(drawFFT[index]);
                startX = (float) (sinWave[i] * c);
                startY = (float) -(cosWave[i] * c);
                c = c-e;
                x = (float) (sinWave[i] * c);
                y = (float) -(cosWave[i] * c);
                canvas.drawLine(startX, startY, x, y, fftPaint);
                canvas.drawLine(-startX, startY, -x, y, fftPaint);//垂直对称
            }
        }

        //Log.d("drawThread","drawWave[0] height-->" + Math.abs(drawWave[0]) * radius / 128);
        //canvas.drawCircle(0,0,hr, cleanPaint);
    }

    private void setPaintColor(Paint paint, int b) {
        paint.setColor(Color.rgb(255, 180 - b, 40));
    }

    private float e;//增长

    private float calWaveLineLength(float b) {
        return b * hr * 2 / (128.0f * 3) + hr + e;
    }

    private float calFftLineLength(float b) {
        //加长一半
        return hr - b * hr * 2 / (128.0f * 3) * 1.5f;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        /*timerRunFlag = true;
        new MyTimeThread().start();*/
        draw();
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

    public void stop() {
        runFlag = false;
    }

    public void start() {
        if (runFlag == false) {
            if (drawThread != null && drawThread.isAlive()) {
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

    private void calNowDataAndDraw() {
        boolean isDraw = true;
        zhen = (int) ((System.currentTimeMillis() - starDrawTime) / perFrameTime) + 1;
        zhen = Math.min(zhen, buZhenShu);
        if (lastZhen == zhen) {
            Log.d("drawThread","lastZhen-->" + lastZhen + ";zhen-->" + zhen);
            try {
                Thread.sleep(perFrameTime/4);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            return;
        }
        synchronized (synObject) {
            Log.d("drawThread", "zhen-->" + zhen + "/" + buZhenShu);
            if (zhen < buZhenShu) {
                float bl = (float) zhen / buZhenShu;
                if (toFFT != null) {
                    for (int i = 0; i < toFFT.length; i++) {
                        nowFFT[i] = (int) (lastFFT[i] + ((toFFT[i] - lastFFT[i]) * bl));
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
            } else {
                Log.d("drawThread", "帧数画完");
                isDraw = false;
            }
            lastZhen = zhen;
        }
        if (isDraw)
            draw();
    }

    @Override
    public void run() {
        while (runFlag) {
            if (toFFT != null || toWave != null) {
                calNowDataAndDraw();
            }
            /*try {
                Thread.sleep(perFrameTime/4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
        if (toFFT != null)
            drawFFT = new int[toFFT.length];
        if (toWave != null)
            drawWave = new byte[toWave.length];
        draw();
        System.out.println("drawThread stop");
    }
}