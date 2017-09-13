package lveapp.fr.downloadfromgoogledrive;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Maranatha on 30/08/2017.
 */

public class DownloadService extends Service
{
    private static BroadcastReceiver downloadReceiver;

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        registerScreenOffReceiver();
    }

    @Override
    public void onDestroy()
    {
        unregisterReceiver(downloadReceiver);
        downloadReceiver = null;
    }

    private void registerScreenOffReceiver()
    {
        downloadReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    Toast.makeText(context, context.getResources().getString(R.string.lb_download_end), Toast.LENGTH_LONG).show();
                    Log.i("TAG_DOWNLOAD_FINISHED", context.getResources().getString(R.string.lb_download_end));
                }
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
    }
}
