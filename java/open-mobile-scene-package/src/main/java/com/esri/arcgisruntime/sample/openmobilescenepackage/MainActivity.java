package com.esri.arcgisruntime.sample.openmobilescenepackage;

import java.io.File;

import android.Manifest;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.content.pm.PackageManager;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.MobileScenePackage;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.SceneView;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private SceneView mSceneView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mSceneView = findViewById(R.id.sceneView);
    // create a scene and add it to the scene view
    ArcGISScene scene = new ArcGISScene(Basemap.createImagery());

    // add base surface for elevation data
    final Surface surface = new Surface();
    ArcGISTiledElevationSource elevationSource = new ArcGISTiledElevationSource(
        getString(R.string.elevation_image_service_url));
    surface.getElevationSources().add(elevationSource);
    scene.setBaseSurface(surface);

    requestReadPermission();

  }

  private void checkReadSupport() {
    final String mspkPath =
        Environment.getExternalStorageDirectory() + getString(R.string.config_data_sdcard_offline_dir);

    ListenableFuture<Boolean> isDirectReadSupported = MobileScenePackage.isDirectReadSupportedAsync(mspkPath);
    isDirectReadSupported.addDoneListener(() ->
    {
      try {
        if (isDirectReadSupported.get()) {
          MobileScenePackage directReadMSPK = new MobileScenePackage(mspkPath);
          Log.d(TAG, directReadMSPK.getPath() + "i am here");
          loadMobileScenePackage(directReadMSPK);

        } else {

          String mspkCachePath = getCacheDir().getPath() + "/MSPK";
          Log.d(TAG, mspkCachePath + "file locally unpacked");
          MobileScenePackage.unpackAsync(mspkPath, mspkCachePath).addDoneListener(() -> {
            MobileScenePackage unpackedMSPK = new MobileScenePackage(mspkCachePath);
            loadMobileScenePackage(unpackedMSPK);
          });
        }
      } catch (Exception e) {
        Toast.makeText(MainActivity.this, "Mobile Scene Package direct read could not be determined",
            Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void loadMobileScenePackage(MobileScenePackage mobileScenePackage) {
    mobileScenePackage.loadAsync();

    mobileScenePackage.addDoneLoadingListener(() ->
    {
      if (mobileScenePackage.getLoadStatus() == LoadStatus.LOADED && mobileScenePackage.getScenes().size() > 0) {
        mSceneView.setScene(mobileScenePackage.getScenes().get(0));
      } else {
        Toast.makeText(MainActivity.this, "Failed to load scene package",
            Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  protected void onPause() {
    mSceneView.pause();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mSceneView.resume();
  }

  @Override
  protected void onDestroy() {
    mSceneView.dispose();
    super.onDestroy();
  }

  /**
   * Request read external storage for API level 23+.
   */
  private void requestReadPermission() {
    // define permission to request
    String[] reqPermission = { Manifest.permission.READ_EXTERNAL_STORAGE };
    int requestCode = 2;
    if (ContextCompat.checkSelfPermission(this, reqPermission[0]) == PackageManager.PERMISSION_GRANTED) {
      checkReadSupport();
    } else {
      // request permission
      ActivityCompat.requestPermissions(this, reqPermission, requestCode);
    }
  }

  /**
   * Handle the permissions request response.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      checkReadSupport();
    } else {
      // report to user that permission was denied
      Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

}