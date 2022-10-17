package com.meituan.robust;

import java.util.List;

public interface RobustCallBack {
  /**
   * 获取补丁列表后，回调此方法
   *
   * @param result 补丁
   * @param isNet 补丁
   */
  void onPatchListFetched(boolean result, boolean isNet, List<Patch> patches);

  /**
   * 在获取补丁后，回调此方法
   *
   * @param result 结果
   * @param patch 补丁
   */
  void onPatchFetched(boolean result, boolean isNet, Patch patch);

  /**
   * 在补丁应用后，回调此方法
   *
   * @param result 结果
   * @param patch 补丁
   */
  void onPatchApplied(boolean result, Patch patch);

  void logNotify(String log, String where);

  void exceptionNotify(Throwable throwable, String where);

  void log(String name, String message);
}
