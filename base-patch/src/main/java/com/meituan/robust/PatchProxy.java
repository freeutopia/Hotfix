package com.meituan.robust;

public class PatchProxy {

  public static PatchProxyResult proxy(
      Object[] paramsArray, Object current, HotfixChange redirect, int methodNumber) {
    PatchProxyResult patchProxyResult = new PatchProxyResult();

    try {
      Object[] objects = packObjects(paramsArray, current);
      if (redirect != null && redirect.isSupport(methodNumber)) {
        patchProxyResult.isSupported = true;
        patchProxyResult.result = redirect.accessDispatch(methodNumber, objects);
      }
    } catch (Throwable ignore) {
    }
    return patchProxyResult;
  }

  private static Object[] packObjects(Object[] paramsArray, Object current) {
    if (paramsArray == null) {
      return null;
    }

    int argNum = paramsArray.length;
    Object[] objects = new Object[argNum + 1];

    if (argNum > 0) {
      System.arraycopy(paramsArray, 0, objects, 0, argNum);
    }
    objects[argNum] = current;
    return objects;
  }
}
