package com.himer.android.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.himer.android.common.util.HLog;
import com.himer.android.player.constants.PlayerConstants;
import com.himer.android.player.impl.NotificationHandler;
import com.himer.android.player.util.CollectionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * No comment for you. yeah, come on, bite me~
 * <p>
 * Created by chad on 2018/11/30.
 */
public class PlayerManager implements PlayerConstants, IPlayer {

    private static final String TAG = PlayerManager.class.getSimpleName();

    private static PlayerManager sInstance;

    private Context mAppCtx;

    private Intent mServiceIntent;

    private IBinder mPlayerStubBinder;
    private Player mPlayer;

    private List<IPlayerListener> mPlayerListenerList;

    private List<Audio> mPlayList;

    private NotificationHandler mNotificationHandler;

    private List<Runnable> mPendingAction = new ArrayList<>();

    private boolean mRemoteDead;
    private boolean mConnecting = true;

    private PlayerListener.Stub mPlayerListener = new PlayerListener.Stub() {

        @Override
        public void onPlay() {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onPlay();
            }
        }

        @Override
        public void onPause() {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onPause();
            }
        }

        @Override
        public void onStop() {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onStop();
            }
        }

        @Override
        public void onComplete() {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onComplete();
            }
        }

        @Override
        public void onPositionChange(int position, int duration) {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onPositionChange(position, duration);
            }
        }

        @Override
        public void onBufferingChange(int bufferPosition) {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onBufferingChange(bufferPosition);
            }
        }

        @Override
        public void onError(int errorCode, String errorMessage) {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onError(errorCode, errorMessage);
            }
        }

        @Override
        public void onPlayChange(int index) {
            for (IPlayerListener l : mPlayerListenerList) {
                l.onPlayChange(index);
            }
        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {

            mRemoteDead = true;

            if (mPlayerStubBinder == null) {
                return;
            }
            mPlayerStubBinder.unlinkToDeath(mDeathRecipient, 0);
            mPlayerStubBinder = null;

            mPlayer = null;

            notifyBinderDead();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mConnecting = false;
            mPlayerStubBinder = service;
            try {
                service.linkToDeath(mDeathRecipient, 0);
                mPlayer = Player.Stub.asInterface(service);
                mPlayer.registePlayerListener(mPlayerListener);
                HLog.e(TAG, "onServiceConnected ", mPlayerListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mRemoteDead = false;

            executePendingAction();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            try {
                mPlayer.registePlayerListener(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mPlayerStubBinder = null;
            mPlayer = null;
        }
    };

    private PlayerManager(Context appCtx) {
        mAppCtx = appCtx.getApplicationContext();
        mServiceIntent = new Intent(ACTION_SERVICE);
        mServiceIntent.setPackage(appCtx.getPackageName());
        mPlayerListenerList = new ArrayList<>();

        mNotificationHandler = new NotificationHandler(mAppCtx);
        addPlayerListener(mNotificationHandler);

        startServiceInternal();

        bindService();
    }

    private void startServiceInternal() {
        mAppCtx.startService(mServiceIntent);
    }

    private void stopServiceInternal() {
        mAppCtx.stopService(mServiceIntent);
    }

    public static PlayerManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PlayerManager.class) {
                if (sInstance == null) {
                    sInstance = new PlayerManager(context);
                }
            }
        }
        return sInstance;
    }

    private void notifyBinderDead() {
        //ignor
    }

    private void bindService() {
        mAppCtx.bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        mAppCtx.unbindService(mConnection);
    }

    @Override
    public void play() {
        if (mPlayer != null) {
            try {
                mPlayer.play();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void playIndex(final int index) {
        if (mPlayer != null) {
            try {
                mPlayer.playIndex(index);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else if (mConnecting) {
            addPendingAction(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer == null) {
                        return;
                    }
                    playIndex(index);
                }
            });
        }
    }


    @Override
    public void setAudioList(final List<Audio> audioList) {
        mPlayList = audioList;
        if (mPlayer != null) {
            try {
                mPlayer.setAudioList(audioList);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else if (mConnecting) {
            addPendingAction(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer == null) {
                        return;
                    }
                    setAudioList(audioList);
                }
            });
        }
    }

    @Override
    public void pause() {
        if (mPlayer != null) {
            try {
                mPlayer.pause();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if (mPlayer != null) {
            try {
                mPlayer.stop();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void next() {
        if (mPlayer != null) {
            try {
                mPlayer.next();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void previous() {
        if (mPlayer != null) {
            try {
                mPlayer.previous();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setMode(int mode) {
        if (mPlayer != null) {
            try {
                mPlayer.setMode(mode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void seekTo(int position) {
        if (mPlayer != null) {
            try {
                mPlayer.seekTo(position);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getDuration() {
        if (mPlayer != null) {
            try {
                return mPlayer.getDuration();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public Audio getCurrentAudio() {
        if (mPlayer != null) {
            try {
                return mPlayer.getCurrentAudio();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void release() {
        if (mPlayer != null) {
            try {
                mPlayer.stop();
                mPlayer.registePlayerListener(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        stopServiceInternal();

        unbindService();
    }

    @Override
    public void addPlayerListener(IPlayerListener listener) {
        mPlayerListenerList.add(listener);
    }

    public Audio getAudio(int index) {
        if (CollectionUtil.isIndexInRange(mPlayList, index)) {
            return mPlayList.get(index);
        }
        return null;
    }

    private void addPendingAction(Runnable runnable) {
        if (!mConnecting || mRemoteDead) {
            return;
        }
        synchronized (mPendingAction) {
            mPendingAction.add(runnable);
        }
    }

    private void executePendingAction() {
        synchronized (mPendingAction) {
            while (!mPendingAction.isEmpty()) {
                Runnable action = mPendingAction.remove(0);
                action.run();
            }
        }
    }
}
