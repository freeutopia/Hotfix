package tantan.gradle.plugin.asm;

import com.android.SdkConstants;
import com.meituan.robust.HotfixChange;
import com.meituan.robust.utils.IoUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.AccessFlag;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import tantan.gradle.plugin.HotfixStrategy;

public class AsmInsertImpl {
  public static final String REDIRECT_CLASS_NAME = Type.getDescriptor(HotfixChange.class);
  public final HotfixStrategy strategy;
  private final AtomicInteger insertMethodCount = new AtomicInteger(0);
  private final HashMap<String, Integer> methodMap = new LinkedHashMap<>();
  private final File methodFile;

  public AsmInsertImpl(File methodFile, HotfixStrategy strategy) {
    this.methodFile = methodFile;
    this.strategy = strategy;
  }

  public void insertCode(Set<CtClass> classes, File output)
      throws IOException, CannotCompileException {
    ZipOutputStream outStream = new JarOutputStream(new FileOutputStream(output));
    for (CtClass ctClass : classes) {
      ctClass.setModifiers(AccessFlag.setPublic(ctClass.getModifiers()));
      String className = ctClass.getName().replaceAll("\\.", "/");
      byte[] byteCodeArray = ctClass.toBytecode();
      if (strategy.isSupportClass(ctClass.getName(), String::startsWith)
          && !ctClass.isInterface()) {
        byteCodeArray = transformBytecode(byteCodeArray, className);
      }
      zipFile(byteCodeArray, outStream, className + SdkConstants.DOT_CLASS);
      ctClass.defrost();
    }
    IoUtils.close(outStream);
  }

  public byte[] transformBytecode(byte[] b1, String className) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    ClassReader cr = new ClassReader(b1);
    cr.accept(new MethodBodyAdapter(this, cw, className), ClassReader.EXPAND_FRAMES);
    return cw.toByteArray();
  }

  public boolean finished() {
    try {
      writeMap2File();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private void writeMap2File() throws IOException {
    if (methodFile.exists()
        || (methodFile.getParentFile().mkdirs() && methodFile.createNewFile())) {
      FileOutputStream fileOut = null;
      ObjectOutputStream objOut = null;
      GZIPOutputStream gzip = null;
      try {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        fileOut = new FileOutputStream(methodFile);
        objOut = new ObjectOutputStream(byteOut);
        objOut.writeObject(methodMap);

        // gzip压缩
        gzip = new GZIPOutputStream(fileOut);
        gzip.write(byteOut.toByteArray());
        gzip.flush();
      } catch (IOException ignored) {

      } finally {
        IoUtils.close(gzip, objOut, fileOut);
      }
    }
  }

  protected void zipFile(byte[] bytes, ZipOutputStream zos, String entryName) {
    try {
      ZipEntry entry = new ZipEntry(entryName);
      zos.putNextEntry(entry);
      zos.write(bytes, 0, bytes.length);
      zos.closeEntry();
      zos.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public int putToMethodMap(String key) {
    int value = insertMethodCount.incrementAndGet();
    methodMap.put(key, value);
    return value;
  }
}
