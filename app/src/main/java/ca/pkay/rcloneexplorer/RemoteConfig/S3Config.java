package ca.pkay.rcloneexplorer.RemoteConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import ca.pkay.rcloneexplorer.R;

/** Native remote setup for Amazon S3 and Cloudflare R2 via rclone's s3 backend. */
public class S3Config extends Fragment {
    private static final String ARG_R2 = "cloudflare_r2";

    private boolean cloudflareR2;
    private TextInputLayout nameLayout;
    private TextInputLayout accessLayout;
    private TextInputLayout secretLayout;
    private TextInputLayout regionLayout;
    private TextInputLayout accountLayout;

    private EditText remoteName;
    private EditText accessKey;
    private EditText secretKey;
    private EditText region;
    private EditText accountId;

    public S3Config() {}

    public static S3Config newInstance(boolean cloudflareR2) {
        S3Config fragment = new S3Config();
        Bundle args = new Bundle();
        args.putBoolean(ARG_R2, cloudflareR2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cloudflareR2 = getArguments() != null && getArguments().getBoolean(ARG_R2);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.remote_config_form, container, false);
        ViewGroup formContent = view.findViewById(R.id.form_content);

        nameLayout = view.findViewById(R.id.remote_name_layout);
        nameLayout.setVisibility(View.VISIBLE);
        remoteName = view.findViewById(R.id.remote_name);

        View access = addField(formContent, R.string.s3_access_key_id, false);
        accessLayout = access.findViewById(R.id.text_input_layout);
        accessKey = access.findViewById(R.id.edit_text);

        View secret = addField(formContent, R.string.s3_secret_access_key, true);
        secretLayout = secret.findViewById(R.id.pass_input_layout);
        secretKey = secret.findViewById(R.id.pass);

        if (cloudflareR2) {
            View account = addField(formContent, R.string.r2_account_id, false);
            accountLayout = account.findViewById(R.id.text_input_layout);
            accountId = account.findViewById(R.id.edit_text);
            TextView help = account.findViewById(R.id.helper_text);
            help.setText(R.string.r2_account_id_help);
            help.setVisibility(View.VISIBLE);
        } else {
            View awsRegion = addField(formContent, R.string.s3_region, false);
            regionLayout = awsRegion.findViewById(R.id.text_input_layout);
            region = awsRegion.findViewById(R.id.edit_text);
            region.setText("us-east-1");
        }

        view.findViewById(R.id.next).setOnClickListener(v -> saveRemote());
        view.findViewById(R.id.cancel).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        return view;
    }

    private View addField(ViewGroup parent, int label, boolean password) {
        View field = View.inflate(requireContext(), password
                ? R.layout.config_form_template_password
                : R.layout.config_form_template_edit_text, null);
        field.setPadding(0, 0, 0,
                getResources().getDimensionPixelOffset(R.dimen.config_form_template));
        parent.addView(field);
        TextInputLayout layout = field.findViewById(
                password ? R.id.pass_input_layout : R.id.text_input_layout);
        layout.setHint(getString(label));
        return field;
    }

    private boolean validate(TextInputLayout layout, EditText input, int errorText) {
        boolean valid = input != null && !input.getText().toString().trim().isEmpty();
        layout.setError(valid ? null : getString(errorText));
        return valid;
    }

    private void saveRemote() {
        boolean valid = validate(nameLayout, remoteName, R.string.remote_name_cannot_be_empty);
        valid = validate(accessLayout, accessKey, R.string.required_field) & valid;
        valid = validate(secretLayout, secretKey, R.string.required_field) & valid;
        if (cloudflareR2) {
            valid = validate(accountLayout, accountId, R.string.required_field) & valid;
            if (valid && !accountId.getText().toString().trim().matches("[a-zA-Z0-9]+")) {
                accountLayout.setError(getString(R.string.r2_account_id_invalid));
                valid = false;
            }
        } else {
            valid = validate(regionLayout, region, R.string.required_field) & valid;
        }
        if (!valid) {
            return;
        }

        ArrayList<String> options = S3ConfigOptions.create(
                remoteName.getText().toString(), cloudflareR2,
                accessKey.getText().toString(), secretKey.getText().toString(),
                cloudflareR2 ? "" : region.getText().toString(),
                cloudflareR2 ? accountId.getText().toString() : "");

        RemoteConfigHelper.setupAndWait(requireContext(), options);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
