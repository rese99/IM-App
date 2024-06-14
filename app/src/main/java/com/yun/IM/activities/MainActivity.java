package com.yun.IM.activities;


import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.yun.IM.Fragments.accountFragment;
import com.yun.IM.Fragments.contactsFragment;
import com.yun.IM.Fragments.messageFragment;
import com.yun.IM.R;
import com.yun.IM.utilites.AutoUpdater;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static ImageView imageView;
    private ViewPager

            viewPager;
    private TabLayout tablayout;
    private List<Fragment> fragmentList;
    private String[] titles = {"信息", "联系人", "我"};
    private int[] unSele = {R.drawable.ic_message, R.drawable.ic_contacts, R.drawable.ic_account};
    private int[] onSele = {R.drawable.ic_message_select, R.drawable.ic_contacts_select, R.drawable.ic_account_select};

    public static void visible() {
        imageView.setVisibility(View.GONE);
    }

    public static void invisible() {
        imageView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = findViewById(R.id.viewPager);
        tablayout = findViewById(R.id.tablayout);
        imageView = findViewById(R.id.dot_red);
        initData();
        AutoUpdater autoUpdater = new AutoUpdater(this);
        autoUpdater.CheckUpdate(true);
    }

    public void initData() {
        fragmentList = new ArrayList<>();
        fragmentList.add(new messageFragment());
        fragmentList.add(new contactsFragment());
        fragmentList.add(new accountFragment());

        MainTabAdapter mainTabAdapter = new MainTabAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mainTabAdapter);
        viewPager.setOffscreenPageLimit(0);
        tablayout.setupWithViewPager(viewPager);

        for (int i = 0; i < tablayout.getTabCount(); i++) {
            TabLayout.Tab tab = tablayout.getTabAt(i);
            tab.setCustomView(mainTabAdapter.getView(i));
        }

        tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View view = tab.getCustomView();
                ImageView img = view.findViewById(R.id.img);
                TextView tv = view.findViewById(R.id.tv);
                String title = tv.getText().toString();
                if (title == "信息") {
                    img.setImageResource(onSele[0]);
                } else if (title == "联系人") {
                    img.setImageResource(onSele[1]);
                } else if (title == "我") {
                    img.setImageResource(onSele[2]);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View view = tab.getCustomView();
                ImageView img = view.findViewById(R.id.img);
                TextView tv = view.findViewById(R.id.tv);
                String title = tv.getText().toString();
                if (title == "信息") {
                    img.setImageResource(unSele[0]);
                } else if (title == "联系人") {
                    img.setImageResource(unSele[1]);
                } else if (title == "我") {
                    img.setImageResource(unSele[2]);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
    public class MainTabAdapter extends FragmentPagerAdapter {


        public MainTabAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return fragmentList.get(0);
            } else if (position == 1) {
                return fragmentList.get(1);
            } else if (position == 2) {
                return fragmentList.get(2);
            }
            return fragmentList.get(0);
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        public View getView(int position) {
            View view = View.inflate(MainActivity.this, R.layout.main_tab_item, null);
            ImageView img = view.findViewById(R.id.img);
            TextView tv = view.findViewById(R.id.tv);

            if (tablayout.getTabAt(position).isSelected()) {
                img.setImageResource(onSele[position]);
            } else {
                img.setImageResource(unSele[position]);
            }
            tv.setText(titles[position]);
            tv.setTextColor(tablayout.getTabTextColors());
            return view;
        }
    }
}