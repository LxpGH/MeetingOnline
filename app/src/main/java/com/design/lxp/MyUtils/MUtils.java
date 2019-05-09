package com.design.lxp.MyUtils;

import android.content.Context;

public class MUtils {
    public int dip2px(Context context,int dpValue){
        float scale=context.getResources().getDisplayMetrics().density;
        return (int)(dpValue*scale+0.5f);
    }
}
