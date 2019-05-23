package com.design.lxp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MeetShowActivity extends AppCompatActivity {
    private boolean isGet=false;
    private ImageView room_head_img;
    private TextView room_name_tv;
    private String room_name;
    private TextView meet_theme_info;
    private String meet_theme;
    private TextView meet_type_info;
    private int meet_type;
    private TextView meet_hoster_info;
    private String meet_hoster;
    private TextView meet_size_info;
    private int meet_size;
    private TextView meet_limit_info;
    private int meet_limit;
    private String showId;
    private Runnable showUpdate;
    private final Handler showHandler=new Handler();

    public void initView(){
        room_head_img=findViewById(R.id.room_head_img);
        room_name_tv=findViewById(R.id.room_name_tv);
        meet_theme_info=findViewById(R.id.meet_theme_info);
        meet_type_info=findViewById(R.id.meet_type_info);
        meet_hoster_info=findViewById(R.id.meet_hoster_info);
        meet_size_info=findViewById(R.id.meet_size_info);
        meet_limit_info=findViewById(R.id.meet_limit_info);
        showUpdate=new Runnable() {
            @Override
            public void run() {
                if(!isGet){
                    final String FAILURE="加载失败";
                    room_name_tv.setText(FAILURE);
                    meet_theme_info.setText(FAILURE);
                    meet_type_info.setText(FAILURE);
                    meet_hoster_info.setText(FAILURE);
                    meet_size_info.setText(FAILURE);
                    meet_limit_info.setTextColor(Color.argb(134,255,0,0));
                    meet_limit_info.setText(FAILURE);
                }else{
                    room_name_tv.setText(room_name);
                    meet_theme_info.setText(meet_theme);
                    switch (meet_type){
                        case 1:
                            meet_type_info.setText("家庭会议");
                            break;
                        case 2:
                            meet_type_info.setText("企业会议");
                            break;
                        case 3:
                            meet_type_info.setText("主题会议");
                            break;
                        case 4:
                            meet_type_info.setText("其他会议");
                            break;
                    }
                    meet_hoster_info.setText(meet_hoster);
                    meet_size_info.setText(String.valueOf(meet_size));
                    if(meet_limit==0){
                        meet_limit_info.setTextColor(Color.argb(134,255,0,0));
                        meet_limit_info.setText("拒绝申请加入");
                    }else if(meet_limit==1){
                        meet_limit_info.setTextColor(Color.argb(134,0,255,0));
                        meet_limit_info.setText("允许申请加入");
                    }
                }
            }
        };
        Intent intent=getIntent();
        showId=intent.getStringExtra("select_meetId");
        Log.v("showId"," "+showId);
        getMeetInfo(showId);
        showHandler.postDelayed(showUpdate,1000);
    }

    private final String MeetInfoServerUrl="http://192.168.191.1:8080/MeetShow";
    public void getMeetInfo(String meetId){
        OkHttpClient client =new OkHttpClient();
        FormBody.Builder postFb=new FormBody.Builder();
        postFb.add("meet_id",meetId);
        Request request=new Request.Builder()
                .url(MeetInfoServerUrl)
                .post(postFb.build())
                .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("获取会议室信息结果","失败!");
                isGet=false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    Log.v("获取会议室信息结果","成功!");
                    String rspStr=response.body().string();
                    isGet=true;
                    JSONObject rspJsb= null;
                    try {
                        rspJsb = new JSONObject(rspStr);
                        room_name=rspJsb.getString("room_name");
                        meet_theme=rspJsb.getString("meet_theme");
                        meet_type=rspJsb.getInt("meet_type");
                        meet_hoster=rspJsb.getString("meet_hoster");
                        meet_size=rspJsb.getInt("meet_size");
                        meet_limit=1;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meet_show);
        Log.v("进入MeetShowActivity"," \n");
        initView();
    }
}
