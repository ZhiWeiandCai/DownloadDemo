package com.example.czw.downloaddemo.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.czw.downloaddemo.DownloadService;
import com.example.czw.downloaddemo.MainActivity;
import com.example.czw.downloaddemo.R;
import com.example.czw.downloaddemo.model.FileInfo;
import com.example.czw.downloaddemo.util.LogHelper;

import java.util.List;

/**
 * Created by czw on 2017/4/16.
 */

public class FileListAdapter extends BaseAdapter {
    private static final String TAG = "FileListAdapter";
    private Context mContext;
    private List<FileInfo> mList;

    public FileListAdapter(Context mContext, List<FileInfo> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_download,
                    null);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.tvFileName);
            holder.start = (Button) convertView.findViewById(R.id.btStart);
            holder.stop = (Button) convertView.findViewById(R.id.btStop);
            holder.progress = (ProgressBar) convertView.findViewById(R.id.pbProgress);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final FileInfo fileInfo = mList.get(position);
        holder.text.setText(fileInfo.getFileName());
        holder.progress.setMax(100);
        holder.progress.setProgress(fileInfo.getFinished());
        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DownloadService.class);
                intent.setAction(DownloadService.DownloadStart);
                intent.putExtra("file_info", fileInfo);
                mContext.startService(intent);
            }
        });
        holder.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DownloadService.class);
                intent.setAction(DownloadService.DownloadStop);
                intent.putExtra("file_info", fileInfo);
                mContext.startService(intent);
            }
        });

        return convertView;
    }

    /**
     * 更新列表进度
     *
     * @param id       id
     * @param progress 进度
     */
    public void updateProgress(int id, int progress) {
        LogHelper.i("FileListAdapter", "id=" + id);
        FileInfo fileInfo = mList.get(id);
        fileInfo.setFinished(progress);
        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView text;
        Button start;
        Button stop;
        ProgressBar progress;
    }
}
