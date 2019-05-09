package com.design.lxp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
private Button login;
private EditText user;
private EditText pwd;
private RadioGroup input_mid;
private RadioButton saveUP_btn,saveU_btn,unSave_btn;
private int checkedRadio;
public void initView(){
    login=findViewById(R.id.login_btn);
    login.setOnClickListener(this);
    user=findViewById(R.id.user_et);
    pwd=findViewById(R.id.pwd_et);
    input_mid=findViewById(R.id.input_mid);
    saveUP_btn=findViewById(R.id.saveUP_btn);
    saveU_btn=findViewById(R.id.saveU_btn);
    unSave_btn=findViewById(R.id.unSave_btn);
    share_lg=getSharedPreferences("login",MODE_PRIVATE);
    fetchUser();
    RadioGroup.OnCheckedChangeListener checkListener=new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
            Toast.makeText(MainActivity.this, "您选中了控件"+checkedId, Toast.LENGTH_SHORT).show();
            checkedRadio=checkedId;
        }
    };
    input_mid.setOnCheckedChangeListener(checkListener);
}

/**登录控制函数***/
private Runnable update;
final Handler handler=new Handler();
public void loginControl(){
final String userStr=user.getText().toString();
final String pwdStr=pwd.getText().toString();
if(!formatVerification(userStr,pwdStr)){
    update=null;
    return;
}else if(!isConnected()){
    update=null;
    return;
}else{
    //发起登录请求
    loginPost(userStr,pwdStr);
    update=new Runnable() {
        @Override
        public void run() {
            if(login_result){
                login.setText("登录");
                Toast.makeText(MainActivity.this, "登录成功!", Toast.LENGTH_SHORT).show();
                saveUser(userStr,pwdStr);
                Intent intent=new Intent(MainActivity.this,RoomSelectActivity.class);
                intent.putExtra("login_user",userStr);
                startActivity(intent);
                login_result=false;
            }else{
                login.setText("登录");
                Toast.makeText(MainActivity.this, "帐号密码不匹配!", Toast.LENGTH_SHORT).show();
            }
        }
    };
}
}

/**记住账号密码**/
private SharedPreferences share_lg;
public void saveUser(String userStr,String pwdStr){
    SharedPreferences.Editor editor=share_lg.edit();
    if(checkedRadio==R.id.saveUP_btn){
        editor.putString("user_id",userStr);
        editor.putString("password",pwdStr);
        editor.putInt("is_save",checkedRadio);
    }else if(checkedRadio==R.id.saveU_btn){
        editor.putString("user_id",userStr);
        editor.putString("password",null);
        editor.putInt("is_save",checkedRadio);
    }else if(checkedRadio==R.id.unSave_btn){
        editor.putString("user_id",null);
        editor.putString("password",null);
        editor.putInt("is_save",checkedRadio);
    }
    editor.commit();
}
/**记住账号密码**/

/**取出记住的帐号密码**/
public void fetchUser(){
    user.setText(share_lg.getString("user_id",null));
    pwd.setText(share_lg.getString("password",null));
    int radioId=share_lg.getInt("is_save",R.id.unSave_btn);
    checkedRadio=radioId;
    if(radioId==R.id.unSave_btn){
        unSave_btn.setChecked(true);
    }else if(radioId==R.id.saveUP_btn){
        saveUP_btn.setChecked(true);
    }else if(radioId==R.id.saveU_btn){
        saveU_btn.setChecked(true);
    }
}
/**取出记住的帐号密码**/
/**登录控制函数***/

/**帐号密码格式验证**/
public static final String FORMAT_USER = "^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$";
public static final String FORMAT_PASSWORD = "^[a-zA-Z0-9]{6,12}$";//(至少6位，不包含特殊字符)
public boolean formatVerification(String userStr,String pwdStr){
    if(userStr.equals("")&&!pwdStr.equals("")){
    login.setText("登录");
    Toast.makeText(this,"帐号不能为空!",Toast.LENGTH_SHORT).show();
    return false;
    } else if(!userStr.equals("")&&pwdStr.equals("")){
        login.setText("登录");
        Toast.makeText(this,"密码不能为空!",Toast.LENGTH_SHORT).show();
        return false;
    }else if(userStr.equals("")&&pwdStr.equals("")){
        login.setText("登录");
        Log.v("---------","帐号密码不能为空!");
        Toast.makeText(this,"帐号密码不能为空!",Toast.LENGTH_SHORT).show();
        return false;
    }
    boolean is_user=Pattern.matches(FORMAT_USER,userStr);
    boolean is_pwd=Pattern.matches(FORMAT_PASSWORD,pwdStr);
    if(!is_user&&is_pwd){
        login.setText("登录");
        Toast.makeText(this,"帐号格式不正确!",Toast.LENGTH_SHORT).show();
        return false;
    }else if(is_user&&!is_pwd){
        login.setText("登录");
        Toast.makeText(this,"密码格式不正确!",Toast.LENGTH_SHORT).show();
        return false;
    }else if(!is_user&&!is_pwd){
        login.setText("登录");
        Toast.makeText(this,"帐号密码格式错误!",Toast.LENGTH_SHORT).show();
        return false;
    }
    return true;
}
    /**帐号密码格式验证**/

    /**网络连接验证**/
    public boolean isConnected(){
        ConnectivityManager connectivityManager=(ConnectivityManager)this.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
            if(networkInfo==null){
                login.setText("登录");
                Toast.makeText(this,"网络连接错误",Toast.LENGTH_SHORT).show();
                return false; }
                NetworkInfo.State networkState=networkInfo.getState();
                if(networkState==NetworkInfo.State.DISCONNECTED||networkState==NetworkInfo.State.UNKNOWN){
                    login.setText("登录");
                    Toast.makeText(this,"网络连接错误",Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
    }
    final String serverUrl="http://192.168.191.1:8080/login";
    private boolean login_result=false;
    public void loginPost(String user_str,String pwd){
        OkHttpClient client=new OkHttpClient();
        MediaType JSON=MediaType.parse("application/json;charset=utf-8");
        final JSONObject postJson=new JSONObject();
        try {
            postJson.put("user_id",user_str);
            postJson.put("password",pwd);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v("登录请求",""+postJson.toString());
        RequestBody requestBody=RequestBody.create(JSON,postJson.toString());
        Request request=new Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("登录请求","登录请求失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    String rspStr=response.body().string();
                    Log.v("bool值:",rspStr);
                    try {
                        JSONObject rspJson=new JSONObject(rspStr);
                        login_result=(boolean) rspJson.get("result");
                        Log.v("login_result",""+login_result);
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
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==login.getId()){
           login.setText("登录中...");
            loginControl();
            handler.postDelayed(update,1000);
            //Intent login_intent=new Intent(this,RoomSelectActivity.class);
            //startActivity(login_intent);
        }
    }
}
