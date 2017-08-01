package com.test.onesignal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;

import com.onesignal.ShadowOSUtils;
import com.onesignal.ShadowRoboNotificationManager;
import com.onesignal.example.BlankActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static com.onesignal.OneSignalPackagePrivateHelper.NotificationChannelManager_createNotificationChannel;
import static com.onesignal.OneSignalPackagePrivateHelper.NotificationChannelManager_processChannelList;
import static org.junit.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Config(packageName = "com.onesignal.example",
      shadows = {
         ShadowOSUtils.class,
         ShadowRoboNotificationManager.class},
      instrumentedPackages = {"com.onesignal"},
      sdk = 10000)
@RunWith(RobolectricTestRunner.class)
public class NotificationChannelManagerRunner {

   private Context mContext;
   private BlankActivity blankActivity;
   
   NotificationChannelManagerRunner setContext(Context context) {
      mContext = context;
      return this;
   }
   
   @BeforeClass // Runs only once, before any tests
   public static void setUpClass() throws Exception {
      ShadowLog.stream = System.out;
   }

   @Before
   public void beforeEachTest() throws Exception {
      ActivityController<BlankActivity> blankActivityController = Robolectric.buildActivity(BlankActivity.class).create();
      blankActivity = blankActivityController.get();
      mContext = blankActivity;
   }

   @Test
   public void createNotificationChannelShouldReturnDefaultChannelWithEmptyPayload() throws Exception {
      JSONObject payload = new JSONObject();

      String ret = NotificationChannelManager_createNotificationChannel(blankActivity, payload);

      assertEquals("fcm_fallback_notification_channel", ret);
      NotificationChannel lastChannel = ShadowRoboNotificationManager.lastChannel;
      assertEquals("fcm_fallback_notification_channel", lastChannel.getId());
      assertNotNull(lastChannel.getSound());
      assertTrue(lastChannel.shouldShowLights());
      assertTrue(lastChannel.shouldVibrate());
   }

   @Test
   public void createNotificationChannelCreateBasicChannel() throws Exception {
      JSONObject payload = new JSONObject();
      JSONObject chnl = new JSONObject();
      chnl.put("id", "test_id");
      payload.put("chnl", chnl);

      String ret = NotificationChannelManager_createNotificationChannel(blankActivity, payload);

      NotificationChannel channel = ShadowRoboNotificationManager.lastChannel;
      assertEquals("test_id", ret);
      assertEquals("test_id", ShadowRoboNotificationManager.lastChannel.getId());
      assertNotNull(channel.getSound());
      assertTrue(channel.shouldShowLights());
      assertTrue(channel.shouldVibrate());
   }

   @Test
   public void createNotificationChannelWithALlOptionsl() throws Exception {
      JSONObject payload = new JSONObject();
      JSONObject chnl = new JSONObject();

      chnl.put("id", "test_id");
      chnl.put("nm", "Test Name");
      chnl.put("grp", "grp_id");
      chnl.put("grp_nm", "Group Name");
      chnl.put("imp", NotificationManager.IMPORTANCE_MAX);
      chnl.put("lght", false);
      chnl.put("ledc", "FFFF0000");
      chnl.put("vib", false);
      chnl.put("vib_pt", new JSONArray("[1,2,3,4]"));
      chnl.put("snd_nm", "notification");
      chnl.put("lck", Notification.VISIBILITY_SECRET);
      chnl.put("bdg", true);
      chnl.put("bdnd", true);

      payload.put("chnl", chnl);

      String ret = NotificationChannelManager_createNotificationChannel(blankActivity, payload);

      NotificationChannel channel = ShadowRoboNotificationManager.lastChannel;
      assertEquals("test_id", ret);
      assertEquals("test_id", ShadowRoboNotificationManager.lastChannel.getId());
      assertEquals("Test Name", channel.getName());
      assertEquals("grp_id", channel.getGroup());
      NotificationChannelGroup group = ShadowRoboNotificationManager.lastChannelGroup;
      assertEquals("grp_id", group.getId());
      assertEquals("Group Name", group.getName());
      assertNotNull(channel.getSound());
      assertFalse(channel.shouldShowLights());
      assertEquals(-65536, channel.getLightColor());
      assertTrue(channel.shouldVibrate()); // Setting a pattern enables vibration
      assertArrayEquals(new long[]{1,2,3,4}, channel.getVibrationPattern());
      assertEquals(NotificationManager.IMPORTANCE_MAX, channel.getImportance());
      assertEquals("content://settings/system/notification_sound", channel.getSound().toString());
      assertEquals(Notification.VISIBILITY_SECRET, channel.getLockscreenVisibility());
      assertTrue(channel.canShowBadge());
      assertTrue(channel.canBypassDnd());
   }
   
   @Test
   public void useOtherChannelWhenItIsAvailable() throws Exception {
      JSONObject payload = new JSONObject();
      payload.put("oth_chnl", "existing_id");
      
      JSONObject chnl = new JSONObject();
      chnl.put("id", "test_id");
      payload.put("chnl", chnl);
      
      String ret = NotificationChannelManager_createNotificationChannel(blankActivity, payload);
      
      // Should create and use the payload type as the "existing_id" didn't exist.
      assertEquals("test_id", ret);
    
      // Create the missing channel and using the same payload we should use this existing_id now.
      createChannel("existing_id");
      ret = NotificationChannelManager_createNotificationChannel(blankActivity, payload);
      assertEquals("existing_id", ret);
   }
   
   @Test
   public void processPayloadWithOutChannelList() throws Exception {
      createChannel("local_existing_id");
      createChannel("OS_existing_id");
   
      NotificationChannelManager_processChannelList(blankActivity, new JSONObject());
      
      assertNotNull(getChannel("local_existing_id"));
      assertNotNull(getChannel("OS_existing_id"));
   }
   
   @Test
   public void processPayloadCreatingNewChannel() throws Exception {
      createChannel("local_existing_id");
      
      JSONArray channelList = new JSONArray();
      JSONObject channelItem = new JSONObject();
      
      channelItem.put("id", "OS_id1");
   
      channelList.put(channelItem);
      JSONObject payload = new JSONObject();
      payload.put("chnl_lst", channelList);
      
      NotificationChannelManager_processChannelList(blankActivity, payload);
      
      assertNotNull(getChannel("local_existing_id"));
      assertNotNull(getChannel("OS_id1"));
   }
   
   @Test
   public void processPayloadDeletingOldChannel() throws Exception {
      NotificationChannelManager_processChannelList(blankActivity, createBasicChannelListPayload());
      assertChannelsForBasicChannelList();
   }
   
   JSONObject createBasicChannelListPayload() throws JSONException {
      createChannel("local_existing_id");
      createChannel("OS_existing_id");
      
      JSONArray channelList = new JSONArray();
      JSONObject channelItem = new JSONObject();
   
      channelItem.put("id", "OS_id1");
   
      channelList.put(channelItem);
      JSONObject payload = new JSONObject();
      payload.put("chnl_lst", channelList);
      return payload;
   }
   
   void assertChannelsForBasicChannelList() {
      assertNotNull(getChannel("local_existing_id"));
      assertNull(getChannel("OS_existing_id"));
      assertNotNull(getChannel("OS_id1"));
   }
   
   private NotificationChannel getChannel(String id) {
      NotificationManager notificationManager =
          (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
      return notificationManager.getNotificationChannel(id);
   }
   
   private void createChannel(String id) {
      NotificationManager notificationManager =
          (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
      NotificationChannel channel = new NotificationChannel(id,"name", NotificationManager.IMPORTANCE_DEFAULT);
      notificationManager.createNotificationChannel(channel);
   }
}