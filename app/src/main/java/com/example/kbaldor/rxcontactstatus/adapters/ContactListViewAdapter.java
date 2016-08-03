package com.example.kbaldor.rxcontactstatus.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kbaldor.rxcontactstatus.R;

import java.util.ArrayList;

public class ContactListViewAdapter extends BaseAdapter {

    ArrayList<UserInformation> marrList;
    ArrayList<String> contactNames;
    private LayoutInflater layoutInflater;
    private Context mContext;
    private Object mutex;

    public ContactListViewAdapter(Context context, ArrayList listData, ArrayList contactNames, Object mutex) {
        this.contactNames = contactNames;
        this.mutex = mutex;
        this.marrList = listData;
        layoutInflater = LayoutInflater.from(context);
        mContext = context;
    }

    public int getCount() {
        synchronized (mutex) {
            return marrList.size();
        }
    }

    @Override
    public Object getItem(int position) {
        synchronized (mutex) {
            return marrList.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {

            convertView = layoutInflater.inflate(R.layout.row_contact, null);

            holder = new ViewHolder();
            holder.txtContactName = (TextView) convertView.findViewById(R.id.txtContactName);
            holder.onlineStatus = (ImageView) convertView.findViewById(R.id.statusIcon);

            holder.deleteIcon = (ImageView) convertView.findViewById(R.id.deleteIcon);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        synchronized (mutex) {

            final UserInformation userInfo = marrList.get(position);
            holder.txtContactName.setText(userInfo.userName);

            holder.deleteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    contactNames.remove(userInfo.getUserName());
                    marrList.remove(position);
                    notifyDataSetChanged();
                }
            });

            if(userInfo.isOnline == true) {
                //holder.onlineStatus.setVisibility(View.VISIBLE);
                holder.onlineStatus.setImageResource(R.drawable.online);
            } else {
                //holder.onlineStatus.setVisibility(View.INVISIBLE);
                holder.onlineStatus.setImageResource(R.drawable.offline);
            }
        }

        return convertView;
    }

    //row datas to viewed in a line recored
    static class ViewHolder {
        protected TextView txtContactName;
        protected ImageView deleteIcon;
        protected ImageView onlineStatus;
    }
}
