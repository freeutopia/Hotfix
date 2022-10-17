package com.meituan.robust.utils;

import com.meituan.robust.HotfixChange;
import java.util.Map;
import java.util.WeakHashMap;

public class PatchTemplate implements HotfixChange {
  public static final String MATCH_ALL_PARAMETER = "(\\w*\\.)*\\w*";

  public PatchTemplate() {}

  private static final Map<Object, Object> keyToValueRelation = new WeakHashMap<>();

  @Override
  public Object accessDispatch(int methodId, Object[] params) {
    return null;
  }

  @Override
  public boolean isSupport(int methodId) {
    return true;
  }

  // 解决boolean被优化成byte的问题
  private static Object fixObj(Object booleanObj) {
    if (booleanObj instanceof Byte) {
      byte byteValue = (Byte) booleanObj;
      boolean booleanValue = byteValue != 0x00;
      return new Boolean(booleanValue);
    }
    return booleanObj;
  }
}
