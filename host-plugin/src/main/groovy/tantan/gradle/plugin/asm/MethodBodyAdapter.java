package tantan.gradle.plugin.asm;

import com.meituan.robust.Constants;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodBodyAdapter extends ClassVisitor implements Opcodes {
  private final String className;
  private final AsmInsertImpl asmInsert;

  public MethodBodyAdapter(AsmInsertImpl insert, ClassWriter cw, String className) {
    super(Opcodes.ASM7, cw);
    this.className = className;
    this.asmInsert = insert;
  }

  @Override
  public FieldVisitor visitField(
      int access, String name, String descriptor, String signature, Object value) {
    // public static ChangeQuickRedirect $change;
    return super.visitField(
        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
        Constants.INSERT_FIELD_NAME,
        AsmInsertImpl.REDIRECT_CLASS_NAME,
        null,
        null);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if (isSupportMethod(access, name)) {
      if (!isPublic(access)) {
        access = setPublic(access);
      }

      int methodIndex = asmInsert.putToMethodMap(className + "/" + name + desc);
      mv = new MethodBodyInserter(mv, className, desc, methodIndex, name, access);
    }

    return mv;
  }

  private boolean isSupportMethod(int access, String name) {
    // 类初始化函数和构造函数过滤
    if ("<init>".equals(name)) {
      return false;
    }

    if ((access & Opcodes.ACC_ABSTRACT) != 0) {
      return false;
    }

    if ((access & Opcodes.ACC_NATIVE) != 0) {
      return false;
    }

    if ((access & Opcodes.ACC_INTERFACE) != 0) {
      return false;
    }

    return (access & Opcodes.ACC_DEPRECATED) == 0;
  }

  private boolean isPublic(int access) {
    return (access & Opcodes.ACC_PUBLIC) != 0;
  }

  private int setPublic(int access) {
    return (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
  }
}
