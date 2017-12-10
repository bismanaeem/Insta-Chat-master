package social.chat.whatsapp.fb.messenger.messaging;

import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.support.v4.view.ViewPager;

import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by mohak on 22/1/17.
 */

public class FloatingBubble extends Service implements CustomPagerAdapter.Clicked {

    /**
     * check if service is running
     */
    static boolean isServiceRunning;
    /**
     * ViewPager Adapter
     */
    CustomPagerAdapter adapter;
    /**
     * contains list of keys
     */
    ArrayList<String> keys = new ArrayList<>();
    /**
     * check if chat window is attached
     */
    boolean isWindowAttached = false;
    /**
     * bubble view
     */

    CircleImageView bubble;
    /**
     * store user reply to chat
     */
    ArrayList<replyModel> replyData;
    /**
     * window manager
     */
    private WindowManager windowManager;
    /**
     * image view for stopping the service
     */
    private ImageView removeBubble;
    /**
     * initial exact coordinates of chat head
     */
    private int x_init_cord, y_init_cord;
    /**
     * initial relative coordinates of chat head
     */
    private int x_init_margin, y_init_margin;
    /**
     * width and height of device
     */
    private int widthOfDev, heightOfDev;
    /**
     * arranges data in key values pair
     */
    private LinkedHashMap<String, ArrayList<Object>> listHashMap;
    /**
     * previous location of chat head
     */
    private int click_x, click_y;
    /**
     * checks orientation
     * 1 = Portrait (Default)
     * 2 = Landscape
     */
    private int configuration = 1;

    /**
     * layout params of window manager for chat head
     */
    private WindowManager.LayoutParams imageWindowParams;

    /**
     * open WhatsApp directly
     */
    private ImageView openWhatsApp;

    /**
     * notifies user of a new message
     */
    private View newMessage;

    private LinearLayout removeView, bubbleView, horizontalLinearLayout, chatLinear;
    private LayoutInflater inflater;
    private ArrayList<NotificationModel> msgsData;
    private ViewPager view_pager;
    private RelativeLayout relative;
    private HorizontalScrollView horizontal_scroller;

    private NotificationWear notificationWear;


