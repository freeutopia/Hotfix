package com.utopia.robust;

import android.util.Log;
import com.meituan.robust.annotaion.Modify;

public class Test {
  private static final Object A = new MainActivity();
  private String test = "crash-3!";

  static {
    Log.e("lt-log", "---");
  }

  public Test() {}

  public Test(String test) {
    this.test = test;
  }

  @Modify
  public String getMessage(String msg) {
    return "test:" + msg;
  }

  public Boolean getMessage(String msg, String msg2) {
    return false;
  }

  public int getMessage() {
    return 1;
  }
}
