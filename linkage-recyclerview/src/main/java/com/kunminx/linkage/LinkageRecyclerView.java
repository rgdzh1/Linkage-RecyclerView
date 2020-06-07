package com.kunminx.linkage;
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
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.kunminx.linkage.adapter.LinkagePrimaryAdapter;
import com.kunminx.linkage.adapter.LinkageSecondaryAdapter;
import com.kunminx.linkage.adapter.viewholder.LinkagePrimaryViewHolder;
import com.kunminx.linkage.bean.BaseGroupedItem;
import com.kunminx.linkage.bean.DefaultGroupedItem;
import com.kunminx.linkage.contract.ILinkagePrimaryAdapterConfig;
import com.kunminx.linkage.contract.ILinkageSecondaryAdapterConfig;
import com.kunminx.linkage.defaults.DefaultLinkagePrimaryAdapterConfig;
import com.kunminx.linkage.defaults.DefaultLinkageSecondaryAdapterConfig;
import com.kunminx.linkage.manager.RecyclerViewScrollHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by KunMinX at 19/4/27
 */
public class LinkageRecyclerView<T extends BaseGroupedItem.ItemInfo> extends RelativeLayout {

    private static final int DEFAULT_SPAN_COUNT = 1;
    private static final int SCROLL_OFFSET = 0;

    private Context mContext;

    private RecyclerView mRvPrimary;
    private RecyclerView mRvSecondary;
    private LinearLayout mLinkageLayout;

    private LinkagePrimaryAdapter mPrimaryAdapter;
    private LinkageSecondaryAdapter mSecondaryAdapter;
    private FrameLayout mHeaderContainer;
    // 次Rv悬挂头View,专门展示组名用
    private TextView mTvHeader;
    // 组名称集合
    private List<String> mInitGroupNames;
    // 原始数据集合
    private List<BaseGroupedItem<T>> mInitItems;
    // 头部元素对应的索引
    private List<Integer> mHeaderPositions = new ArrayList<>();
    // 次Rv悬挂头高度
    private int mTitleHeight;
    // 屏幕中次Rv屏幕中第一个可见条目在数据源中的索引.
    private int mFirstVisiblePosition;
    // 上一次在悬挂头View中的名称
    private String mLastGroupName;
    private LinearLayoutManager mSecondaryLayoutManager;
    private LinearLayoutManager mPrimaryLayoutManager;

    private boolean mScrollSmoothly = true;

    public LinkageRecyclerView(Context context) {
        super(context);
    }

    public LinkageRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public LinkageRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * LinkageRecyclerView控件被初始化的时候,就会将R.layout.layout_linkage_view布局中的主RecyclerView与次RecyclerView等都通过findViewById找到
     *
     * @param context
     * @param attrs
     */
    private void initView(Context context, @Nullable AttributeSet attrs) {
        this.mContext = context;
        View view = LayoutInflater.from(context).inflate(R.layout.layout_linkage_view, this);
        // 主Rv
        mRvPrimary = (RecyclerView) view.findViewById(R.id.rv_primary);
        // 次要Rv
        mRvSecondary = (RecyclerView) view.findViewById(R.id.rv_secondary);
        // 次Rv中,在次Rv上方的专门展示组名称用的容器
        mHeaderContainer = (FrameLayout) view.findViewById(R.id.header_container);
        // 外部父容器
        mLinkageLayout = (LinearLayout) view.findViewById(R.id.linkage_layout);
    }

