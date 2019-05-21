package com.design.lxp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private EditText ruser_et;
    private EditText ruserName_et;
    private EditText rpwd_et;
    private EditText repwd_et;
    private TextView ruser_info;
    private TextView ruserName_info;
    private TextView rpwd_info;
    private TextView repwd_info;
    private Button register_btn;
    private TextView go_login;
    private Runnable regUpdate;
    private Runnable timeUpdate;
    private final Handler regHandler=new Handler();
    private final Handler timeHandler=new Handler();


    public void initView(){
        ruser_et=findViewById(R.id.ruser_et);
        ruserName_et=findViewById(R.id.ruserName_et);
        rpwd_et=findViewById(R.id.rpwd_et);
        repwd_et=findViewById(R.id.repwd_et);
        ruser_info=findViewById(R.id.ruser_info);
        ruserName_info=findViewById(R.id.ruserName_info);
        rpwd_info=findViewById(R.id.rpwd_info);
        repwd_info=findViewById(R.id.repwd_info);
        register_btn=findViewById(R.id.register_btn);
        go_login=findViewById(R.id.go_login);
        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register_btn.setText("注册中...");
                registerControl();
                regHandler.postDelayed(regUpdate,2000);
            }
        });
        go_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(RegisterActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

    }
    private  int t=3;/**计时时间3秒**/
    public void registerControl(){
        final String ruserStr=ruser_et.getText().toString();
        final String ruserNameStr=ruserName_et.getText().toString();
        final String rpwdStr=rpwd_et.getText().toString();
        final String repwdStr=repwd_et.getText().toString();
        /**格式检查
         * 帐号为空
         * 密码为空
         * 帐号密码为空
         * 帐号格式不正确
         * 密码格式不正确
         * 帐号密码格式不正确
         * 两次密码不一致
         * **/
        if(!isRegisterFormat(ruserStr,ruserNameStr,rpwdStr,repwdStr)){
            regUpdate=null;
            return ;
        }else if(!isConnected()){
            regUpdate=null;
            return ;
        }else{
            /**注册请求**/
            registerPost(ruserStr,ruserNameStr,repwdStr);
            regUpdate=new Runnable() {
                @Override
                public void run() {
                    if(register_result){
                        repwd_info.setText("");
                        rpwd_info.setText("");
                        ruserName_info.setText("");
                        ruser_info.setText("");
                        register_btn.setText("注册");
                        Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                        timeUpdate=new Runnable() {
                            @Override
                            public void run() {
                                register_btn.setText(String.valueOf(t--)+"秒后跳转登录");
                                if(t==0){
                                    Intent intent=new Intent(RegisterActivity.this,MainActivity.class);
                                    intent.putExtra("register_user",ruserStr);
                                    intent.putExtra("register_pwd",rpwdStr);
                                    startActivity(intent);
                                    return ;
                                }
                                timeHandler.postDelayed(timeUpdate,1000);
                            }
                        };
                        timeHandler.post(timeUpdate);
                    }else{
                        register_btn.setText("注册");
                        ruser_et.requestFocus();
                        ruser_et.setSelection(ruserStr.length());
                        Toast.makeText(RegisterActivity.this, "帐号已经注册或无法连接到服务器!", Toast.LENGTH_SHORT).show();
                    }
                }
            };
        }



    }

    /**登录信息格式验证**/
    public static final String FORMAT_USER = "^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$";
    public static final String FORMAT_PASSWORD = "^[a-zA-Z0-9]{6,12}$";//(至少6位，不包含特殊字符)
    public boolean isRegisterFormat(String user,String userName,String pwd,String repwd){
        boolean isValue=true;
        if(repwd.equals("")){
            isValue=false;
            register_btn.setText("注册");
            repwd_et.requestFocus();
            repwd_info.setText("确认密码不能为空!");
        }
        if(pwd.equals("")){
            isValue=false;
            register_btn.setText("注册");
            rpwd_et.requestFocus();
            rpwd_info.setText("密码不能为空!");
        }
        if(userName.equals("")){
            isValue=false;
            register_btn.setText("注册");
            ruserName_et.requestFocus();
            ruserName_info.setText("用户名不能为空!");
        }
        if(user.equals("")){
            isValue=false;
            register_btn.setText("注册");
            ruser_et.requestFocus();
            ruser_info.setText("帐号不能为空!");
        }
        /**if(user.equals("")&&!pwd.equals("")){
            register_btn.setText("注册");
            ruser_et.requestFocus();
            Toast.makeText(this, "帐号不能为空!", Toast.LENGTH_SHORT).show();
            return false;
        }else if(!user.equals("")&&pwd.equals("")){
            register_btn.setText("注册");
            rpwd_et.requestFocus();
            Toast.makeText(this, "密码不能为空!", Toast.LENGTH_SHORT).show();
            return false;
        }else if(user.equals("")&&pwd.equals("")){
            register_btn.setText("注册");
            ruser_et.requestFocus();
            Toast.makeText(this, "帐号密码不能为空!", Toast.LENGTH_SHORT).show();
            return false;
        }else if(!user.equals("")&&!pwd.equals("")&&repwd.equals("")){
            register_btn.setText("注册");
            repwd_et.requestFocus();
            Toast.makeText(this, "请再次输入密码确认!", Toast.LENGTH_SHORT).show();
            return false;
        }**/
        if(!isValue){
            return isValue;
        }
        boolean isValid=true;
        boolean is_user=Pattern.matches(FORMAT_USER,user);
        boolean is_pwd=Pattern.matches(FORMAT_PASSWORD,pwd);
        Log.v("帐号？？？？",""+user+is_user+ " " +is_pwd) ;
        if(!is_user&&is_pwd){
            isValid=false;
            register_btn.setText("注册");
            ruser_et.requestFocus();
            ruser_et.setSelection(user.length());
            Toast.makeText(this,"帐号格式不正确!",Toast.LENGTH_SHORT).show();
        }else if(is_user&&!is_pwd){
            isValid=false;
            register_btn.setText("注册");
            rpwd_et.requestFocus();
            rpwd_et.setSelection(pwd.length());
            Toast.makeText(this,"密码格式不正确!",Toast.LENGTH_SHORT).show();
        }else if(!is_user&&!is_pwd){
            isValid=false;
            register_btn.setText("注册");
            ruser_et.requestFocus();
            ruser_et.setSelection(user.length());
            Toast.makeText(this,"帐号密码格式错误!",Toast.LENGTH_SHORT).show();
        }else if(!pwd.equals(repwd)){
            isValid=false;
            register_btn.setText("注册");
            repwd_et.requestFocus();
            repwd_et.setSelection(repwd.length());
            Toast.makeText(this, "两次密码不匹配!", Toast.LENGTH_SHORT).show();
        }
        return isValid;
    }
    /**网络连接验证**/
    public boolean isConnected(){
        ConnectivityManager connectivityManager=(ConnectivityManager)this.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
        if(networkInfo==null){
            register_btn.setText("注册");
            Toast.makeText(this,"网络连接错误",Toast.LENGTH_SHORT).show();
            return false; }
        NetworkInfo.State networkState=networkInfo.getState();
        if(networkState==NetworkInfo.State.DISCONNECTED||networkState==NetworkInfo.State.UNKNOWN){
            register_btn.setText("注册");
            Toast.makeText(this,"网络连接错误",Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private final String registerServerUrl="http://192.168.191.1:8080/register";
    private boolean register_result=false;
    public void registerPost(String user,String userName,String pwd){
        OkHttpClient client =new OkHttpClient();
        MediaType JSON=MediaType.parse("application/json;charset=utf-8");
        final JSONObject postJsb=new JSONObject();
        try {
            postJsb.put("user_id",user);
            postJsb.put("user_name",userName);
            postJsb.put("password",pwd);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v("注册请求",""+postJsb.toString());
        RequestBody requestBody=RequestBody.create(JSON,postJsb.toString());
        Request request=new Request.Builder()
                .url(registerServerUrl)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("注册请求","注册请求失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    String rspStr=response.body().string();
                    Log.v("bool值:",rspStr);
                    try {
                        JSONObject rspJson=new JSONObject(rspStr);
                        register_result=(boolean) rspJson.get("register_result");
                        Log.v("register_result",""+register_result);
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
        setContentView(R.layout.activity_register);
        initView();
    }
}
