package com.design.lxp;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import okhttp3.*;
import okio.ByteString;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.SocketHandler;

public class WebSocketTestActivity extends AppCompatActivity {
    public void initView(){
        setListener();
        mHandler.postDelayed(heartBeatRunnable,HEART_BEAT_RATE);//发送心跳包，由于运营商网关问题，需要发送心跳包保持连接
    }

    private long sendTime=0L;
    //发送心跳包
    private Handler mHandler=new Handler();
    //发送心跳包频率:2s
    private static final long HEART_BEAT_RATE=2*1000;

//发送心跳包
    private Runnable heartBeatRunnable=new Runnable() {
    @Override
    public void run() {
        if(System.currentTimeMillis()-sendTime>=HEART_BEAT_RATE){
            String message =sendData();
            mSocket.send(message);
            sendTime=System.currentTimeMillis();
        }
        mHandler.post(this); //每隔一定的时间，对长连接进行一次心跳检测
    }
};
private WebSocket mSocket;

private String testServerUrl="ws://192.168.191.1:8080/webSocket/lxp";
   private OkHttpClient client;
    public void setListener(){//实际请求
        client=new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3,TimeUnit.SECONDS)
                .connectTimeout(3,TimeUnit.SECONDS)
                .build();
        Request request=new Request.Builder().url(testServerUrl).build();
        EchoWebSocketListener socketListener=new EchoWebSocketListener();

        client.newWebSocket(request,socketListener);
        client.dispatcher().executorService().shutdown();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_socket_test);
        initView();
    }

private final class EchoWebSocketListener extends WebSocketListener {
@Override
public void onOpen(WebSocket webSocket, Response response){
    super.onOpen(webSocket,response);
    mSocket=webSocket;
    output("WebSocket连接结果","连接成功!");
}

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        super.onMessage(webSocket, bytes);
        output("WebSocket接收bytes数据",""+bytes.hex());
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        output("WebSocket,服务端发送来的消息",text);
        //连接断开测试
        /**
        if(!TextUtils.isEmpty(text)){
            if(mSocket!=null){
                mSocket.close(1000,null);
            }
            if(mHandler!=null){
                mHandler.removeCallbacksAndMessages(null);
                mHandler=null;
            }
        }**/
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        super.onClosed(webSocket, code, reason);
        output("WebSocket closed",reason);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        super.onClosing(webSocket, code, reason);
        output("WebSocket closing",reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        output("WebSocket failure",t.getMessage());
    }

}
    private void output(final String TAG, final String text){//ui更新线程
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,text);
            }
        });
    }
    private String sendData(){
        String jsonHead="";
        JSONObject testPostJsb=new JSONObject();
        try {
            testPostJsb.put("meet_id","000001");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jsonHead=testPostJsb.toString();
        Log.i("TAG" , "sendData: " + jsonHead) ;
        return jsonHead;
    }
}
