package com.example.lbstest;

import com.example.lbstest.gson.cityId;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

public class Utility {
    public static cityId handleCityIdResponse(String response)
    {
        try {
            JSONObject jsonObject=new JSONObject(response);
            JSONArray jsonArray=jsonObject.getJSONArray("HeWeather6");
            return new Gson().fromJson(jsonArray.getJSONObject(0).toString(),cityId.class);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
