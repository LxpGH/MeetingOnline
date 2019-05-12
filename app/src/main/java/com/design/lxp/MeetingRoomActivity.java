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
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.alex.livertmppushsdk.FdkAacEncode;
import com.alex.livertmppushsdk.RtmpSessionManager;
import com.alex.livertmppushsdk.SWVideoEncoder;

import com.design.lxp.MyUtils.MUtils;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
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
    private boolean isCloseBtnAdd=false;
    private Camera mCamera;
    private Button closeBtn;
    private ImageView camera_switch,micPhone_switch,start_switch,end_switch;
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
        isCloseBtnAdd=false;/**需要设置成false,以保证再次添加closeBtn时状态对应**/
        //Stop();
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        //包括停止图像音频数据处理推流线程,关闭音频采集设备
        //_AudioRecorder.stop();
        //Stop();
        wm.removeView(sfViewEx);
        //mCamera.release();如果还需继续使用,不能释放
        windowManager.removeView(view);
        /**关闭窗口时,在这里释放iMediaPlayer资源，正常应该在点击退出时关闭**/
        this.release();
        this.release1();
        camera_switch.setImageResource(R.drawable.ic_camera);
        camera_switch.setEnabled(true);
        end_switch.setImageResource(R.drawable.exit_grey);
        end_switch.setEnabled(false);
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
        lp.flags=FLAG_NOT_TOUCH_MODAL|FLAG_WATCH_OUTSIDE_TOUCH;
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
    private Boolean isFirst=true;
    private Runnable urlUpdate=new Runnable() {
        @Override
        public void run() {
            _rtmpUrl=v1_url;
            initAudioRecoder();
            RtmpStartMessage();
            initSurfaceView();
            initCloseBtn();
            add_view();
            add_surface();//动态添加
            add_closeBtn();
            //play();
        }
    };
    private final Handler urlHandler=new Handler();
    public void initView(){
        cRelative=findViewById(R.id.cRelative);
        view=View.inflate(MeetingRoomActivity.this,R.layout.preview_window,null);
        operators_box=findViewById(R.id.operators_box);
        operators_box.setScrimColor(Color.TRANSPARENT);
        operators_box.openDrawer(Gravity.START);
        /**初始化工具栏**/
        initOperator();

        /**四个按纽的监听器设置**/
        camera_switch=findViewById(R.id.camera_switch);
        end_switch=findViewById(R.id.end_switch);
        camera_switch.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(v.getId()==camera_switch.getId()){
                    if(isFirst){
                        String meet_id="000001";
                        getVideoURL(meet_id);
                        Log.v("推流地址",""+_rtmpUrl);
                        urlHandler.postDelayed(urlUpdate,500);
                        isFirst=false;
                    }else{
                        initSurfaceView();
                        initCloseBtn();
                        add_view();
                        add_surface();//动态添加
                        add_closeBtn();
                        play();
                    }
                    //必须在摄像机初始化前开始推流,因为如果相机先初始化,会先给队列加锁而无法释放出来继续执行

                    isCloseBtnAdd=true;
                    //so库加载和监听器事件加载
                    /***只有开始预览时才获取视频流，这样才不会导致视频与音频脱节而导致只有声音，没有画面，play()获取到的流是其执行那一刻开始算起的***/
                    /******问题出在xml定义sufaceView时,已经创建,无法回调surfaceCreated()方法，导致无法打开摄像头*******/
                    camera_switch.setImageResource(R.drawable.ic_camera_open);
                    camera_switch.setEnabled(false);
                    end_switch.setImageResource(R.drawable.exit_red);
                    end_switch.setEnabled(true);
                }
            }
        });
        end_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.getId()==end_switch.getId()){
                    closeDialog();
                    end_switch.setImageResource(R.drawable.exit_grey);
                    end_switch.setEnabled(false);
                }
            }
        });
        /**四个按纽的监听器设置**/
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
            if(_iCameraCodecType==ImageFormat.YV12){
                Log.v("当前数据格式1",_iCameraCodecType+" ");
                yuv420=new byte[YUV.length];
                _swEncH264.swapYV12toI420_Ex(YUV,yuv420,HEIGHT_DEF,WIDTH_DEF);
            }else if(_iCameraCodecType==ImageFormat.NV21){
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
            if(yuvFormat==ImageFormat.YV12){
                mYV12Flag=ImageFormat.YV12;
            }else if(yuvFormat==ImageFormat.NV21){
                mNV21Flag=ImageFormat.NV21;
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
        Log.v("InitCamera","InitCamera.....");
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
        /**if(isDeviceSupport()){
            initAEC(getAudioSession());
            initAudioTrack();
        }**/
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
    private int _iCameraCodecType = ImageFormat.NV21;
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
                    yuvQueueLock.lock();//设置锁,保证摄像头预览函数过早清空YUVQueue
                    byte[] yuvData=YUVQueue.poll();
                    yuvQueueLock.unlock();
                    if(yuvData==null){
                        continue;
                    }
                    yuvEditData=_swEncH264.YUV420pRotate270(yuvData,HEIGHT_DEF,WIDTH_DEF);
                    /**这句话处理后为空值**/
                    byte[] _h264Data=_swEncH264.EncoderH264(yuvEditData);
                    Log.v("数据插入成功!",""+yuvEditData);
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
       //_rtmpUrl="rtmp://ossrs.net/live/12345678";
        rsMgr.Start(_rtmpUrl);
        Log.v("Start:","Start........");
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
    public void ReStart(){
        _bStartFlag=true;
        int iFormat=_iCameraCodecType;
        _swEncH264.start(iFormat);
        _h264EncoderThread.start();
        _AacEncoderThread.start();

        _AudioRecorder.startRecording();

        //rsMgr.Start(_rtmpUrl);
        YUVQueue=new LinkedList<byte[]>();
    }
    public void RtmpStartMessage(){
        Message msg=new Message();
        msg.what=ID_RTMP_PUSH_START;
        Bundle b=new Bundle();
        b.putInt("ret",0);
        msg.setData(b);
        mHandler.sendMessage(msg);
    }
    //private Boolean isClose=false;
    public void initAll(){
        initView();
        /**进入会议室，预先加载服务器地址和播放器，等待推流服务器数据**/
        getPlayUrl("000001");
        /***需要延迟一定时间等待服务器分发推流地址***/
        playHandler.postDelayed(playUpdate,1000);
        //initAudioRecoder();
        //RtmpStartMessage();
        //Intent camera_intent=new Intent();
        //_rtmpUrl=camera_intent.getStringExtra(StartActivity.RTMPURL_MESSAGE);

        //isClose=!isClose;
    }
    /**********WEbRTC*************/

    /**********WEbRTC*************/
    /**********播放端拉流*************/
    private SurfaceView video,video1,video2,video3,video4;
    private Button full,volum3;

    private IMediaPlayer iMediaPlayer=null;
    private IMediaPlayer iMediaPlayer1=null;
    private IMediaPlayer iMediaPlayer2=null;
    private IMediaPlayer iMediaPlayer3=null;
    private IMediaPlayer iMediaPlayer4=null;

    private String mVideoUrl=null;
    private String mVideoUrl1=null;
    private String mVideoUrl2=null;
    private String mVideoUrl3=null;
    private String mVideoUrl4=null;

    private VideoPlayerListener listener,listener1,listener2,listener3,listener4;

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

    public void setVideo1Url(String url1){
        if(TextUtils.equals(null,mVideoUrl1)){
            mVideoUrl1=url1;
            createVideo1Content();
        }else{
            mVideoUrl1=url1;
            load1();
        }
    }

    public void setVideo2Url(String url2){
        if(TextUtils.equals(null,mVideoUrl2)){
            mVideoUrl2=url2;
            createVideo2Content();
        }else{
            mVideoUrl2=url2;
            load2();
        }
    }

    public void setVideo3Url(String url3){
        if(TextUtils.equals(null,mVideoUrl3)){
            mVideoUrl3=url3;
            createVideo3Content();
        }else{
            mVideoUrl3=url3;
            load3();
        }
    }

    public void setVideo4Url(String url4){
        if(TextUtils.equals(null,mVideoUrl4)){
            mVideoUrl4=url4;
            createVideo4Content();
        }else{
            mVideoUrl4=url4;
            load4();
        }
    }

    private void createVideoContent(){
        video=new SurfaceView(this);
        RelativeLayout s=findViewById(R.id.main_video);
        RelativeLayout.LayoutParams lp_video=new RelativeLayout.LayoutParams(
                s.getWidth(),(int)(s.getWidth()*((float)HEIGHT_DEF/(float)WIDTH_DEF)));
        lp_video.addRule(RelativeLayout.ALIGN_START,R.id.main_video);
        lp_video.addRule(RelativeLayout.ALIGN_TOP,R.id.main_video);
        video.setLayoutParams(lp_video);
        video.setZ(0);
        int[] location=new int[2];
        s.getLocationInWindow(location);
        video.setX(location[0]);
        video.setY(location[1]);
        //video.setAlpha(0);/强行设置透明度会导致组件无法显示/
        video.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        video.getHolder().setKeepScreenOn(true);
        video.getHolder().setFormat(PixelFormat.OPAQUE);
        video.getHolder().setFixedSize(WIDTH_DEF,HEIGHT_DEF);
        video.getHolder().addCallback(videoCallBack);
        this.addContentView(video,lp_video);
    }
    private void createVideo1Content(){
        video1=new SurfaceView(this);
        RelativeLayout s=findViewById(R.id.left_video1_box);
        TextView t=findViewById(R.id.left_video1_title);
        MUtils mUtils=new MUtils();
        int lp_height=s.getHeight()-t.getHeight()-mUtils.dip2px(this,5);
        int lp_width=(int)(lp_height*((float)WIDTH_DEF/(float)HEIGHT_DEF));
        Log.v("-------------------------------------",""+lp_height+" "+lp_width);
        RelativeLayout.LayoutParams lp_video=new RelativeLayout.LayoutParams(
                lp_width,lp_height);
        lp_video.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp_video.addRule(RelativeLayout.ALIGN_TOP,R.id.video1);
        video1.setLayoutParams(lp_video);
        video1.setZ(0);
        int[] location=new int[2];
        s.getLocationInWindow(location);
        /**水平居中**/
        int x=location[0]+(int)(s.getWidth()*0.5-lp_width*0.5);
        video1.setX(x);
        video1.setY(location[1]);
        video1.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        video1.getHolder().setKeepScreenOn(true);
        video1.getHolder().setFixedSize(WIDTH_DEF,HEIGHT_DEF);
        video1.getHolder().setFormat(PixelFormat.OPAQUE);
        video1.getHolder().addCallback(videoCallBack1);
        this.addContentView(video1,lp_video);
    }

    private void createVideo2Content(){
        video2=new SurfaceView(this);
        RelativeLayout s=findViewById(R.id.left_video2_box);
        TextView t=findViewById(R.id.left_video2_title);
        MUtils mUtils=new MUtils();
        int lp_height=s.getHeight()-t.getHeight()-mUtils.dip2px(this,5);
        int lp_width=(int)(lp_height*((float)WIDTH_DEF/(float)HEIGHT_DEF));
        Log.v("-------------------------------------",""+lp_height+" "+lp_width);
        RelativeLayout.LayoutParams lp_video=new RelativeLayout.LayoutParams(
                lp_width,lp_height);
        lp_video.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp_video.addRule(RelativeLayout.ALIGN_TOP,R.id.video2);
        video2.setLayoutParams(lp_video);
        video2.setZ(0);
        int[] location=new int[2];
        s.getLocationInWindow(location);
        /**水平居中**/
        int x=location[0]+(int)(s.getWidth()*0.5-lp_width*0.5);
        video2.setX(x);
        video2.setY(location[1]);
        video2.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        video2.getHolder().setKeepScreenOn(true);
        video2.getHolder().setFixedSize(WIDTH_DEF,HEIGHT_DEF);
        video2.getHolder().setFormat(PixelFormat.OPAQUE);
        video2.getHolder().addCallback(videoCallBack2);
        this.addContentView(video2,lp_video);
    }


    private void createVideo3Content(){
        video3=new SurfaceView(this);
        RelativeLayout s=findViewById(R.id.right_video1_box);
        TextView t=findViewById(R.id.right_video1_title);
        MUtils mUtils=new MUtils();
        int lp_height=s.getHeight()-t.getHeight()-mUtils.dip2px(this,5);
        int lp_width=(int)(lp_height*((float)WIDTH_DEF/(float)HEIGHT_DEF));
        Log.v("-------------------------------------",""+lp_height+" "+lp_width);
        RelativeLayout.LayoutParams lp_video=new RelativeLayout.LayoutParams(
                lp_width,lp_height);
        lp_video.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp_video.addRule(RelativeLayout.ALIGN_TOP,R.id.video3);
        video3.setLayoutParams(lp_video);
        video3.setZ(0);
        int[] location=new int[2];
        s.getLocationInWindow(location);
        /**水平居中**/
        int x=location[0]+(int)(s.getWidth()*0.5-lp_width*0.5);
        video3.setX(x);
        video3.setY(location[1]);
        video3.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        video3.getHolder().setKeepScreenOn(true);
        video3.getHolder().setFixedSize(WIDTH_DEF,HEIGHT_DEF);
        video3.getHolder().setFormat(PixelFormat.OPAQUE);
        video3.getHolder().addCallback(videoCallBack3);
        this.addContentView(video3,lp_video);
    }

    private void createVideo4Content(){
        video4=new SurfaceView(this);
        RelativeLayout s=findViewById(R.id.right_video2_box);
        TextView t=findViewById(R.id.right_video2_title);
        MUtils mUtils=new MUtils();
        int lp_height=s.getHeight()-t.getHeight()-mUtils.dip2px(this,5);
        int lp_width=(int)(lp_height*((float)WIDTH_DEF/(float)HEIGHT_DEF));
        Log.v("-------------------------------------",""+lp_height+" "+lp_width);
        RelativeLayout.LayoutParams lp_video=new RelativeLayout.LayoutParams(
                lp_width,lp_height);
        lp_video.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp_video.addRule(RelativeLayout.ALIGN_TOP,R.id.video4);
        video4.setLayoutParams(lp_video);
        video4.setZ(0);
        int[] location=new int[2];
        s.getLocationInWindow(location);
        /**水平居中**/
        int x=location[0]+(int)(s.getWidth()*0.5-lp_width*0.5);
        video4.setX(x);
        video4.setY(location[1]);
        video4.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        video4.getHolder().setKeepScreenOn(true);
        video4.getHolder().setFixedSize(WIDTH_DEF,HEIGHT_DEF);
        video4.getHolder().setFormat(PixelFormat.OPAQUE);
        video4.getHolder().addCallback(videoCallBack4);
        this.addContentView(video4,lp_video);
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
    public void load1(){
        createPlayer1();
        try {
            iMediaPlayer1.setDataSource(mVideoUrl1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        video1.measure(WIDTH_DEF,HEIGHT_DEF);
//    Log.v("获取到的视频分辨率",iMediaPlayer.getVideoWidth()+","+iMediaPlayer.getVideoHeight());
        iMediaPlayer1.setDisplay(video1.getHolder());
        iMediaPlayer1.prepareAsync();
    }

    public void load2(){
        createPlayer2();
        try {
            iMediaPlayer2.setDataSource(mVideoUrl2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        video2.measure(WIDTH_DEF,HEIGHT_DEF);
//    Log.v("获取到的视频分辨率",iMediaPlayer.getVideoWidth()+","+iMediaPlayer.getVideoHeight());
        iMediaPlayer2.setDisplay(video2.getHolder());
        iMediaPlayer2.prepareAsync();
    }

    public void load3(){
        createPlayer3();
        try {
            iMediaPlayer3.setDataSource(mVideoUrl3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        video3.measure(WIDTH_DEF,HEIGHT_DEF);
//    Log.v("获取到的视频分辨率",iMediaPlayer.getVideoWidth()+","+iMediaPlayer.getVideoHeight());
        iMediaPlayer3.setDisplay(video3.getHolder());
        iMediaPlayer3.prepareAsync();
    }

    public void load4(){
        createPlayer4();
        try {
            iMediaPlayer4.setDataSource(mVideoUrl4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        video4.measure(WIDTH_DEF,HEIGHT_DEF);
//    Log.v("获取到的视频分辨率",iMediaPlayer.getVideoWidth()+","+iMediaPlayer.getVideoHeight());
        iMediaPlayer4.setDisplay(video4.getHolder());
        iMediaPlayer4.prepareAsync();
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
    public void createPlayer1(){
        if(iMediaPlayer1!=null){
            //清空脏数据
            iMediaPlayer1.stop();
            iMediaPlayer1.setDisplay(null);
            iMediaPlayer1.release();
        }
        IjkMediaPlayer ijkMediaPlayer=new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        /**otherMethod**/
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

//                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 8);
        //YYH add
        ijkMediaPlayer.setOption(1, "analyzemaxduration", 100L);
        ijkMediaPlayer.setOption(1, "probesize", 10240L);
        ijkMediaPlayer.setOption(4, "packet-buffering", 0L);
        ijkMediaPlayer.setOption(1, "flush_packets", 1L);
        ijkMediaPlayer.setOption(4, "framedrop", 1L);
        iMediaPlayer1=ijkMediaPlayer;
        if(listener1!=null){
            iMediaPlayer1.setOnInfoListener(listener1);
            iMediaPlayer1.setOnPreparedListener(listener1);
            iMediaPlayer1.setOnSeekCompleteListener(listener1);
            iMediaPlayer1.setOnBufferingUpdateListener(listener1);
            iMediaPlayer1.setOnErrorListener(listener1);
        }
    }

    public void createPlayer2(){
        if(iMediaPlayer2!=null){
            //清空脏数据
            iMediaPlayer2.stop();
            iMediaPlayer2.setDisplay(null);
            iMediaPlayer2.release();
        }
        IjkMediaPlayer ijkMediaPlayer=new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        /**otherMethod**/
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

//                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 8);
        //YYH add
        ijkMediaPlayer.setOption(1, "analyzemaxduration", 100L);
        ijkMediaPlayer.setOption(4, "packet-buffering", 0L);
        ijkMediaPlayer.setOption(1, "probesize", 10240L);
        ijkMediaPlayer.setOption(4, "framedrop", 1L);
        ijkMediaPlayer.setOption(1, "flush_packets", 1L);
        iMediaPlayer2=ijkMediaPlayer;
        if(listener2!=null){
            iMediaPlayer2.setOnInfoListener(listener2);
            iMediaPlayer2.setOnPreparedListener(listener2);
            iMediaPlayer2.setOnSeekCompleteListener(listener2);
            iMediaPlayer2.setOnErrorListener(listener2);
            iMediaPlayer2.setOnBufferingUpdateListener(listener2);

        }
    }

    public void createPlayer3(){
        if(iMediaPlayer3!=null){
            //清空脏数据
            iMediaPlayer3.stop();
            iMediaPlayer3.setDisplay(null);
            iMediaPlayer3.release();
        }
        IjkMediaPlayer ijkMediaPlayer=new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        /**otherMethod**/
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

//                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 8);
        //YYH add
        ijkMediaPlayer.setOption(1, "analyzemaxduration", 100L);
        ijkMediaPlayer.setOption(1, "probesize", 10240L);
        ijkMediaPlayer.setOption(4, "packet-buffering", 0L);
        ijkMediaPlayer.setOption(4, "framedrop", 1L);
        ijkMediaPlayer.setOption(1, "flush_packets", 1L);
        iMediaPlayer3=ijkMediaPlayer;
        if(listener3!=null){
            iMediaPlayer3.setOnInfoListener(listener3);
            iMediaPlayer3.setOnPreparedListener(listener3);
            iMediaPlayer3.setOnSeekCompleteListener(listener3);
            iMediaPlayer3.setOnErrorListener(listener3);
            iMediaPlayer3.setOnBufferingUpdateListener(listener3);

        }
    }

    public void createPlayer4(){
        if(iMediaPlayer4!=null){
            //清空脏数据
            iMediaPlayer4.stop();
            iMediaPlayer4.setDisplay(null);
            iMediaPlayer4.release();
        }
        IjkMediaPlayer ijkMediaPlayer=new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        /**otherMethod**/
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

//                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 8);
        //YYH add
        ijkMediaPlayer.setOption(1, "analyzemaxduration", 100L);
        ijkMediaPlayer.setOption(4, "packet-buffering", 0L);
        ijkMediaPlayer.setOption(1, "probesize", 10240L);
        ijkMediaPlayer.setOption(4, "framedrop", 1L);
        ijkMediaPlayer.setOption(1, "flush_packets", 1L);
        iMediaPlayer4=ijkMediaPlayer;
        if(listener4!=null){
            iMediaPlayer4.setOnInfoListener(listener4);
            iMediaPlayer4.setOnPreparedListener(listener4);
            iMediaPlayer4.setOnSeekCompleteListener(listener4);
            iMediaPlayer4.setOnErrorListener(listener4);
            iMediaPlayer4.setOnBufferingUpdateListener(listener4);
        }
    }


    public void setListener(VideoPlayerListener listener){
        this.listener=listener;
        if(iMediaPlayer!=null){
            iMediaPlayer.setOnPreparedListener(listener);
        }
    }
    public void setListener1(VideoPlayerListener listener1){
        this.listener1=listener1;
        if(iMediaPlayer1!=null){
            iMediaPlayer1.setOnPreparedListener(listener1);
        }
    }

    public void setListener2(VideoPlayerListener listener2){
        this.listener2=listener2;
        if(iMediaPlayer2!=null){
            iMediaPlayer2.setOnPreparedListener(listener2);
        }
    }

    public void setListener3(VideoPlayerListener listener3){
        this.listener3=listener3;
        if(iMediaPlayer3!=null){
            iMediaPlayer3.setOnPreparedListener(listener3);
        }
    }

    public void setListener4(VideoPlayerListener listener4){
        this.listener4=listener4;
        if(iMediaPlayer4!=null){
            iMediaPlayer4.setOnPreparedListener(listener4);
        }
    }
    /**
     *  下面封装了一下控制视频的方法video
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
    /**video**/


    /**video1**/
    public void start1() {
        if (iMediaPlayer1 != null) {
            iMediaPlayer1.start();
        }
    }

    public void release1() {
        if (iMediaPlayer1 != null) {
            iMediaPlayer1.reset();
            iMediaPlayer1.release();
            iMediaPlayer1 = null;
        }
    }

    public void pause1() {
        if (iMediaPlayer1 != null) {
            iMediaPlayer1.pause();
        }
    }

    public void stop1() {
        if (iMediaPlayer1 != null) {
            iMediaPlayer1.stop();
        }
    }


    public void reset1() {
        if (iMediaPlayer1 != null) {
            iMediaPlayer1.reset();
        }
    }


    public long getDuration1() {
        if (iMediaPlayer1 != null) {
            return iMediaPlayer1.getDuration();
        } else {
            return 0;
        }
    }


    public long getCurrentPosition1() {
        if (iMediaPlayer1 != null) {
            return iMediaPlayer1.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public void seekTo1(long l) {
        if (iMediaPlayer1 != null) {
            iMediaPlayer1.seekTo(l);
        }
    }
    /**video1**/

    /**video2**/
    public void start2() {
        if (iMediaPlayer2 != null) {
            iMediaPlayer2.start();
        }
    }

    public void release2() {
        if (iMediaPlayer2 != null) {
            iMediaPlayer2.reset();
            iMediaPlayer2.release();
            iMediaPlayer2 = null;
        }
    }

    public void pause2() {
        if (iMediaPlayer2 != null) {
            iMediaPlayer2.pause();
        }
    }

    public void stop2() {
        if (iMediaPlayer2 != null) {
            iMediaPlayer2.stop();
        }
    }


    public void reset2() {
        if (iMediaPlayer2 != null) {
            iMediaPlayer2.reset();
        }
    }


    public long getDuration2() {
        if (iMediaPlayer2 != null) {
            return iMediaPlayer2.getDuration();
        } else {
            return 0;
        }
    }


    public long getCurrentPosition2() {
        if (iMediaPlayer2 != null) {
            return iMediaPlayer2.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public void seekTo2(long l) {
        if (iMediaPlayer2 != null) {
            iMediaPlayer2.seekTo(l);
        }
    }
    /**video2**/

    /**video3**/
    public void start3() {
        if (iMediaPlayer3 != null) {
            iMediaPlayer3.start();
        }
    }

    public void release3() {
        if (iMediaPlayer3 != null) {
            iMediaPlayer3.reset();
            iMediaPlayer3.release();
            iMediaPlayer3 = null;
        }
    }

    public void pause3() {
        if (iMediaPlayer3 != null) {
            iMediaPlayer3.pause();
        }
    }

    public void stop3() {
        if (iMediaPlayer3 != null) {
            iMediaPlayer3.stop();
        }
    }


    public void reset3() {
        if (iMediaPlayer3 != null) {
            iMediaPlayer3.reset();
        }
    }


    public long getDuration3() {
        if (iMediaPlayer3 != null) {
            return iMediaPlayer3.getDuration();
        } else {
            return 0;
        }
    }


    public long getCurrentPosition3() {
        if (iMediaPlayer3 != null) {
            return iMediaPlayer3.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public void seekTo3(long l) {
        if (iMediaPlayer3 != null) {
            iMediaPlayer3.seekTo(l);
        }
    }
    /**video3**/

    /**video4**/
    public void start4() {
        if (iMediaPlayer4 != null) {
            iMediaPlayer4.start();
        }
    }

    public void release4() {
        if (iMediaPlayer4 != null) {
            iMediaPlayer4.reset();
            iMediaPlayer4.release();
            iMediaPlayer4 = null;
        }
    }

    public void pause4() {
        if (iMediaPlayer4 != null) {
            iMediaPlayer4.pause();
        }
    }

    public void stop4() {
        if (iMediaPlayer4 != null) {
            iMediaPlayer4.stop();
        }
    }


    public void reset4() {
        if (iMediaPlayer4 != null) {
            iMediaPlayer4.reset();
        }
    }


    public long getDuration4() {
        if (iMediaPlayer4 != null) {
            return iMediaPlayer4.getDuration();
        } else {
            return 0;
        }
    }


    public long getCurrentPosition4() {
        if (iMediaPlayer4 != null) {
            return iMediaPlayer4.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public void seekTo4(long l) {
        if (iMediaPlayer4 != null) {
            iMediaPlayer4.seekTo(l);
        }
    }
    /**video3**/

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
    private SurfaceHolder.Callback videoCallBack1=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            load1();
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    private SurfaceHolder.Callback videoCallBack2=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            load2();
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };


    private SurfaceHolder.Callback videoCallBack3=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            load3();
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    private SurfaceHolder.Callback videoCallBack4=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            load4();
        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    private final String playServerUrl="http://192.168.191.1:8080/getPlayURL";
    private int play_sit=-1;
    private String play_url=null;
    private String play1_url=null;
    private String play2_url=null;
    private String play3_url=null;
    private String play4_url=null;

    private Runnable playUpdate=new Runnable() {
        @Override
        public void run() {
            play();
        }
    };
    private final Handler playHandler=new Handler();
    public void getPlayUrl(String meet_id){
        OkHttpClient client=new OkHttpClient();
        final MediaType JSON=MediaType.parse("application/json;charset=utf-8");
        JSONObject playPostjsb=new JSONObject();
        try {
            playPostjsb.put("meet_id",meet_id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody=RequestBody.create(JSON,playPostjsb.toString());
        Request request=new Request.Builder()
                        .url(playServerUrl)
                        .post(requestBody)
                        .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("获取播放流地址结果","失败!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            if(response.isSuccessful()){
                String rspStr=response.body().string();
                Log.v("获取播放流地址结果","成功!");
                try {
                    JSONArray rspJsa=new JSONArray(rspStr);
                    int length=rspJsa.length();
                    for(int i=0;i<length;i++){
                        JSONObject rspJsb=rspJsa.getJSONObject(i);
                        int url_suffix=rspJsb.getInt("address_id");
                        int sit=rspJsb.getInt("address_sit");
                        switch(sit){
                            case 0:
                                play_url=url_prefix+url_suffix;
                                Log.v("服务器地址",""+play_url);
                                break;
                            case 1:
                                play1_url=url_prefix+url_suffix;
                                break;
                            case 2:
                                play2_url=url_prefix+url_suffix;
                                break;
                            case 3:
                                play3_url=url_prefix+url_suffix;
                                break;
                            case 4:
                                play4_url=url_prefix+url_suffix;
                                break;
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            }
        });
    }
    public void play(){
        try {
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
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            String v_url="rtmp://ossrs.net/live/1";
            this.setListener(video_listener);
            this.setVideoUrl(play_url);
            //String v1_url="rtmp://ossrs.net/live/12345676";
            this.setListener1(video_listener);
            Log.v("窗口1地址",play1_url);
            this.setVideo1Url(play1_url);

            this.setListener2(video_listener);
            this.setVideo2Url(play2_url);

            this.setListener3(video_listener);
            this.setVideo3Url(play3_url);

            this.setListener4(video_listener);
            this.setVideo4Url(play4_url);
        }catch(Exception e){
            this.finish();
        }
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
                    if(isCloseBtnAdd){
                        closeDialog();
                    }
                    return;
                }
                else if(SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)){
                    // 长按Home键 或者 activity切换键
                    if(isCloseBtnAdd){
                        closeDialog();
                    }
                    return ;
                }
                else if(SYSTEM_DIALOG_REASON_LOCK.equals(reason)){
                    // 锁屏
                    if(isCloseBtnAdd){
                        closeDialog();
                    }
                    return ;
                }
                else if(SYSTEM_DIALOG_REASON_ASSIST.equals(reason)){
                    // samsung 长按Home键
                    if(isCloseBtnAdd){
                        closeDialog();
                    }
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
    private TabLayout tools_bar;
    public void  initOperator(){
        tools_bar=findViewById(R.id.tools_bar);
        List<String> toolsList=new ArrayList<>();
        toolsList.add("成员");
        toolsList.add("文件");
        toolsList.add("详情");
        toolsList.add("主持");
        toolsList.add("投票");
        for(int i=0;i<toolsList.size();i++){
            tools_bar.addTab(tools_bar.newTab().setText(toolsList.get(i)));
        }
        initToolsPager();
        TabLayout.OnTabSelectedListener toolsListener=new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tool_vp.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };

        tools_bar.setOnTabSelectedListener(toolsListener);
    }
    final String videoServerUrl="http://192.168.191.1:8080/getVideoURL";
    //推流地址固定前缀
    private final String url_prefix="rtmp://ossrs.net/live/";
    /**5个视频位的推流/拉流地址**/
    private String v_url,v1_url,v2_url,v3_url,v4_url;
    private int sit_num=-1;
    public void getVideoURL(String meet_id){
        OkHttpClient client=new OkHttpClient();
        final MediaType JSON=MediaType.parse("application/json;charset=utf-8");
        final JSONObject recordPostJson=new JSONObject();
        try {
            recordPostJson.put("meet_id",meet_id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody=RequestBody.create(JSON,recordPostJson.toString());
        Request request=new Request.Builder()
                        .url(videoServerUrl)
                        .post(requestBody)
                        .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("获取推流服务器地址请求结果","失败!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    String rspStr=response.body().string();
                    try {
                        JSONObject rspJsb=new JSONObject(rspStr);
                        String url_suffix=rspJsb.getString("address_id");
                        int seat=rspJsb.getInt("address_sit");

                        switch (seat){
                            case 1:
                                sit_num=1;
                                v1_url=url_prefix+url_suffix;
                                Log.v("v1_url",""+v1_url);
                                break;
                            case 2:
                                sit_num=2;
                                v2_url=url_prefix+url_suffix;
                                break;
                            case 3:
                                sit_num=4;
                                v3_url=url_prefix+url_suffix;
                                break;
                            case 4:
                                sit_num=4;
                                v4_url=url_prefix+url_suffix;
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }
    private DrawerLayout operators_box;
    private ViewPager tool_vp;
    private View member_pager,file_pager,details_pager,host_pager,vote_pager;
    public void initToolsPager(){
        tool_vp=findViewById(R.id.operators);
        List<View> myViewList=new ArrayList<>();
        member_pager=View.inflate(this,R.layout.tab_member,null);
        file_pager=View.inflate(this,R.layout.tab_file,null);
        details_pager=View.inflate(this,R.layout.tab_details,null);
        host_pager=View.inflate(this,R.layout.tab_host,null);
        vote_pager=View.inflate(this,R.layout.tab_vote,null);
        myViewList.add(member_pager);
        myViewList.add(file_pager);
        myViewList.add(details_pager);
        myViewList.add(host_pager);
        myViewList.add(vote_pager);
        ViewPager.OnPageChangeListener toolPagerListener=new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }
            @Override
            public void onPageSelected(int position) {
                tools_bar.getTabAt(position).select();
            }
            @Override
            public void onPageScrollStateChanged(int i) {

            }
        };
        ToolPagerAdapter toolPagerAdapter=new ToolPagerAdapter(this,myViewList);
        tool_vp.setAdapter(toolPagerAdapter);
        tool_vp.setCurrentItem(0);
        tool_vp.setOnPageChangeListener(toolPagerListener);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getPermission();
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
    }
    @Override
    public void finish(){
        super.finish();
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
            String[] permissions={Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.SYSTEM_ALERT_WINDOW,Manifest.permission.RECORD_AUDIO
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

class ToolPagerAdapter extends PagerAdapter{
    private Context context;
    private List<View> mViewList;
    public ToolPagerAdapter(Context context,List<View> viewList){
        this.context=context;
        this.mViewList=viewList;
    }
    @Override
    public int getCount() {
        return mViewList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view==o;
    }

    @Override
    public void destroyItem(ViewGroup container,int position,Object object){
        container.removeView(mViewList.get(position));
    }
    @Override
    public Object instantiateItem(ViewGroup container, int position){
        container.addView(mViewList.get(position));
        return mViewList.get(position);
    }
}

