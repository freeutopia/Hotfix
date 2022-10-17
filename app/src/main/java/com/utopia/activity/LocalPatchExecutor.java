package com.utopia.activity;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.meituan.robust.HotfixChange;
import com.meituan.robust.Patch;
import com.meituan.robust.PatchedClassInfo;
import com.meituan.robust.PatchesInfo;
import com.meituan.robust.RobustCallBack;
import java.lang.reflect.Field;
import java.util.List;

public class LocalPatchExecutor extends Thread {
  protected Context context;
  protected LocalPatchManipulateImp patchManipulate;
  protected RobustCallBack robustCallBack;

  public LocalPatchExecutor(
      Context context, LocalPatchManipulateImp patchManipulate, RobustCallBack robustCallBack) {
    this.context = context.getApplicationContext();
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
      Log.e("robust", "PatchExecutor run", t);
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
    Log.d("robust", " patchManipulate list size is " + patches.size());
    for (Patch p : patches) {
      if (p.isAppliedSuccess()) {
        Log.d("robust", "p.isAppliedSuccess() skip " + p.getLocalPath());
        continue;
      }
      if (patchManipulate.ensurePatchExist(p)) {
        boolean currentPatchResult = false;
        try {
          currentPatchResult = patch(context, p);
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

        Log.d(
            "robust",
            "patch LocalPath:" + p.getLocalPath() + ",apply result " + currentPatchResult);
      }
    }
  }

  protected boolean patch(Context context, Patch patch) {
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

    ClassLoader classLoader = LocalPatchExecutor.class.getClassLoader();

    Class<?> patchClass, sourceClass;

    Class<?> patchesInfoClass;
    PatchesInfo patchesInfo = null;
    try {
      Log.d("robust", "patch patch_info_name:" + patch.getPatchesInfoImplClassFullName());
      patchesInfoClass = classLoader.loadClass(patch.getPatchesInfoImplClassFullName());
      patchesInfo = (PatchesInfo) patchesInfoClass.newInstance();
    } catch (Throwable t) {
      Log.e("robust", "patch failed 188 ", t);
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
      if (TextUtils.isEmpty(patchedClassName) || TextUtils.isEmpty(patchClassName)) {
        robustCallBack.logNotify(
            "patchedClasses or patchClassName is empty, patch info:"
                + "id = "
                + patch.getName()
                + ",md5 = "
                + patch.getMd5(),
            "class:PatchExecutor method:patch line:131");
        continue;
      }
      Log.d("robust", "current path:" + patchedClassName);
      try {
        try {
          sourceClass = classLoader.loadClass(patchedClassName.trim());
        } catch (ClassNotFoundException e) {
          isClassNotFoundException = true;
          continue;
        }

        Field[] fields = sourceClass.getDeclaredFields();
        Log.d("robust", "oldClass :" + sourceClass + "     fields " + fields.length);
        Field changeQuickRedirectField = null;
        for (Field field : fields) {
          if (TextUtils.equals(
                  field.getType().getCanonicalName(), HotfixChange.class.getCanonicalName())
              && TextUtils.equals(
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
          Log.d(
              "robust",
              "current path:"
                  + patchedClassName
                  + " something wrong !! can  not find:ChangeQuickRedirect in"
                  + patchClassName);
          continue;
        }
        Log.d(
            "robust",
            "current path:" + patchedClassName + " find:ChangeQuickRedirect " + patchClassName);
        try {
          patchClass = classLoader.loadClass(patchClassName);
          Object patchObject = patchClass.newInstance();
          changeQuickRedirectField.setAccessible(true);
          changeQuickRedirectField.set(null, patchObject);
          Log.d("robust", "changeQuickRedirectField set success " + patchClassName);
        } catch (Throwable t) {
          Log.e("robust", "patch failed! ");
          robustCallBack.exceptionNotify(t, "class:PatchExecutor method:patch line:163");
        }
      } catch (Throwable t) {
        Log.e("robust", "patch failed! ");
        //                robustCallBack.exceptionNotify(t, "class:PatchExecutor method:patch
        // line:169");
      }
    }
    Log.d("robust", "patch finished ");
    if (isClassNotFoundException) {
      return false;
    }
    return true;
  }
}
