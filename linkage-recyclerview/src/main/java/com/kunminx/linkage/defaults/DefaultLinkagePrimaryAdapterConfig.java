package com.kunminx.linkage.defaults;
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
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.kunminx.linkage.R;
import com.kunminx.linkage.adapter.viewholder.LinkagePrimaryViewHolder;
import com.kunminx.linkage.contract.ILinkagePrimaryAdapterConfig;

/**
 * Create by KunMinX at 19/5/8
 */
public class DefaultLinkagePrimaryAdapterConfig implements ILinkagePrimaryAdapterConfig {

    private static final int MARQUEE_REPEAT_LOOP_MODE = -1;
    private static final int MARQUEE_REPEAT_NONE_MODE = 0;
    private Context mContext;
    private OnPrimaryItemBindListener mListener;
    private OnPrimaryItemClickListner mClickListner;

    public void setListener(OnPrimaryItemBindListener listener,
                            OnPrimaryItemClickListner clickListner) {
        mListener = listener;
        mClickListner = clickListner;
    }

    @Override
    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public int getLayoutId() {
        return R.layout.default_adapter_linkage_primary;
    }

    @Override
    public int getGroupTitleViewId() {
        return R.id.tv_group;
    }

    @Override
    public int getRootViewId() {
        return R.id.layout_group;
    }

    /***
     * 该方法主要是对组名View进行一些列的设置
     * @param holder   LinkagePrimaryViewHolder 用来获取组名View
     * @param selected selected of this position 当前组名View是否被选中
     * @param title    title of this position 组的名称
     */
    @Override
    public void onBindViewHolder(LinkagePrimaryViewHolder holder, boolean selected, String title) {
        // 获取组名View
        TextView tvTitle = ((TextView) holder.mGroupTitle);
        // 为组名View设置组的名称
        tvTitle.setText(title);

        // 设置组名View是否选中的相应背景
        tvTitle.setBackgroundColor(mContext.getResources().getColor(selected ? R.color.colorPurple : R.color.colorWhite));
        // 设置组名View是否选中的相应字体颜色
        tvTitle.setTextColor(ContextCompat.getColor(mContext, selected ? R.color.colorWhite : R.color.colorGray));
        // 设置组名View如果没有被选中则组名称文字末尾省略号,如果被选中了就跑马灯展示
        tvTitle.setEllipsize(selected ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
        // 设置视图是否可以获取焦点
        tvTitle.setFocusable(selected);
        // 设置视图可否获取焦点并保持焦点
        tvTitle.setFocusableInTouchMode(selected);
        // 设置组名View被选中了可以重复动画选框,如果没有被选中则不能有动画.
        tvTitle.setMarqueeRepeatLimit(selected ? MARQUEE_REPEAT_LOOP_MODE : MARQUEE_REPEAT_NONE_MODE);

        if (mListener != null) {
            mListener.onBindViewHolder(holder, title);
        }
    }

    @Override
    public void onItemClick(LinkagePrimaryViewHolder holder, View view, String title) {
        if (mClickListner != null) {
            mClickListner.onItemClick(holder, view, title);
        }
    }

    public interface OnPrimaryItemClickListner {
        /**
         * we suggest you get position by holder.getAdapterPosition
         *
         * @param holder primaryHolder
         * @param view   view
         * @param title  groupTitle
         */
        void onItemClick(LinkagePrimaryViewHolder holder, View view, String title);
    }

    public interface OnPrimaryItemBindListener {
        /**
         * Note: Please do not override rootView click listener in here, because of linkage selection rely on it.
         * and we suggest you get position by holder.getAdapterPosition
         *
         * @param primaryHolder primaryHolder
         * @param title         groupTitle
         */
        void onBindViewHolder(LinkagePrimaryViewHolder primaryHolder, String title);
    }
}
