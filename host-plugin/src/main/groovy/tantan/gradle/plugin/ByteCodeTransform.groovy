package tantan.gradle.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.meituan.robust.Constants
import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import tantan.gradle.plugin.asm.AsmInsertImpl

class ByteCodeTransform extends Transform implements Plugin<Project> {
    private Project project
    static Logger logger
    private HotfixStrategy strategy

    @Override
    void apply(Project target) {
        project = target
        logger = project.logger
        strategy = new HotfixStrategy(new File(project.projectDir, Constants.ROBUST_XML))

        if (strategy.isForceInsert || !isDebugTask()) {
            project.android.registerTransform(this)
            logger.quiet "Register robust transform successful !!!"
        }
    }

    boolean isDebugTask() {
        def taskNames = project.gradle.startParameter.taskNames
        for (int index = 0; index < taskNames.size(); ++index) {
            if (taskNames[index].endsWith("Debug")) {
                return true
            }
        }
        return false
    }

    @Override
    String getName() {
        return "robust"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }


    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        logger.quiet '================robust start================'

        ClassPool classPool = new ClassPool()
        project.android.bootClasspath.each {
            classPool.appendClassPath((String) it.absolutePath)
        }

        Set<CtClass> inputClasses = JavaClass2CtClass.toCtClasses(inputs, classPool)
        def file = new File(project.projectDir.absolutePath, "${getName()}/methodsMap.robust")
        AsmInsertImpl asmInsert = new AsmInsertImpl(file, strategy)
        asmInsert.insertCode(inputClasses, getOutputFile(outputProvider))
        asmInsert.finished()

        logger.quiet '================robust   end================'
    }

    private File getOutputFile(TransformOutputProvider provider) {
        provider.deleteAll()
        File jarFile = provider.getContentLocation("main", getOutputTypes(), getScopes(),
                Format.JAR)

        if (jarFile.exists()) {
            jarFile.delete()
        }

        if (!jarFile.getParentFile().exists()) {
            jarFile.getParentFile().mkdirs()
        }

        return jarFile
    }
}