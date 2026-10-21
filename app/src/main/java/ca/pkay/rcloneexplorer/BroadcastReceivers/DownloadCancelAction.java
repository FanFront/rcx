package ca.pkay.rcloneexplorer.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ca.pkay.rcloneexplorer.Services.DownloadService;
import ca.pkay.rcloneexplorer.Services.TransferManager;

public class DownloadCancelAction extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent downloadIntent = new Intent(context, DownloadService.class);
        TransferManager.cancel(TransferManager.Kind.DOWNLOAD);
        context.stopService(downloadIntent);
    }
}
