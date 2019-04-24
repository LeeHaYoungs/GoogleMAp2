package com.example.hayoung.googlemap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    String[] permission_list = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    ArrayList<Double> las_list;
    ArrayList<Double> lng_list;
    ArrayList<String> name_list;
    ArrayList<String> vincinty_list;

    ArrayList<Marker> markers_list;

    String[] category_name_array = {
            "모두","ATM","은행","미용실","카페","교회","학교","식당"
    };
    String[] category_value_array = {
            "all","atm","bank","beauty_salon","cafe","church","school","restaurant"
    };


    Location myLocation;
    LocationManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        las_list = new ArrayList<>();
        lng_list = new ArrayList<>();
        name_list = new ArrayList<>();
        vincinty_list = new ArrayList<>();
        markers_list = new ArrayList<>();

        checkPermission();
    }


    public void checkPermission() {
        boolean isGrant = false;
        for (String str : permission_list) {
            if (ContextCompat.checkSelfPermission(this, str) == PackageManager.PERMISSION_GRANTED) {
            } else {
                isGrant = false;
                break;
            }
        }
        if (isGrant == false) {
            ActivityCompat.requestPermissions(this, permission_list, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGrant = true;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                isGrant = false;
                break;
            }
        }
        // 모든 권한을 허용했다면 사용자 위치를 측정한다.
        if (isGrant == true) {
            getMyLocation();
        }
    }

    // 주변 카테고리 리스트
    private void showCategoryList() {
        // 카테고리를 선택 할 수 있는 리스트를 띄운다.
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("장소 타입 선택");
        ArrayAdapter<String> adapter= new ArrayAdapter<String>(
                this,android.R.layout.simple_list_item_1,category_name_array
        );
        DialogListener listener=new DialogListener();
        builder.setAdapter(adapter,listener);
        builder.setNegativeButton("취소",null);
        builder.show();
    }
    // 다이얼로그의 리스너
    class DialogListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            // 사용자가 선택한 항목 인덱스번째의 type 값을 가져온다.
            String type=category_value_array[i];
            // 주변 정보를 가져온다
            getNearbyPlace(type);
        }
    }

    //주변 정보 가져오기
    public void getNearbyPlace(String type_keyword){
        NetworkThread thread=new NetworkThread(type_keyword);
        thread.start();

    }
    //주변 정보 가져오는 스레드
    class NetworkThread extends Thread{
        String type_keyword;
        public NetworkThread(String type_keyword){
            this.type_keyword=type_keyword;
        }
        @Override
        public void run() {
            try{
                //데이터를 담아놓을 리스트를 초기화한다.
                las_list.clear();
                lng_list.clear();
                name_list.clear();
                vincinty_list.clear();

                // 접속할 페이지 주소
                String site="https://maps.googleapis.com/maps/api/place/nearbysearch/json";
                site+="?location="+myLocation.getLatitude()+","
                        +myLocation.getLongitude()
                        +"&radius=1000&sensor=false&language=ko"
                        +"&key=AIzaSyCs-UXo7-EeUIun*******************";
                if(type_keyword!=null && type_keyword.equals("all")==false){
                    site+="&types="+type_keyword;
                }
                // 접속
                URL url=new URL(site);
                URLConnection conn=url.openConnection();
                // 스트림 추출
                InputStream is=conn.getInputStream();
                InputStreamReader isr =new InputStreamReader(is,"utf-8");
                BufferedReader br=new BufferedReader(isr);
                String str=null;
                StringBuffer buf=new StringBuffer();
                // 읽어온다
                do{
                    str=br.readLine();
                    if(str!=null){
                        buf.append(str);
                    }
                }while(str!=null);
                String rec_data=buf.toString();
                // JSON 데이터 분석
                JSONObject root=new JSONObject(rec_data);
                //status 값을 추출한다.
                String status=root.getString("status");
                // 가져온 값이 있을 경우에 지도에 표시한다.
                if(status.equals("OK")){
                    //results 배열을 가져온다
                    JSONArray results=root.getJSONArray("results");
                    // 개수만큼 반복한다.
                    for(int i=0; i<results.length() ; i++){
                        // 객체를 추출한다.(장소하나의 정보)
                        JSONObject obj1=results.getJSONObject(i);
                        // 위도 경도 추출
                        JSONObject geometry=obj1.getJSONObject("geometry");
                        JSONObject location=geometry.getJSONObject("location");
                        double lat=location.getDouble("lat");
                        double lng=location.getDouble("lng");
                        // 장소 이름 추출
                        String name=obj1.getString("name");
                        // 대략적인 주소 추출
                        String vicinity=obj1.getString("vicinity");
                        // 데이터를 담는다.
                        las_list.add(lat);
                        lng_list.add(lng);
                        name_list.add(name);
                        vincinty_list.add(vicinity);
                    }
                    showMarker();
                }
                else{
                    Toast.makeText(getApplicationContext(),"가져온 데이터가 없습니다.",Toast.LENGTH_LONG).show();
                }

            }catch (Exception e){e.printStackTrace();}
        }
    }

    // 지도에 마커를 표시한다
    public void showMarker(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 지도에 마커를 표시한다.
                // 지도에 표시되어있는 마커를 모두 제거한다.
                for(Marker marker : markers_list){
                    marker.remove();
                }
                markers_list.clear();
                // 가져온 데이터의 수 만큼 마커 객체를 만들어 표시한다.
                for(int i= 0 ; i< las_list.size() ; i++){
                    // 값 추출
                    double lat= las_list.get(i);
                    double lng=lng_list.get(i);
                    String name=name_list.get(i);
                    String vicinity=vincinty_list.get(i);
                    // 생성할 마커의 정보를 가지고 있는 객체를 생성
                    MarkerOptions options=new MarkerOptions();
                    // 위치설정
                    LatLng pos=new LatLng(lat,lng);
                    options.position(pos);
                    // 말풍선이 표시될 값 설정
                    options.title(name);
                    options.snippet(vicinity);
                    // 아이콘 설정
                    //BitmapDescriptor icon= BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher);
                    //options.icon(icon);
                    // 마커를 지도에 표시한다.
                    Marker marker= mMap.addMarker(options);
                    markers_list.add(marker);
                }
            }
        });
    }
    // 현재 위치를 가져온다.
    public void getMyLocation() {
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // 권한이 모두 허용되어 있을 때만 동작하도록 한다.
        int chk1 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int chk2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (chk1 == PackageManager.PERMISSION_GRANTED && chk2 == PackageManager.PERMISSION_GRANTED) {
            myLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            showMyLocation();
        }
        // 새롭게 위치를 측정한다.
        GpsListener listener = new GpsListener();
        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, listener);
        }
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, listener);
        }
    }

    // GPS Listener
    class GpsListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            // 현재 위치 값을 저장한다.
            myLocation = location;
            // 위치 측정을 중단한다.
            manager.removeUpdates(this);
            // 지도를 현재 위치로 이동시킨다.
            showMyLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(, );
       // mMap.addMarker(new MarkerOptions().position(sydney).title(""));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void showMyLocation() {

        if (myLocation == null) {
            return;
        }

        double lat = myLocation.getLatitude();
        double lng = myLocation.getAccuracy();

        LatLng position = new LatLng(lat, lng);

        CameraUpdate update1 = CameraUpdateFactory.newLatLng(position);
        mMap.moveCamera(update1);

        CameraUpdate update2 = CameraUpdateFactory.zoomTo(15);
        mMap.animateCamera(update2);

        mMap.setMyLocationEnabled(true);

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public boolean onCreateOptionMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }


    public boolean onOptionsItemselected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.item1:
                getMyLocation();
                break;
            case R.id.item2:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
