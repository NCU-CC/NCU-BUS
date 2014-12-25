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
import android.widget.ListView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
        position = getArguments().getInt("position",0);

        Log.e("debug","Fragement"+position+".OnCreateView()");

        ViewGroup rootView;
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_detail, container, false);


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
        Log.e("debug","Fragement"+position+".OnActivityCraeted()");
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
                try{
                    int topRowVerticalPosition =
                            (listView == null || listView.getChildCount() == 0) ?
                                    0 : listView.getChildAt(0).getTop();
                    swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
                }catch(NullPointerException e){
                    e.printStackTrace();
                }

            }
        });
    }


    public void onStart(){
        super.onStart();
        Log.e("debug","Fragement"+position+".OnStart()");
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
        Log.e("debug","Fragement"+position+".OnResume()");
        new DownloadBusTask().execute();
    }

    public void onPause(){
        super.onStop();
        Log.e("debug","Fragement"+position+".OnPause()");
        handler.removeCallbacks(runnable);
    }

    public void onStop(){
        super.onStop();
        Log.e("debug","Fragement"+position+".OnStop()");
        handler.removeCallbacks(runnable);
    }

    public void onDestroyView (){
        super.onDestroyView();
        Log.e("debug","Fragement"+position+".OnDestoryView()");
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
            calculatePosition();
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
            calculatePosition();
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
                    if(links.size()==0){
                        Log.e("debug","No data retrieved");
                        return;}
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
                try{

                    String url = "http://www.taiwanbus.tw/aspx/dyBus/BusXMLLine.aspx?Mode=4&RunId=8193";
                    HttpClient client = new DefaultHttpClient();
                    HttpGet get = new HttpGet(url);
                    HttpResponse response = client.execute(get);
                    HttpEntity resEntity = response.getEntity();
                    Log.e("debug","request sent");
                    Log.e("debug","ContentLength: "+response.getEntity().getContentLength());


                    if(resEntity.getContentLength() == 0){return;}//if there are no contents, skip all the string processing.

                    String result = EntityUtils.toString(resEntity);
                    //String result = "8193_,1_,281415_,松山機場_,121.55132_,25.06326_,123101|8193_,2_,247146_,台北大學_,121.54174_,25.05793_,103492|8193_,3_,247147_,行天宮_,121.533381_,25.063401_,35222|8193_,6_,247154_,祐民醫院_,121.20372_,24.95706_,119598|8193_,7_,295962_,新明國中_,121.21335_,24.95597_,129209|8193_,8_,247159_,舊社_,121.216327_,24.955388_,123275|8193_,9_,247366_,中壢公車站_,121.22373_,24.95424_,121509|20869@未發車,20721@未發車,20366@未發車,34273@未發車,31691@未發車,34298@未發車,32658@未發車";
                    Log.e("debug","Content: " + result);
                    String[] array = result.split(",");

                    //debug
                    for(int i=0;i<array.length;i++){
                        Log.e("debug","array[" + i + "]: " + array[i]);
                    }

                    ArrayList<String> BusTime = new ArrayList<String>();
                    ArrayList<String> BusStop = new ArrayList<String>();

                    for(int i=0;i<array.length;i++){
                        if(array[i].contains("@")){
                            BusTime.add( array[i].substring(array[i].indexOf("@") + 1,array[i].length()) );
                        }else{
                            if(!array[i].contains("0")&&!array[i].contains("1")&&!array[i].contains("2")&&!array[i].contains("3")&&!array[i].contains("4")&&!array[i].contains("5")&&!array[i].contains("6")&&!array[i].contains("7")&&!array[i].contains("8")&&!array[i].contains("9")){
                                BusStop.add(array[i].replace("_",""));
                            }
                        }
                    }

                    for(int i=0;i<BusStop.size();i++){
                        Log.e("debug","BusStop.get(" + i + "): " + BusStop.get(i));
                        Log.e("debug","BusTime.get(" + i + "): " + BusTime.get(i));
                    }

                    //write data

                    itemList.clear();
                    Data.datas[position].clear();
                    for(int i=0;i<BusStop.size();i++){
                        BusData PData = new BusData();
                        PData.busStop = BusStop.get(i);
                        PData.busTime = BusTime.get(i);
                        itemList.add(PData);
                        Data.datas[position].add(PData);
                    }


                }catch(Exception e){
                    e.printStackTrace();
                }

            /*
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
                }*/
            }
        }else{// if there aren't any active network

            itemList.clear();
            itemList.addAll(Data.datas[position]);
        }


    }

    private void calculatePosition(){
        List<String> providers = locationMgr.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {
            Location l = locationMgr.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {//accuracy數值越小越精準
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return;
        }

        /*
        // 先試試看其他APP提供的位置
        // 取得位置提供者，不下條件，讓系統決定最適用者，true 表示生效的 provider
        String provider = this.locationMgr.getBestProvider(new Criteria(), true);
        if (provider != null) {
            location = this.locationMgr.getLastKnownLocation(provider);
        }
        if(location == null){//假如別的APP提供的位置為空，再試試看用自己的GPS
            location = this.locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Log.e("debug","provider failed to provide location");
        }
        if(location == null){//GPS不行的話，就試試看wifi定位
            location = this.locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Log.e("debug","GPS failed to provide location");
        }
        if(location == null){//全部不行的話，就沒辦法了，直接跳出此函式
            Log.e("debug","Network failed to provide location");
            return;
        }
        */

        Log.e("debug","location.getLatitude()"+bestLocation.getLatitude());

        int nearestBusStop = 0;
        float minDis[] = {0};
        bestLocation.distanceBetween(bestLocation.getLatitude(),bestLocation.getLongitude(),
                Constant.latitudes[position][0],Constant.longitudes[position][0],minDis);

        for(int i=1; i < Constant.latitudes[position].length; i++){
            float temp[] = {0};
            bestLocation.distanceBetween(bestLocation.getLatitude(),bestLocation.getLongitude(),
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
