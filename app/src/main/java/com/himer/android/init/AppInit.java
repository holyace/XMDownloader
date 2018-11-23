package com.himer.android.init;

import android.app.Application;

import com.chad.android.common.service.ServiceConfig;
import com.chad.android.common.service.ServiceManager;
import com.himer.android.Global;

/**
 * No comment for you. yeah, come on, bite me~
 * <p>
 * Created by chad on 2018/11/16.
 */
public class AppInit {

    public static void init(Application app) {

        Global.setApplication(app);

        ServiceConfig config = new ServiceConfig();
        ServiceManager.init(config);
    }
}
