package com.example.czw.downloaddemo.model;

/**
 * 线程信息
 * Created by czw on 2017/4/9.
 */

public class ThreadInfo {
    private int id;
    private String url;
    private int finished;
    private int start;
    private int end;

    public ThreadInfo() {
    }

    public ThreadInfo(int id, String url, int finished, int start, int end) {
        this.id = id;
        this.url = url;
        this.finished = finished;
        this.start = start;
        this.end = end;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getFinished() {
        return finished;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "ThreadInfo{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", finished=" + finished +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
