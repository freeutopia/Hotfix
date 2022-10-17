package com.utopia.activity;

import com.meituan.robust.Patch;
import com.meituan.robust.PatchManipulate;
import java.util.ArrayList;
import java.util.List;

public class LocalPatchManipulateImp extends PatchManipulate {

  @Override
  protected List<Patch> fetchPatchList() {

    // connect to network to get patch list on servers
    // 在这里去联网获取补丁列表
    Patch patch = new Patch();
    patch.setPatchesInfoImplClassFullName("com.utopia.robust.patch.PatchesInfoImpl");
    List<Patch> patches = new ArrayList<>();
    patches.add(patch);
    return patches;
  }

  @Override
  protected boolean verifyPatch(Patch patch) {
    return true;
  }

  @Override
  protected boolean ensurePatchExist(Patch patch) {
    return true;
  }

  @Override
  protected ClassLoader getDexClassloader(String dexPath, String optDirectory) {
    return null;
  }
}
