package com.example.lbstest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.example.lbstest.gson.cityId;
import com.example.lbstest.gson.cityInfo_basic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/*
利用百度地图的定位功能，得出经纬度
根据经纬度，利用和风天气的城市搜索API（https://dev.heweather.com/docs/search/find）能够得出和风天气API中城市的特定代号
根据这个城市代号，可以直接向和风天气的天气API（https://dev.heweather.com/docs/api/weather）请求该城市的天气信息
Create by Lalmon.
4/18/2019
 */

public class MainActivity extends AppCompatActivity {


    public LocationClient mLocationClient;
    private Button startLBS;//用于点击进行点位信息输出
    private TextView CityID_show;//向服务器请求回来的城市ID展示


    public double LongitudeId;//经度
    public double LatitudeId;//纬度
    public String CityID;//根据经纬度得出的城市ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        定位监听器，一旦调用requestLocation()函数，就会触发MyLocationListener()
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        setContentView(R.layout.activity_main);

        startLBS=(Button) findViewById(R.id.startLBS);
        CityID_show=(TextView)findViewById(R.id.position_text_content);
        List<String> permissionList=new ArrayList<>();//用于把3个权限的判断生成list传递出去判断
//        权限判断
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED)
        {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
        {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty())
        {
            String [] permissions=permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }
        else
        {
          requestLocation();
        }

        //        点击定位按钮监听器
        startLBS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //所有权限都开启之后，进行定位
                requestLocation();
                CityID_show.setText(CityID);
            }
        });
    }

    //    定位函数
    private void requestLocation()
    {
        initLocation();//定位初始化
        mLocationClient.start();//开始定位
    }
    //    定位初始化
    private void initLocation()
    {
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(5000);//每隔5s刷新一下
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);//开启GPS定位
        mLocationClient.setLocOption(option);
    }


//    请求权限小窗口
    @Override
    public void onRequestPermissionsResult(int requestCode,String [] permissions, int [] grantResults)
    {
        switch (requestCode)
        {
            case 1:
                if (grantResults.length>0)
                {
                    for (int result:grantResults)
                    {
                        if (result != PackageManager.PERMISSION_GRANTED)
                        {
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();//权限同意之后进行定位
                }else
                {
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
                default:
        }
    }

//    定位结果信息赋值
    public class MyLocationListener implements BDLocationListener{
        @Override
    public void onReceiveLocation(BDLocation location)
        {
            LongitudeId=location.getLongitude();//经度获取
            LatitudeId=location.getLatitude();//纬度获取
            requestCityInfo(LongitudeId,LatitudeId);//根据经纬度，请求服务器
        }

    }

//    用经纬度向服务器请求获取城市json
    public void requestCityInfo(double longitude,double latitude)
    {
        String cityUrl="https://search.heweather.net/find?location="+longitude+","+latitude+"&key=8e669fb35db1436496ad76e9aec7ba60";
        System.out.println("请求链接："+cityUrl);

        HttpUtil.sendOkHttpRequest(cityUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseCityInfoText=response.body().string();
                System.out.println("返回的信息："+responseCityInfoText);
//                把返回的数据交到Utility进行Gson解析
                cityId CITYID=new cityId();
                CITYID=Utility.handleCityIdResponse(responseCityInfoText);
                for (cityInfo_basic basic:CITYID.basicsList)
                {
                    /*
                    根据当前经纬度得出的城市的ID，可利用该ID直接向和风天气API请求该城市的天气信息
                     */
                    CityID=basic.cityID;
                }
                System.out.println("最后的一步，ID："+CityID);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"经纬度请求城市信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

}
