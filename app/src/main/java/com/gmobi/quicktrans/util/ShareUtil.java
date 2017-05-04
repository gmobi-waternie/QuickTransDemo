package com.gmobi.quicktrans.util;

import android.content.Context;
import android.content.Intent;

/**
 * Created by water on 2017/5/3.
 */

public class ShareUtil {
    public static  void shareSend(Context context,String url)
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Share From QuickTransDemo");
        intent.putExtra(Intent.EXTRA_TEXT, "url:" + url); // 分享的内容
        context.startActivity(Intent.createChooser(intent, "分享"));
    }
}
