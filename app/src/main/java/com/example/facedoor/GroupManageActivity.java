package com.example.facedoor;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.facedoor.base.BaseAppCompatActivity;
import com.example.facedoor.db.DBUtil;
import com.example.facedoor.model.Group;
import com.example.facedoor.ui.DropEditText;
import com.example.facedoor.util.ErrorDesc;
import com.example.facedoor.util.StringUtils;
import com.example.facedoor.util.ToastShow;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.IdentityListener;
import com.iflytek.cloud.IdentityResult;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class GroupManageActivity extends BaseAppCompatActivity implements OnClickListener {
    private final static String TAG = GroupManageActivity.class.getSimpleName();

    private DropEditText mGroupDrop;
    private Toast mToast;
    private ProgressDialog mProDialog;

    private EditText mDBIP;
    private EditText mDoorIP;
    private EditText mDoorContoller;
    private EditText mDoorTime;
    private EditText mPlatformIP;
    private EditText mDoorNum;
    private EditText mDbAgent;
    private EditText mVoiceValue;
    private EditText mDetectTimeValue;
    private String groupId2;

    // 身份验证对象
    private IdentityVerifier mIdVerifier;

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_group_manage;
    }

    @Override
    protected void initViewsAndEvents() {
        Button btnCreate = (Button) findViewById(R.id.btn_create);
        Button btnDelete = (Button) findViewById(R.id.btn_delete);
        Button btnAdd = (Button) findViewById(R.id.btn_add);
        btnCreate.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnAdd.setOnClickListener(this);
        findViewById(R.id.btn_dbip).setOnClickListener(this);
        findViewById(R.id.btn_doorip).setOnClickListener(this);
        findViewById(R.id.btn_dbAgent).setOnClickListener(this);
        findViewById(R.id.btn_doorController).setOnClickListener(this);
        findViewById(R.id.btn_openTime).setOnClickListener(this);
        findViewById(R.id.btn_platformIP).setOnClickListener(this);
        findViewById(R.id.btn_doorNum).setOnClickListener(this);
        findViewById(R.id.btn_voiceValue).setOnClickListener(this);
        findViewById(R.id.btn_detectTimeValue).setOnClickListener(this);


        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        String dbIP = config.getString(MyApp.DBIP_KEY, "");
        String doorip = config.getString(MyApp.DOORIP_KEY, "");
        String doorcontroller = config.getString(MyApp.DOOR_CONTROLLER, "");
        String opentime = config.getString(MyApp.OPEN_TIME, "");
        String platformip = config.getString(MyApp.PLATFORM_IP, "");
        String doornum = config.getString(MyApp.DOOR_NUM, "");
        String dbagent = config.getString(MyApp.DB_AGENT, "");
        String voiceValue = config.getString(MyApp.VOICE_VALUE, "60");
        String detectTimeValue = config.getString(MyApp.DETECT_TIME_VALUE, "30");

        mGroupDrop = (DropEditText) findViewById(R.id.drop_edit);
        mDBIP = (EditText) findViewById(R.id.et_dbip);
        mDoorIP = (EditText) findViewById(R.id.et_doorip);
        mDoorContoller = (EditText) findViewById(R.id.et_doorController);
        mDoorTime = (EditText) findViewById(R.id.et_openTime);
        mPlatformIP = (EditText) findViewById(R.id.et_platformIP);
        mDoorNum = (EditText) findViewById(R.id.et_doorNum);
        mDbAgent = (EditText) findViewById(R.id.et_dbAgent);
        mVoiceValue = (EditText) findViewById(R.id.et_voiceValue);
        mDetectTimeValue = (EditText) findViewById(R.id.et_detectTimeValue);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        if (dbIP != null) {
            mDBIP.setText(dbIP);
        }
        if (mDoorIP != null) {
            mDoorIP.setText(doorip);
        }
        if (mDoorContoller != null) {
            mDoorContoller.setText(doorcontroller);
        }
        if (mDoorTime != null) {
            mDoorTime.setText(opentime);
        }
        if (mPlatformIP != null) {
            mPlatformIP.setText(platformip);
        }
        if (mDoorNum != null) {
            mDoorNum.setText(doornum);
        }
        if (mDbAgent != null) {
            mDbAgent.setText(dbagent);
        }
        if (mVoiceValue != null) {
            mVoiceValue.setText(voiceValue);
        }
        if (mDetectTimeValue != null) {
            mDetectTimeValue.setText(detectTimeValue);
        }

        mProDialog = new ProgressDialog(this);
        // 等待框设置为不可取消
        mProDialog.setCancelable(true);
        mProDialog.setCanceledOnTouchOutside(false);
        mProDialog.setTitle("请稍候");

        mProDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // cancel进度框时,取消正在进行的操作
                if (null != mIdVerifier) {
                    mIdVerifier.cancel();
                }
            }
        });

        mIdVerifier = IdentityVerifier.createVerifier(this, new InitListener() {

            @Override
            public void onInit(int errorCode) {
                if (errorCode == ErrorCode.SUCCESS) {
                    // ToastShow.showTip(mToast, "引擎初始化成功");
                } else {
                    ToastShow.showTip(mToast, "引擎初始化失败。错误码：" + errorCode);
                }
            }
        });
    }


    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.btn_create:
                createGroup();
                break;
            case R.id.btn_delete:
                deleteGroup();
                break;
            case R.id.btn_add:
                //该功能是防止用户卸载程序后，其组也消失了，该入口安全性有待考虑
                addGroup();
                break;
            case R.id.btn_dbip:
                setDBIP();
                break;
            case R.id.btn_doorip:
                setDoorIP();
                break;
            case R.id.btn_doorController:
                setDoorController();
                break;
            case R.id.btn_openTime:
                setOpenTime();
                break;
            case R.id.btn_platformIP:
                setPlatformIP();
                break;
            case R.id.btn_doorNum:
                setDoorNum();
                break;
            case R.id.btn_dbAgent:
                setDbAgent();
                break;
            case R.id.btn_voiceValue:
                setVoiceValue();
                break;
            case R.id.btn_detectTimeValue:
                setDetectTimeValue();
                break;
        }
    }

    private void setDoorNum() {
        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        String strDoorNum = mDoorNum.getText().toString();
        editor.putString(MyApp.DOOR_NUM, strDoorNum);
        editor.commit();
        ToastShow.showTip(mToast, "设置成功");
    }

    private void setDbAgent() {
        verifyIP(mDbAgent, MyApp.DB_AGENT);
    }

    private void setPlatformIP() {
        String platformIP = mPlatformIP.getText().toString();
        Observable.just(platformIP).map(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String arg0) {
                InetAddress addr;
                Boolean reachable = false;
                try {
                    addr = InetAddress.getByName(arg0);
                    reachable = addr.isReachable(3000);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return reachable;
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean reachable) {
                        if (reachable) {
                            SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                            SharedPreferences.Editor editor = config.edit();
                            editor.putString(MyApp.PLATFORM_IP, mPlatformIP.getText().toString());
                            editor.commit();
                            ToastShow.showTip(mToast, "设置成功");
                        } else {
                            ToastShow.showTip(mToast, "无效的IP地址");
                        }
                    }
                });
    }

    private void setOpenTime() {
        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        String strOpenTime = mDoorTime.getText().toString();
        editor.putString(MyApp.OPEN_TIME, strOpenTime);
        editor.commit();
        ToastShow.showTip(mToast, "设置成功");
    }

    private void setDoorController() {
        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        String strDoorController = mDoorContoller.getText().toString();
        editor.putString(MyApp.DOOR_CONTROLLER, strDoorController);
        editor.commit();
        ToastShow.showTip(mToast, "设置成功");
    }

    private void setVoiceValue() {
        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        String voiceValue = mVoiceValue.getText().toString();
        editor.putString(MyApp.VOICE_VALUE, voiceValue);
        editor.commit();
        ToastShow.showTip(mToast, "设置成功");
    }

    private void setDetectTimeValue() {
        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        String voiceValue = mDetectTimeValue.getText().toString();
        editor.putString(MyApp.DETECT_TIME_VALUE, voiceValue);
        editor.commit();
        ToastShow.showTip(mToast, "设置成功");
    }

    private void verifyIP(final EditText ip, final String key) {
        String regEx = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(regEx);
        String strIP = ip.getText().toString();
        Matcher matcher = pattern.matcher(strIP);
        boolean rs = matcher.matches();
        System.out.println(strIP);
        System.out.println(rs);
        if (rs == false) {
            ToastShow.showTip(mToast, "IP地址不正确");
            return;
        }

        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        editor.putString(key, ip.getText().toString());
        editor.commit();
        ToastShow.showTip(mToast, "设置成功");

    }

    @Override
    protected void onResume() {
        super.onResume();
        ArrayList<Group> groups = MyApp.getDBManage(this).getGroups();
        BaseAdapter adp = mGroupDrop.getAdapter();
        mGroupDrop.setList(this, groups);
        if (adp != null) {
            adp.notifyDataSetChanged();
        }

    }

    /**
     * 开启进度条
     */
    private void startProgress(String msg) {
        mProDialog.setMessage(msg);
        mProDialog.show();
        findViewById(R.id.layout_group_manage).setEnabled(false);
    }

    /**
     * 关闭进度条
     */
    private void stopProgress() {
        mProDialog.dismiss();
        findViewById(R.id.layout_group_manage).setEnabled(true);
    }

    private void createGroup() {
        if (StringUtils.isEmpty(mDBIP.getText().toString())) {
            ToastShow.showTip(mToast, "sorry，请先设置数据库IP");
            return;
        }
        ArrayList<String> groupId = MyApp.getDBManage(this).getGroupId();
        if (groupId.size() != 0) {
            ToastShow.showTip(mToast, "sorry，每台机器只能创建一个组");
            return;
        }
        String groupName = mGroupDrop.getText();
        if (StringUtils.isEmpty(groupName)) {
            mGroupDrop.requestFocus();
            ToastShow.showTip(mToast, "sorry，组名不能为空");
            return;
        }

        startProgress("正在创建组...");
        // sst=add，scope=group，group_name=famil;
        // 设置人脸模型操作参数
        // 清空参数
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ipt");
        // 用户id
        mIdVerifier.setParameter(SpeechConstant.AUTH_ID, MyApp.adminId);

        // 设置模型参数，若无可以传空字符传
        StringBuffer params = new StringBuffer();
        // params.append("auth_id=" + MyApp.adminId);
        params.append("scope=group");
        params.append(",group_name=" + groupName);
        // 执行模型操作
        mIdVerifier.execute("ipt", "add", params.toString(), mCreateListener);
    }

    private void addGroup() {
        if (StringUtils.isEmpty(mDBIP.getText().toString())) {
            ToastShow.showTip(mToast, "sorry，请先设置数据库IP");
            return;
        }
        String groupName = mGroupDrop.getText();
        if (StringUtils.isEmpty(groupName)) {
            mGroupDrop.requestFocus();
            ToastShow.showTip(mToast, "sorry，组编号不能为空");
            return;
        }
        startProgress("正在添加组...");
        //人脸识别有结果后，执行保存图片，图片保存完之后，就进行页面的跳转
        groupId2 = mGroupDrop.getText().toString();
        Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> subscriber) {
                DBUtil dbUtil = new DBUtil(GroupManageActivity.this);
                String name = null;
                try {
                    name = dbUtil.queryGroup(groupId2);
                    subscriber.onNext(name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onNext(String groupName) {
                        stopProgress();
                        if (!StringUtils.isEmpty(groupName)) {
                            //插入到数据库中
                            ArrayList<String> groupIds = MyApp.getDBManage(GroupManageActivity.this).getGroupId();
                            if (groupIds != null && groupIds.size() > 0) {
                                if (groupIds.get(0).equals(groupId2)) {
                                    ToastShow.showTip(mToast, "sorry，该鉴别组已建立");
                                    return;
                                }
                            } else {
                                MyApp.getDBManage(GroupManageActivity.this).insertGroup(groupName, groupId2);
                            }
                            //刷新列表适配器
                            ArrayList<Group> groups = MyApp.getDBManage(GroupManageActivity.this).getGroups();
                            Log.e("fyc哈", groups.size() + "");
                            BaseAdapter adp = mGroupDrop.getAdapter();
                            mGroupDrop.setList(GroupManageActivity.this, groups);
                            if (adp != null) {
                                adp.notifyDataSetChanged();
                            }
                        } else {
                            ToastShow.showTip(GroupManageActivity.this, "sorry，该组不存在");
                        }
                    }
                });
    }

    /**
     * 创建组监听器
     */
    private IdentityListener mCreateListener = new IdentityListener() {

        @Override
        public void onResult(IdentityResult result, boolean islast) {
            stopProgress();
            Log.d(TAG, result.getResultString());
            try {
                JSONObject resObj = new JSONObject(result.getResultString());
                final String groupId = resObj.getString("group_id");
                final String groupName = mGroupDrop.getText();
                mGroupDrop.getEditText().setText(groupId);
                //设置光标始终靠右
                if (!StringUtils.isEmpty(mGroupDrop.getEditText().getText().toString())) {
                    CharSequence text = mGroupDrop.getEditText().getText();
                    if (text instanceof Spannable) {
                        Spannable spanText = (Spannable) text;
                        Selection.setSelection(spanText, text.length());
                    }
                }
                ToastShow.showTip(mToast, "组创建成功");
                //插入到数据库中
                MyApp.getDBManage(GroupManageActivity.this).insertGroup(groupName, groupId);
                //刷新列表适配器
                ArrayList<Group> groups = MyApp.getDBManage(GroupManageActivity.this).getGroups();
                BaseAdapter adp = mGroupDrop.getAdapter();
                mGroupDrop.setList(GroupManageActivity.this, groups);
                if (adp != null) {
                    adp.notifyDataSetChanged();
                }
                new Thread() {
                    public void run() {
                        DBUtil dbUtil = new DBUtil(GroupManageActivity.this);
                        dbUtil.insertGroup(groupId, groupName);
                    }
                }.start();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

        @Override
        public void onError(SpeechError error) {
            Log.d(TAG, error.getPlainDescription(true));
            ToastShow.showTip(mToast, ErrorDesc.getDesc(error) + ":" + error.getErrorCode());
            stopProgress();
        }
    };

    private void deleteGroup() {
        String groupId = mGroupDrop.getText();
        if (TextUtils.isEmpty(groupId)) {
            ToastShow.showTip(mToast, "请选择组");
            return;
        }
        /*
         * String groupId = MainActivity.dbManage.queryGroupId(groupName);
		 * if(groupId == null){ ToastShow.showTip(mToast, "组名不存在"); return; }
		 */
        startProgress("正在删除...");

        // sst=add，auth_id=eqhe，group_id=123456，scope=person
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ipt");
        // 用户id
        mIdVerifier.setParameter(SpeechConstant.AUTH_ID, MyApp.adminId);

        // 设置模型参数，若无可以传空字符传
        StringBuffer params2 = new StringBuffer();
        params2.append("scope=group");
        params2.append(",group_id=" + groupId);
        // 执行模型操作
        mIdVerifier.execute("ipt", "delete", params2.toString(), mDeleteListener);
    }

    private IdentityListener mDeleteListener = new IdentityListener() {
        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d(TAG, result.getResultString());
            try {
                JSONObject resObj = new JSONObject(result.getResultString());
                int ret = resObj.getInt("ret");
                if (0 != ret) {
                    onError(new SpeechError(ret));
                    return;
                } else {
                    ToastShow.showTip(mToast, "删除组成功");
                    mGroupDrop.getEditText().setText("");
                    final String groupId = resObj.getString("group_id");
                    MyApp.getDBManage(GroupManageActivity.this).deleteGroup(groupId);
                    ArrayList<Group> groups = MyApp.getDBManage(GroupManageActivity.this).getGroups();
                    BaseAdapter adp = mGroupDrop.getAdapter();
                    mGroupDrop.setList(GroupManageActivity.this, groups);
                    if (adp != null) {
                        adp.notifyDataSetChanged();
                    }
                    new Thread() {
                        public void run() {
                            DBUtil dbUtil = new DBUtil(GroupManageActivity.this);
                            dbUtil.deleteGroup(groupId);
                        }
                    }.start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            stopProgress();
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {

        }

        @Override
        public void onError(SpeechError error) {
            Log.d(TAG, error.getPlainDescription(true));
            ToastShow.showTip(mToast, ErrorDesc.getDesc(error) + ":" + error.getErrorCode());
            stopProgress();
        }
    };

    private void setDBIP() {
        String dbIP = mDBIP.getText().toString();
        Observable.just(dbIP).map(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String arg0) {
                DBUtil dbUtil = new DBUtil(arg0);
                return dbUtil.testConnection();
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean connected) {
                        if (connected) {
                            SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                            SharedPreferences.Editor editor = config.edit();
                            editor.putString(MyApp.DBIP_KEY, mDBIP.getText().toString());
                            editor.commit();
                            ToastShow.showTip(mToast, "设置成功");
                        } else {
                            ToastShow.showTip(mToast, "无效的IP地址");
                        }
                    }
                });
    }

    private void setDoorIP() {
        String doorIP = mDoorIP.getText().toString();
        Observable.just(doorIP).map(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String arg0) {
                InetAddress addr;
                Boolean reachable = false;
                try {
                    addr = InetAddress.getByName(arg0);
                    reachable = addr.isReachable(3000);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return reachable;
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean reachable) {
                        if (reachable) {
                            SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                            SharedPreferences.Editor editor = config.edit();
                            editor.putString(MyApp.DOORIP_KEY, mDoorIP.getText().toString());
                            editor.commit();
                            ToastShow.showTip(mToast, "设置成功");
                        } else {
                            ToastShow.showTip(mToast, "无效的IP地址");
                        }
                    }
                });
    }

    protected void onDestroy() {
        super.onDestroy();
        if (null != mIdVerifier) {
            mIdVerifier.destroy();
            mIdVerifier = null;
        }
    }
}
