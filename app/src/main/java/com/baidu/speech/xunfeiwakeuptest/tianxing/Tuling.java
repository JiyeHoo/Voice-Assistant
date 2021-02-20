package com.baidu.speech.xunfeiwakeuptest.tianxing;

import java.util.List;

/**
 * @author JiyeHoo
 * @date 20-12-20 下午10:46
 */
public class Tuling {

    private int code;
    private String msg;
    private List<Newslist> newslist;
    public void setCode(int code) {
        this.code = code;
    }
    public int getCode() {
        return code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
    public String getMsg() {
        return msg;
    }

    public void setNewslist(List<Newslist> newslist) {
        this.newslist = newslist;
    }
    public List<Newslist> getNewslist() {
        return newslist;
    }

}