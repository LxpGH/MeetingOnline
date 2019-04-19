package com.design.lxp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.media.*;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.alex.livertmppushsdk.FdkAacEncode;
import com.alex.livertmppushsdk.RtmpSessionManager;
import com.alex.livertmppushsdk.SWVideoEncoder;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MediaInfo;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;


import java.io.*;
import java.text.Format;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

public class MeetingRoomActivity extends AppCompatActivity {
    private RelativeLayout cRelative,previewBox;
    private SurfaceView sfViewEx;
    private final int WIDTH_DEF=480;
    private final int HEIGHT_DEF=640;
    private final int FRAMERATE_DEF=20;
    private final int BITRATE_DEF=800*1000;
    private Camera mCamera;
    private Button SwCamerabutton,closeBtn;
    private SurfaceHolder.Callback sfh_Callback;
    private Camera.ShutterCallback shutterCallback;
    private Camera.PictureCallback pictureCallback;
    private  WindowManager windowManager,wm,wm1;
    private WindowManager.LayoutParams layoutParams,lp,lp1;
    private RelativeLayout.LayoutParams params,closeParams;
    public final static  int ID_RTMP_PUSH_START=100;
    private View  view;


    float srcX,srcY,devX,devY;
    private View.OnTouchListener sftListener=new View.OnTouchListener() {//to->   sfViewEx.setOnTouchListener(sftListener);
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action_event=motionEvent.getAction();
            float sumX=0;
            float sumY=0;
            switch (action_event){
                case MotionEvent.ACTION_DOWN:
                    srcX=motionEvent.getRawX();/**此处不能用GetX()和GetY()函数,他们获得的是相对于组件的坐标只有手指相对组件位置滑动时才能够拖动
                 **/
                    srcY=motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    devX=motionEvent.getRawX();
                    devY=motionEvent.getRawY();
                    sumX=devX-srcX;
                    sumY=devY-srcY;
                    srcX=devX;
                    srcY=devY;
                    Log.v("移动的距离"," "+sumX+" "+sumY);
                    if(Math.abs(sumX)>3||Math.abs(sumY)>3){
                        refreshView((int)(sumX),(int)(sumY));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return true;
        }
    };
    public void refreshView(int rx,int ry){ //刷新悬浮窗位置
        int screenBarHeight,screenBarWidth;
        DisplayMetrics dm=this.getResources().getDisplayMetrics();
        int screenWidth=dm.widthPixels;
        int screenHeight=dm.heightPixels;
        View rootView=view.getRootView();
        Rect r=new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        screenBarHeight=r.top;
        screenBarWidth=10;

        lp.x+=rx;
        lp.y+=ry;
        layoutParams.x+=rx;
        layoutParams.y+=ry;

        /**防止surface坐标超出窗口导致按纽位置出现异常,加减宽高一半,是因为lp.x和lp.y是以组件中心为基准的
         * 当然还要考虑到屏幕顶部栏的高度screenHeight**/
        if(lp.x>(screenWidth/2-lp.width/2-screenBarWidth)){lp.x=(screenWidth/2-lp.width/2-screenBarWidth);}
        if(lp.x<(-screenWidth/2+lp.width/2+screenBarWidth)){lp.x=(-screenWidth/2+lp.width/2+screenBarWidth);}
        if(lp.y>(screenHeight/2-lp.height/2-screenBarHeight)){lp.y=(screenHeight/2-lp.height/2-screenBarHeight);}
        if(lp.y<(-screenHeight/2+lp.height/2+screenBarHeight)){lp.y=(-screenHeight/2+lp.height/2+screenBarHeight);}
        /**防止surface坐标超出窗口导致按纽位置出现异常**/
        //String xy="("+lp.x+","+lp.y+")";
        //SwCamerabutton.setText(xy);
        lp1.x=lp.x+lp.width/2-lp1.height/2;//535
        lp1.y=lp.y-lp.height/2+lp1.height/4;//-580
        //Log.v("拖动后坐标"," "+lp.x+" "+lp.y);
        //windowManager.updateViewLayout(view,layoutParams);
        wm.updateViewLayout(sfViewEx,lp);
        wm1.updateViewLayout(closeBtn,lp1);
    }
    public void initSurfaceView(){
        sfViewEx=new SurfaceView(this);
        sfViewEx.setOnTouchListener(sftListener);
        params=new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.width=WIDTH_DEF;
        params.height=HEIGHT_DEF;
        params.addRule(RelativeLayout.ALIGN_TOP,(view.findViewById(R.id.preview_box).getId()));
        params.addRule(RelativeLayout.ALIGN_END,(view.findViewById(R.id.preview_box).getId()));
        sfViewEx.setLayoutParams(params);
        sfViewEx.getHolder().setFixedSize(HEIGHT_DEF,WIDTH_DEF);
        sfViewEx.setZ(0);/****通过调整显示层次来控制按纽显示在sufaceView的上层****/
        sfViewEx.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        sfViewEx.getHolder().setKeepScreenOn(true);
        sfViewEx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("===========111","我执行过了!");
            }
        });
        sfViewEx.getHolder().addCallback(sfh_Callback);
    }
    public void initCloseBtn(){
        closeBtn=new Button(this);
        closeParams=new RelativeLayout.LayoutParams(0,0);
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        closeBtn.setText("关闭");
        closeBtn.setTextColor(Color.argb(100,255,255,255));
        closeBtn.setBackgroundColor(Color.argb(0,0,0,0));
        closeBtn.setZ(1);/****通过调整显示层次来控制按纽显示在sufaceView的上层，否则会由于现实容器背景导致边缘黑色****/
        closeBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(v.getId()==closeBtn.getId()){
                 closeDialog();
                }
            }
        });
    }
    public void closeDialog(){
        wm1.removeView(closeBtn);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        wm.removeView(sfViewEx);
        //mCamera.release();如果还需继续使用,不能释放
        windowManager.removeView(view);
    }
    public void add_view(){
        windowManager=(WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE  );
        layoutParams=new WindowManager.LayoutParams();
        layoutParams.type=WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        layoutParams.token=MeetingRoomActivity.this.getWindow().getDecorView().getWindowToken();
        layoutParams.flags = FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE;
        layoutParams.width=1000;//大小直接设置可能导致分辨率不适配
        layoutParams.height=1000;
        layoutParams.x=300;
        layoutParams.y=-300;
        layoutParams.gravity=Gravity.CENTER_HORIZONTAL;
        windowManager.addView(view,layoutParams);
    }
    public void add_surface(){
        wm=(WindowManager) view.getContext().getSystemService(WINDOW_SERVICE) ;
        lp=new WindowManager.LayoutParams();
        lp.width=WIDTH_DEF;
        lp.height=HEIGHT_DEF;
        Log.v("width",lp.height+"");
        lp.x=0;
        lp.y=0;
        lp.gravity=Gravity.CENTER_HORIZONTAL;
        wm.addView(sfViewEx,lp);
    }
    public void add_closeBtn(){
        wm1=(WindowManager) sfViewEx.getContext().getSystemService(WINDOW_SERVICE) ;
        lp1=new WindowManager.LayoutParams();
        lp1.width=150;
        lp1.height=150;
        lp1.flags=FLAG_NOT_TOUCH_MODAL|FLAG_WATCH_OUTSIDE_TOUCH;
        //lp1.gravity=Gravity.END|Gravity.TOP;
        lp1.x=lp.x+lp.width/2-lp1.height/2;//535
        lp1.y=lp.y-lp.height/2+lp1.height/4;//-580
        wm1.addView(closeBtn,lp1);
    }
    public void initView(){
        cRelative=findViewById(R.id.cRelative);
        view=View.inflate(MeetingRoomActivity.this,R.layout.preview_window,null);
        sfh_Callback=new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.v("===========","我执行过了!");
                if (mCamera != null) {
                    InitCamera();
                    return;
                }
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);//开启摄像头
                InitCamera();
            }
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if(success){
                            InitCamera();
                            mCamera.cancelAutoFocus();
                        }
                    }
                });
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            }
        };
        SwCamerabutton=findViewById(R.id.SwCamerabutton);
        SwCamerabutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(v.getId()==SwCamerabutton.getId()){
                    initSurfaceView();
                    initCloseBtn();
                    add_view();
                    add_surface();//动态添加
                    add_closeBtn();
                    play();
                    /***只有开始预览时才获取视频流，这样才不会导致视频与音频脱节而导致只有声音，没有画面，play()获取到的流是其执行那一刻开始算起的***/

                    /******问题出在xml定义sufaceView时,已经创建，无法回调surfaceCreated()方法，导致无法打开摄像头*******/
                }
            }
        });


    }
    private Queue<byte[]> YUVQueue=new LinkedList<byte[]>();
    private Lock yuvQueueLock=new ReentrantLock();
    private Camera.PreviewCallback previewCallback=new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] YUV, Camera camera) {
            /***通过——bStartFlag判断是否获取数据***/
            /**开始获取数据，用byte[] yuv420 接收数据，通过YUV获取数据**/
            if(!_bStartFlag){
                return ;
            }
            byte[] yuv420=null;
            if(_iCameraCodecType==android.graphics.ImageFormat.YV12){
                Log.v("当前数据格式1",_iCameraCodecType+" ");
                yuv420=new byte[YUV.length];
                _swEncH264.swapYV12toI420_Ex(YUV,yuv420,HEIGHT_DEF,WIDTH_DEF);
            }else if(_iCameraCodecType==android.graphics.ImageFormat.NV21){
                Log.v("当前数据格式2",_iCameraCodecType+" ");
                yuv420=_swEncH264.swapNV21toI420(YUV,HEIGHT_DEF,WIDTH_DEF);
            }
            if(yuv420==null){
                return ;
            }
            if(!_bStartFlag){
                return ;
            }
            yuvQueueLock.lock();
            if(YUVQueue.size()>1){
                YUVQueue.clear();
            }
            YUVQueue.offer(yuv420);
            yuvQueueLock.unlock();
        }
    };
    public void InitCamera() {
        Camera.Parameters ps=mCamera.getParameters();
        /**获取摄像头支持的预览格式,并将推流的视频流设置为相应格式**/
        List<Integer> PreviewFormats=ps.getSupportedPreviewFormats();
        Integer mYV12Flag=0;
        Integer mNV21Flag=0;
        for(Integer yuvFormat:PreviewFormats){
            if(yuvFormat==android.graphics.ImageFormat.YV12){
                mYV12Flag=android.graphics.ImageFormat.YV12;
            }else if(yuvFormat==android.graphics.ImageFormat.NV21){
                mNV21Flag=android.graphics.ImageFormat.NV21;
            }
            if(mYV12Flag!=0){
                _iCameraCodecType=mYV12Flag;
            }else if(mNV21Flag!=0){
                _iCameraCodecType=mNV21Flag;
            }
        }
        ps.setPreviewSize(HEIGHT_DEF,WIDTH_DEF);
        ps.setPreviewFormat(_iCameraCodecType);
        ps.setPreviewFrameRate(FRAMERATE_DEF);

        mCamera.setDisplayOrientation(90);
        ps.setRotation(90);
        mCamera.setPreviewCallback(previewCallback);
        mCamera.setParameters(ps);
        try {
            mCamera.setPreviewDisplay(sfViewEx.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.cancelAutoFocus();
        mCamera.startPreview();
    }

    private  AudioRecord _AudioRecorder=null;
    private AcousticEchoCanceler mAec;
    private int _iRecorderBufferSize=0;
    private byte[] _RecorderBuffer=null;
    private FdkAacEncode _fdkaacEnc = null;
    private int _fdkaacHandle = 0;
    private final int SAMPLE_RATE_DEF = 22050;
    private final int CHANNEL_NUMBER_DEF = 2;
    public static boolean isDeviceSupport(){
        return AcousticEchoCanceler.isAvailable();
    }
    public int getAudioSession(){
        return _AudioRecorder.getAudioSessionId();
    }
    public boolean initAEC(int audioSession){
        Log.v("-----------","我支持回声消除"+" "+audioSession);
        if(mAec!=null){
            return false;
        }
        mAec=AcousticEchoCanceler.create(audioSession);
        mAec.setEnabled(true);
        return mAec.getEnabled();
    }
    private AudioTrack at;
    public void initAudioTrack(){
        at=new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                , _iRecorderBufferSize * 2
                , AudioTrack.MODE_STREAM, getAudioSession());
    }
    public void initAudioRecoder(){

        _iRecorderBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_DEF,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        _AudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_DEF, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, _iRecorderBufferSize);
        if(isDeviceSupport()){
            initAEC(getAudioSession());
            initAudioTrack();
        }
        //AudioRecord(采集来源(麦克风),采样率,采样声道,采样格式和每次采样大小,缓冲区大小)
        _RecorderBuffer = new byte[_iRecorderBufferSize];

        _fdkaacEnc = new FdkAacEncode();
        _fdkaacHandle = _fdkaacEnc.FdkAacInit(SAMPLE_RATE_DEF, CHANNEL_NUMBER_DEF);
        /**将音频使用aac编码**/
    }
    private Handler mHandler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            Bundle b=msg.getData();
            int ret;
            switch(msg.what){
                case ID_RTMP_PUSH_START:
                    Start();
                    break;
            }
        }
    };

    private String _rtmpUrl;
    private RtmpSessionManager rsMgr;
    private int _iCameraCodecType = android.graphics.ImageFormat.NV21;
    private SWVideoEncoder _swEncH264=null;
    private boolean _bStartFlag = false;

    private Thread _h264EncoderThread = null;
    private byte[] yuvEditData= new byte[WIDTH_DEF * HEIGHT_DEF * 3 / 2];
    private Runnable _h264Runnable=new Runnable() {
        @Override
        public void run() {
            while(!_h264EncoderThread.isInterrupted()&&_bStartFlag){
                int iSize=YUVQueue.size();
                if(iSize>0){
                    yuvQueueLock.lock();//设置锁，保证摄像头预览函数过早清空YUVQueue
                    byte[] yuvData=YUVQueue.poll();
                    yuvQueueLock.unlock();
                    if(yuvData==null){
                        continue;
                    }
                    yuvEditData=_swEncH264.YUV420pRotate270(yuvData,HEIGHT_DEF,WIDTH_DEF);
                    byte[] _h264Data=_swEncH264.EncoderH264(yuvEditData);
                    if(_h264Data!=null) {
                        rsMgr.InsertVideoData(_h264Data);
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            YUVQueue.clear();//不能写进while层
        }
    };
    private Thread _AacEncoderThread=null;
    private Runnable _AacEncoderRunnable =new Runnable() {
        @Override
        public void run() {
            long sleepTime=SAMPLE_RATE_DEF*16*2/_RecorderBuffer.length;
            Log.v("音频推流间隔：",""+sleepTime);
            while(!_AacEncoderThread.interrupted()&&_bStartFlag){
                //读取音频设备数据并获取其长度，等同于Camera的PreviewCallBack
                int iPcmLen=_AudioRecorder.read(_RecorderBuffer,0,_RecorderBuffer.length);
                if(((iPcmLen!=_AudioRecorder.ERROR_BAD_VALUE)&&(iPcmLen!=0))){
                    if(_fdkaacHandle!=0){
                        byte[] aacBuffer=_fdkaacEnc.FdkAacEncode(_fdkaacHandle,_RecorderBuffer);
                        if(aacBuffer!=null){
                            long iLen=aacBuffer.length;
                            rsMgr.InsertAudioData(aacBuffer);
                        }
                    }
                }else{
                    Log.v("------------","######fail to get PCM data！");
                }
                try{
                    Thread.sleep(sleepTime/10);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            Log.v("----------","AAC Encoder Thread ended ......");
        }
    };

    public void Start(){
        rsMgr=new RtmpSessionManager();
        _rtmpUrl="rtmp://ossrs.net/live/12345678";
        rsMgr.Start(_rtmpUrl);

        int iFormat=_iCameraCodecType;
        _swEncH264=new SWVideoEncoder(WIDTH_DEF,HEIGHT_DEF,FRAMERATE_DEF,BITRATE_DEF);
        _swEncH264.start(iFormat);

        _bStartFlag=true;
        _h264EncoderThread=new Thread(_h264Runnable);
        _h264EncoderThread.setPriority(Thread.MAX_PRIORITY);
        _h264EncoderThread.start();

        _AudioRecorder.startRecording();
        _AacEncoderThread=new Thread(_AacEncoderRunnable);
        _AacEncoderThread.setPriority(Thread.MAX_PRIORITY);
        _AacEncoderThread.start();
    }
    public void Stop(){
        _bStartFlag=false;

        _AacEncoderThread.interrupt();
        _h264EncoderThread.interrupt();

        _AudioRecorder.stop();
        _swEncH264.stop();

        rsMgr.Stop();

        yuvQueueLock.lock();
        YUVQueue.clear();
        yuvQueueLock.unlock();
    }
    public void RtmpStartMessage(){
        android.os.Message msg=new android.os.Message();
        msg.what=ID_RTMP_PUSH_START;
        Bundle b=new Bundle();
        b.putInt("ret",0);
        msg.setData(b);
        mHandler.sendMessage(msg);
    }
    public void initAll(){
        initView();
        initAudioRecoder();
        Intent camera_intent=new Intent();
        //_rtmpUrl=camera_intent.getStringExtra(StartActivity.RTMPURL_MESSAGE);
        RtmpStartMessage();
    }
    /**********WEbRTC*************/

    /**********WEbRTC*************/
    /**********播放端拉流*************/
    private SurfaceView video;
    private Button full,volum3;

    private IMediaPlayer iMediaPlayer=null;

    private String mVideoUrl=null;

    private VideoPlayerListener listener;

    public abstract class VideoPlayerListener implements IMediaPlayer.OnBufferingUpdateListener,
            IMediaPlayer.OnCompletionListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnInfoListener,
            IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnErrorListener,
            IMediaPlayer.OnSeekCompleteListener {
    }

    public void setVideoUrl(String url){
        if(TextUtils.equals(null,mVideoUrl)){
            mVideoUrl=url;
            createVideoContent();
        }else{
            mVideoUrl=url;
            load();
        }
    }

    private void createVideoContent(){
        video=new SurfaceView(this);
        RelativeLayout.LayoutParams lp_video=new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,640);
        video.setLayoutParams(lp_video);
        video.setZ(0);
        video.setAlpha(0);
        video.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        video.getHolder().setKeepScreenOn(true);
        video.getHolder().setFixedSize(WIDTH_DEF,HEIGHT_DEF);
        video.getHolder().setFormat(PixelFormat.OPAQUE);
        video.getHolder().addCallback(videoCallBack);
        this.addContentView(video,lp_video);
    }
    public void load(){
        createPlayer();
        try {
            iMediaPlayer.setDataSource(mVideoUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        video.measure(WIDTH_DEF,HEIGHT_DEF);
//    Log.v("获取到的视频分辨率",iMediaPlayer.getVideoWidth()+","+iMediaPlayer.getVideoHeight());
        iMediaPlayer.setDisplay(video.getHolder());
        iMediaPlayer.prepareAsync();
    }

    public void createPlayer(){
        if(iMediaPlayer!=null){
            //清空脏数据
            iMediaPlayer.stop();
            iMediaPlayer.setDisplay(null);
            iMediaPlayer.release();
        }
        IjkMediaPlayer ijkMediaPlayer=new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        /**otherMethod**/
        //YYH delete start
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 48);
        //YYH delete end

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

//                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 8);
        //YYH add
        ijkMediaPlayer.setOption(1, "analyzemaxduration", 100L);
        ijkMediaPlayer.setOption(1, "probesize", 10240L);
        ijkMediaPlayer.setOption(1, "flush_packets", 1L);
        ijkMediaPlayer.setOption(4, "packet-buffering", 0L);
        ijkMediaPlayer.setOption(4, "framedrop", 1L);
        //YYH end
        /**otherMethod**/
        //开启硬解码
        //ijkMediaPlayer.setOption(ijkMediaPlayer.OPT_CATEGORY_PLAYER,"mediacodec",1);

        /**播放前的最大探测时间**/
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"analyzemaxduration",100L);
        /**播放前的探测size**/
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"probesize",1024L);
        /**每处理一个packet之后刷新io上下文**/
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"flush_packets",1L);
        /**设置直播开启预缓冲,这里关闭防止播放丢帧卡顿**/
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"packet-buffering",1L);
        /**跳帧处理，当CPU处理较慢时，进行跳帧处理，保证播放流程，画面和声音同步**/
        //ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"framedrop",5);
        iMediaPlayer=ijkMediaPlayer;
        if(listener!=null){
            iMediaPlayer.setOnPreparedListener(listener);
            iMediaPlayer.setOnInfoListener(listener);
            iMediaPlayer.setOnSeekCompleteListener(listener);
            iMediaPlayer.setOnBufferingUpdateListener(listener);
            iMediaPlayer.setOnErrorListener(listener);
        }
    }

    public void setListener(VideoPlayerListener listener){
        this.listener=listener;
        if(iMediaPlayer!=null){
            iMediaPlayer.setOnPreparedListener(listener);
        }
    }
    /**
     *  下面封装了一下控制视频的方法
     */

    public void start() {
        if (iMediaPlayer != null) {
            iMediaPlayer.start();
        }
    }

    public void release() {
        if (iMediaPlayer != null) {
            iMediaPlayer.reset();
            iMediaPlayer.release();
            iMediaPlayer = null;
        }
    }

    public void pause() {
        if (iMediaPlayer != null) {
            iMediaPlayer.pause();
        }
    }

    public void stop() {
        if (iMediaPlayer != null) {
            iMediaPlayer.stop();
        }
    }


    public void reset() {
        if (iMediaPlayer != null) {
            iMediaPlayer.reset();
        }
    }


    public long getDuration() {
        if (iMediaPlayer != null) {
            return iMediaPlayer.getDuration();
        } else {
            return 0;
        }
    }


    public long getCurrentPosition() {
        if (iMediaPlayer != null) {
            return iMediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }


    public void seekTo(long l) {
        if (iMediaPlayer != null) {
            iMediaPlayer.seekTo(l);
        }
    }
    private SurfaceHolder.Callback videoCallBack=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            load();
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    public void play(){
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        }catch(Exception e){
            this.finish();
        }
        VideoPlayerListener video_listener=new VideoPlayerListener() {
            @Override
            public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
                Log.v("-------------","我使用的sessionID:"+iMediaPlayer.getAudioSessionId());
            }

            @Override
            public void onCompletion(IMediaPlayer iMediaPlayer) {

            }

            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                return false;
            }

            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                return false;
            }

            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                iMediaPlayer.start();
            }

            @Override
            public void onSeekComplete(IMediaPlayer iMediaPlayer) {

            }

            @Override
            public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int i, int i1, int i2, int i3) {

            }
        };
        String v_url="rtmp://ossrs.net/live/12345678";
        this.setListener(video_listener);
        this.setVideoUrl(v_url);
    }
    /**********播放端拉流*************/

    /**Home键监听：防止用户点击底部菜单键将程序推到后台，系统回收Camera资源，
     * 而不会下回添加的surfaceView导致再次返回程序时无法正常使用程序的情况**/
    public class HomeWatcherReceiver extends BroadcastReceiver {
        private static final String LOG_TAG="HomeReceiver";
        private static final String SYSTEM_DIALOG_REASON_KEY="reason";
        //长按Home键或切换键
        private static final String SYSTEM_DIALOG_REASON_RECENT_APPS="recentapps";
        //短按Home键
        private static final String SYSTEM_DIALOG_RESON_HOME_KEY="homekey";
        //锁频
        private static final String SYSTEM_DIALOG_REASON_LOCK="lock";
        //samsung长按Home键
        private static final String SYSTEM_DIALOG_REASON_ASSIST="assist";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            Log.v(LOG_TAG," "+action);
            if(action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)){
                String reason=intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                Log.v(LOG_TAG," "+reason);
                if(SYSTEM_DIALOG_RESON_HOME_KEY.equals(reason)){
                    // 短按Home键
                    closeDialog();
                    return;
                }
                else if(SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)){
                    // 长按Home键 或者 activity切换键
                    closeDialog();
                    return ;
                }
                else if(SYSTEM_DIALOG_REASON_LOCK.equals(reason)){
                    // 锁屏
                    closeDialog();
                    return ;
                }
                else if(SYSTEM_DIALOG_REASON_ASSIST.equals(reason)){
                    // samsung 长按Home键
                    closeDialog();
                    return ;
                }
            }
        }
    }

    private static HomeWatcherReceiver hwr=null;
    private void registerHomeKeyReceiver(Context context){
        hwr=new HomeWatcherReceiver();
        final IntentFilter homeFilter=new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.registerReceiver(hwr,homeFilter);
    }
    private void unregisterHomeKeyReceiver(Context context){
        if(null!=hwr){
            context.unregisterReceiver(hwr);
        }
    }
    /**Home键监听**/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getPermission();
        registerHomeKeyReceiver(this);
        initAll();

    }
    @Override
    protected void onResume() {
        super.onResume();
        registerHomeKeyReceiver(this);
    }
    @Override
    protected  void onPause(){
        super.onPause();
        unregisterHomeKeyReceiver(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterHomeKeyReceiver(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_MENU){
        closeDialog();
        return false;
    }else{
        return super.onKeyDown(keyCode,event);
    }
    }

    public void getPermission(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            //判断是否申请过
            int REQUEST_CODE_CONTACT=101;
            String[] permissions={Manifest.permission.CAMERA,Manifest.permission.SYSTEM_ALERT_WINDOW,Manifest.permission.RECORD_AUDIO
                    ,Manifest.permission.MODIFY_AUDIO_SETTINGS};
            for(String str:permissions){
                if(this.checkSelfPermission(str)!=PackageManager.PERMISSION_GRANTED){
                    this.requestPermissions(permissions,REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }
    }
}

