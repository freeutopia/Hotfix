package tantan.gradle.plugin

import com.android.SdkConstants
import com.android.build.api.transform.TransformInput
import javassist.ClassPool
import javassist.CtClass
import java.util.jar.JarEntry
import java.util.jar.JarFile
import org.apache.commons.io.FileUtils

class JavaClass2CtClass {
    static Set<CtClass> toCtClasses(Collection<TransformInput> inputs, ClassPool classPool) {
        Set<CtClass> allClass = new ArrayList<>()
        allClass.addAll(directoryInputs(inputs, classPool))
        allClass.addAll(jarInputs(inputs, classPool))
        return allClass
    }


    def static directoryInputs(Collection<TransformInput> inputs, ClassPool classPool) {
        Set<CtClass> classes = new ArrayList<>()
        inputs.each {
            it.directoryInputs.each {
                def rootDirPath = it.file.absolutePath
                classPool.insertClassPath(rootDirPath)
                FileUtils.listFiles(it.file, null, true).each {
                    CtClass aClass = transformToCtClass(classPool, rootDirPath, it.absolutePath)
                    if (aClass != null) {
                        classes.add(aClass)
                    }
                }
            }
        }
        return classes
    }

    def static jarInputs(Collection<TransformInput> inputs, ClassPool classPool) {
        Set<CtClass> ctClassSet = new ArrayList<>()
        inputs.each {
            it.jarInputs.each {
                classPool.insertClassPath(it.file.absolutePath)
                Enumeration<JarEntry> classes = new JarFile(it.file).entries()
                while (classes.hasMoreElements()) {
                    JarEntry libClass = classes.nextElement()
                    CtClass aClass = transformToCtClass(classPool, null, libClass.getName())
                    if (aClass != null) {
                        ctClassSet.add(aClass)
                    }
                }
            }
        }
        return ctClassSet
    }

    private static CtClass transformToCtClass(ClassPool classPool, String root, String fullClassPath) {
        if (fullClassPath.endsWith(SdkConstants.DOT_CLASS)) {
            def suffixLen = SdkConstants.DOT_CLASS.length()
            int prefixLen = 0
            if (root != null) {
                prefixLen = root.length() + 1
            }
            try {
                def classPath = fullClassPath.substring(prefixLen, fullClassPath.length() - suffixLen).replaceAll(File.separator, '.')
                return classPool.get(classPath)
            } catch (Throwable ignored) {
                println "class not found exception class name:  $fullClassPath "
            }
        }
        return null
    }
}