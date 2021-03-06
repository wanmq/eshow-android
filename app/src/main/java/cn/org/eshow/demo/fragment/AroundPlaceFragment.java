package cn.org.eshow.demo.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.org.eshow.demo.R;
import cn.org.eshow.demo.adapter.PlaceAdapter;
import cn.org.eshow.demo.bean.AMapLocationBean;
import cn.org.eshow.demo.bean.AMapLocationListResultBean;
import cn.org.eshow.demo.common.Global;
import cn.org.eshow.demo.network.AMapResponseListener;
import cn.org.eshow.demo.network.NetworkInterface;
import cn.org.eshow.framwork.util.AbJsonUtil;
import cn.org.eshow.framwork.util.AbLogUtil;
import cn.org.eshow.framwork.util.AbToastUtil;
import cn.org.eshow.framwork.util.AbViewUtil;
import jazzylistview.JazzyListView;

/**
 * 地图周边分页
 * Created by daikting on 16/3/23.
 */
public class AroundPlaceFragment extends Fragment implements View.OnClickListener{
    private Context mContext;

    public static final String INTENT_CURRENT_LOCATION = "current.location";

    private String type;
    private String location;
    private JazzyListView listView;
    private TextView tvNoData;
    private List<AMapLocationBean> aroundList;
    private PlaceAdapter placeAdapter;
    private RelativeLayout rlProgress;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                    Bundle savedInstanceState) {

        final Activity parentActivity = this.getActivity();
        mContext = this.getActivity();
        View view = inflater.inflate(R.layout.fragment_aroundplace, null);
        AbViewUtil.scaleContentView((LinearLayout) view.findViewById(R.id.llParent));
        rlProgress = (RelativeLayout) view.findViewById(R.id.rlProgress);
        rlProgress.setVisibility(View.VISIBLE);
        tvNoData = (TextView) view.findViewById(R.id.tvNoData);
        tvNoData.setOnClickListener(this);
        tvNoData.setVisibility(View.GONE);
        listView = (JazzyListView) view.findViewById(R.id.listview);
        listView.setVisibility(View.GONE);
        NetworkInterface.placeAround(mContext, location, type, placeAroundListener);
        aroundList = new ArrayList<AMapLocationBean>();
        placeAdapter = new PlaceAdapter(mContext,aroundList);
        listView.setAdapter(placeAdapter);
        registerBroadcast();
        return view;
    }

    /**
     * 设置请求地址
     * @param type
     * @param location
     */
    public void setAroundInfo(String type,String location){
        this.type = type;
        this.location = location;
    }

    /**
     * 请求高德地图获取周边信息的接口回调
     */
    AMapResponseListener placeAroundListener = new AMapResponseListener(mContext) {

        @Override
        public void onAMapSucess(JSONObject resultJson) {
            AbLogUtil.d(mContext, "resultJson");
            AMapLocationListResultBean aMapLocationListResult = (AMapLocationListResultBean) AbJsonUtil.fromJson(resultJson.toString(), AMapLocationListResultBean.class);
            if(aMapLocationListResult != null){
                aroundList.clear();
                aroundList.addAll(aMapLocationListResult.getPois());
                placeAdapter.notifyDataSetChanged();
            }else{
                rlProgress.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                tvNoData.setVisibility(View.VISIBLE);
                tvNoData.setText("请求失败，点击可以重试！");
                tvNoData.setClickable(true);
            }
        }

        @Override
        public void onAMapNoData() {
            tvNoData.setText("未获取到数据");
            tvNoData.setVisibility(View.VISIBLE);
            tvNoData.setClickable(false);
            listView.setVisibility(View.GONE);
        }

        @Override
        public void onAMapNotify(String notify) {
            AbToastUtil.showToast(mContext, notify);

        }

        @Override
        public void onStart() {
            rlProgress.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            tvNoData.setVisibility(View.GONE);
        }

        @Override
        public void onFinish() {
            rlProgress.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onFailure(int statusCode, String content, Throwable error) {
            rlProgress.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
            tvNoData.setText("请求失败，点击可以重试！");
            tvNoData.setClickable(true);
        }
    };

    /**
     * 注册用于更新界面的广播
     */
    private void registerBroadcast(){
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Global.EShow_Broadcast_Action.ACTION_LOCATION_CHANGED);
        BroadcastReceiver mItemViewListClickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent){
                String action = intent.getAction();
                AbLogUtil.d(mContext, "broadcast action:" + action);

                if(action.equals(Global.EShow_Broadcast_Action.ACTION_LOCATION_CHANGED)){//要求刷新列表
                    location = intent.getStringExtra(INTENT_CURRENT_LOCATION);
                    NetworkInterface.placeAround(mContext, location, type, placeAroundListener);
                }
            }
        };
        broadcastManager.registerReceiver(mItemViewListClickReceiver, intentFilter);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tvNoData:
                NetworkInterface.placeAround(mContext, location, type, placeAroundListener);
                break;
        }
    }
}
