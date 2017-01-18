package com.rohan.callnote;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.rohan.callnote.models.User;
import com.rohan.callnote.network.APIClient;
import com.rohan.callnote.network.response.ApiResponse;
import com.rohan.callnote.utils.Constants;
import com.rohan.callnote.utils.UserUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BaseCallNoteActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    GoogleApiClient mGoogleApiClient;
    private static BaseCallNoteActivity instance;
    private ProgressDialog signInProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        instance = this;

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        switchFragment(new LoginFragment(), LoginFragment.class.getSimpleName());

        signInProgressDialog = new ProgressDialog(this);
        signInProgressDialog.setMessage("Signing in...");
        signInProgressDialog.setCancelable(false);
        signInProgressDialog.setCanceledOnTouchOutside(false);
    }

    public static BaseCallNoteActivity getInstance() {
        return instance;
    }

    public void signIn() {

        GoogleSignInOptions googleSignInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.server_client_id))
                        .requestEmail()
                        .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, Constants.GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == Constants.GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()) {
            Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();

            GoogleSignInAccount acct = result.getSignInAccount();

            String name = acct.getDisplayName();
            String email = acct.getEmail();
            String token = acct.getIdToken();

            Call<ApiResponse<User>> call = APIClient.getApiService().signUp(name, email, token);
            call.enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    if (response.isSuccessful()) {
                        User user = response.body().getData();
                        UserUtil.saveUser(user);
                        switchFragment(new NotesFragment(), false, NotesFragment.class.getSimpleName());
                        dismissSignInProgressDialog();
                    } else {
                        Toast.makeText(BaseCallNoteActivity.this, "Failed to sign in. Please try later.", Toast.LENGTH_SHORT).show();
                        dismissSignInProgressDialog();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    Log.i("sign up failed", t.getMessage());
                    Toast.makeText(BaseCallNoteActivity.this, "Unable to sign in right now. Please try later.", Toast.LENGTH_SHORT).show();
                    dismissSignInProgressDialog();
                }
            });

        } else {
            Toast.makeText(BaseCallNoteActivity.this, "Unable to sign in right now. Please try later.", Toast.LENGTH_SHORT).show();
            dismissSignInProgressDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_sign_out:
                signOut();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        Toast.makeText(BaseCallNoteActivity.this, "Signed Out", Toast.LENGTH_SHORT).show();
                        UserUtil.logout();
                        switchFragment(new LoginFragment(), LoginFragment.class.getSimpleName());
                    }
                }
        );
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    /**
     * Method Called to replace the to replace the container i.e. R.id.containerView  with the new fragment
     *
     * @param fragment pass value of the fragment will replace the container
     */
    public void switchFragment(Fragment fragment) {
        switchFragment(fragment, null);
    }

    /**
     * Method Called to replace the to replace the container i.e. R.id.containerView  with the new fragment
     *
     * @param fragment pass value of the fragment will replace the container
     * @param tag
     */
    public void switchFragment(Fragment fragment, String tag) {
        switchFragment(fragment, false, null, tag);
    }

    /**
     * Method Called to replace the to replace the container i.e. R.id.containerView  with the new fragment
     *
     * @param fragment       pass value of the fragment will replace the container
     * @param addToBackStack
     * @param tag
     */
    public void switchFragment(Fragment fragment, boolean addToBackStack, String tag) {
        switchFragment(fragment, addToBackStack, null, tag);
    }

    /**
     * Method Called to replace the to replace the container i.e. R.id.containerView  with the new fragment
     *
     * @param fragment
     * @param addToBackStack
     * @param bundle
     * @param tag
     */
    public void switchFragment(Fragment fragment, boolean addToBackStack, Bundle bundle, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }

        if (bundle != null) {
            fragment.setArguments(bundle);
        }
        fragmentTransaction.replace(R.id.containerView, fragment, tag).commit();

    }

    public void showSignInProgressDialog() {
        signInProgressDialog.show();
    }

    public void dismissSignInProgressDialog() {
        signInProgressDialog.dismiss();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }
}
