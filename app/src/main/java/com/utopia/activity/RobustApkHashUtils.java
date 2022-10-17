package com.utopia.activity;

import android.content.Context;
import com.meituan.robust.Constants;
import com.meituan.robust.utils.StringUtils;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

public class RobustApkHashUtils {
  private static String robustApkHashValue;

  public static String readRobustApkHash(Context context) {

    if (StringUtils.isEmpty(robustApkHashValue)) {
      robustApkHashValue = readRobustApkHashFile(context);
    }

    return robustApkHashValue;
  }

  private static String readRobustApkHashFile(Context context) {
    String value = "";
    if (null == context) {
      return value;
    }

    try {
      value = readFirstLine(context, Constants.ROBUST_APK_HASH_FILE_NAME);
    } catch (Throwable ignored) {

    }

    return value;
  }

  private static String readFirstLine(Context context, String fileName) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));

      return reader.readLine();
    } catch (IOException e) {
      return "";
    } finally {
      close(reader);
    }
  }

  private static void close(Closeable io) {
    if (io != null) {
      try {
        io.close();
      } catch (IOException ignored) {

      }
    }
  }
}
