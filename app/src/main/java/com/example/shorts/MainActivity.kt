<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/viewPager"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical" />

    <!-- Garis merah progress bawah -->
    <View
        android:layout_width="match_parent"
        android:layout_height="3dp"
        android:layout_gravity="bottom"
        android:layout_marginBottom="56dp"
        android:background="#FF0000" />

    <!-- Bottom Nav persis screenshot -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:layout_gravity="bottom"
        android:background="#000000"
        android:orientation="horizontal"
        android:gravity="center"
        android:weightSum="4">

        <ImageView android:layout_width="0dp" android:layout_height="28dp" android:layout_weight="1" android:src="@drawable/ic_home" android:tint="#FFFFFF" />
        <ImageView android:layout_width="0dp" android:layout_height="28dp" android:layout_weight="1" android:src="@drawable/ic_shorts_tab" android:tint="#FFFFFF" />
        <ImageView android:layout_width="0dp" android:layout_height="32dp" android:layout_weight="1" android:src="@drawable/ic_add" android:tint="#FFFFFF" />
        <ImageView android:layout_width="0dp" android:layout_height="28dp" android:layout_weight="1" android:src="@drawable/ic_profile" android:tint="#FFFFFF" />
    </LinearLayout>
</FrameLayout>
