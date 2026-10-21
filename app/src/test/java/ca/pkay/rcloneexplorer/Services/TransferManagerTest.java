package ca.pkay.rcloneexplorer.Services;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class TransferManagerTest {
    @Test
    public void parsesWholeTransferPercent() {
        assertEquals(42, TransferManager.parsePercent("45 MiB / 100 MiB, 42%, 1 MiB/s, ETA 1m"));
        assertEquals(-1, TransferManager.parsePercent("3 files / 10 files"));
        assertEquals(-1, TransferManager.parsePercent(null));
        assertEquals(-1, TransferManager.parsePercent("100 MiB, 101%"));
    }

    @Test
    public void cancellationKillsRunningProcessAndIsNotReportedAsFailure() {
        TransferManager.start(TransferManager.Kind.UPLOAD, "video.mp4");
        StubProcess process = new StubProcess();
        TransferManager.attach(TransferManager.Kind.UPLOAD, process);
        TransferManager.update(TransferManager.Kind.UPLOAD, "1 MiB / 20 MiB, 5%, 2 MiB/s");
        assertEquals(5, TransferManager.snapshot(TransferManager.Kind.UPLOAD).percent);
        assertTrue(TransferManager.cancel(TransferManager.Kind.UPLOAD));
        assertTrue(process.destroyed);
        TransferManager.complete(TransferManager.Kind.UPLOAD, false);
        assertEquals(TransferManager.State.CANCELLED,
                TransferManager.snapshot(TransferManager.Kind.UPLOAD).state);
        assertFalse(TransferManager.cancel(TransferManager.Kind.UPLOAD));
    }

    @Test
    public void cancellationBeforeProcessLaunchKillsLateProcess() {
        TransferManager.start(TransferManager.Kind.DOWNLOAD, "archive.zip");
        TransferManager.cancel(TransferManager.Kind.DOWNLOAD);
        StubProcess process = new StubProcess();
        TransferManager.attach(TransferManager.Kind.DOWNLOAD, process);
        assertTrue(process.destroyed);
        TransferManager.complete(TransferManager.Kind.DOWNLOAD, false);
        assertEquals(TransferManager.State.CANCELLED,
                TransferManager.snapshot(TransferManager.Kind.DOWNLOAD).state);
    }

    @Test
    public void completedTransferMayBeReplacedByNextTransfer() {
        TransferManager.start(TransferManager.Kind.DOWNLOAD, "one");
        TransferManager.complete(TransferManager.Kind.DOWNLOAD, true);
        assertEquals(TransferManager.State.COMPLETED,
                TransferManager.snapshot(TransferManager.Kind.DOWNLOAD).state);
        TransferManager.start(TransferManager.Kind.DOWNLOAD, "two");
        assertEquals("two", TransferManager.snapshot(TransferManager.Kind.DOWNLOAD).name);
        assertEquals(TransferManager.State.RUNNING,
                TransferManager.snapshot(TransferManager.Kind.DOWNLOAD).state);
    }

    private static final class StubProcess extends Process {
        boolean destroyed;
        @Override public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public int waitFor() { return destroyed ? 143 : 0; }
        @Override public int exitValue() { return destroyed ? 143 : 0; }
        @Override public void destroy() { destroyed = true; }
    }
}
