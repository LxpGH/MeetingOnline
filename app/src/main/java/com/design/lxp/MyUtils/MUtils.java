package com.design.lxp.MyUtils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import static java.lang.Long.valueOf;

public class MUtils {
    public int dip2px(Context context,int dpValue){
        float scale=context.getResources().getDisplayMetrics().density;
        return (int)(dpValue*scale+0.5f);
    }

    public int px2dip(Context context,float pxValue){
        float scale=context.getResources().getDisplayMetrics().density;
        return (int)(pxValue/scale+0.5f);
    }

    /***uri转换成path**/
    public String uri2path(Context context,Uri uri ){
        String path=null;
        if(DocumentsContract.isDocumentUri(context,uri)){
            /**documentUri类型**/
            String docId=DocumentsContract.getDocumentId(uri);
            Log.v("docId"," "+docId);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id=docId.split(":")[1];//解析出数字格式
                Log.v("Id"," "+id);
                String selection =MediaStore.Images.Media._ID+"="+id;
                path=getImagePath(context,uri,selection);
            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri= ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(docId));
                path=getImagePath(context,contentUri,null);
            }

        }else if("content".equalsIgnoreCase(uri.getScheme())){
            //如果是普通类型 用普通方法处理
            path=getImagePath(context,uri,null);
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            //如果file类型位uri直街获取图片路径即可
            path=uri.getPath();
        }
        return path;
    }

    public String  getImagePath(Context context,Uri uri,String selection){
        String path=null;
        //通过Uri和selection来获取真实图片路径
        Cursor cursor=context.getContentResolver().query(uri,null,selection,null,null);
        if(cursor!=null){
            if(cursor.moveToFirst()){
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
    /**图片剪切成圆形**/
    public Bitmap getOvalBitmap(Bitmap bitmap,int width,int height) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getWidth(), Bitmap.Config.ARGB_8888);//将图片转换成bitmap
        Canvas canvas = new Canvas(output);//创建画布
        final int color = 0xff424242;
        final Paint paint = new Paint();//创建画刷
        /**由于图片分成width>height和width<height**/
        final Rect rectV = new Rect(0, 0, bitmap.getWidth(),(int)(bitmap.getWidth()*((float)height/(float)width)));//创建剪切区域
        final Rect rectH= new Rect(0, 0,bitmap.getWidth(),bitmap.getWidth());//创建剪切区域
        //final Rect rectH= new Rect(0, 0, (int)(bitmap.getHeight()*((float)width/(float)height)),bitmap.getHeight());//创建剪切区域
        Rect rect=new Rect();
        if(bitmap.getWidth()<bitmap.getHeight()){
            rect=rectV;
        }else if(bitmap.getWidth()>=bitmap.getHeight()){
            rect=rectH;
        }
        RectF rectF=new RectF(rect);

        paint.setAntiAlias(true);//设置成抗锯齿
        canvas.drawARGB(0, 0, 0, 0);//设置画布背景
        paint.setColor(color);
        canvas.drawOval(rectF, paint);//根据矩形区域剪切画圆形
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));//设置画刷的src和dst区域的混合模式
        canvas.drawBitmap(bitmap, rect,rect, paint);//根据参数进行剪切源图片区域并转换成bitmap格式
        return output;
    }
}
