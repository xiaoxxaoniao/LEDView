package com.zql.android.led;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;

import java.util.concurrent.Executors;

/**
 * Created by scott on 2017/6/14.
 */

public class LEDView extends SurfaceView implements SurfaceHolder.Callback,Runnable{

    private SurfaceHolder mHolder;

    private Handler mHandler;

    private HandlerThread mThread;

    private String mLEDStr ="";

    private int mLEDHight = 0, mLEDWidth = 0;

    private final int kFPS = 1000/20;

    private TextPaint mTextPaint ;

    private int kTextSize = 30;

    private int kRawBitmapHight = 40;

    private int mLEDColor = Color.parseColor("#EC3A36");

    private int kTextBaseLine = kRawBitmapHight/2 + kTextSize/2 - kTextSize/10;

    private LEDData mLEDData;

    private long mT1,mT2,mTD;

    private boolean mIsDestory = false;

    private final String kSpace = "";
    public LEDView(Context context) {
        this(context,null);
    }

    public LEDView(Context context, AttributeSet attrs) {
        this(context,attrs,0);
    }

    public LEDView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public void setLED(String str ,@ColorInt int ledColor, int ledTextSize,int ledPixel){
        if(str == null || str.trim().length() == 0){
            mLEDStr = kSpace + "Hello LED \uD83D\uDE00";
        }else {
            mLEDStr = kSpace + str;
        }
        mLEDColor = ledColor;
        if(ledTextSize > 0){
            kTextSize = ledTextSize;
        }
        if(ledPixel>0){
            kRawBitmapHight = ledPixel;
        }
        mLEDData = null;
        makeRawBitmap();
    }

    public void setLEDContent(String content){
        if(content == null || content.trim().length() == 0){
            mLEDStr = kSpace + "Hello LED \uD83D\uDE00";
        }else {
            mLEDStr = kSpace+content;
        }
        mLEDData = null;
        makeRawBitmap();
    }

    public void setLEDTextColor(int color){
        mLEDColor = color;
        mLEDData = null;
        makeRawBitmap();
    }

    public void setLEDSize(int size){
        kRawBitmapHight = size;
        mLEDData = null;
        makeRawBitmap();
    }

    public void setLEDTextSize(int size){
        kTextSize = size;
        mLEDData = null;
        makeRawBitmap();
    }
    public int getLEDColor(){
        return mLEDColor;
    }
    private void init(){

        mHolder = getHolder();
        mHolder.addCallback(this);
        mThread = new HandlerThread("led-looper");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);


        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mLEDHight = getWidth();
                mLEDWidth = getHeight();
                Log.d("scott","   led size " + " h : " + mLEDHight + "   w : " + mLEDWidth);
                getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d("scott"," surface creates");
        mIsDestory = false;
        if(mLEDData == null){
            mHandler.postDelayed(this,kFPS);
            setLED(mLEDStr,mLEDColor,kTextSize,kRawBitmapHight);
        }else {
            mHandler.postDelayed(this,kFPS);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d("scott"," surface Changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d("scott"," surface destroy");
        mIsDestory = true;
    }

    private void makeRawBitmap(){
        if(mLEDStr == null || mLEDStr.trim().length() == 0) return;
        if(mLEDData != null) return;

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (LEDView.this){
                    mLEDData = null;
                }
                mTextPaint.setColor(mLEDColor);
                mTextPaint.setTextSize(kTextSize);
                kTextBaseLine = kRawBitmapHight/2 + kTextSize/2 - kTextSize/10;
                float textLen = mTextPaint.measureText(mLEDStr);
                Bitmap bitmap = Bitmap.createBitmap((int)textLen,kRawBitmapHight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawText(mLEDStr,0,kTextBaseLine,mTextPaint);
                Canvas LEDCanvas = mHolder.lockCanvas();
                if(LEDCanvas != null){
                    try {
                        LEDFrame ledFrame = new LEDFrame(bitmap.getWidth(),bitmap.getHeight());
                        initLEDFrame(ledFrame,bitmap);
                        bitmap.recycle();
                        int scale = mLEDWidth/kRawBitmapHight;
                        int screenW = mLEDHight/scale;
                        Log.d("scott","  screen w = " + screenW);
                        LEDData ledData = new LEDData();
                        ledData.frame = ledFrame;
                        ledData.pixelH = kRawBitmapHight;
                        ledData.pixelW = (int)textLen;
                        ledData.scale = scale;
                        ledData.ledScreenH = kRawBitmapHight;
                        ledData.LedScreenW = screenW;
                        synchronized (LEDView.this){
                            mLEDData = ledData;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        mHolder.unlockCanvasAndPost(LEDCanvas);
                    }
                }

            }
        });
    }

    private void initLEDFrame(LEDFrame ledFrame,Bitmap bitmap){
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Log.d("scott","bitmap w : " + w + "   bitmap h = " + h);
        for(int x = 0;x<w;x++){
            for(int y = 0;y<h;y++){
                ledFrame.fillData(y,x,bitmap.getPixel(x,y));
            }
        }
    }

    @Override
    public void run() {

        mT1 = System.currentTimeMillis();
        if(mIsDestory) return;
        if(mHolder == null) return;
        Canvas LEDCanvas = mHolder.lockCanvas();
        if(LEDCanvas != null){
            try {
                if(mLEDData != null){
                    LEDCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    int fixH = mLEDData.getFixLEDHeight();
                    int fixW = mLEDData.getFixLEDWidth();

                    for(int i = 0;i<fixH;i+=2){
                        for(int j = 0;j<fixW;j+=2){
                            int pX = j*mLEDData.scale;
                            int pY = i*mLEDData.scale;
                            if(mLEDData.getPixelColor(i,j) != 0){
                                mTextPaint.setColor(mLEDData.getPixelColor(i,j));
                                LEDCanvas.drawCircle(pX,pY,mLEDData.scale/2,mTextPaint);
                            }
                        }
                    }
                    mLEDData.nextFrame();
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                mHolder.unlockCanvasAndPost(LEDCanvas);
            }
        }
        mT2 = System.currentTimeMillis();
        mTD = mT2 - mT1;
        if(mTD < kFPS){
            mHandler.postDelayed(this,kFPS-mTD);
        }else {
            mHandler.post(this);
        }

    }

    private class LEDFrame{
        int[][] frameData;

        LEDFrame(int w,int h){
            frameData = new int[h][w];
        }

        void fillData(int r,int c,int value){
            frameData[r][c] = value;
        }

        void dump(){
            StringBuilder stringBuilder = new StringBuilder();
            for(int i = 0;i<frameData.length;i++){
                for(int j = 0;j<frameData[i].length;j++){
                    stringBuilder.append(frameData[i][j] + " ");
                }
                stringBuilder.append("\n");
            }
            Log.d("scott",stringBuilder.toString());
        }

        int[][] getData(){
            return frameData;
        }
    }

    private class LEDData{
        LEDFrame frame;
        int scale;
        int pixelW;
        int pixelH;
        int ledScreenH;
        int LedScreenW;

        int delta = 0;
        void nextFrame(){
            if(LedScreenW >= pixelW) return;
            delta ++ ;
        }

        int getFixLEDWidth(){
            return pixelW<=LedScreenW?pixelW:LedScreenW;
        }

        int getFixLEDHeight(){
            return pixelH;
        }

        int getPixelColor(int i,int j){
            if(j+delta< pixelW){
                return frame.frameData[i][j + delta];
            }else {
                return frame.frameData[i][(j + delta)%pixelW];
            }
        }
    }
}
