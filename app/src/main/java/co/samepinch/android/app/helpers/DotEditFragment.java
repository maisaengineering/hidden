package co.samepinch.android.app.helpers;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.aviary.android.feather.headless.utils.MegaPixels;
import com.aviary.android.feather.library.Constants;
import com.aviary.android.feather.sdk.FeatherActivity;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.R;
import co.samepinch.android.app.SPApplication;
import co.samepinch.android.app.helpers.misc.Permissions;
import co.samepinch.android.app.helpers.module.DaggerStorageComponent;
import co.samepinch.android.app.helpers.module.StorageComponent;
import co.samepinch.android.data.dto.User;
import co.samepinch.android.rest.ReqSetBody;
import co.samepinch.android.rest.Resp;
import co.samepinch.android.rest.RespUserDetails;
import co.samepinch.android.rest.RestClient;

import static co.samepinch.android.app.helpers.AppConstants.API.USERS;

public class DotEditFragment extends Fragment {
    public static final String TAG = "DotEditFragment";
    private static Uri outputFileUri;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.view_avatar)
    SimpleDraweeView mAvatarView;

    @Bind(R.id.input_fname)
    EditText mFNameText;

    @Bind(R.id.input_lname)
    EditText mLNameText;

    @Bind(R.id.input_email)
    EditText mEmailText;

    @Bind(R.id.input_phno)
    EditText mPhoneNumber;
