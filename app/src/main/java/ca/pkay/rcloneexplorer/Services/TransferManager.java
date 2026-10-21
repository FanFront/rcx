package ca.pkay.rcloneexplorer.Services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-process status for the active upload and download. IntentService serializes
 * work in each direction, so at most one process of each kind runs at a time.
 * No state is persisted: after a process restart there cannot be a live job
 * owned by the old process to display or cancel.
 */
public final class TransferManager {
    public enum Kind { UPLOAD, DOWNLOAD }
    public enum State { IDLE, RUNNING, CANCELLING, CANCELLED, COMPLETED, FAILED }

    public static final class Snapshot {
        public final State state;
        public final String name;
        public final String details;
        public final int percent;

        private Snapshot(State state, String name, String details, int percent) {
            this.state = state;
            this.name = name;
            this.details = details;
            this.percent = percent;
        }
    }

    private static final Pattern PERCENT = Pattern.compile(",\\s*(\\d{1,3})%");
    private static final Slot UPLOAD = new Slot();
    private static final Slot DOWNLOAD = new Slot();

    private static final class Slot {
        State state = State.IDLE;
        String name = "";
        String details = "";
        int percent = -1;
        Process process;
        boolean cancelRequested;
    }

    private TransferManager() { }

    private static Slot slot(Kind kind) {
        return kind == Kind.UPLOAD ? UPLOAD : DOWNLOAD;
    }

    public static synchronized void start(Kind kind, String name) {
        Slot slot = slot(kind);
        slot.state = State.RUNNING;
        slot.name = name == null ? "" : name;
        slot.details = "";
        slot.percent = -1;
        slot.cancelRequested = false;
        slot.process = null;
    }

    /** If the user cancels before rclone starts, kill it as soon as it appears. */
    public static synchronized void attach(Kind kind, Process process) {
        Slot slot = slot(kind);
        if (slot.cancelRequested) {
            if (process != null) {
                process.destroy();
            }
        } else {
            slot.process = process;
        }
    }

    public static synchronized void update(Kind kind, String details) {
        Slot slot = slot(kind);
        if (slot.state != State.RUNNING || details == null || details.isEmpty()) {
            return;
        }
        slot.details = details;
        int percent = parsePercent(details);
        if (percent >= 0) {
            slot.percent = percent;
        }
    }

    /** Cancel only the active transfer in the requested direction. */
    public static synchronized boolean cancel(Kind kind) {
        Slot slot = slot(kind);
        if (slot.state != State.RUNNING && slot.state != State.CANCELLING) {
            return false;
        }
        slot.cancelRequested = true;
        slot.state = State.CANCELLING;
        if (slot.process != null) {
            slot.process.destroy();
            slot.process = null;
        }
        return true;
    }

    public static synchronized boolean wasCancelled(Kind kind) {
        return slot(kind).cancelRequested;
    }

    public static synchronized void complete(Kind kind, boolean success) {
        Slot slot = slot(kind);
        slot.state = slot.cancelRequested ? State.CANCELLED
                : success ? State.COMPLETED : State.FAILED;
        if (success && !slot.cancelRequested) {
            slot.percent = 100;
        }
        slot.process = null;
    }

    public static synchronized Snapshot snapshot(Kind kind) {
        Slot slot = slot(kind);
        return new Snapshot(slot.state, slot.name, slot.details, slot.percent);
    }

    /** Return -1 when rclone does not provide a whole-transfer percentage. */
    public static int parsePercent(String text) {
        if (text == null) {
            return -1;
        }
        Matcher matcher = PERCENT.matcher(text);
        if (!matcher.find()) {
            return -1;
        }
        int value = Integer.parseInt(matcher.group(1));
        return value <= 100 ? value : -1;
    }
}
