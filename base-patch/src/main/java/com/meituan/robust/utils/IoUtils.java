package com.meituan.robust.utils;

import java.io.Closeable;
import java.io.IOException;

public class IoUtils {
  public static void close(Closeable... ios) {
    if (ios != null && ios.length > 0) {
      for (Closeable closeable : ios) {
        if (closeable == null) {
          continue;
        }

        try {
          closeable.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
