package ca.pkay.rcloneexplorer.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ca.pkay.rcloneexplorer.Services.UploadService;
import ca.pkay.rcloneexplorer.Services.TransferManager;

public class UploadCancelAction extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent uploadIntent = new Intent(context, UploadService.class);
        TransferManager.cancel(TransferManager.Kind.UPLOAD);
        context.stopService(uploadIntent);
    }
}
