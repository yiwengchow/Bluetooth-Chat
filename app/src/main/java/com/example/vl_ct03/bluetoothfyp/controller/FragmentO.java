package com.example.vl_ct03.bluetoothfyp.controller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.vl_ct03.bluetoothfyp.R;

public class FragmentO extends Fragment {
    public static FragmentO newInstance() {
        FragmentO fragment = new FragmentO();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_o, container, false);
    }
}
