package com.meituan.robust;

public class PatchedClassInfo {
  public String patchedClassName;
  public String patchClassName;

  public PatchedClassInfo(String patchedClassName, String patchClassName) {
    this.patchedClassName = patchedClassName;
    this.patchClassName = patchClassName;
  }
}
