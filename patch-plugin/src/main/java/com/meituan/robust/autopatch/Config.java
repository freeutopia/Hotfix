package com.meituan.robust.autopatch;

import static com.meituan.robust.Constants.DEFAULT_MAPPING_FILE;

import com.meituan.robust.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.ClassPool;
import javassist.CtMethod;

/**
 * Created by mivanzhang on 16/12/2.
 *
 * <p>members read from robust.xml
 */
public final class Config {
  public static boolean catchReflectException = false;
  public static boolean supportProGuard = true;
  public static boolean isLogging = true;
  public static boolean isManual = false;
  public static String patchPackageName = Constants.PATCH_PACKAGENAME;
  public static String mappingFilePath;
  public static final Set<String> patchMethodSignatureSet = new HashSet<>();
  public static final List<String> newlyAddedClassNameList = new ArrayList<>();
  public static final Set<String> newlyAddedMethodSet = new HashSet<>();
  public static final List<String> modifiedClassNameList = new ArrayList<>();
  public static final List<String> hotfixPackageList = new ArrayList<>();
  public static final LinkedHashMap<String, Integer> methodMap = new LinkedHashMap<>();
  public static String robustGenerateDirectory;
  public static final Map<String, List<CtMethod>> invokeSuperMethodMap = new HashMap<>();
  public static ClassPool classPool = new ClassPool();
  public static final Set<String> methodNeedPatchSet = new HashSet<>();
  public static final List<CtMethod> addedSuperMethodList = new ArrayList<>();
  public static final Set<String> noNeedReflectClassSet = new HashSet<>();

  public static void init() {
    catchReflectException = false;
    isLogging = true;
    isManual = false;
    patchPackageName = Constants.PATCH_PACKAGENAME;
    mappingFilePath = DEFAULT_MAPPING_FILE;
    patchMethodSignatureSet.clear();
    newlyAddedClassNameList.clear();
    modifiedClassNameList.clear();
    hotfixPackageList.clear();
    newlyAddedMethodSet.clear();
    invokeSuperMethodMap.clear();
    classPool = new ClassPool();
    methodNeedPatchSet.clear();
    addedSuperMethodList.clear();
    noNeedReflectClassSet.clear();
    noNeedReflectClassSet.addAll(Constants.NO_NEED_REFLECT_CLASS);
    supportProGuard = true;
  }
}
