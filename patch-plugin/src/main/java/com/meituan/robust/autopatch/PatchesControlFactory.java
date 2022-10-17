package com.meituan.robust.autopatch;

import static com.meituan.robust.autopatch.Config.classPool;

import com.meituan.robust.Constants;
import com.meituan.robust.utils.JavaUtils;
import com.meituan.robust.utils.PatchTemplate;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import org.gradle.api.logging.Logger;

public class PatchesControlFactory {
  private static final PatchesControlFactory patchesControlFactory = new PatchesControlFactory();

  private CtClass createControlClass(CtClass modifiedClass, Logger logger) throws Exception {
    CtClass patchClass =
        classPool.get(NameManger.getInstance().getPatchName(modifiedClass.getName()));
    patchClass.defrost();
    CtClass controlClass =
        classPool.getAndRename(
            PatchTemplate.class.getCanonicalName(),
            NameManger.getInstance().getPatchControlName(modifiedClass.getSimpleName()));
    String getRealParameterMethodBody =
        new StringBuilder("public Object getRealParameter(Object obj) {")
            .append(
                String.format(
                    "if(obj instanceof %s){return new %s(obj);}",
                    modifiedClass.getName(), patchClass.getName()))
            .append("return obj;}")
            .toString();
    controlClass.addMethod(CtMethod.make(getRealParameterMethodBody, controlClass));

    controlClass
        .getDeclaredMethod("accessDispatch")
        .insertBefore(getAccessDispatchMethodBody(patchClass, modifiedClass.getName()));

    controlClass
        .getDeclaredMethod("isSupport")
        .insertBefore(getIsSupportMethodBody(patchClass, modifiedClass.getName()));
    controlClass.defrost();
    return controlClass;
  }

  private static String getAccessDispatchMethodBody(CtClass patchClass, String modifiedClassName)
      throws NotFoundException {
    StringBuilder accessDispatchMethodBody = new StringBuilder();
    if (Config.catchReflectException) {
      accessDispatchMethodBody.append("try{");
    }
    // create patch instance
    accessDispatchMethodBody.append(patchClass.getName() + " patch= null;\n");
    accessDispatchMethodBody.append(
        "Object obj = paramArrayOfObject[paramArrayOfObject.length - 1];\n");
    accessDispatchMethodBody.append(" if (obj != null) {\n");
    accessDispatchMethodBody.append(" if (keyToValueRelation.get(obj) == null) {\n");

    accessDispatchMethodBody.append("patch=new " + patchClass.getName() + "(obj);\n");
    accessDispatchMethodBody.append(" keyToValueRelation.put(obj, null);\n");
    accessDispatchMethodBody.append("}else{");
    accessDispatchMethodBody.append(
        "patch=(" + patchClass.getName() + ") keyToValueRelation.get(obj);\n");
    accessDispatchMethodBody.append("}");
    accessDispatchMethodBody.append("}\n");
    accessDispatchMethodBody.append("else{");

    accessDispatchMethodBody.append("patch=new " + patchClass.getName() + "(null);\n");
    accessDispatchMethodBody.append("}");
    //        patchClass.declaredMethods.each {
    for (CtMethod method : patchClass.getDeclaredMethods()) {
      CtClass[] parametertypes = method.getParameterTypes();
      String methodSignure =
          JavaUtils.getJavaMethodSignure(method)
              .replaceAll(patchClass.getName(), modifiedClassName);
      String methodLongName = modifiedClassName + "." + methodSignure;
      Integer methodNumber = Config.methodMap.get(methodLongName);
      // just Forward methods with methodNumber
      if (methodNumber != null) {
        accessDispatchMethodBody.append(" if(" + methodNumber + " == methodNumber){\n");
        String methodName = method.getName();
        if (AccessFlag.isPrivate(method.getModifiers())) {
          methodName = Constants.ROBUST_PUBLIC_SUFFIX + method.getName();
        }
        if (method.getReturnType().getName().equals("void")) {
          accessDispatchMethodBody.append("(patch." + methodName + "(");
        } else {
          switch (method.getReturnType().getName()) {
            case "boolean":
              accessDispatchMethodBody.append("return Boolean.valueOf(patch." + methodName + "(");
              break;
            case "byte":
              accessDispatchMethodBody.append("return Byte.valueOf(patch." + methodName + "(");
              break;
            case "char":
              accessDispatchMethodBody.append("return Character.valueOf(patch." + methodName + "(");
              break;
            case "double":
              accessDispatchMethodBody.append("return Double.valueOf(patch." + methodName + "(");
              break;
            case "float":
              accessDispatchMethodBody.append("return Float.valueOf(patch." + methodName + "(");
              break;
            case "int":
              accessDispatchMethodBody.append("return Integer.valueOf(patch." + methodName + "(");
              break;
            case "long":
              accessDispatchMethodBody.append("return Long.valueOf(patch." + methodName + "(");
              break;
            case "short":
              accessDispatchMethodBody.append("return Short.valueOf(patch." + methodName + "(");
              break;
            default:
              accessDispatchMethodBody.append("return (patch." + methodName + "(");
              break;
          }
        }
        for (int index = 0; index < parametertypes.length; index++) {
          if (booleanPrimeType(parametertypes[index].getName())) {
            accessDispatchMethodBody.append(
                "(("
                    + JavaUtils.getWrapperClass(parametertypes[index].getName())
                    + ") (fixObj(paramArrayOfObject["
                    + index
                    + "]))");
            accessDispatchMethodBody.append(
                ")" + JavaUtils.wrapperToPrime(parametertypes[index].getName()));
            if (index != parametertypes.length - 1) {
              accessDispatchMethodBody.append(",");
            }
          } else {
            accessDispatchMethodBody.append(
                "(("
                    + JavaUtils.getWrapperClass(parametertypes[index].getName())
                    + ") (paramArrayOfObject["
                    + index
                    + "])");
            accessDispatchMethodBody.append(
                ")" + JavaUtils.wrapperToPrime(parametertypes[index].getName()));
            if (index != parametertypes.length - 1) {
              accessDispatchMethodBody.append(",");
            }
          }
        }
        accessDispatchMethodBody.append("));}\n");
      }
    }
    if (Config.catchReflectException) {
      accessDispatchMethodBody.append(" } catch (Throwable e) {");
      accessDispatchMethodBody.append(" e.printStackTrace();}");
    }
    return accessDispatchMethodBody.toString();
  }

  private static String getIsSupportMethodBody(CtClass patchClass, String modifiedClassName)
      throws NotFoundException {
    StringBuilder methodsIds = new StringBuilder(":");
    for (CtMethod method : patchClass.getDeclaredMethods()) {
      String methodSignure =
          JavaUtils.getJavaMethodSignure(method)
              .replaceAll(patchClass.getName(), modifiedClassName);
      String methodLongName = modifiedClassName + "." + methodSignure;
      Integer methodNumber = Config.methodMap.get(methodLongName);
      // just Forward methods with methodNumber
      if (methodNumber != null) {
        // 一前一后的冒号作为匹配锚点，只有一边有的话可能会有多重匹配的bug
        methodsIds.append(methodNumber).append(":");
      }
    }
    return String.format("return \"%s\".contains(\":methodNumber:\");", methodsIds.toString());
  }

  public static CtClass createPatchesControl(CtClass modifiedClass, Logger logger)
      throws Exception {
    return patchesControlFactory.createControlClass(modifiedClass, logger);
  }

  public static boolean booleanPrimeType(String typeName) {
    return "boolean".equals(typeName);
  }
}
