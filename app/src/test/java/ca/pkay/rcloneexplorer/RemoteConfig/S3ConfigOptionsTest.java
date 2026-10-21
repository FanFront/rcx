package ca.pkay.rcloneexplorer.RemoteConfig;

import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class S3ConfigOptionsTest {
    private String option(ArrayList<String> arguments, String name) {
        int at = arguments.indexOf(name);
        assertTrue("Missing option: " + name, at >= 2);
        return arguments.get(at + 1);
    }

    @Test
    public void createsCloudflareR2Remote() {
        ArrayList<String> args = S3ConfigOptions.create("r2", true,
                "ACCESS", "SECRET", "", "abc123def456");
        assertEquals("r2", args.get(0));
        assertEquals("s3", args.get(1));
        assertEquals("Cloudflare", option(args, "provider"));
        assertEquals("false", option(args, "env_auth"));
        assertEquals("ACCESS", option(args, "access_key_id"));
        assertEquals("SECRET", option(args, "secret_access_key"));
        assertEquals("auto", option(args, "region"));
        assertEquals("https://abc123def456.r2.cloudflarestorage.com",
                option(args, "endpoint"));
        assertEquals("private", option(args, "acl"));
        assertEquals("true", option(args, "no_check_bucket"));
    }

    @Test
    public void createsAwsS3RemoteWithoutR2Options() {
        ArrayList<String> args = S3ConfigOptions.create("aws", false,
                "ACCESS", "SECRET", "ap-southeast-1", "");
        assertEquals("s3", args.get(1));
        assertEquals("AWS", option(args, "provider"));
        assertEquals("ap-southeast-1", option(args, "region"));
        assertFalse(args.contains("endpoint"));
        assertFalse(args.contains("no_check_bucket"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingCredentials() {
        S3ConfigOptions.create("r2", true, "", "secret", "", "account");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedAccountId() {
        S3ConfigOptions.create("r2", true, "key", "secret", "",
                "account.example.com/unsafe");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingAwsRegion() {
        S3ConfigOptions.create("s3", false, "key", "secret", "", "");
    }
}
