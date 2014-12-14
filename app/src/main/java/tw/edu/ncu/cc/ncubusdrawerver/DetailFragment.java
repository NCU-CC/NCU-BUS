package tw.edu.ncu.cc.ncubusdrawerver;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;


public class DetailFragment extends Fragment {

    private LocationManager locationMgr;
    private ConnectivityManager conMgr;
    private NetworkInfo activeNetwork;

    private static Handler handler;
    private static Runnable runnable;
    private boolean isAnotherAsyncTaskRunning = false;

    SwipeRefreshLayout swipeRefreshLayout;
    ArrayList<BusData> itemList = new ArrayList<BusData>();
    ListView listView;
    BusAdapter listAdapter;
    int position = 0;

    public static DetailFragment newInstance(int position) {
        DetailFragment myFragment = new DetailFragment();

        Bundle args = new Bundle();
        args.putInt("position", position);
        myFragment.setArguments(args);

        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                        R.layout.fragment_detail, container, false);

        position = getArguments().getInt("position",0);

        //debug
        BusData testData = new BusData();
        testData.busStop = "暫無資料";
        testData.busTime = "暫無資料";
        itemList.add(testData);

        if(Data.datas[position].size()>0){
            itemList.clear();
            itemList.addAll(Data.datas[position]);
        }

        locationMgr = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);

        conMgr = (ConnectivityManager) getActivity().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = conMgr.getActiveNetworkInfo();

        return rootView;
    }

    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        swipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.laySwipe);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new DownloadBusTask().execute();
            }
        });

        listView = (ListView) getView().findViewById(R.id.detailList);
        listAdapter = new BusAdapter(getActivity(),R.layout.bus_item,itemList);
        listView.setAdapter(listAdapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (listView == null || listView.getChildCount() == 0) ?
                                0 : listView.getChildAt(0).getTop();
                swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
            }
        });
    }


    public void onStart(){
        super.onStart();
        new DownloadBusTask().execute();//第一次進來的時候執行
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                new DownloadBusTask().execute();
                handler.postDelayed(this, 30000);//之後每30秒執行一次
            }
        };
    }

    public void onResume(){
        super.onResume();
        new DownloadBusTask().execute();
    }

    public void onPause(){
        super.onStop();
        handler.removeCallbacks(runnable);
    }

    public void onStop(){
        super.onStop();
        handler.removeCallbacks(runnable);
    }

    public void onDestroyView (){
        super.onDestroyView();
        //clean up these stored references by setting
        //them back to null or Activity would be leaked.
        swipeRefreshLayout = null;
        listView = null;
    }

    private class DownloadBusTask extends AsyncTask<Integer, Void, Void> {


        @Override
        protected void onPreExecute(){
            if(isAnotherAsyncTaskRunning){
                this.cancel(true);
            }
            isAnotherAsyncTaskRunning = true;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TypedValue typedValue = new TypedValue();
                    swipeRefreshLayout.setProgressViewOffset(false, 0,
                            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        @Override
        protected Void doInBackground(Integer... params) {
            getBusData(position);
            calculatePosition();//測試中
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... v) {
            if(this.isCancelled()){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            listAdapter.notifyDataSetChanged();
            if(getActivity()!= null){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
            isAnotherAsyncTaskRunning= false;
        }
    }

    private void getBusData(int position){
        if (activeNetwork != null && activeNetwork.isConnected()) {
            Log.e("Downloading", "position" + position);
            if(position<=4){
                try {
                    URL url = new URL(Constant.urls[position]);
                    Document doc = Jsoup.parse(url, 3000);
                    Elements links = doc.select("td.PDA_font1");
                    Log.e("debug", "links.size()=" + links.size());
                    String linkText;
                    itemList.clear();
                    Data.datas[position].clear();
                    for (int i = 0; i < links.size() - 2; i += 3) {
                        final BusData PData = new BusData();
                        Element link = links.get(i + 1);
                        linkText = link.text();
                        PData.busStop = linkText;
                        link = links.get(i + 2);
                        linkText = link.text();
                        PData.busTime = linkText;
                        itemList.add(PData);
                        Data.datas[position].add(PData);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }


            }else{
                try {
                    URL url = new URL(Constant.urls[position]);
                    Document doc = Jsoup.parse(url, 3000);
                    Elements links = doc.select("tbody:has(tr.ttego1) > tr > td");

                    //測試
                    for(int i=0;i<links.size();i++){
                        Log.e("debug","links.get(" + i + ")" + links.get(i).text());
                    }

                    // 實際寫進資料的部分
                    String linkText;
                    itemList.clear();
                    Data.datas[position].clear();
                    for (int i = 3; i < links.size() - 1; i += 2) {
                        BusData PData = new BusData();
                        Element link = links.get(i);
                        linkText = link.text();
                        PData.busStop = linkText;
                        link = links.get(i + 1);
                        linkText = link.text();
                        PData.busTime = linkText;
                        itemList.add(PData);
                        Data.datas[position].add(PData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{// if there aren't any active network
            itemList.clear();
            itemList.addAll(Data.datas[position]);
        }


    }

    private void calculatePosition(){
        // 取得位置提供者，不下條件，讓系統決定最適用者，true 表示生效的 provider
        String provider = this.locationMgr.getBestProvider(new Criteria(), true);
        if (provider == null) {//無可用的provider
            return;
        }
        Location location = this.locationMgr.getLastKnownLocation(provider);
        if(location == null){//沒有位置可以拿
            return;
        }

        Log.e("debug","location.getLatitude()"+location.getLatitude());

        int nearestBusStop = 0;
        float minDis[] = {0};
        location.distanceBetween(location.getLatitude(),location.getLongitude(),
                Constant.latitudes[position][0],Constant.longitudes[position][0],minDis);

        for(int i=1; i < Constant.latitudes[position].length; i++){
            float temp[] = {0};
            location.distanceBetween(location.getLatitude(),location.getLongitude(),
                    Constant.latitudes[position][i],Constant.longitudes[position][i],temp);
            if(minDis[0]>temp[0]){
                minDis[0]=temp[0];
                nearestBusStop = i;
            }
        }

        if(listView != null){
            if(nearestBusStop + 12 < listView.getCount()){
                listView.smoothScrollToPosition(nearestBusStop + 12);
            }else{
                listView.smoothScrollToPosition(listView.getCount()-1);
            }
        }


        Log.e("debug", "nearestBusStop=" + nearestBusStop);
    }


}
