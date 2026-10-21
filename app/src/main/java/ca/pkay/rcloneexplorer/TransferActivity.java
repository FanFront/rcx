package ca.pkay.rcloneexplorer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ca.pkay.rcloneexplorer.BroadcastReceivers.DownloadCancelAction;
import ca.pkay.rcloneexplorer.BroadcastReceivers.UploadCancelAction;
import ca.pkay.rcloneexplorer.Services.TransferManager;

/** Live transfer page, accessible from the navigation drawer or notifications. */
public class TransferActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            refreshState();
            handler.postDelayed(this, 500);
        }
    };

    private TextView uploadName;
    private TextView uploadState;
    private TextView uploadDetails;
    private ProgressBar uploadProgress;
    private Button stopUpload;
    private TextView downloadName;
    private TextView downloadState;
    private TextView downloadDetails;
    private ProgressBar downloadProgress;
    private Button stopDownload;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.applyTheme(this);
        setContentView(R.layout.activity_transfers);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        uploadName = findViewById(R.id.transfer_upload_name);
        uploadState = findViewById(R.id.transfer_upload_state);
        uploadDetails = findViewById(R.id.transfer_upload_details);
        uploadProgress = findViewById(R.id.transfer_upload_progress);
        stopUpload = findViewById(R.id.transfer_stop_upload);
        downloadName = findViewById(R.id.transfer_download_name);
        downloadState = findViewById(R.id.transfer_download_state);
        downloadDetails = findViewById(R.id.transfer_download_details);
        downloadProgress = findViewById(R.id.transfer_download_progress);
        stopDownload = findViewById(R.id.transfer_stop_download);

        stopUpload.setOnClickListener(v -> cancel(TransferManager.Kind.UPLOAD));
        stopDownload.setOnClickListener(v -> cancel(TransferManager.Kind.DOWNLOAD));
        refreshState();
    }

    private void cancel(TransferManager.Kind kind) {
        // Immediately destroy the process; the receiver also stops queued work.
        TransferManager.cancel(kind);
        Intent intent = kind == TransferManager.Kind.UPLOAD
                ? new Intent(this, UploadCancelAction.class)
                : new Intent(this, DownloadCancelAction.class);
        sendBroadcast(intent);
        refreshState();
    }

    private void refreshState() {
        render(TransferManager.snapshot(TransferManager.Kind.UPLOAD),
                uploadName, uploadState, uploadDetails, uploadProgress, stopUpload);
        render(TransferManager.snapshot(TransferManager.Kind.DOWNLOAD),
                downloadName, downloadState, downloadDetails, downloadProgress, stopDownload);
    }

    private void render(TransferManager.Snapshot snapshot, TextView name, TextView state,
                        TextView details, ProgressBar progress, Button stop) {
        name.setText(snapshot.name.isEmpty()
                ? getString(R.string.transfer_no_active)
                : snapshot.name);

        int stateText;
        switch (snapshot.state) {
            case RUNNING:
                stateText = R.string.transfer_running;
                break;
            case CANCELLING:
                stateText = R.string.transfer_stopping;
                break;
            case CANCELLED:
                stateText = R.string.transfer_stopped;
                break;
            case COMPLETED:
                stateText = R.string.transfer_completed;
                break;
            case FAILED:
                stateText = R.string.transfer_failed;
                break;
            default:
                stateText = R.string.transfer_idle;
        }
        state.setText(stateText);

        boolean active = snapshot.state == TransferManager.State.RUNNING
                || snapshot.state == TransferManager.State.CANCELLING;
        details.setText(snapshot.details.isEmpty() && active
                ? getString(R.string.transfer_waiting)
                : snapshot.details);
        details.setVisibility(snapshot.details.isEmpty() && !active ? View.GONE : View.VISIBLE);
        progress.setVisibility(active ? View.VISIBLE : View.GONE);
        if (active) {
            progress.setIndeterminate(snapshot.percent < 0);
            if (snapshot.percent >= 0) {
                progress.setProgress(snapshot.percent);
            }
        }
        stop.setVisibility(active ? View.VISIBLE : View.GONE);
        stop.setEnabled(snapshot.state == TransferManager.State.RUNNING);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
