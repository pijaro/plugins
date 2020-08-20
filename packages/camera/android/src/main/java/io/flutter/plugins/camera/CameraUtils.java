package io.flutter.plugins.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.util.Range;
import android.util.Size;
import android.util.Log;
import io.flutter.plugins.camera.Camera.ResolutionPreset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Provides various utilities for camera. */
public final class CameraUtils {

  private CameraUtils() {}

  static Size computeBestPreviewSize(String cameraName, ResolutionPreset preset) {
    if (preset.ordinal() > ResolutionPreset.high.ordinal()) {
      preset = ResolutionPreset.high;
    }

    CamcorderProfile profile =
        getBestAvailableCamcorderProfileForResolutionPreset(cameraName, preset);
    return new Size(profile.videoFrameWidth, profile.videoFrameHeight);
  }

  static List<Size> getCaptureSizes(StreamConfigurationMap streamConfigurationMap) {
    return Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG));
  }

  static Size computeBestCaptureSize(StreamConfigurationMap streamConfigurationMap) {
    // For still image captures, we use the largest available size.
    return Collections.max(
        Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
        new CompareSizesByArea());
  }

  public static List<Map<String, Object>> getAvailableCameras(Activity activity)
      throws CameraAccessException {
    CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    String[] cameraNames = cameraManager.getCameraIdList();
    List<Map<String, Object>> cameras = new ArrayList<>();
    for (String cameraName : cameraNames) {
      HashMap<String, Object> details = new HashMap<>();
      CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraName);
      details.put("name", cameraName);
      int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
      details.put("sensorOrientation", sensorOrientation);

      int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
      switch (lensFacing) {
        case CameraMetadata.LENS_FACING_FRONT:
          details.put("lensFacing", "front");
          break;
        case CameraMetadata.LENS_FACING_BACK:
          details.put("lensFacing", "back");
          break;
        case CameraMetadata.LENS_FACING_EXTERNAL:
          details.put("lensFacing", "external");
          break;
      }

      StreamConfigurationMap streamConfigurationMap =
        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      List<String> sizesStringList = new ArrayList<>();

      float[] apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
      List<Float> aperturesList = new ArrayList<>();
      if(apertures != null) {
        for (float f : apertures) {
          aperturesList.add(f);
        }
        details.put("apertures", aperturesList);
        Log.w("tag", "Apertures: " + aperturesList.toString());
      } else {
        Log.w("tag", "Apertures None");
      }

      float[] focal_lengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
      List<Float> focalsList = new ArrayList<>();
      if(focal_lengths != null) {
        for (float f : focal_lengths) {
          focalsList.add(f);
        }
        details.put("focal_lengths", focalsList);
        Log.w("tag", "Focal l: " + focalsList.toString());
      } else {
        Log.w("tag", "Focal None");
      }

      Range<Long> exposure_time_ranges = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
      List<Long> listsOfExposures = new ArrayList<>();
      if(exposure_time_ranges != null) {
        listsOfExposures.add(exposure_time_ranges.getLower());
        listsOfExposures.add(exposure_time_ranges.getUpper());
        Log.w("tag", "Exposures: " + exposure_time_ranges.toString());
      } else {
        Log.w("tag", "EXPOSURE NULL");
      }
      details.put("exposure_time_range", listsOfExposures);

      Range<Integer> sensitivity_ranges = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
      List<Integer> listsOfSensitivityRanges = new ArrayList<>();
      if(sensitivity_ranges != null) {
        listsOfSensitivityRanges.add(sensitivity_ranges.getLower());
        listsOfSensitivityRanges.add(sensitivity_ranges.getUpper());
        Log.w("tag", "Sensitivity ranges: " + sensitivity_ranges.toString());
      } else {
        Log.w("tag", "Sensitivity NULL");
      }
      details.put("sensitivity_range", listsOfSensitivityRanges);


      List<Size> sizesList = getCaptureSizes(streamConfigurationMap);
      for(Size s : sizesList) {
        sizesStringList.add(s.toString());
      }
      details.put("outputs", sizesStringList);

      cameras.add(details);
    }
    return cameras;
  }

  static CamcorderProfile getBestAvailableCamcorderProfileForResolutionPreset(
      String cameraName, ResolutionPreset preset) {
    int cameraId = Integer.parseInt(cameraName);
    switch (preset) {
        // All of these cases deliberately fall through to get the best available profile.
      case max:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }
      case ultraHigh:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_2160P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_2160P);
        }
      case veryHigh:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
        }
      case high:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
        }
      case medium:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        }
      case low:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QVGA);
        }
      default:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
        } else {
          throw new IllegalArgumentException(
              "No capture session available for current capture session.");
        }
    }
  }

  private static class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow.
      return Long.signum(
          (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
  }
}
