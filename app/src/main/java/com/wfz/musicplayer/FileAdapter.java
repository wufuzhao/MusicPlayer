package com.wfz.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wufuzhao on 2016/10/19.
 */
public class FileAdapter extends BaseAdapter {
    List<File> allFiles = new ArrayList<File>();
    Context mainActivity;

    public FileAdapter(List<File> allFiles, Context mainActivity) {
        super();
        this.allFiles = allFiles;
        this.mainActivity = mainActivity;
    }

    @Override
    public int getCount() {
        return allFiles.size();
    }

    @Override
    public Object getItem(int arg0) {
        return allFiles.get(arg0);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mainActivity);
        if (convertView == null) {

            convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }
        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        textView.setTextSize(20);
        textView.setText(allFiles.get(position).getName());
        return convertView;
    }

}