    @Override
    public void onCreate() {

        super.onCreate();

        Log.d("Lines", "Service created");

        EventBus.getDefault().register(this);

        isServiceRunning = false;

        listHashMap = new LinkedHashMap<>();
        replyData = new ArrayList<>();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        heightOfDev = displaymetrics.heightPixels;
        widthOfDev = displaymetrics.widthPixels;


        addRemoveView();
        addBubbleView();

        bubble.setOnTouchListener(new View.OnTouchListener() {

            boolean isLongclick = false, inBound = false;
            long time_start = 0, time_end = 0;

            Handler handler_longClick = new Handler();
            Runnable runnable_longClick = new Runnable() {
                @Override
                public void run() {

                    isLongclick = true;
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent event) {


                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) bubbleView.getLayoutParams();
                WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();


                int x_cord = (int) event.getRawX();
                int y_cord = (int) event.getRawY();

                int x_cord_Destination, y_cord_Destination;


                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        time_start = System.currentTimeMillis();
                        handler_longClick.postDelayed(runnable_longClick, 1000);

                        x_init_cord = x_cord;
                        y_init_cord = y_cord;

                        x_init_margin = layoutParams.x;
                        y_init_margin = layoutParams.y;


                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int x_diff_move = x_cord - x_init_cord;
                        int y_diff_move = y_cord - y_init_cord;

                        x_cord_Destination = x_init_margin + x_diff_move;
                        y_cord_Destination = y_init_margin + y_diff_move;

                        if (isLongclick) {

                            if (((x_cord >= (widthOfDev / 2 - (removeBubble.getWidth())) && x_cord <= (widthOfDev / 2 + (removeBubble.getWidth()))
                                    && y_cord > (heightOfDev - ((removeBubble.getHeight() * 2)))) && configuration == 1) ||
                                    configuration == 2 && (x_cord >= heightOfDev / 2 - (removeBubble.getWidth()) && x_cord <= heightOfDev / 2 + removeBubble.getWidth()
                                            && y_cord > widthOfDev - (removeBubble.getHeight() * 2))) {

                                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                v.vibrate(5);
                                inBound = true;

                            } else {

                                inBound = false;

                                int x_cord_remove, y_cord_remove;
                                if (configuration == 1) {

                                    x_cord_remove = ((widthOfDev - (removeBubble.getWidth())) / 2);
                                    y_cord_remove = (heightOfDev - ((removeBubble.getHeight() * 2)));


                                } else {

                                    y_cord_remove = ((widthOfDev - (removeBubble.getHeight() * 2)));
                                    x_cord_remove = ((heightOfDev - (removeBubble.getWidth())) / 2);

                                }

                                param_remove.x = x_cord_remove;
                                param_remove.y = y_cord_remove;

                                windowManager.updateViewLayout(removeView, param_remove);
                                removeView.setVisibility(View.VISIBLE);
                            }

                        }

                        layoutParams.x = x_cord_Destination;
                        layoutParams.y = y_cord_Destination;

                        windowManager.updateViewLayout(bubbleView, layoutParams);

                        return true;
                    case MotionEvent.ACTION_UP:

                        isLongclick = false;
                        removeView.setVisibility(View.GONE);

                        if (inBound) {
                            inBound = false;
                            stopSelf();
                            return true;
                        }

                        int x_diff = x_cord - x_init_cord;
                        int y_diff = y_cord - y_init_cord;

                        if (Math.abs(x_diff) < 20 && Math.abs(y_diff) < 20) {

                            time_end = System.currentTimeMillis();
                            if ((time_end - time_start) < 300) {

                                if (!isWindowAttached) {

                                    animateView(layoutParams.x, 0, layoutParams.y, 0);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {

                                            addChatLayout();

                                        }
                                    }, 300);
                                } else {

                                    removeChatWindow();

                                }

                            }


                        } else {

                            if (configuration == 1) {

                                if (layoutParams.x < widthOfDev / 2)
                                    animateView(layoutParams.x, 0, layoutParams.y, layoutParams.y);
                                else
                                    animateView(layoutParams.x, (widthOfDev - bubble.getWidth()), layoutParams.y, layoutParams.y);
                            } else {

                                if (layoutParams.x < heightOfDev / 2)
                                    animateView(layoutParams.x, 0, layoutParams.y, layoutParams.y);
                                else
                                    animateView(layoutParams.x, (heightOfDev - bubble.getWidth()), layoutParams.y, layoutParams.y);
                            }

                        }

                        return true;

                }
                return false;
            }
        });
    }

    private void addChatLayout() {


        isWindowAttached = true;
        imageWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        newMessage.setVisibility(View.GONE);
        openWhatsApp.setVisibility(View.VISIBLE);
        view_pager.setVisibility(View.VISIBLE);
        relative.setVisibility(View.VISIBLE);
        horizontal_scroller.setVisibility(View.VISIBLE);

        windowManager.updateViewLayout(bubbleView, imageWindowParams);

        bubbleView.setFocusableInTouchMode(true);
        bubbleView.requestFocus();

        bubbleView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {

                if (i == KeyEvent.KEYCODE_BACK) {

                    removeChatWindow();
                    return true;
                } else
                    return false;

            }
        });

        openWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                removeChatWindow();
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);

            }
        });
    }


    private void animateView(int startX, int endX, int startY, int endY) {

        ValueAnimator translateX = ValueAnimator.ofInt(startX, endX);
        ValueAnimator translateY = ValueAnimator.ofInt(startY, endY);

        translateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                updateViewLayout(bubbleView, val, -1);

            }
        });

        translateX.setDuration(250).start();

        translateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                updateViewLayout(bubbleView, -1, val);

            }
        });

        translateY.setDuration(250).start();
    }

    private void updateViewLayout(LinearLayout bubbleView, int animateX, int animateY) {


        if (animateX != -1) imageWindowParams.x = animateX;
        if (animateY != -1) imageWindowParams.y = animateY;

        windowManager.updateViewLayout(bubbleView, imageWindowParams);
    }


    private void addBubbleView() {

        bubbleView = (LinearLayout) inflater.inflate(R.layout.bubble_layout, null);
        bubble = (CircleImageView) bubbleView.findViewById(R.id.bubble);
        newMessage = bubbleView.findViewById(R.id.newmessage);
        openWhatsApp = (ImageView) bubbleView.findViewById(R.id.openWhatsapp);

        relative = (RelativeLayout) bubbleView.findViewById(R.id.rel);
        horizontal_scroller = (HorizontalScrollView) bubbleView.findViewById(R.id.scroller);
        view_pager = (ViewPager) bubbleView.findViewById(R.id.pager);
        horizontalLinearLayout = (LinearLayout) bubbleView.findViewById(R.id.horizontalLinear);
        chatLinear = (LinearLayout) bubbleView.findViewById(R.id.mainLinear);


        imageWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        imageWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        imageWindowParams.x = 10;
        imageWindowParams.y = heightOfDev / 2;


        windowManager.addView(bubbleView, imageWindowParams);

        view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


            }

            @Override
            public void onPageSelected(int position) {

                bubbleView.setFocusableInTouchMode(true);
                bubbleView.requestFocus();

                horizontal_scroller.scrollTo(horizontalLinearLayout.getChildAt(position).getLeft(), 0);
                horizontalLinearLayout.getChildAt(position).findViewById(R.id.currentItem).setBackgroundColor(Color.parseColor("#ff69b4"));

                for (int i = 0; i < horizontalLinearLayout.getChildCount(); i++) {

                    if (i != position)
                        horizontalLinearLayout.getChildAt(i).findViewById(R.id.currentItem).setBackgroundColor(Color.parseColor("#065E52"));
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


    }

    private void removeChatWindow() {

        isWindowAttached = false;

        horizontal_scroller.setVisibility(View.GONE);
        relative.setVisibility(View.GONE);
        view_pager.setVisibility(View.GONE);

        imageWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        imageWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        imageWindowParams.x = 10;

        if (configuration == 1)
            imageWindowParams.y = heightOfDev / 2;
        else
            imageWindowParams.y = widthOfDev / 2;

        windowManager.updateViewLayout(bubbleView, imageWindowParams);


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        /**
         * 1 = Portrait
         * 2 = Landscape
         */

        configuration = newConfig.orientation;

        if (configuration == 1) {

            imageWindowParams.x = 10;
            imageWindowParams.y = heightOfDev / 2;


        } else {

            imageWindowParams.x = 10;
            imageWindowParams.y = widthOfDev / 2;

        }

        windowManager.updateViewLayout(bubbleView, imageWindowParams);
    }

    private void addRemoveView() {

        removeView = (LinearLayout) inflater.inflate(R.layout.remove_bubble, null);
        removeBubble = (ImageView) removeView.findViewById(R.id.removeImg);
        removeBubble.setImageResource(R.drawable.circle_cross);
        WindowManager.LayoutParams paramRemove = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        paramRemove.gravity = Gravity.TOP | Gravity.LEFT;

        removeView.setVisibility(View.GONE);
        windowManager.addView(removeView, paramRemove);

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        replyData.clear();
        isServiceRunning = false;
        EventBus.getDefault().post(new clearListEvent());

        if (windowManager != null && bubbleView != null) {
            windowManager.removeView(bubbleView);
            windowManager.removeView(removeView);

            try {

                windowManager.removeView(chatLinear);
            } catch (Exception e) {

            /* view not found */

            }

        }

        EventBus.getDefault().unregister(this);

    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {


        if (intent != null && intent.getParcelableArrayListExtra(Constants.msgs) != null) {

            msgsData = intent.getParcelableArrayListExtra(Constants.msgs);

            if (!isWindowAttached)
                newMessage.setVisibility(View.VISIBLE);

            isServiceRunning = true;

            arrangeData();
            arrangeKeys();
            setAdapter();
            setClickListner();

        }


        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * set click listener to all the children of horizontal scrollview
     */
    private void setClickListner() {

        if (horizontalLinearLayout.getChildAt(view_pager.getCurrentItem()) != null)
            horizontalLinearLayout.getChildAt(view_pager.getCurrentItem()).findViewById(R.id.currentItem).setBackgroundColor(Color.parseColor("#ff69b4"));

        for (int i = 0; i < horizontalLinearLayout.getChildCount(); i++) {

            final int finalI = i;

            horizontalLinearLayout.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    view_pager.setCurrentItem(finalI);
                    horizontalLinearLayout.getChildAt(finalI).findViewById(R.id.currentItem).setBackgroundColor(Color.parseColor("#ff69b4"));
                }
            });
        }
    }


    @Subscribe
    public void getnotificationWear(NotificationWear wear) {

        notificationWear = wear;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getNotiifcationData(postNotificationData data) {

        if (!isWindowAttached)
            newMessage.setVisibility(View.VISIBLE);

        msgsData.clear();
        msgsData.addAll(data.msgs);
        arrangeData();
        arrangeKeys();
        setClickListner();

        view_pager.getAdapter().notifyDataSetChanged();

         /* Set the number of pages that should be retained to either side of the current page in the view hierarchy in an idle state.
         * Pages beyond this limit will be recreated from the adapter when needed */
        view_pager.setOffscreenPageLimit(adapter.getCount());

        for (String key : keys) {

            RecyclerView list = ((RecyclerView) view_pager.findViewWithTag(key));
            ListAdapter listadapter = (ListAdapter) list.getAdapter();
            listadapter.swap(listHashMap.get(key));

        }

    }

    /**
     * get all the keys for the HashMap and add them to horizontal scrollview
     */
    private void arrangeKeys() {

        keys.clear();
        horizontalLinearLayout.removeAllViews();

        for (String key : listHashMap.keySet()) {

            keys.add(key);

            LinearLayout headLinear = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.scrolltext, null);
            TextView tv = (TextView) headLinear.findViewById(R.id.title);
            tv.setText(key);

            horizontalLinearLayout.addView(headLinear);

        }

        horizontal_scroller.setHorizontalScrollBarEnabled(false);
    }


    /**
     * convert array list of data to Linked HashMap and add reply messages if any
     */
    public void arrangeData() {

        listHashMap.clear();

        for (int i = 0; i < msgsData.size(); i++) {

            if (msgsData.get(i).getGroup().equals("-null_123")) {

                if (listHashMap.containsKey(msgsData.get(i).getUserName().trim())) {
                    listHashMap.get(msgsData.get(i).getUserName().trim()).add(msgsData.get(i));

                } else {

                    ArrayList<Object> singleDataList = new ArrayList<>();
                    singleDataList.add(msgsData.get(i));
                    listHashMap.put(msgsData.get(i).getUserName().trim(), singleDataList);
                }

            } else {

                if (listHashMap.containsKey(msgsData.get(i).getGroup().trim())) {
                    listHashMap.get(msgsData.get(i).getGroup().trim()).add(msgsData.get(i));

                } else {

                    ArrayList<Object> singleDataList = new ArrayList<>();
                    singleDataList.add(msgsData.get(i));
                    listHashMap.put(msgsData.get(i).getGroup().trim(), singleDataList);
                }

            }

            for (int j = 0; j < replyData.size(); j++)
                if (listHashMap.containsKey(replyData.get(j).getKey())) {

                    if (replyData.get(j).getPos() == listHashMap.get(replyData.get(j).getKey()).size())
                        listHashMap.get(replyData.get(j).getKey()).add(replyData.get(j));
                }


        }
    }

    /**
     * set adapter to the viewpager
     */
    void setAdapter() {

        int pos = view_pager.getCurrentItem();
        adapter = new CustomPagerAdapter(this, listHashMap, keys);
        adapter.setItemClickListner(this);
        view_pager.setAdapter(adapter);
        view_pager.setCurrentItem(pos);
        view_pager.setOffscreenPageLimit(adapter.getCount());

    }

    @Override
    public void itemClicked(int pos, String message) {

        Log.d("Lines", "Clicked");

        if (message.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty !", Toast.LENGTH_SHORT).show();
            return;
        }

        EventBus.getDefault().post(new postEvent(keys.get(pos), keys.size()));

        if (notificationWear == null) {

            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
            return;
        }

        RemoteInput[] remoteInputs = new RemoteInput[notificationWear.remoteInputs.size()];
        Intent localIntent = new Intent();
        Bundle localBundle = notificationWear.bundle;

        int i = 0;

        for (RemoteInput remoteIn : notificationWear.remoteInputs) {
            remoteInputs[i] = remoteIn;
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), message);
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {

            if (remoteInputs.length <= pos) {

                Toast.makeText(this, "Something went wrong please close the app and try again", Toast.LENGTH_SHORT).show();
                return;
            }

            if (remoteInputs[pos].getLabel().equals("Reply to " + keys.get(pos))) {
                notificationWear.pendingIntent.get(pos).send(FloatingBubble.this, 0, localIntent);
            } else {


                for (int x = 0; x < remoteInputs.length; x++) {
                    if (remoteInputs[x].getLabel().equals("Reply to " + keys.get(pos))) {
                        notificationWear.pendingIntent.get(x).send(FloatingBubble.this, 0, localIntent);
                        break;
                    }
                    Log.d("Lines", remoteInputs[x].getLabel().toString());
                }

            }

            replyModel messageReply = new replyModel();
            messageReply.setMessage(message);
            messageReply.setKey(keys.get(pos));
            messageReply.setPos(listHashMap.get(keys.get(pos)).size());
            messageReply.setTime(System.currentTimeMillis());

            replyData.add(messageReply);

            arrangeData();

            ListAdapter adapter = (ListAdapter) ((RecyclerView) view_pager.findViewWithTag(keys.get(pos))).getAdapter();
            adapter.swap(listHashMap.get(keys.get(pos)));


        } catch (PendingIntent.CanceledException e) {


        }


    }


    @Subscribe
    public void closeBubble(closeBubbleEvent event) {

        removeChatWindow();
    }


}