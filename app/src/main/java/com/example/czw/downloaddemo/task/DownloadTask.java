package com.example.czw.downloaddemo.task;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.example.czw.downloaddemo.DownloadService;
import com.example.czw.downloaddemo.model.FileInfo;
import com.example.czw.downloaddemo.model.ThreadInfo;
import com.example.czw.downloaddemo.provider.MyDatabaseManager;
import com.example.czw.downloaddemo.util.LogHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by czw on 2017/4/11.
 */

public class DownloadTask {
    private static final String Tag = "DownloadTast";
    private Context mContext;
    private FileInfo mFileInfo;
    private int mFinish;
    public volatile boolean mPause;
    private int mThreadCount = 1;
    private List<DownloadThread> mDThread;

    public DownloadTask(Context context, FileInfo fileInfo, int threadCount) {
        mContext = context;
        mFileInfo = fileInfo;
        mThreadCount = threadCount;
    }

    public void download() {
        LogHelper.i(Tag, "pause:temp=改动");
        List<ThreadInfo> list = new LinkedList<>();
        ThreadInfo threadInfo;
        String selection = MyDatabaseManager.MyDbColumns.NAME + " = ?";
        String[] selectionArgs = {mFileInfo.getUrl()};
        Cursor cursor = mContext.getContentResolver().query(
                MyDatabaseManager.MyDbColumns.CONTENT_URI, null, selection, selectionArgs,
                null);
        int cursorCount = cursor.getCount();
        LogHelper.i(Tag, "-count=" + cursorCount);
        if (cursor == null || cursorCount == 0) {

            //获得每个线程的下载长度
            int length = mFileInfo.getLength() / mThreadCount;
            for (int i = 0; i < mThreadCount; i++) {
                threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), 0, length * i,
                        (i + 1) * length - 1);
                if (i == mThreadCount - 1) {
                    threadInfo.setEnd(mFileInfo.getLength());
                }
                list.add(threadInfo);
            }
        } else {
            cursor.moveToFirst();
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(
                        MyDatabaseManager.MyDbColumns.UID));
                String url = cursor.getString(cursor.getColumnIndexOrThrow(
                        MyDatabaseManager.MyDbColumns.NAME));
                int start = cursor.getInt(cursor.getColumnIndexOrThrow(
                        MyDatabaseManager.MyDbColumns.START));
                int end = cursor.getInt(cursor.getColumnIndexOrThrow(
                        MyDatabaseManager.MyDbColumns.END));
                int finish = cursor.getInt(cursor.getColumnIndexOrThrow(
                        MyDatabaseManager.MyDbColumns.FINISHED));
                threadInfo = new ThreadInfo(id, url, finish, start, end);
                list.add(threadInfo);
                LogHelper.i(Tag, "---一条记录");
            } while (cursor.moveToNext());

        }
        mDThread = new ArrayList<>();
        for (ThreadInfo tInfo:
             list) {
            DownloadThread dt = new DownloadThread(tInfo);
            dt.start();
            mDThread.add(dt);
        }
    }

    private synchronized void checkAllThreadFinish() {
        boolean allFinish = true;
        for (DownloadThread dt : mDThread) {
            if (!dt.isTFinish) {
                allFinish = false;
            }
        }
        if (allFinish) {
            Intent i = new Intent(DownloadService.DownloadFinish);
            i.putExtra("fileinfo", mFileInfo);
            mContext.sendBroadcast(i);
        }
    }

    class DownloadThread extends Thread {
        private ThreadInfo mThreadInfo;
        public boolean isTFinish;

        public DownloadThread(ThreadInfo threadInfo) {
            mThreadInfo = threadInfo;
        }

        public void run() {

            //向数据库插入线程信息
            String selection = MyDatabaseManager.MyDbColumns.UID + " = ? and " +
                    MyDatabaseManager.MyDbColumns.NAME + " = ?";
            String[] selectionArgs = {mThreadInfo.getId() + "", mThreadInfo.getUrl()};
            Cursor cursor = mContext.getContentResolver().query(
                    MyDatabaseManager.MyDbColumns.CONTENT_URI, null, selection, selectionArgs,
                    null);
            if (cursor == null || cursor.getCount() == 0) {
                LogHelper.i(Tag, "----cursor为0条记录");
                ContentValues values = new ContentValues();
                values.put(MyDatabaseManager.MyDbColumns.UID, mThreadInfo.getId());
                values.put(MyDatabaseManager.MyDbColumns.NAME, mThreadInfo.getUrl());
                values.put(MyDatabaseManager.MyDbColumns.START, mThreadInfo.getStart());
                values.put(MyDatabaseManager.MyDbColumns.END, mThreadInfo.getEnd());
                values.put(MyDatabaseManager.MyDbColumns.FINISHED, mThreadInfo.getFinished());
                mContext.getContentResolver().insert(MyDatabaseManager.MyDbColumns.CONTENT_URI,
                        values);
            }

            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            try {
                URL url = new URL(mThreadInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");
                //设置下载位置
                int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + mThreadInfo.getEnd());
                LogHelper.i(Tag, "start-end:" +"bytes=" + start + "-" + mThreadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownloadService.DownloadFile_path, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                Intent intent = new Intent(DownloadService.DownloadUpdate);
                mFinish += mThreadInfo.getFinished();
                //开始下载
                int responseCode = conn.getResponseCode();
                LogHelper.i(Tag, "responseCode:" + responseCode);
                if (responseCode == HttpURLConnection.HTTP_PARTIAL) {

                    InputStream is = conn.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int length = -1;
                    long time = System.currentTimeMillis();
                    while((length = is.read(buffer)) != -1) {

                        raf.write(buffer, 0, length);
                        mFinish += length;
                        mThreadInfo.setFinished(mThreadInfo.getFinished() + length);
                        if (System.currentTimeMillis() - time > 500) {
                            LogHelper.i(Tag, mFinish + "");
                            time = System.currentTimeMillis();
                            intent.putExtra("finished", mFinish * 100 / mFileInfo.getLength());
                            intent.putExtra("id", mFileInfo.getId());
                            mContext.sendBroadcast(intent);
                        }
                        //在下载暂停时保存下载进度
                        if (mPause) {
                            ContentValues values = new ContentValues();
                            String where = MyDatabaseManager.MyDbColumns.UID + " = ? and " +
                                    MyDatabaseManager.MyDbColumns.NAME + " = ?";
                            String[] args = new String[] {mThreadInfo.getId() + "", mThreadInfo
                            .getUrl()};
                            values.put(MyDatabaseManager.MyDbColumns.FINISHED,
                                    mThreadInfo.getFinished());

                            int temp = mContext.getContentResolver().update(MyDatabaseManager
                                    .MyDbColumns.CONTENT_URI, values, where, args);
                            LogHelper.i(Tag, "pause:temp=" + temp);
                            return;
                        }
                    }
                    isTFinish = true;
                    String where = MyDatabaseManager.MyDbColumns.UID + " = ? and " +
                            MyDatabaseManager.MyDbColumns.NAME + " = ?";
                    String[] args = new String[] {mThreadInfo.getId() + "", mThreadInfo
                            .getUrl()};
                    mContext.getContentResolver().delete(MyDatabaseManager
                            .MyDbColumns.CONTENT_URI, where, args);
                    checkAllThreadFinish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null)
                    conn.disconnect();
                if (raf != null)
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
}