//
//    @Bind(R.id.input_password)
//    EditText mPasswordText;

    @Bind(R.id.input_aboutMe)
    EditText mAboutMe;

    @Bind(R.id.input_change_password)
    TextView mChangePassword;

    @Bind(R.id.input_blogUrl)
    EditText mBlogUrl;

    ProgressDialog progressDialog;
    User mUser;
    Map<String, String> mImageTaskMap;
    View mView;
    String mCurrentPhotoPath = null;
    private LocalHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // progress dialog properties
        progressDialog = new ProgressDialog(getActivity(),
                R.style.dialog);
        progressDialog.setCancelable(Boolean.FALSE);
        mHandler = new LocalHandler(this);

        mImageTaskMap = new HashMap<>();
    }

    @OnClick(R.id.view_avatar)
    public void openImageIntent() {
        Permissions.askPermission(new Permissions.OnActionPermitted() {
            @Override
            public void onPermitted() {
                try {
                    // Determine Uri of camera image to save.
                    final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "SamePinch" + File.separator);
                    root.mkdirs();
                    final String fname = Utils.getUniqueImageFilename();
                    final File sdImageMainDirectory = new File(root, fname);
                    outputFileUri = Uri.fromFile(sdImageMainDirectory);

                    // Camera.
                    final List<Intent> cameraIntents = new ArrayList<>();
                    final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    final PackageManager packageManager = getActivity().getPackageManager();
                    final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
                    cameraIntents.add(createTakePictureIntent());

                    // Filesystem.
                    final Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    // Chooser of filesystem options.
                    final Intent chooserIntent = Intent.createChooser(galleryIntent, "Choose Picture...");

                    // Add the camera options.
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
                    startActivityForResult(chooserIntent, AppConstants.KV.REQUEST_CHOOSE_PICTURE.getIntValue());

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = SPApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private Intent createTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(SPApplication.getContext(),
                        "co.samepinch.android.app.fp",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                return takePictureIntent;
//                startActivityForResult(takePictureIntent, AppConstants.KV.REQUEST_CHOOSE_PICTURE.getIntValue());
            }
        }

        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConstants.KV.REQUEST_CHOOSE_PICTURE.getIntValue()) {
                if (StringUtils.isNotBlank(mCurrentPhotoPath)) {
                    outputFileUri = Uri.parse(mCurrentPhotoPath);
                    mCurrentPhotoPath = null;
                } else {
                    outputFileUri = intent.getData();
                }
                //outputFileUri = (intent == null || MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction())) ? outputFileUri : (intent == null ? null : intent.getData());
                Intent editorIntent = new Intent(getActivity(), FeatherActivity.class);
                editorIntent.setData(outputFileUri);

                editorIntent.putExtra(Constants.EXTRA_IN_API_KEY_SECRET, "df619be610e54ffc");
                editorIntent.putExtra(Constants.EXTRA_IN_HIRES_MEGAPIXELS, MegaPixels.Mp3.ordinal());
                editorIntent.putExtra(Constants.EXTRA_TOOLS_DISABLE_VIBRATION, "any");
                editorIntent.putExtra(Constants.EXTRA_OUTPUT_FORMAT, Bitmap.CompressFormat.JPEG.name());
                editorIntent.putExtra(Constants.EXTRA_OUTPUT_QUALITY, 55);
                startActivityForResult(editorIntent, AppConstants.KV.REQUEST_EDIT_PICTURE.getIntValue());
            } else if (requestCode == AppConstants.KV.REQUEST_EDIT_PICTURE.getIntValue()) {
                Uri processedImageUri = Uri.parse("file://" + intent.getData());

                Bundle extra = intent.getExtras();
                if (null != extra) {
                    // image has been changed by the user?
                    boolean changed = extra.getBoolean(Constants.EXTRA_OUT_BITMAP_CHANGED);
                }
                try {
                    InputStream localImageIS = getActivity().getContentResolver().openInputStream(Uri.parse(processedImageUri.toString()));
                    byte[] localImageBytes = Utils.getBytes(localImageIS);
                    String localImageEnc = Base64.encodeToString(localImageBytes, Base64.DEFAULT);

                    mImageTaskMap.clear();
                    mImageTaskMap.put(processedImageUri.toString(), null);
                    new ImageUploadTask().execute("droid.jpeg", localImageEnc, processedImageUri.toString());

                    mAvatarView.setImageURI(processedImageUri);
                    mAvatarView.refreshDrawableState();
                } catch (Exception e) {
                    // muted
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (!Utils.isLoggedIn()) {
            getActivity().finish();
        }

        mView = inflater.inflate(R.layout.dot_edit, container, false);
        ButterKnife.bind(this, mView);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            String userStr = Utils.PreferencesManager.getInstance().getValue(AppConstants.API.PREF_AUTH_USER.getValue());
            Gson gson = new Gson();
            mUser = gson.fromJson(userStr, User.class);
        } catch (Exception e) {
            // muted
            getActivity().finish();
        }

        setupData(mUser);


        toolbar.setTitle(StringUtils.EMPTY);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hack to get click working
                (getActivity()).onBackPressed();
            }
        });

        // display change password conditionally
        mChangePassword.setVisibility(Utils.isLoggedInViaEmailPassword() ? View.VISIBLE : View.GONE);

        return mView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.dot_edit_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                (getActivity()).onBackPressed();
                return true;

            case R.id.menuitem_update:
                saveAction(299);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupData(User user) {
        if (StringUtils.isNotBlank(user.getPhoto())) {
            mAvatarView.setImageURI(Uri.parse(user.getPhoto()));
        }

        mFNameText.setText(user.getFname());
        mLNameText.setText(user.getLname());
        mEmailText.setText(user.getEmail());
        mPhoneNumber.setText(user.getPhno());
        mAboutMe.setText(user.getSummary());
        mBlogUrl.setText(user.getBlog());
    }

    public void saveAction(final int delay) {
        if (!validate()) {
            return;
        }
        User user = new User();
        boolean hasChanges = false;
        String fName = mFNameText.getText().toString();
        if (!StringUtils.equals(fName, mUser.getFname())) {
            hasChanges = true;
            user.setFname(fName);
        }

        String lName = mLNameText.getText().toString();
        if (!StringUtils.equals(lName, mUser.getLname())) {
            hasChanges = true;
            user.setLname(lName);
        }

        String phno = mPhoneNumber.getText().toString();
        if (!StringUtils.equals(phno, mUser.getEmail())) {
            hasChanges = true;
            user.setPhno(phno);
        }

        String email = mEmailText.getText().toString();
        if (!StringUtils.equals(email, mUser.getEmail())) {
            hasChanges = true;
            user.setEmail(email);
        }

        String aboutMe = mAboutMe.getText().toString();
        if (!StringUtils.equals(aboutMe, mUser.getSummary())) {
            hasChanges = true;
            user.setSummary(aboutMe);
        }

        String blog = mBlogUrl.getText().toString();
        if (!StringUtils.equals(blog, mUser.getBlog())) {
            hasChanges = true;
            user.setBlog(blog);
        }

        if (!mImageTaskMap.isEmpty()) {
            hasChanges = true;
            String imgVal = null;
            for (Map.Entry<String, String> entry : mImageTaskMap.entrySet()) {
                imgVal = entry.getValue();
                break;
            }
            if (imgVal == null && delay > 1) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int nextCheck = delay % 2;
                        saveAction(nextCheck > 0 ? nextCheck : 99);
                    }
                }, delay);
                return;
            } else {
                user.setImageKey(imgVal);
            }
        }

        // check if has changes
        if (!hasChanges) {
            Snackbar.make(mView, "no changes detected...", Snackbar.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }

        progressDialog.setMessage("updating...");
        progressDialog.show();

        new DotUpdateTask().execute(user);
    }

    public boolean validate() {
        boolean valid = true;

//        String fName = mFNameText.getText().toString();
//        String lName = mLNameText.getText().toString();
        String email = mEmailText.getText().toString();
        String blogUrl = mBlogUrl.getText().toString();

//        if (fName.isEmpty() || fName.length() < 1) {
//            mFNameText.setError("at least 1 character");
//            valid = false;
//        } else {
//            mFNameText.setError(null);
//        }
//
//        if (lName.isEmpty() || lName.length() < 1) {
//            mLNameText.setError("at least 1 character");
//            valid = false;
//        } else {
//            mLNameText.setError(null);
//        }

        if (email.isEmpty() || !Utils.isValidEmail(email)) {
            mEmailText.setError("enter a valid email address");
            valid = false;
        } else {
            mEmailText.setError(null);
        }


        if (StringUtils.isNotBlank(blogUrl) && !Utils.isValidUri(blogUrl)) {
            mBlogUrl.setError("must be a valid url");
            valid = false;
        } else {
            mBlogUrl.setError(null);
        }

        return valid;
    }

    @OnClick(R.id.input_change_password)
    public void changePasswordClick() {
        View passwordChangeView = LayoutInflater.from(getContext()).inflate(R.layout.change_password, null);
        final EditText currPassView = (EditText) passwordChangeView.findViewById(R.id.input_password_curr);
        final EditText newPassView = (EditText) passwordChangeView.findViewById(R.id.input_password_new);
        final EditText confirmPassView = (EditText) passwordChangeView.findViewById(R.id.input_password_new_confirm);

        new MaterialDialog.Builder(getContext())
                .theme(Theme.LIGHT)
                .title(R.string.change_password_title)
                .customView(passwordChangeView, Boolean.TRUE)
                .positiveText(R.string.change_password_positive)
                .negativeText(R.string.change_password_negative)
                .autoDismiss(false)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        boolean hasErr = false;

                        String curr = currPassView.getText().toString();
                        String nev = newPassView.getText().toString();
                        String confirm = confirmPassView.getText().toString();

                        // validate
                        if (StringUtils.isBlank(curr)) {
                            currPassView.setError(getString(R.string.reqd_err));
                            hasErr = true;
                        } else if (StringUtils.isBlank(nev)) {
                            newPassView.setError(getString(R.string.reqd_err));
                            hasErr = true;
                        } else if (StringUtils.isBlank(confirm)) {
                            confirmPassView.setError(getString(R.string.reqd_err));
                            hasErr = true;
                        } else if (!StringUtils.equals(nev, confirm)) {
                            newPassView.setError(getString(R.string.pass_must_match));
                            confirmPassView.setError(getString(R.string.pass_must_match));
                            hasErr = true;
                        }

                        if (!hasErr) {
                            //continue
                            new ChangePasswordTask().execute(curr, nev, confirm);
                            dialog.dismiss();
                            progressDialog.setMessage("changing password...");
                            progressDialog.show();
                        }
                    }
                })
                .show();
    }


    private static final class LocalHandler extends Handler {
        private final WeakReference<DotEditFragment> mActivity;

        public LocalHandler(DotEditFragment parent) {
            mActivity = new WeakReference<DotEditFragment>(parent);
        }
    }

    private class UpdateDotTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... tags) {
            return false;
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (isRemoving()) {
                return;
            }
            Utils.dismissSilently(progressDialog);
        }
    }

    private class DotUpdateTask extends AsyncTask<User, Integer, User> {
        @Override
        protected User doInBackground(User... users) {
            if (users == null || users.length < 1) {
                return null;
            }

            try {
                ReqSetBody req = new ReqSetBody();
                // set base args
                req.setToken(Utils.getNonBlankAppToken());
                req.setCmd("update");

                Map<String, String> args = new HashMap<>();
                Gson gson = new Gson();
                String userStr = gson.toJson(users[0]);

                Type mapType = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> body = gson.fromJson(userStr, mapType);

                // set body
                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());
                HttpEntity<ReqSetBody> payloadEntity = new HttpEntity<>(req, headers);

                ResponseEntity<RespUserDetails> resp = RestClient.INSTANCE.handle().exchange(USERS.getValue(), HttpMethod.POST, payloadEntity, RespUserDetails.class);
                User updated;
                if (resp != null && resp.getBody() != null && (updated = resp.getBody().getBody()) != null) {
                    return updated;
                }
            } catch (Exception e) {
                // muted
            }
            return null;
        }

        @Override
        protected void onPostExecute(User updatedUser) {
            Utils.dismissSilently(progressDialog);
            try {
                if (updatedUser != null) {
                    Gson gson = new Gson();
                    // update stored user info
                    String userStr = Utils.PreferencesManager.getInstance().getValue(AppConstants.API.PREF_AUTH_USER.getValue());
                    User user = gson.fromJson(userStr, User.class);
                    user.setFname(updatedUser.getFname());
                    user.setLname(updatedUser.getLname());
                    user.setEmail(updatedUser.getEmail());
                    user.setPinchHandle(updatedUser.getPinchHandle());
                    user.setSummary(updatedUser.getSummary());
                    user.setBlog(updatedUser.getBlog());
                    user.setBadges(updatedUser.getBadges());
                    user.setPhoto(updatedUser.getPhoto());
                    user.setPhno(updatedUser.getPhno());
                    user.setVerified(updatedUser.getVerified());
                    user.setImageKey(updatedUser.getImageKey());
                    Utils.PreferencesManager.getInstance().setValue(AppConstants.API.PREF_AUTH_USER.getValue(), gson.toJson(user));

                    // flag to refresh wall
                    Utils.PreferencesManager.getInstance().setValue(AppConstants.APP_INTENT.KEY_FRESH_WALL_FLAG.getValue(), Boolean.TRUE.toString());

                    // local setup
                    mUser = user;
                    setupData(mUser);
                    Snackbar.make(mView, "updated successfully.", Snackbar.LENGTH_SHORT).show();
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();

                    return;
                }
            } catch (Exception e) {
                // muted
            }
            Snackbar.make(mView, AppConstants.APP_INTENT.KEY_MSG_GENERIC_ERR.getValue(), Snackbar.LENGTH_LONG).show();
        }
    }

    class ChangePasswordTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            Bundle respBundle = new Bundle();

            try {
                StorageComponent component = DaggerStorageComponent.create();
                ReqSetBody req = component.provideReqSetBody();
                // set base args
                req.setToken(Utils.getNonBlankAppToken());
                req.setCmd("change_password");

                Map<String, String> body = new HashMap<>();
                body.put("current_password", args[0]);
                body.put("password", args[1]);
                body.put("password_confirmation", args[2]);

                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());

                HttpEntity<ReqSetBody> payloadEntity = new HttpEntity<>(req, headers);
                ResponseEntity<Resp> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.USERS.getValue(), HttpMethod.POST, payloadEntity, Resp.class);
                return resp.getBody().getMessage();
            } catch (Exception e) {
                Resp resp = Utils.parseAsRespSilently(e);
                if (resp != null && StringUtils.isNotBlank(resp.getMessage())) {
                    return resp.getMessage();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Utils.dismissSilently(progressDialog);

            if (result == null) {
                Snackbar.make(mView, AppConstants.APP_INTENT.KEY_MSG_GENERIC_ERR.getValue(), Snackbar.LENGTH_SHORT).show();
                return;
            }
            Snackbar.make(mView, result, Snackbar.LENGTH_LONG).show();
        }
    }

    class ImageUploadTask extends AsyncTask<String, Integer, Bundle> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bundle doInBackground(String... args) {
            Bundle respBundle = new Bundle();

            try {
                StorageComponent component = DaggerStorageComponent.create();
                ReqSetBody req = component.provideReqSetBody();
                // set base args
                req.setToken(Utils.getNonBlankAppToken());
                req.setCmd("s3upload");

                Map<String, String> body = new HashMap<>();
                body.put("name", args[0]);
                body.put("content", args[1]);
                body.put("content_type", "image/jpeg");

                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());

                HttpEntity<ReqSetBody> payloadEntity = new HttpEntity<>(req, headers);
                ResponseEntity<Resp> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.USERS.getValue(), HttpMethod.POST, payloadEntity, Resp.class);
                if (resp.getBody() != null) {
                    Map<String, Object> respBody = resp.getBody().getBody();
                    if (respBody != null) {
                        String v = (String) respBody.get(AppConstants.APP_INTENT.KEY_KEY.getValue());
                        respBundle.putString(args[2], v);
                        return respBundle;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "err uploading...");

            }
            return respBundle;
        }

        @Override
        protected void onPostExecute(Bundle result) {
            Utils.dismissSilently(progressDialog);

            if (result == null || mImageTaskMap.isEmpty()) {
                return;
            }
            for (String k : result.keySet()) {
                mImageTaskMap.put(k, result.getString(k));
            }
        }
    }
}