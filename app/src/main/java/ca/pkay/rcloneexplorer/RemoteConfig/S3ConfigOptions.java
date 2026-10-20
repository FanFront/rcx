package ca.pkay.rcloneexplorer.RemoteConfig;

import java.util.ArrayList;

/**
 * Builds "rclone config create" arguments for AWS S3 and Cloudflare R2.
 * This class has no Android dependencies so its behavior can be unit tested.
 */
public final class S3ConfigOptions {
    private S3ConfigOptions() {}

    public static ArrayList<String> create(String name, boolean cloudflareR2,
                                           String accessKey, String secretKey,
                                           String region, String accountId) {
        name = clean(name);
        accessKey = clean(accessKey);
        secretKey = clean(secretKey);
        region = clean(region);
        accountId = clean(accountId);

        if (name.isEmpty() || accessKey.isEmpty() || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Name and access credentials are required");
        }
        if (cloudflareR2) {
            if (!accountId.matches("[a-zA-Z0-9]+")) {
                throw new IllegalArgumentException("Invalid Cloudflare account ID");
            }
        } else if (region.isEmpty()) {
            throw new IllegalArgumentException("AWS region is required");
        }

        ArrayList<String> args = new ArrayList<>();
        args.add(name);
        args.add("s3");
        add(args, "provider", cloudflareR2 ? "Cloudflare" : "AWS");
        add(args, "env_auth", "false");
        add(args, "access_key_id", accessKey);
        add(args, "secret_access_key", secretKey);
        add(args, "region", cloudflareR2 ? "auto" : region);

        if (cloudflareR2) {
            add(args, "endpoint", "https://" + accountId + ".r2.cloudflarestorage.com");
            add(args, "acl", "private");
            // Bucket-scoped R2 tokens often cannot check/create buckets.
            add(args, "no_check_bucket", "true");
        }
        return args;
    }

    private static void add(ArrayList<String> args, String key, String value) {
        args.add(key);
        args.add(value);
    }

    private static String clean(String input) {
        return input == null ? "" : input.trim();
    }
}
