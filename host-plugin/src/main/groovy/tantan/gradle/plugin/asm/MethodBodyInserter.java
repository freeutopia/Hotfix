package tantan.gradle.plugin.asm;

import com.meituan.robust.Constants;
import com.meituan.robust.PatchProxyResult;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class MethodBodyInserter extends GeneratorAdapter implements Opcodes {
  private final String className;
  private final Type returnType;
  private final Type[] args;
  private final boolean isStatic;
  private final int methodId;

  public MethodBodyInserter(
      MethodVisitor mv, String className, String desc, int methodId, String name, int access) {
    super(Opcodes.ASM7, mv, access, name, desc);
    this.className = className;
    this.returnType = Type.getReturnType(desc);
    this.isStatic = isStatic(access);
    this.methodId = methodId;
    this.args = Type.getArgumentTypes(desc);
  }

  @Override
  public void visitCode() {
    prepareMethodParameters();
    createInsertCode();
  }

  private boolean isStatic(int access) {
    return (access & Opcodes.ACC_STATIC) != 0;
  }

  /** 插入代码 */
  private void createInsertCode() {
    // 开始调用
    String resultType = Type.getDescriptor(PatchProxyResult.class);
    visitMethodInsn(
        Opcodes.INVOKESTATIC,
        Constants.PROXY_CLASS_NAME,
        "proxy",
        "([Ljava/lang/Object;Ljava/lang/Object;"
            + AsmInsertImpl.REDIRECT_CLASS_NAME
            + "I)"
            + resultType,
        false);

    int local = newLocal(Type.getType(resultType));
    storeLocal(local);

    loadLocal(local);
    visitFieldInsn(
        Opcodes.GETFIELD, Type.getType(resultType).getInternalName(), "isSupported", "Z");

    // if isSupported
    Label l1 = new Label();
    visitJumpInsn(Opcodes.IFEQ, l1);

    // 判断是否有返回值，代码不同
    if ("V".equals(returnType.getDescriptor())) {
      visitInsn(Opcodes.RETURN);
    } else {
      loadLocal(local);
      visitFieldInsn(
          Opcodes.GETFIELD,
          Type.getType(resultType).getInternalName(),
          "result",
          "Ljava/lang/Object;");
      // 强制转化类型
      forceCheckCast(returnType.getDescriptor());

      // 这里还需要做返回类型不同返回指令也不同
      visitInsn(AsmUtils.getReturnTypeCode(returnType.getDescriptor()));
    }

    visitLabel(l1);
  }

  private void prepareMethodParameters() {
    // 第一个参数：new Object[]{...};,如果方法没有参数直接传入new Object[0]
    if (args.length == 0) {
      visitInsn(Opcodes.ICONST_0);
      visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
    } else {
      createObjectArray();
    }

    // 第二个参数：this,如果方法是static的话就直接传入null
    if (isStatic) {
      visitInsn(Opcodes.ACONST_NULL);
    } else {
      visitVarInsn(Opcodes.ALOAD, 0);
    }

    // 第三个参数：$change
    visitFieldInsn(
        Opcodes.GETSTATIC,
        className,
        Constants.INSERT_FIELD_NAME,
        AsmInsertImpl.REDIRECT_CLASS_NAME);

    // 第四个参数：methodId
    push(methodId);
  }

  /** 创建局部参数代码 = */
  private void createObjectArray() {
    // Opcodes.ICONST_0 ~ Opcodes.ICONST_5 这个指令范围
    int argsCount = args.length;
    // 声明 Object[argsCount];
    if (argsCount >= 6) {
      visitIntInsn(Opcodes.BIPUSH, argsCount);
    } else {
      visitInsn(Opcodes.ICONST_0 + argsCount);
    }
    visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

    // 如果是static方法，没有this隐含参数
    int loadIndex = (isStatic ? 0 : 1);

    // 填充数组数据
    for (int i = 0; i < argsCount; i++) {
      visitInsn(Opcodes.DUP);
      if (i <= 5) {
        visitInsn(Opcodes.ICONST_0 + i);
      } else {
        visitIntInsn(Opcodes.BIPUSH, i);
      }

      // 这里又要做特殊处理，在实践过程中发现个问题：public void xxx(long a, boolean b, double c,int d)
      // 当一个参数的前面一个参数是long或者是double类型的话，后面参数在使用LOAD指令，加载数据索引值要+1
      // 个人猜想是和long，double是8个字节的问题有关系。这里做了处理
      // 比如这里的参数：[a=LLOAD 1] [b=ILOAD 3] [c=DLOAD 4] [d=ILOAD 6];
      if (i >= 1) {
        // 这里需要判断当前参数的前面一个参数的类型是什么
        if ("J".equals(args[i - 1].getDescriptor()) || "D".equals(args[i - 1].getDescriptor())) {
          // 如果前面一个参数是long，double类型，load指令索引就要增加1
          loadIndex++;
        }
      }

      createPrimateTypeObj(loadIndex, args[i].getDescriptor());
      loadIndex++;
    }
  }

  /** 创建基本类型对应的对象 */
  private void createPrimateTypeObj(int loadIndex, String typeS) {
    String fullClassName = AsmUtils.getTypeName(typeS);
    if (fullClassName != null) {
      visitTypeInsn(Opcodes.NEW, fullClassName);
      visitInsn(Opcodes.DUP);
      visitVarInsn(Opcodes.ILOAD, loadIndex);
      visitMethodInsn(Opcodes.INVOKESPECIAL, fullClassName, "<init>", "(" + typeS + ")V", false);
    } else {
      visitVarInsn(Opcodes.ALOAD, loadIndex);
    }
    visitInsn(Opcodes.AASTORE);
  }

  /** 基本类型需要做对象类型分装 */
  private void forceCheckCast(String typeS) {
    if ("Z".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean"); // 强制转化类型
      visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()" + typeS, false);
    } else if ("B".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte"); // 强制转化类型
      visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()" + typeS, false);
    } else if ("C".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character"); // 强制转化类型
      visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()" + typeS, false);
    } else if ("S".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short"); // 强制转化类型
      visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()" + typeS, false);
    } else if ("I".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer"); // 强制转化类型
      visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()" + typeS, false);
    } else if ("F".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float"); // 强制转化类型
      visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()" + typeS, false);
    } else if ("D".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double"); // 强制转化类型
      visitMethodInsn(
          Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()" + typeS, false);
    } else if ("J".equals(typeS)) {
      visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long"); // 强制转化类型
      visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()" + typeS, false);
    } else {
      String newTypeStr;
      if (typeS.startsWith("[")) { // 数组
        newTypeStr = typeS;
      } else { // L
        newTypeStr = typeS.substring(1, typeS.length() - 1);
      }
      visitTypeInsn(Opcodes.CHECKCAST, newTypeStr);
    }
  }
}
