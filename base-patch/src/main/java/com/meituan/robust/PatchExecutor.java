package com.meituan.robust;

import com.meituan.robust.utils.StringUtils;
import java.lang.reflect.Field;
import java.util.List;

public class PatchExecutor extends Thread {
  protected PatchManipulate patchManipulate;
  protected RobustCallBack robustCallBack;

  public PatchExecutor(PatchManipulate patchManipulate, RobustCallBack robustCallBack) {
    this.patchManipulate = patchManipulate;
    this.robustCallBack = robustCallBack;
  }

  @Override
  public void run() {
    try {
      // 拉取补丁列表
      List<Patch> patches = fetchPatchList();
      // 应用补丁列表
      applyPatchList(patches);
    } catch (Throwable t) {
      robustCallBack.log("robust", "PatchExecutor run:" + t);
      robustCallBack.exceptionNotify(t, "class:PatchExecutor,method:run,line:36");
    }
  }

  /** 拉取补丁列表 */
  protected List<Patch> fetchPatchList() {
    return patchManipulate.fetchPatchList();
  }

  /** 应用补丁列表 */
  protected void applyPatchList(List<Patch> patches) {
    if (null == patches || patches.isEmpty()) {
      return;
    }
    robustCallBack.log("robust", " patchManipulate list size is " + patches.size());
    for (Patch p : patches) {
      if (p.isAppliedSuccess()) {
        robustCallBack.log("robust", "p.isAppliedSuccess() skip " + p.getLocalPath());
        continue;
      }
      if (patchManipulate.ensurePatchExist(p)) {
        boolean currentPatchResult = false;
        try {
          currentPatchResult = patch(p);
        } catch (Throwable t) {
          robustCallBack.exceptionNotify(t, "class:PatchExecutor method:applyPatchList line:69");
        }

        if (currentPatchResult) {
          // 设置patch 状态为成功
          p.setAppliedSuccess(true);
          // 统计PATCH成功率 PATCH成功
          robustCallBack.onPatchApplied(true, p);

        } else {
          // 统计PATCH成功率 PATCH失败
          robustCallBack.onPatchApplied(false, p);
        }

        robustCallBack.log(
            "robust",
            "patch LocalPath:" + p.getLocalPath() + ",apply result " + currentPatchResult);
      }
    }
  }

  protected boolean patch(Patch patch) {
    if (!patchManipulate.verifyPatch(patch)) {
      robustCallBack.logNotify(
          "verifyPatch failure, patch info:"
              + "id = "
              + patch.getName()
              + ",md5 = "
              + patch.getMd5(),
          "class:PatchExecutor method:patch line:107");
      return false;
    }

    ClassLoader classLoader = null;

    try {
      classLoader =
          patchManipulate.getDexClassloader(patch.getTempPath(), patch.getName() + patch.getMd5());
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
    if (null == classLoader) {
      return false;
    }

    Class<?> patchClass, sourceClass;

    Class<?> patchesInfoClass;
    PatchesInfo patchesInfo = null;
    try {
      robustCallBack.log(
          "robust", "patch patch_info_name:" + patch.getPatchesInfoImplClassFullName());
      patchesInfoClass = classLoader.loadClass(patch.getPatchesInfoImplClassFullName());
      patchesInfo = (PatchesInfo) patchesInfoClass.newInstance();
    } catch (Throwable t) {
      robustCallBack.log("robust", "patch failed 188 " + t);
    }

    if (patchesInfo == null) {
      robustCallBack.logNotify(
          "patchesInfo is null, patch info:"
              + "id = "
              + patch.getName()
              + ",md5 = "
              + patch.getMd5(),
          "class:PatchExecutor method:patch line:114");
      return false;
    }

    // classes need to patch
    List<PatchedClassInfo> patchedClasses = patchesInfo.getPatchedClassesInfo();
    if (null == patchedClasses || patchedClasses.isEmpty()) {
      //            robustCallBack.logNotify("patchedClasses is null or empty, patch info:" + "id =
      // " + patch.getName() + ",md5 = " + patch.getMd5(), "class:PatchExecutor method:patch
      // line:122");
      // 手写的补丁有时候会返回一个空list
      return true;
    }

    boolean isClassNotFoundException = false;
    for (PatchedClassInfo patchedClassInfo : patchedClasses) {
      String patchedClassName = patchedClassInfo.patchedClassName;
      String patchClassName = patchedClassInfo.patchClassName;
      if (StringUtils.isEmpty(patchedClassName) || StringUtils.isEmpty(patchClassName)) {
        robustCallBack.logNotify(
            "patchedClasses or patchClassName is empty, patch info:"
                + "id = "
                + patch.getName()
                + ",md5 = "
                + patch.getMd5(),
            "class:PatchExecutor method:patch line:131");
        continue;
      }
      robustCallBack.log("robust", "current path:" + patchedClassName);
      try {
        try {
          sourceClass = classLoader.loadClass(patchedClassName.trim());
        } catch (ClassNotFoundException e) {
          isClassNotFoundException = true;
          continue;
        }

        Field[] fields = sourceClass.getDeclaredFields();
        robustCallBack.log("robust", "oldClass :" + sourceClass + "     fields " + fields.length);
        Field changeQuickRedirectField = null;
        for (Field field : fields) {
          if (StringUtils.equals(
                  field.getType().getCanonicalName(), HotfixChange.class.getCanonicalName())
              && StringUtils.equals(
                  field.getDeclaringClass().getCanonicalName(), sourceClass.getCanonicalName())) {
            changeQuickRedirectField = field;
            break;
          }
        }
        if (changeQuickRedirectField == null) {
          robustCallBack.logNotify(
              "changeQuickRedirectField  is null, patch info:"
                  + "id = "
                  + patch.getName()
                  + ",md5 = "
                  + patch.getMd5(),
              "class:PatchExecutor method:patch line:147");
          robustCallBack.log(
              "robust",
              "current path:"
                  + patchedClassName
                  + " something wrong !! can  not find:ChangeQuickRedirect in"
                  + patchClassName);
          continue;
        }
        robustCallBack.log(
            "robust",
            "current path:" + patchedClassName + " find:ChangeQuickRedirect " + patchClassName);
        try {
          patchClass = classLoader.loadClass(patchClassName);
          Object patchObject = patchClass.newInstance();
          changeQuickRedirectField.setAccessible(true);
          changeQuickRedirectField.set(null, patchObject);
          robustCallBack.log("robust", "changeQuickRedirectField set success " + patchClassName);
        } catch (Throwable t) {
          robustCallBack.log("robust", "patch failed! ");
          robustCallBack.exceptionNotify(t, "class:PatchExecutor method:patch line:163");
        }
      } catch (Throwable t) {
        robustCallBack.log("robust", "patch failed! ");
        //                robustCallBack.exceptionNotify(t, "class:PatchExecutor method:patch
        // line:169");
      }
    }
    robustCallBack.log("robust", "patch finished ");
    return !isClassNotFoundException;
  }
}
