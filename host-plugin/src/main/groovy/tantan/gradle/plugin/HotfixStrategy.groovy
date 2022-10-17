package tantan.gradle.plugin

import com.meituan.robust.Constants
import kotlin.jvm.functions.Function2
import org.gradle.api.Project

class HotfixStrategy {
    public final List<String> hotfixPackageList = new ArrayList<>()
    public final List<String> hotfixMethodList = new ArrayList<>()
    public final List<String> exceptPackageList = new ArrayList<>()
    public final List<String> exceptMethodList = new ArrayList<>()
    public final boolean isForceInsert


    HotfixStrategy(File configFile) {
        def robust = new XmlSlurper().parse(configFile)

        for (name in robust.packname.name) {
            hotfixPackageList.add(name.text())
        }
        for (name in robust.exceptPackname.name) {
            exceptPackageList.add(name.text())
        }
        for (name in robust.hotfixMethod.name) {
            hotfixMethodList.add(name.text())
        }
        for (name in robust.exceptMethod.name) {
            exceptMethodList.add(name.text())
        }

        isForceInsert = robust.switch.forceInsert != null && "true" == String.valueOf(robust.switch.forceInsert.text())
    }


    /** 剔除指定的类 */
    final boolean isSupportClass(
            String className, Function2<String, String, Boolean> compare) {

        for (String exceptName : exceptPackageList) {
            if (compare.invoke(className, exceptName)) {
                return false
            }
        }

        for (String name : hotfixPackageList) {
            if (compare.invoke(className, name)) {
                return true
            }
        }

        return false
    }
}