    /**
     * 该方法是用来设置次Rv中的布局格式
     */
    private void setLevel2LayoutManager() {
        if (mSecondaryAdapter.isGridMode()) {
            mSecondaryLayoutManager = new GridLayoutManager(mContext,
                    mSecondaryAdapter.getConfig().getSpanCountOfGridMode());
            ((GridLayoutManager) mSecondaryLayoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (((BaseGroupedItem<T>) mSecondaryAdapter.getItems().get(position)).isHeader) {
                        return mSecondaryAdapter.getConfig().getSpanCountOfGridMode();
                    }
                    return DEFAULT_SPAN_COUNT;
                }
            });
        } else {
            mSecondaryLayoutManager = new LinearLayoutManager(mContext, RecyclerView.VERTICAL, false);
        }
        mRvSecondary.setLayoutManager(mSecondaryLayoutManager);
    }

    /**
     * 主要适配器与次要适配器的初始化
     *
     * @param primaryAdapterConfig   主要适配器配置
     * @param secondaryAdapterConfig 次要适配器配置
     */
    private void initRecyclerView(ILinkagePrimaryAdapterConfig primaryAdapterConfig, ILinkageSecondaryAdapterConfig secondaryAdapterConfig) {
        // mInitGroupNames: 表示组名称集合
        // 创建主要适配器
        mPrimaryAdapter = new LinkagePrimaryAdapter(mInitGroupNames, primaryAdapterConfig,
                new LinkagePrimaryAdapter.OnLinkageListener() {
                    @Override
                    public void onLinkageClick(LinkagePrimaryViewHolder holder, String title) {
                        if (isScrollSmoothly()) {
                            // 是平滑滚动
                            // mRvSecondary:次Rv
                            // LinearSmoothScroller.SNAP_TO_START:平滑滚动置顶
                            // mHeaderPositions.get(holder.getAdapterPosition()): holder.getAdapterPosition()获取的是组名对应的组名集合中的索引,
                            // 然后mHeaderPositions.get(index)获取的是组名称在原始数据集合中索引值,这样其实就拿到了次Rv中组名的索引了,然后再调用
                            // RecyclerViewScrollHelper.smoothScrollToPosition()方法将该组名对应的item滑动到次Rv的顶部.
                            RecyclerViewScrollHelper.smoothScrollToPosition(mRvSecondary,
                                    LinearSmoothScroller.SNAP_TO_START,
                                    mHeaderPositions.get(holder.getAdapterPosition()));
                        } else {
                            mSecondaryLayoutManager.scrollToPositionWithOffset(
                                    mHeaderPositions.get(holder.getAdapterPosition()), SCROLL_OFFSET);
                        }
                    }
                });
        mPrimaryLayoutManager = new LinearLayoutManager(mContext);
        mRvPrimary.setLayoutManager(mPrimaryLayoutManager);
        // 为主Rv设置主适配器
        mRvPrimary.setAdapter(mPrimaryAdapter);

        // 创建次要适配器
        // mInitItems:原始数据集合
        mSecondaryAdapter = new LinkageSecondaryAdapter(mInitItems, secondaryAdapterConfig);
        // 该方法是用来设置次Rv中的布局格式
        setLevel2LayoutManager();
        // 为次Rv设置适配器
        mRvSecondary.setAdapter(mSecondaryAdapter);
    }

    /**
     * 次Rv悬挂滑动,并且关联上主Rv滑动到相应的组名条目
     */
    private void initLinkageSecondary() {
        if (mTvHeader == null && mSecondaryAdapter.getConfig() != null) {
            // 获取次要适配器DefaultLinkageSecondaryAdapterConfig对象
            ILinkageSecondaryAdapterConfig config = mSecondaryAdapter.getConfig();
            // 获取View,这个View就是次要适配器中的悬挂头布局
            int layout = config.getHeaderLayoutId();
            View view = LayoutInflater.from(mContext).inflate(layout, null);
            // 将次Rv悬挂头View添加到展示次Rv组名的容器中
            mHeaderContainer.addView(view);
            // 获取次Rv悬挂头View,专门展示组名用
            mTvHeader = view.findViewById(config.getHeaderTextViewId());
        }
        // mFirstVisiblePosition:屏幕中次Rv屏幕中第一个可见条目在数据源中的索引.
        // 获取数据源中第一个可见条目对应的数据是否有头信息.
        if (mInitItems.get(mFirstVisiblePosition).isHeader) {
            // 如果该条目是有头信息的,那么次Rv悬挂头View就展示该条目对应的组信息.
            mTvHeader.setText(mInitItems.get(mFirstVisiblePosition).header);
        }
        // 监听次Rv滚动
        mRvSecondary.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 次Rv悬挂头高度
                mTitleHeight = mTvHeader.getMeasuredHeight();
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 次Rv在屏幕中显示的第一个Item所对应的条目
                int firstPosition = mSecondaryLayoutManager.findFirstVisibleItemPosition();
                // 次Rv在屏幕中完全显示的第一个Item所对应的条目
                int firstCompletePosition = mSecondaryLayoutManager.findFirstCompletelyVisibleItemPosition();
                List<BaseGroupedItem<T>> items = mSecondaryAdapter.getItems();
                // 假如屏幕中第一个完整显示的item条目距离屏幕顶端的距离比次Rv悬挂头的高度还要小,
                // 那么随着第一个完整显示的item条目向上的移动时,悬挂头也要向上移动.
                // 效果就是第一个完整显示的item条目将悬挂头顶出屏幕,或者item下滑时,悬挂头展示出来.
                if (firstCompletePosition > 0 && (firstCompletePosition) < items.size() && items.get(firstCompletePosition).isHeader) {
                    View view = mSecondaryLayoutManager.findViewByPosition(firstCompletePosition);
                    if (view != null && view.getTop() <= mTitleHeight) {
                        mTvHeader.setY(view.getTop() - mTitleHeight);
                    }
                }

                // Here is the logic of group title changes and linkage:

                boolean groupNameChanged = false;

                if (mFirstVisiblePosition != firstPosition && firstPosition >= 0) {
                    // 假设屏幕中第一个显示的item索引与上次屏幕中第一个显示item索引不同的话,
                    // 那么就更新mFirstVisiblePosition值
                    mFirstVisiblePosition = firstPosition;
                    // 将次Rv的悬挂头显示出来
                    mTvHeader.setY(0);
                    // 取得该条目对应的数据
                    // 判断该条目是头还是内容条目,最终获取当前条目对应的组名称
                    String currentGroupName = items.get(mFirstVisiblePosition).isHeader
                            ? items.get(mFirstVisiblePosition).header
                            : items.get(mFirstVisiblePosition).info.getGroup();

                    if (TextUtils.isEmpty(mLastGroupName) || !mLastGroupName.equals(currentGroupName)) {
                        // 如果当前item对应的组名称为空或者当前屏幕中显示的第一个item的组名称与上一次item对应的组名称不同.
                        // 1.更新mLastGroupName组名称
                        // 2.标记Rx悬挂头中的组名称已经改了
                        // 3.更改次Rx悬挂头的组名称
                        mLastGroupName = currentGroupName;
                        groupNameChanged = true;
                        mTvHeader.setText(mLastGroupName);
                    }
                }

                // 假如当前次Rx悬挂头的中组名称已经更改了
                if (groupNameChanged) {
                    // 获取组名称集合
                    List<String> groupNames = mPrimaryAdapter.getStrings();
                    // 对该组名称集合进行遍历
                    for (int i = 0; i < groupNames.size(); i++) {
                        // 如果次Rv中悬挂头中的组名称与组名称集合中的某个元素相等,获取该元素的索引.设置主Rv该索引对应的条目被选中.
                        if (groupNames.get(i).equals(mLastGroupName)) {
                            // 设置条目被选中
                            mPrimaryAdapter.setSelectedPosition(i);
                            // 平滑的滑动某条目,并将该条目置顶.
                            RecyclerViewScrollHelper.smoothScrollToPosition(mRvPrimary, LinearSmoothScroller.SNAP_TO_END, i);
                        }
                    }
                }
            }
        });
    }

    private int dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) ((dp * displayMetrics.density) + 0.5f);
    }

    /**
     * @param linkageItems           原始数据源
     * @param primaryAdapterConfig   主要适配器配置
     * @param secondaryAdapterConfig 次要适配器配置
     */
    public void init(List<BaseGroupedItem<T>> linkageItems, ILinkagePrimaryAdapterConfig primaryAdapterConfig, ILinkageSecondaryAdapterConfig secondaryAdapterConfig) {
        // 主要适配器与次要适配器的初始化,以及为Rv设置适配器的工作
        initRecyclerView(primaryAdapterConfig, secondaryAdapterConfig);
        // 原始数据集合
        this.mInitItems = linkageItems;
        String lastGroupName = null;
        List<String> groupNames = new ArrayList<>();
        // 遍历原始数据集合
        if (mInitItems != null && mInitItems.size() > 0) {
            for (BaseGroupedItem<T> item1 : mInitItems) {
                if (item1.isHeader) {
                    // 获取原始数据集合中每个组的名称,并存入集合groupNames中
                    groupNames.add(item1.header);
                    // 获取原始数据集合中最后一个组的名称,并用变量lastGroupName接收
                    lastGroupName = item1.header;
                }
            }
        }
        // 获取头部元素的索引存入集合中
        if (mInitItems != null) {
            for (int i = 0; i < mInitItems.size(); i++) {
                if (mInitItems.get(i).isHeader) {
                    // 如果原始数据中的某个元素是头部元素,就将该头部元素对应的索引存入mHeaderPositions集合中
                    mHeaderPositions.add(i);
                }
            }
        }

        DefaultGroupedItem.ItemInfo info = new DefaultGroupedItem.ItemInfo(null, lastGroupName);
        // 创建一个DefaultGroupedItem对象,该对象中有用的变量就是DefaultGroupedItem.ItemInfo对象中的group(这个值表示的是组名称)
        BaseGroupedItem<T> footerItem = (BaseGroupedItem<T>) new DefaultGroupedItem(info);
        // 将表示最后组信息的对象存入原始数据集合中.
        mInitItems.add(footerItem);
        // 将组名称集合交由mInitGroupNames变量保存
        this.mInitGroupNames = groupNames;
        // 经过上面的那些初始化适配器,处理数据,设置适配器等完成之后就开始正式为两个Rv设置新的数据了.
        mPrimaryAdapter.initData(mInitGroupNames);
        mSecondaryAdapter.initData(mInitItems);
        //  次Rv悬挂滑动,并且关联上主Rv滑动到相应的组名条目
        initLinkageSecondary();
    }

    /**
     * 起始方法
     *
     * @param linkageItems
     */
    public void init(List<BaseGroupedItem<T>> linkageItems) {
        init(linkageItems, new DefaultLinkagePrimaryAdapterConfig(), new DefaultLinkageSecondaryAdapterConfig());
    }

    public void setDefaultOnItemBindListener(
            DefaultLinkagePrimaryAdapterConfig.OnPrimaryItemClickListner primaryItemClickListner,
            DefaultLinkagePrimaryAdapterConfig.OnPrimaryItemBindListener primaryItemBindListener,
            DefaultLinkageSecondaryAdapterConfig.OnSecondaryItemBindListener secondaryItemBindListener,
            DefaultLinkageSecondaryAdapterConfig.OnSecondaryHeaderBindListener headerBindListener,
            DefaultLinkageSecondaryAdapterConfig.OnSecondaryFooterBindListener footerBindListener) {

        if (mPrimaryAdapter.getConfig() != null) {
            ((DefaultLinkagePrimaryAdapterConfig) mPrimaryAdapter.getConfig())
                    .setListener(primaryItemBindListener, primaryItemClickListner);
        }
        if (mSecondaryAdapter.getConfig() != null) {
            ((DefaultLinkageSecondaryAdapterConfig) mSecondaryAdapter.getConfig())
                    .setItemBindListener(secondaryItemBindListener, headerBindListener, footerBindListener);
        }
    }

    public void setLayoutHeight(float dp) {
        ViewGroup.LayoutParams lp = mLinkageLayout.getLayoutParams();
        lp.height = dpToPx(getContext(), dp);
        mLinkageLayout.setLayoutParams(lp);
    }

    public boolean isGridMode() {
        return mSecondaryAdapter.isGridMode();
    }

    public void setGridMode(boolean isGridMode) {
        mSecondaryAdapter.setGridMode(isGridMode);
        setLevel2LayoutManager();
        mRvSecondary.requestLayout();
    }

    /**
     * 是否是平滑滚动
     * @return
     */
    public boolean isScrollSmoothly() {
        return mScrollSmoothly;
    }

    public void setScrollSmoothly(boolean scrollSmoothly) {
        this.mScrollSmoothly = scrollSmoothly;
    }

    public LinkagePrimaryAdapter getPrimaryAdapter() {
        return mPrimaryAdapter;
    }

    public LinkageSecondaryAdapter getSecondaryAdapter() {
        return mSecondaryAdapter;
    }

    public List<Integer> getHeaderPositions() {
        return mHeaderPositions;
    }

}
