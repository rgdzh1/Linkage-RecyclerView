package com.kunminx.linkage.adapter;
/*
 * Copyright (c) 2018-2019. KunMinX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kunminx.linkage.adapter.viewholder.LinkagePrimaryViewHolder;
import com.kunminx.linkage.contract.ILinkagePrimaryAdapterConfig;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Create by KunMinX at 19/4/29
 */
public class LinkagePrimaryAdapter extends RecyclerView.Adapter<LinkagePrimaryViewHolder> {
    // 组名称集合
    private List<String> mStrings;
    private Context mContext;
    private View mView;
    private int mSelectedPosition;
    // DefaultLinkagePrimaryAdapterConfig对象适配器配置信息
    private ILinkagePrimaryAdapterConfig mConfig;
    // 组名控件被点击之后的回调
    private OnLinkageListener mLinkageListener;

    public List<String> getStrings() {
        return mStrings;
    }

    public ILinkagePrimaryAdapterConfig getConfig() {
        return mConfig;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    /**
     * 更新选中item
     * @param selectedPosition
     */
    public void setSelectedPosition(int selectedPosition) {
        mSelectedPosition = selectedPosition;
        notifyDataSetChanged();
    }

    public LinkagePrimaryAdapter(List<String> strings, ILinkagePrimaryAdapterConfig config, OnLinkageListener linkageListener) {
        mStrings = strings;// 组名称集合
        if (mStrings == null) {
            mStrings = new ArrayList<>();
        }
        mConfig = config;// DefaultLinkagePrimaryAdapterConfig对象适配器配置信息
        mLinkageListener = linkageListener;// 组名控件被点击之后的回调
    }

    /**
     * 更新列表数据
     * @param list
     */
    public void initData(List<String> list) {
        mStrings.clear();
        if (list != null) {
            mStrings.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LinkagePrimaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        // 为DefaultLinkagePrimaryAdapterConfig对象设置mContext.
        mConfig.setContext(mContext);
        // mConfig.getLayoutId(): 返回DefaultLinkagePrimaryAdapterConfig对象中的R.layout.default_adapter_linkage_primary布局ID
        // 获取R.layout.default_adapter_linkage_primary布局对应的View
        mView = LayoutInflater.from(mContext).inflate(mConfig.getLayoutId(), parent, false);
        // LinkagePrimaryViewHolder对象中持有组名View引用以及DefaultLinkagePrimaryAdapterConfig适配器配置对象引用
        return new LinkagePrimaryViewHolder(mView, mConfig);
    }

    @Override
    public void onBindViewHolder(@NonNull final LinkagePrimaryViewHolder holder, int position) {

        // 改变组名View背景为选中状态
        holder.mLayout.setSelected(true);
        // 获取当前组名View对应的索引
        final int adapterPosition = holder.getAdapterPosition();
        // 获取组名称
        final String title = mStrings.get(adapterPosition);
        // 对组名View控件中的内容或者样式进行一些设置.
        mConfig.onBindViewHolder(holder, adapterPosition == mSelectedPosition, title);
        // holder.itemView:代表的是组名View的父控件,可以看作是组名View,也就是设置点击组名View时候的回调.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果初始化主适配器的时候传递了回调对象,那么就执行回调对象中的回调方法.
                if (mLinkageListener != null) {
                    // 在该回调方法中,次Rv相应组的第一条item将被移动到屏幕顶端.
                    mLinkageListener.onLinkageClick(holder, title);
                }
                // DefaultLinkagePrimaryAdapterConfig中的如果有回调也可以被调用.
                mConfig.onItemClick(holder, v, title);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mStrings.size();
    }

    /**
     * only for linkage logic of level primary adapter. not use for outside logic
     * users can archive onLinkageClick in configs instead.
     */
    public interface OnLinkageListener {
        void onLinkageClick(LinkagePrimaryViewHolder holder, String title);
    }
}
