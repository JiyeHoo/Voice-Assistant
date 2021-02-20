package com.baidu.speech.xunfeiwakeuptest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.xunfeiwakeuptest.tianxing.Tuling;
import com.google.gson.Gson;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SpeechActivity extends AppCompatActivity {

    private final String TAG = "Test";
    private final String resultType = "plain";

    private Button mBtnStart, mBtnTts;
    private TextView mTvShow, mTvAi;
    private SpeechRecognizer mIat;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    private Toast mToast;

    private StringBuffer buffer = new StringBuffer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        setParam();//tts设置
//        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5fddf856");

        //使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);

        mBtnStart = findViewById(R.id.btn_start);
        mBtnTts = findViewById(R.id.btn_tts);
        mTvShow = findViewById(R.id.tv_show);
        mTvAi = findViewById(R.id.tv_ai);
        mTvShow.setText("开始识别...");
        buffer.setLength(0);
        startVoiceWithoutUI();
        mBtnStart.setOnClickListener(v -> {
            mTvShow.setText("开始识别...");
            buffer.setLength(0);
            startVoiceWithoutUI();
        });
        mBtnTts.setOnClickListener(v -> {
            startTts(mTvAi.getText().toString());
        });
    }

    private void startVoiceWithoutUI() {
        if (null == mIat) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        //设置语法ID和 SUBJECT 为空，以免因之前有语法调用而设置了此参数；或直接清空所有参数，具体可参考 DEMO 的示例。
        mIat.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
        mIat.setParameter(SpeechConstant.SUBJECT, null);
        //设置返回结果格式，目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);
        //此处engineType为“cloud”
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
        //设置语音输入语言，zh_cn为简体中文
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        //设置结果返回语言
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
       // 设置语音前端点:静音超时时间，单位ms，即用户多长时间不说话则当做超时处理
        //取值范围{1000～10000}
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        //设置语音后端点:后端点静音检测时间，单位ms，即用户停止说话多长时间内即认为不再输入，
        //自动停止录音，范围{0~10000}
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        //设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");


        //开始识别，并设置监听器
        mIat.startListening(mRecogListener);
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private InitListener mInitListener = code -> {
        Log.d(TAG, "SpeechRecognizer init() code = " + code);
        if (code != ErrorCode.SUCCESS) {
            showTip("初始化失败，错误码：" + code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    };

    private RecognizerListener mRecogListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            //Log.d("result", "返回音频数据："+data.length);
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Log.d("###reslut", "开始说话");
            showTip("开始说话");
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Log.d("###reslut", "结束说话");
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if (resultType.equals("json")) {
                Log.d(TAG, String.valueOf(results));
            }else if(resultType.equals("plain")) {
                Log.d("###reslut", results.getResultString());
                buffer.append(results.getResultString());
                mTvShow.setText(buffer.toString());
                if (!startApp(buffer.toString())) {
                    startAi(buffer.toString());
                }
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            showTip(speechError.getPlainDescription(true));
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
        }
    };

    private void startAi(String msg) {
        Log.d(TAG, "开始请求AI");
        mTvAi.setText("");
//        String url = "http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + msg;
        String url = "http://api.tianapi.com/txapi/tuling/index?key=4712f2e103c88e089f655c53ac3f28ef&question=" + msg;


        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d(TAG, "请求失败");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                Log.d(TAG, "请求成功");
                String string = response.body().string();
                Gson gson = new Gson();
                Tuling tuling = gson.fromJson(string, Tuling.class);
                String reText = tuling.getNewslist().get(0).getReply();
                Log.d(TAG, reText);
                runOnUiThread(() -> {
                    mTvAi.setText(reText.replace("{br}", "\n"));
                    startTts(mTvAi.getText().toString());
                });
            }
        });
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = code -> {
        Log.d(TAG, "InitListener init() code = " + code);
        if (code != ErrorCode.SUCCESS) {
            Log.d(TAG, "初始化失败,错误码："+code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        } else {
            // 初始化成功，之后可以调用startSpeaking方法
            // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
            // 正确的做法是将onCreate中的startSpeaking调用移至这里
        }
    };

    /**
     * 参数设置
     * @return
     */
    private void setParam(){
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);

        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //支持实时音频返回，仅在synthesizeToUri条件下支持
        mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
        //	mTts.setParameter(SpeechConstant.TTS_BUFFER_TIME,"1");

        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "x2_yifei");
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "50");


        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "pcm");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.pcm");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != mTts ){
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {

        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {

        }

        @Override
        public void onSpeakPaused() {

        }

        @Override
        public void onSpeakResumed() {

        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {

        }

        @Override
        public void onCompleted(SpeechError speechError) {

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    private void startTts(String text) {
        mTts.startSpeaking(text, mTtsListener);
    }

    //开启软件
    private boolean startApp(String appName) {
        if ("打开QQ".equals(appName)) {
            PackageManager packageManager = getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage("com.tencent.mobileqq");
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }
}