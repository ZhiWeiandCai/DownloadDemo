package com.example.czw.downloaddemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.czw.downloaddemo.adapter.FileListAdapter;
import com.example.czw.downloaddemo.model.FileInfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String FileUrl1 = "http://www.imooc.com/mobile/appdown";
    private static final String FileName1 = "mukewang.apk";
    private static final String FileUrl2 = "http://sw.bos.baidu.com/" +
            "sw-search-sp/software/b625da79c2b30/kugou_8.1.45.19805_setup.exe";
    private static final String FileName2 = "kugou_8.1.45.19805_setup.exe";
    private ListView mLv;
    private List<FileInfo> mList;
    private FileListAdapter mFLAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化组件
        mLv = (ListView) findViewById(R.id.lvFileName);
        mList = new ArrayList<>();
        //创建文件信息对象
        FileInfo fileInfo = new FileInfo(0, FileUrl1, FileName1, 0, 0);
        FileInfo fileInfo2 = new FileInfo(1, FileUrl2, FileName2, 0, 0);
        mList.add(fileInfo);
        mList.add(fileInfo2);
        mFLAdapter = new FileListAdapter(this, mList);
        mLv.setAdapter(mFLAdapter);
        //广播解释器
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.DownloadUpdate);
        filter.addAction(DownloadService.DownloadFinish);
        registerReceiver(mBReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBReceiver);
        super.onDestroy();
    }

    BroadcastReceiver mBReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == DownloadService.DownloadUpdate) {
                int finish = intent.getIntExtra("finished", 0);
                int id = intent.getIntExtra("id", 0);
                mFLAdapter.updateProgress(id, finish);
            } else if (intent.getAction() == DownloadService.DownloadFinish) {
                FileInfo fileinfo = (FileInfo) intent.getSerializableExtra("fileinfo");
                //更新进度为100
                mFLAdapter.updateProgress(fileinfo.getId(), 100);
                Toast.makeText(
                        MainActivity.this,
                        fileinfo.getFileName() + "下载完成",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };
}
