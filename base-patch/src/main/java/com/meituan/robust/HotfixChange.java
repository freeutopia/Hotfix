package com.meituan.robust;

public interface HotfixChange {

  Object accessDispatch(int methodId, Object[] params);

  boolean isSupport(int methodId);
}
