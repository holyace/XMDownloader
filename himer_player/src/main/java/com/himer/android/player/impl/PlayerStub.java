package com.himer.android.player.impl;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import com.himer.android.common.util.HLog;
import com.himer.android.player.Audio;
import com.himer.android.player.ErrorHandler;
import com.himer.android.player.Player;
import com.himer.android.player.PlayerListener;
import com.himer.android.player.constants.PlayerState;
import com.himer.android.player.util.CollectionUtil;

import java.io.IOException;
import java.util.List;

/**
 * No comment for you. yeah, come on, bite me~
 * <p>
 * Created by chad on 2018/11/30.
 */
public class PlayerStub extends Player.Stub implements
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {

    private static final String TAG = PlayerStub.class.getSimpleName();

    private static final int UPDATE_DURATION = 1 * 1000; //1s

    private MiniPlayer mPlayer;
    private List<Audio> mAudioList;
    private int mCurrentIndex = -1;
    private Audio mCurrentAudio;

    private Handler mHandler;

    private PlayerListener mPlayerListener;
    private ErrorHandler mErrorHandle;

    private Runnable mUpdatePositionTask = new Runnable() {
        @Override
        public void run() {
            if (mPlayer == null || mPlayerListener == null) {
                return;
            }
            if (PlayerState.STARTED != mPlayer.getPlayerState()) {
                return;
            }
            int position = mPlayer.getCurrentPosition();
            int duration = mPlayer.getDuration();
            try {
                mPlayerListener.onPositionChange(position, duration);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mHandler.postDelayed(mUpdatePositionTask, UPDATE_DURATION);
        }
    };

    public PlayerStub() {
        mPlayer = new MiniPlayer();
        mHandler = new Handler(Looper.getMainLooper());
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnSeekCompleteListener(this);
        mPlayer.setOnCompletionListener(this);
    }

    @Override
    public void playIndex(int index) {
        if (!CollectionUtil.isIndexInRange(mAudioList, index)) {
            return;
        }
        if (index == mCurrentIndex) {
            play();
            return;
        }
        mCurrentIndex = index;
        Audio audio = mAudioList.get(index);
        mCurrentAudio = audio;
        try {
            pause();
            mPlayer.reset();
            mPlayer.setDataSource(audio.getPath());
            handlePlayChange(index);
            mPlayer.asyncStart();
            handlePlayStart();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setAudioList(List<Audio> audioList) {
        mAudioList = audioList;
        mCurrentAudio = null;
        mCurrentIndex = -1;
    }

    @Override
    public void pause() {
        if (PlayerState.STARTED == mPlayer.getPlayerState()) {
            mPlayer.pause();
            handlePlayPause();
        }
    }

    @Override
    public void play() {
        PlayerState state = mPlayer.getPlayerState();
        if (PlayerState.PREPARED == state ||
                PlayerState.PAUSED == state ||
                PlayerState.COMPLETED == state) {
            mPlayer.start();
            handlePlayStart();
        }
    }

    @Override
    public void stop() {
        PlayerState state = mPlayer.getPlayerState();
        if (PlayerState.PREPARED == state ||
                PlayerState.STARTED == state ||
                PlayerState.PAUSED == state ||
                PlayerState.COMPLETED == state) {
            mPlayer.stop();
            handlePlayStop();
        }
        mCurrentAudio = null;
        mCurrentIndex = -1;
    }

    public void playOrPause() {
        PlayerState state = mPlayer.getPlayerState();
        if (PlayerState.STARTED == state) {
            pause();
        } else if (PlayerState.PAUSED == state ||
                PlayerState.PREPARED == state ||
                PlayerState.COMPLETED == state) {
            play();
        }
    }

    private int getNextIndex() {
        int index = mCurrentIndex + 1;
        if (!CollectionUtil.isIndexInRange(mAudioList, index)) {
            index = 0;
        }
        return index;
    }

    private int getPreviousIndex() {
        int index = mCurrentIndex - 1;
        if (!CollectionUtil.isIndexInRange(mAudioList, index)) {
            index = mAudioList.size() - 1;
        }
        return index;
    }

    @Override
    public void next() {
        if (CollectionUtil.isEmpty(mAudioList)) {
            return;
        }
        playIndex(getNextIndex());
    }

    @Override
    public void previous() {
        if (CollectionUtil.isEmpty(mAudioList)) {
            return;
        }
        playIndex(getPreviousIndex());
    }

    @Override
    public void setMode(int mode) {

    }

    @Override
    public void seekTo(int position) {
        PlayerState state = mPlayer.getPlayerState();
        if (PlayerState.PREPARED == state ||
                PlayerState.STARTED == state ||
                PlayerState.PAUSED == state ||
                PlayerState.COMPLETED == state) {
            mPlayer.seekTo(position);
        }
    }

    @Override
    public int getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public Audio getCurrentAudio() {
        return mCurrentAudio;
    }

    @Override
    public void registePlayerListener(PlayerListener listener) {
        mPlayerListener = listener;
        HLog.e(TAG, "registePlayerListener ", listener);
    }

    @Override
    public void registeErrorHandler(ErrorHandler errorHandler) {
        mErrorHandle = errorHandler;
    }

    private void handlePlayStart() {
        if (mPlayerListener != null) {
            try {
                mPlayerListener.onPlay();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        startUpdatePosition();
    }

    private void handlePlayChange(int index) {
        if (mPlayerListener != null) {
            try {
                mPlayerListener.onPlayChange(index);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePlayPause() {
        if (mPlayerListener != null) {
            try {
                mPlayerListener.onPause();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePlayStop() {
        if (mPlayerListener != null) {
            try {
                mPlayerListener.onStop();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePlayComplete() {
        if (mPlayerListener != null) {
            try {
                mPlayerListener.onComplete();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (mPlayerListener != null) {
            try {
                mPlayerListener.onBufferingChange(percent);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setVolume(float left, float right) {
        mPlayer.setVolume(left, right);
    }

    public PlayerState getPlayerState() {
        if (mPlayer != null) {
            return mPlayer.getPlayerState();
        }
        return null;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    private void startUpdatePosition() {
        stopUpdatePosition();
        mHandler.post(mUpdatePositionTask);
    }

    private void stopUpdatePosition() {
        mHandler.removeCallbacks(mUpdatePositionTask);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handlePlayComplete();
        //play mode
        next();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        boolean handle = false;
        if (mErrorHandle != null) {
            try {
                handle = mErrorHandle.onError(what, extra);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return handle;
    }
}
