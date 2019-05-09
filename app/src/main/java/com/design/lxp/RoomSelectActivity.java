
package com.design.lxp;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

public class RoomSelectActivity extends AppCompatActivity implements
        TabLayout.OnTabSelectedListener{
    private ListView mLeftMenu,mRightMenu;
    private ListAdapter leftAdapter,rightAdapter;
    private DrawerLayout drawly;
    private ImageView userHead;
    private Button openMenu,closeMenu;
    private View.OnClickListener openListener,closeListener;
    private android.support.v7.widget.Toolbar roomHead;
    private TabLayout tabTitle;
    private List<String> mTitleArray;
    private List<View> myViewList;
    private ViewPager vp_content;
    private View join_pager,build_pager;
    private ViewPager.OnPageChangeListener vp_listener;
    private ExpandableListView expandableListView;
    private AutoCompleteTextView search_et;
    private List<String> resultArray;
    private ArrayAdapter<String> resultAdapter;
    private Context context=this;
    public void initView(){
        initTitleView();
        initLeftMenu();
        initRightMenu();
        openMenu=findViewById(R.id.menuOpen_btn);
        closeMenu=findViewById(R.id.menuClose_btn);
        openListener=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.v("---------","点击打开");
                drawly.openDrawer(Gravity.START);
            }
        };
        closeListener=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.v("---------","点击关闭");
                drawly.closeDrawer(Gravity.START);
            }
        };
        openMenu.setOnClickListener(openListener);
        closeMenu.setOnClickListener(closeListener);
    }
    public void initTitleView(){
        roomHead=findViewById(R.id.room_head);
        tabTitle=findViewById(R.id.tab_title);
        mTitleArray=new ArrayList<>();
        setSupportActionBar(roomHead);//toolbar.setNavigationOnClickListener需放到其后
        roomHead.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(RoomSelectActivity.this,MeetingRoomActivity.class);
                startActivity(intent);
            }
        });
        mTitleArray.add("加入会议室");
        mTitleArray.add("创建会议室");
        initTabLayout();
        initTabViewPager();
        initSpinner();
    }
    public void initTabLayout(){
        tabTitle.addTab(tabTitle.newTab().setText(mTitleArray.get(0)));
        tabTitle.addTab(tabTitle.newTab().setText(mTitleArray.get(1)));
        tabTitle.setOnTabSelectedListener(this);
    }

    public void initSpinner(){
        search_et=join_pager.findViewById(R.id.search_et);
        search_et.setDropDownHorizontalOffset(0);
        search_et.setDropDownVerticalOffset(0);
        search_et.setAdapter(getResultAdapter());
        search_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String str=editable.toString();
                if(str.length()>=2){
                    /**根据输入后的文本通过网络请求获取会议室数据进行添加备选词**/
                    resultArray.add("会议4");
                    resultArray.add("会议5");
                    resultArray.add("会议6");
                    resultAdapter=new ArrayAdapter<String>(context,R.layout.item_select,resultArray);
                    search_et.setAdapter(resultAdapter);
                }
            }
        });
    }

    public ArrayAdapter getResultAdapter(){
        resultArray=new ArrayList<>();
        resultArray.add("会议1");
        resultArray.add("会议2");
        resultArray.add("会议3");
         resultAdapter=new ArrayAdapter<String>(this,R.layout.item_select,resultArray);
        return resultAdapter;
    }
   private  List<Meet> m1=new ArrayList<>();
    private  List<Meet> m2=new ArrayList<>();
    private  List<Meet> m3=new ArrayList<>();
    private  List<Meet> m4=new ArrayList<>();

    public MeetAdapter getMeetAdapter() {
        List<String> groupList = new ArrayList<>();
        groupList.add("家庭会议");
        groupList.add("企业会议");
        groupList.add("主题会议");
        groupList.add("其他会议");
        List<List<Meet>> meets = new ArrayList<>();
        meets.add(m1);
        meets.add(m2);
        meets.add(m3);
        meets.add(m4);
        meetAdapter=new MeetAdapter(this,groupList,meets);
        return meetAdapter;
    }
    private MeetAdapter meetAdapter;
    private View typeView;
    private final Handler handler =new Handler();
    private Runnable meetListUpdate;
    public void initRecycleView(){
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        List<ExpandableListView> typeList=new ArrayList<>();
        getMeetList();
        MeetListUpdate();
        handler.postDelayed(meetListUpdate,2000);
    }
    public void MeetListUpdate(){
        meetListUpdate=new Runnable() {
            @Override
            public void run() {
                for(Meet_ meetItem:meetList_) {
                    switch (meetItem.getMeet_type()) {
                        case 1:
                            Meet_ meet1 = new Meet_();
                            meet1.setMeet_theme(meetItem.getMeet_theme());
                            meet1.setMeet_size(meetItem.getMeet_size());
                            meet1.setMeet_limit(meetItem.getMeet_limit());
                            m1List.add(meet1);
                            break;
                        case 2:
                            Meet_ meet2 = new Meet_();
                            meet2.setMeet_theme(meetItem.getMeet_theme());
                            meet2.setMeet_size(meetItem.getMeet_size());
                            meet2.setMeet_limit(meetItem.getMeet_limit());
                            m2List.add(meet2);
                            break;
                        case 3:
                            Meet_ meet3 = new Meet_();
                            meet3.setMeet_theme(meetItem.getMeet_theme());
                            meet3.setMeet_size(meetItem.getMeet_size());
                            meet3.setMeet_limit(meetItem.getMeet_limit());
                            m3List.add(meet3);
                            break;
                        case 4:
                            Meet_ meet4 = new Meet_();
                            meet4.setMeet_theme(meetItem.getMeet_theme());
                            meet4.setMeet_size(meetItem.getMeet_size());
                            meet4.setMeet_limit(meetItem.getMeet_limit());
                            m4List.add(meet4);
                            break;
                    }
                }
                int m1_length,m2_length,m3_length,m4_length;
                /**家庭会议**/
                m1_length=m1List.size();
                Meet mt1=new Meet();
                Log.v("m1_length"," "+m1_length);
                for(int n=0;n<m1_length;n++){
                    if(n%2==0&&n<m1_length-1){
                        Log.v("我添加了","数据");
                        mt1=new Meet();
                        mt1.setLeftTitle(m1List.get(n).getMeet_theme());
                        mt1.setLeftSize(m1List.get(n).getMeet_size());
                        mt1.setLeftLimit(m1List.get(n).getMeet_limit());
                    }else if(n%2==0&&n==m1_length-1){
                        mt1=new Meet();
                        mt1.setLeftSize(m1List.get(n).getMeet_size());
                        mt1.setLeftLimit(m1List.get(n).getMeet_limit());
                        mt1.setLeftTitle(m1List.get(n).getMeet_theme());
                        m1.add(mt1);
                    }else if(n%2!=0){
                        mt1.setRightTitle(m1List.get(n).getMeet_theme());
                        mt1.setRightSize(m1List.get(n).getMeet_size());
                        mt1.setRightLimit(m1List.get(n).getMeet_limit());
                        m1.add(mt1);
                    }
                }
                /**家庭会议**/

                /**企业会议**/
                m2_length=m2List.size();
                Meet mt2=new Meet();
                Log.v("m2_length"," "+m2_length);
                for(int n=0;n<m2_length;n++){
                    if(n%2==0&&n<m2_length-1){
                        mt2=new Meet();
                        mt2.setLeftTitle(m2List.get(n).getMeet_theme());
                        mt2.setLeftSize(m2List.get(n).getMeet_size());
                        mt2.setLeftLimit(m2List.get(n).getMeet_limit());
                    }else if(n%2==0&&n>=m2_length-1){
                        mt2=new Meet();
                        mt2.setLeftTitle(m2List.get(n).getMeet_theme());
                        mt2.setLeftSize(m2List.get(n).getMeet_size());
                        mt2.setLeftLimit(m2List.get(n).getMeet_limit());
                        m2.add(mt2);
                    }else if(n%2!=0){
                        mt2.setRightSize(m2List.get(n).getMeet_size());
                        mt2.setRightTitle(m2List.get(n).getMeet_theme());
                        mt2.setRightLimit(m2List.get(n).getMeet_limit());
                        m2.add(mt2);
                    }
                }
                /**企业会议**/

                /**主题会议**/
                m3_length=m3List.size();
                Meet mt3=new Meet();
                Log.v("m3_length"," "+m3_length);
                for(int n=0;n<m3_length;n++){
                    if(n%2==0&&n<m3_length-1){
                        mt3=new Meet();
                        mt3.setLeftSize(m3List.get(n).getMeet_size());
                        mt3.setLeftTitle(m3List.get(n).getMeet_theme());
                        mt3.setLeftLimit(m3List.get(n).getMeet_limit());
                    }else if(n%2==0&&n>=m3_length-1){
                        mt3=new Meet();
                        mt3.setLeftTitle(m3List.get(n).getMeet_theme());
                        mt3.setLeftSize(m3List.get(n).getMeet_size());
                        mt3.setLeftLimit(m3List.get(n).getMeet_limit());
                        m3.add(mt3);
                    }else if(n%2!=0){
                        mt3.setRightTitle(m3List.get(n).getMeet_theme());
                        mt3.setRightLimit(m3List.get(n).getMeet_limit());
                        mt3.setRightSize(m3List.get(n).getMeet_size());
                        m3.add(mt3);
                    }
                }
                /**主题会议**/

                /**其他会议**/
                m4_length=m4List.size();
                Meet mt4=new Meet();
                Log.v("m4_length"," "+m4_length);
                int l4=m4_length-1;
                for(int n=0;n<m4_length;n++){
                    Log.v("n   "," "+" "+(n%2==0&&n>=l4));
                    if(n%2==0&&n<l4){
                        mt4=new Meet();
                        mt4.setLeftSize(m4List.get(n).getMeet_size());
                        mt4.setLeftTitle(m4List.get(n).getMeet_theme());
                        mt4.setLeftLimit(m4List.get(n).getMeet_limit());
                    }else if(n%2==0&&n>=l4){
                        Log.v("我有执行到这","+++++++");
                        mt4=new Meet();
                        mt4.setLeftTitle(m4List.get(n).getMeet_theme());
                        mt4.setLeftSize(m4List.get(n).getMeet_size());
                        mt4.setLeftLimit(m4List.get(n).getMeet_limit());
                        m4.add(mt4);
                    }else if(n%2!=0){
                        mt4.setRightSize(m4List.get(n).getMeet_size());
                        Log.v("会议规模"," "+m4List.get(n).getMeet_size());
                        mt4.setRightLimit(m4List.get(n).getMeet_limit());
                        mt4.setRightTitle(m4List.get(n).getMeet_theme());
                        m4.add(mt4);
                    }
                }
                /**其他会议**/
                expandableListView=join_pager.findViewById(R.id.type);
                expandableListView.setAdapter(getMeetAdapter());
                /**将可扩展列表添加到RecycleView**/
                Log.v("--------","我添加了视图");
            }
        };
    }
    public void initTabViewPager(){
        vp_content=findViewById(R.id.room_vp);
        myViewList=new ArrayList<>();
        join_pager=View.inflate(this,R.layout.meet_join,null);
        build_pager=View.inflate(this,R.layout.meet_build,null);
        vp_listener=new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }
            @Override
            public void onPageSelected(int position) {
                tabTitle.getTabAt(position).select();
            }
            @Override
            public void onPageScrollStateChanged(int i) {
            }
        };
        initRecycleView();
        myViewList.add(join_pager);
        myViewList.add(build_pager);
        RoomPagerAdapter roomPagerAdapter=new RoomPagerAdapter(this,myViewList);
        vp_content.setAdapter(roomPagerAdapter);
        vp_content.setCurrentItem(0);
        vp_content.addOnPageChangeListener(vp_listener);
    }
    final String meetServerUrl="http://192.168.191.1:8080/getMeets";
    private Runnable listUpdate;
    private final Handler listHandler=new Handler();
    private List<Meet_> m1List=new ArrayList<>();
    private List<Meet_> m2List=new ArrayList<>();
    private List<Meet_> m3List=new ArrayList<>();
    private List<Meet_> m4List=new ArrayList<>();
    private int roomListLength;
    private JSONObject meetjsb;
    private JSONArray jsonArray;
    private List<Meet> r_meets=new ArrayList<>();
    public void getMeetList(){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                        .url(meetServerUrl)
                        .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("获取会议室列表","获取会议室列表失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String rspStr = response.body().string();
                    try {
                        jsonArray = new JSONArray(rspStr);
                        roomListLength = jsonArray.length();
                        Log.v("roomListLength",""+roomListLength);
                        /**极有可能是网络延迟导致的数据问题,通过typeUpdate嵌套递归postDelayed()解决**/
                        for(int i=0;i<roomListLength;i++){
                            meetjsb = jsonArray.getJSONObject(i);
                            Meet_ meet_=new Meet_();
                            meet_.setMeet_theme(meetjsb.getString("meet_theme"));
                            Log.v("meetjsb ",i+" "+meetjsb.getString("meet_theme"));
                            meet_.setMeet_size(meetjsb.getInt("total_size"));
                            meet_.setMeet_type(meetjsb.getInt("meet_type"));
                            meet_.setMeet_limit("允许申请");
                            meetList_.add(meet_);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }
        });
    }

    private final String roomServerUrl="http://192.168.191.1:8080/getRoomType";
    private int room_type;
    private String meet_theme=null;
    private int meet_size=0;
    private List<Meet_> meetList_=new ArrayList<>();
    private Meet_ m_;
    private  int m=0;
   public void getRoomType(JSONObject meetjsb){
        OkHttpClient client=new OkHttpClient();
       String room_id=null ;
        m_=new Meet_();
       try {
            meet_theme = meetjsb.get("meet_theme").toString();
            meet_size = (Integer) meetjsb.get("total_size");
             room_id = meetjsb.get("meet_room").toString();
       } catch (JSONException e) {
           e.printStackTrace();
       }

        MediaType JSON=MediaType.parse("application/json;charset=utf-8");
        JSONObject jsbRoom=new JSONObject();
       try {
           jsbRoom.put("room_id",room_id);
       } catch (JSONException e) {
           e.printStackTrace();
       }
       RequestBody requestBody=RequestBody.create(JSON,jsbRoom.toString());
       final Request request=new Request.Builder()
                        .url(roomServerUrl)
                        .post(requestBody)
                        .build();
       client.newCall(request).enqueue(new Callback() {
           @Override
           public void onFailure(Call call, IOException e) {
               Log.v("获取会议室类型","获取会议室类型失败");
           }

           @Override
           public void onResponse(Call call, Response response) throws IOException {
            if(response.isSuccessful()){
                m_.setMeet_theme(meet_theme);
                m_.setMeet_size(meet_size);
                String rspStr=response.body().string();
                try {
                    JSONObject room_type_jsb=new JSONObject(rspStr);
                    room_type=room_type_jsb.getInt("room_type");
                    m++;
                    Log.v("m",""+m);
                    m_.setMeet_type(room_type);
                    m_.setMeet_limit("允许申请");
                    meetList_.add(m_);
                    Log.v("meetList_",""+meetList_.size());
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
        setContentView(R.layout.activity_room_select);
        initView();
    }
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        vp_content.setCurrentItem(tab.getPosition());
    }
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }
    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }
    public void initLeftMenu(){
        userHead=findViewById(R.id.user_head);
        userHead.setImageResource(R.drawable.user);
        drawly=findViewById(R.id.rs_box);
        drawly.setScrimColor(Color.TRANSPARENT);
        mLeftMenu=findViewById(R.id.lv_left);
        setData();
        mLeftMenu.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mLeftMenu.setDivider(null);
        mLeftMenu.setAdapter(leftAdapter);
    }
    public void initRightMenu(){
        mRightMenu=findViewById(R.id.lv_right);
        setData();
        mRightMenu.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mRightMenu.setAdapter(rightAdapter);
    }
    public void setData(){
        Option optionBuild =new Option(R.drawable.ic_build,"创建的会议室");
        Option optionJoin=new Option(R.drawable.ic_join,"加入的会议室");
        Option optionAbout=new Option(R.drawable.ic_about,"关于");
        List<Option> options=new ArrayList<Option>();
        options.add(optionBuild);
        options.add(optionJoin);
        options.add(optionAbout);
        MenuAdapter data_adapter=new MenuAdapter(options,this);
        //data_adapter .setDropDownViewResource(R.layout.list_item);
        leftAdapter=(ListAdapter)data_adapter;
        rightAdapter=(ListAdapter)data_adapter;
    }
}

class Option{
    private int chioceImage;
    private String optionStr;
    public Option(int var1,String var2){
        this.chioceImage=var1;
        this.optionStr=var2;
    }

    public void setChioceImage(int chioceImage) {
        this.chioceImage = chioceImage;
    }

    public int getChioceImage() {
        return chioceImage;
    }

    public void setOptionStr(String optionStr) {
        this.optionStr = optionStr;
    }

    public String getOptionStr() {
        return optionStr;
    }
}
class MenuAdapter extends BaseAdapter {
    private List<Option> options;
    private Context parentContext;
    public MenuAdapter(List<Option> var1, Context var2){
        this.options=var1;
        this.parentContext=var2;
    }
    @Override
    public int getCount() {
        return options.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder=new ViewHolder();
        if(convertView==null){
            convertView=View.inflate(parentContext,R.layout.list_item,null);
            viewHolder.optionBox=convertView.findViewById(R.id.option_box);
            viewHolder.optionIcon=convertView.findViewById(R.id.option_ico);
            viewHolder.optionTxt=convertView.findViewById(R.id.option_txt);
            convertView.setTag(viewHolder);
        }else{
            viewHolder=(ViewHolder) convertView.getTag();
        }
        RelativeLayout.LayoutParams boxParams=new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,200);
        RelativeLayout.LayoutParams iconParams=new RelativeLayout.LayoutParams(80,80);
        iconParams.setMargins(100,60,0,60);
        viewHolder.optionBox.setLayoutParams(boxParams);
        viewHolder.optionIcon.setLayoutParams(iconParams);
        viewHolder.optionIcon.setImageResource(options.get(position).getChioceImage());
        viewHolder.optionTxt.setText(options.get(position).getOptionStr());
        viewHolder.optionBox.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                switch(position) {
                    case 0:
                        Log.v("----------", "点击了build键");
                        break;
                    case 1:
                        Log.v("----------", "点击了join键");
                        break;
                    case 2:
                        Log.v("----------", "点击了about键");
                        break;
                }

                /**设置栏目相应点击事件**/
            }
        });
        return convertView;
    }
    static class ViewHolder{
        private RelativeLayout optionBox;
        private ImageView optionIcon;
        private TextView optionTxt;
    }
}
class RoomPagerAdapter extends PagerAdapter{
    private Context context;
    private List<View> mViewList;
    public RoomPagerAdapter(Context context,List<View> viewList){
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
/**
 class RoomListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
 private List<ExpandableListView> types;
 private MeetAdapter meetAdapter;
 public RoomListAdapter(List<ExpandableListView> types,MeetAdapter meetAdapter){
 this.types=types;
 this.meetAdapter=meetAdapter;
 }
 static class mViewHolder extends RecyclerView.ViewHolder {
 private ExpandableListView meetBox;
 public mViewHolder(View v) {
 super(v);
 this.meetBox=v.findViewById(R.id.type);
 }
 }
 @Override
 public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
 View view=LayoutInflater.from(viewGroup.getContext()).inflate
 (R.layout.type_view,viewGroup,false);
 return new mViewHolder(view);
 }

 @Override
 public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
 ExpandableListView mType=types.get(position);
 mViewHolder vh= (mViewHolder) viewHolder;
 //new MeetAdapter(typeList,meetList);
 vh.meetBox.setAdapter(meetAdapter);
 }

 @Override
 public int getItemCount() {
 return types.size();
 }
 }**/

/***用于整合服务器获取的会议数据进行过渡***/
class Meet_{
    private String meet_theme;
    private int meet_size;
    private String meet_limit;
    private int meet_type;

    public void setMeet_theme(String meet_theme) {
        this.meet_theme = meet_theme;
    }

    public String getMeet_theme() {
        return meet_theme;
    }

    public void setMeet_size(int meet_size) {
        this.meet_size = meet_size;
    }

    public int getMeet_size() {
        return meet_size;
    }

    public void setMeet_limit(String meet_limit) {
        this.meet_limit = meet_limit;
    }

    public String getMeet_limit() {
        return meet_limit;
    }

    public void setMeet_type(int meet_type) {
        this.meet_type = meet_type;
    }

    public int getMeet_type() {
        return meet_type;
    }
}
class Meet{
    private String rightTitle,leftTitle;
    private int rightSize,leftSize;
    private String rightLimit,LeftLimit;

    public void setRightTitle(String rightTitle) {
        this.rightTitle = rightTitle;
    }

    public void setLeftTitle(String leftTitle) {
        this.leftTitle = leftTitle;
    }

    public void setRightSize(int rightSize) {
        this.rightSize = rightSize;
    }

    public void setLeftSize(int leftSize) {
        this.leftSize = leftSize;
    }

    public void setRightLimit(String rightLimit) {
        this.rightLimit = rightLimit;
    }

    public void setLeftLimit(String leftLimit) {
        LeftLimit = leftLimit;
    }

    public String getRightTitle() {
        return rightTitle;
    }

    public String getLeftTitle() {
        return leftTitle;
    }

    public int getLeftSize() {
        return leftSize;
    }

    public int getRightSize() {
        return rightSize;
    }

    public String getRightLimit() {
        return rightLimit;
    }

    public String getLeftLimit() {
        return LeftLimit;
    }
}
class MeetAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> typeNames;
    private List<List<Meet>> meets;

    public MeetAdapter(Context context,List<String> typeNames,List<List<Meet>> meets){
        this.context=context;
        this.typeNames=typeNames;
        this.meets=meets;
    }
    @Override
    public int getGroupCount() {
        return typeNames.size();
    }

    @Override
    public int getChildrenCount(int position) {
        return meets.get(position).size();
    }

    @Override
    public Object getGroup(int position) {
        return typeNames.get(position);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return meets.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int position) {
        return position;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int position, boolean isExpand, View convertView, ViewGroup
            viewGroup) {
        GroupHolder groupHolder;
        if(convertView==null){
            groupHolder=new GroupHolder();
            convertView=LayoutInflater.from(viewGroup.getContext()).inflate
                    (R.layout.group_item,viewGroup,false);
            groupHolder.tvType=convertView.findViewById(R.id.label_group_normal);
            convertView.setTag(groupHolder);
        }else{
            groupHolder= (GroupHolder) convertView.getTag();
        }
        groupHolder.tvType.setText(typeNames.get(position));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View
            convertView, ViewGroup viewGroup) {
        ChildHolder childHolder;
        if(convertView==null){
            childHolder=new ChildHolder();
            convertView=LayoutInflater.from(viewGroup.getContext()).inflate
                    (R.layout.meet_view,viewGroup,false);
            childHolder.meets=convertView.findViewById(R.id.meet_box);
            childHolder.left_box=convertView.findViewById(R.id.left_box);
            childHolder.right_box=convertView.findViewById(R.id.right_box);
            childHolder.meet_space=convertView.findViewById(R.id.meet_space);
            childHolder.left_title=convertView.findViewById(R.id.left_title);
            childHolder.right_title=convertView.findViewById(R.id.right_title);
            childHolder.left_size=convertView.findViewById(R.id.left_size);
            childHolder.right_size=convertView.findViewById(R.id.right_size);
            childHolder.right_limit=convertView.findViewById(R.id.right_limit);
            childHolder.left_limit=convertView.findViewById(R.id.left_limit);
            convertView.setTag(childHolder);
        }else{
            childHolder= (ChildHolder) convertView.getTag();
        }
        int size=meets.get(groupPosition).get(childPosition).getRightSize();
        String  title=meets.get(groupPosition).get
        (childPosition).getRightTitle();
        /**用ViewHolder改变样式时，若其他项没有设置该样式，则会默认全局使用该样式**/
        if(title==null){
            childHolder.left_title.setText(meets.get(groupPosition).get(childPosition).getLeftTitle
                    ());

            childHolder.left_size.setText(String.valueOf(meets.get(groupPosition).get(childPosition).getLeftSize
                    ()));

            childHolder.left_limit.setText(meets.get(groupPosition).get(childPosition).getLeftLimit
                    ());

            childHolder.right_box.setVisibility(View.INVISIBLE);
           return convertView;
        }
            childHolder.right_box.setVisibility(View.VISIBLE);
            childHolder.left_title.setText(meets.get(groupPosition).get(childPosition).getLeftTitle
                    ());
            childHolder.right_title.setText(meets.get(groupPosition).get
                    (childPosition).getRightTitle());
            childHolder.left_size.setText(String.valueOf(meets.get(groupPosition).get(childPosition).getLeftSize
                    ()));
            childHolder.right_size.setText(String.valueOf(meets.get(groupPosition).get(childPosition).getRightSize
                    ()));
            childHolder.left_limit.setText(meets.get(groupPosition).get(childPosition).getLeftLimit
                    ());
            childHolder.right_limit.setText(meets.get(groupPosition).get
                    (childPosition).getRightLimit());

        return convertView;
    }
    static class GroupHolder{
        TextView tvType;
    }
    static class ChildHolder{
        LinearLayout meets,right_box,left_box;
        RelativeLayout meet_space;
        TextView right_title,left_title;
        TextView  right_size,left_size;
        TextView right_limit,left_limit;
    }
    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
