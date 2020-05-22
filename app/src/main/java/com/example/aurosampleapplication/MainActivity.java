package com.example.aurosampleapplication;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;


import com.auro.scholr.core.application.AuroApp;
import com.auro.scholr.home.data.model.AuroScholarDataModel;
import com.auro.scholr.home.presentation.view.fragment.QuizHomeFragment;
import com.auro.scholr.util.AppLogger;
import com.auro.scholr.util.AuroScholar;
import com.auro.scholr.util.encryption.Cryptor;
import com.example.aurosampleapplication.databinding.ActivityMainBinding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.btSdk.setOnClickListener(this);
        binding.btOpen.setOnClickListener(this);
       // binding.enterNumber.setText("8178307851");
        binding.btSdk.setVisibility(View.VISIBLE);
       // printHashKey(this);
        String jsonString ="{\"userid\":\"9999999999\",\"mode\":\"W\"}";
        String secret="5d41402abc4b2a76b9719d911017c592";
        String tt=new Cryptor().HMAC_SHA256(secret,jsonString);
        AppLogger.e("chhonker",tt);
    }

    public static void printHashKey(Context pContext) {
        try {
            PackageInfo info = pContext.getPackageManager().getPackageInfo(pContext.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i("printHashKey", "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e("printHashKey", "printHashKey()", e);
        } catch (Exception e) {
            Log.e("printHashKey", "printHashKey()", e);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_sdk:
                    openSdk();
                    hideKeyboard(this);
                break;
            case R.id.bt_open:
                openFragment(new SampleFragment());
                break;

        }
    }

    public static void hideKeyboard(Context context) {
        if (context == null) {
            return;
        }
        View view = ((AppCompatActivity) context).getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void openSdk() {
        AuroScholarDataModel auroScholarDataModel = new AuroScholarDataModel();
        auroScholarDataModel.setMobileNumber("7503600687");
        auroScholarDataModel.setStudentClass("6");
   /*    auroScholarDataModel.setMobileNumber("7503600686");
        auroScholarDataModel.setScholrId("880426");
        auroScholarDataModel.setStudentClass("6");*/
        //auroScholarDataModel.setRegitrationSource("Scholr");
       // auroScholarDataModel.setShareType("telecaller");
       //auroScholarDataModel.setShareIdentity("9681032476");
        auroScholarDataModel.setActivity(this);
        auroScholarDataModel.setFragmentContainerUiId(R.id.home_container);
        openFragment(AuroScholar.openAuroDashboardFragment(auroScholarDataModel));


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //must param to get the acitivity
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void openFragment(Fragment fragment) {
        ((AppCompatActivity) (this)).getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.home_container, fragment, SampleFragment.class
                        .getSimpleName())
                .addToBackStack(null)
                .commitAllowingStateLoss();

    }



}
