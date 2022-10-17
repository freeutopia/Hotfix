package com.meituan.robust;

import java.util.List;

public abstract class PatchManipulate {
  /**
   * 获取补丁列表
   *
   * @param context
   * @return 相应的补丁列表
   */
  protected abstract List<Patch> fetchPatchList();

  /**
   * 验证补丁文件md5是否一致 如果不存在，则动态下载
   *
   * @param context
   * @param patch
   * @return 校验结果
   */
  protected abstract boolean verifyPatch(Patch patch);

  /**
   * 努力确保补丁文件存在，验证md5是否一致。 如果不存在，则动态下载
   *
   * @param patch
   * @return 是否存在
   */
  protected abstract boolean ensurePatchExist(Patch patch);

  /**
   * 获取加载补丁包的classloader
   *
   * @return classloader
   */
  protected abstract ClassLoader getDexClassloader(String dexPath, String optDirectory);
}
