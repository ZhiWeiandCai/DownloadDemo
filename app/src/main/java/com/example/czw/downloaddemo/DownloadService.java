package com.example.czw.downloaddemo;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.example.czw.downloaddemo.model.FileInfo;
import com.example.czw.downloaddemo.task.DownloadTask;
import com.example.czw.downloaddemo.util.LogHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    public static final String DownloadStart = "download_start";
    public static final String DownloadStop = "download_stop";
    public static final String DownloadUpdate = "download_update";
    public static final String DownloadFinish = "download_finish";
    public static final String DownloadFile_dir_path = Environment.getExternalStorageDirectory().
            toString();
    public static final String DownloadFile_c_path = "/DownloadDemo/";
    public static final String DownloadFile_path = DownloadFile_dir_path +
            DownloadFile_c_path;
    public static final int Msg_InitT = 0;
    Handler mHandler = new DownloadHandler(this);
    private Map<Integer, DownloadTask> mTask = new LinkedHashMap<>();

    public DownloadService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获得activity传来的参数
        if (DownloadStart.equals(intent.getAction())) {
            /*if (mDLTask != null && mDLTask.mPause == true) {
                mDLTask.mPause = false;
                mDLTask.download();
            } else {
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("file_info");
                LogHelper.i(TAG, "start:" + fileInfo.toString());
                new InitThread(fileInfo).start();
            }*/
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("file_info");
            LogHelper.i(TAG, "start:" + fileInfo.toString());
            new InitThread(fileInfo).start();
        } else if (DownloadStop.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("file_info");
            LogHelper.i(TAG, "stop:" + fileInfo.toString());
            DownloadTask dt = mTask.get(fileInfo.getId());
            if (dt != null) {
                dt.mPause = true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //初始化子线程
    class InitThread extends Thread {
        private FileInfo fileInfo;

        public InitThread(FileInfo fileInfo) {
            this.fileInfo = fileInfo;
        }

        @Override
        public void run() {
            HttpURLConnection conn = null;
            RandomAccessFile randomAccessFile = null;
            try {
                URL url = new URL(fileInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");
                int length = -1;
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    length = conn.getContentLength();
                }
                if (length <= 0) {
                    return;
                }
                LogHelper.i(TAG, "DownloadFile_dir_path:" + DownloadFile_dir_path);
                File dir = new File(DownloadFile_path);
                if (!dir.exists()) {
                    boolean temp = dir.mkdirs();
                    LogHelper.i(TAG, "----创建目录" + temp);
                }
                //在本地创建文件
                File file = new File(dir, fileInfo.getFileName());
                randomAccessFile = new RandomAccessFile(file, "rwd");
                randomAccessFile.setLength(length);
                fileInfo.setLength(length);
                mHandler.obtainMessage(Msg_InitT, fileInfo).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                conn.disconnect();
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class DownloadHandler extends Handler {
        private final WeakReference<DownloadService> mService;

        public DownloadHandler(DownloadService downloadService) {
            mService = new WeakReference<>(downloadService);
        }

        @Override
        public void handleMessage(Message msg) {
            DownloadService downloadService = mService.get();
            if (downloadService != null) {
                switch (msg.what) {
                    case Msg_InitT:
                        FileInfo fileInfo = (FileInfo) msg.obj;
                        LogHelper.i(TAG, "Msg_initT:" + fileInfo);

                        DownloadTask task = new DownloadTask(
                                downloadService, fileInfo, 3);
                        task.download();
                        downloadService.mTask.put(fileInfo.getId(), task);
                        break;
                }
            }
        }
    }
}
