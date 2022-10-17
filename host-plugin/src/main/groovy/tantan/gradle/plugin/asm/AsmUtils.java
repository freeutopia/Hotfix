package tantan.gradle.plugin.asm;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;

public class AsmUtils {
  private static final Map<String, String> TYPES_MAP =
      new HashMap<>() {
        {
          put("Z", "java/lang/Boolean");
          put("B", "java/lang/Byte");
          put("C", "java/lang/Character");
          put("S", "java/lang/Short");
          put("I", "java/lang/Integer");
          put("F", "java/lang/Float");
          put("D", "java/lang/Double");
          put("L", "java/lang/Long");
        }
      };
  /** 针对不同类型返回指令不一样 */
  public static int getReturnTypeCode(String typeS) {
    switch (typeS) {
      case "Z":
      case "B":
      case "C":
      case "S":
      case "I":
        return Opcodes.IRETURN;
      case "F":
        return Opcodes.FRETURN;
      case "D":
        return Opcodes.DRETURN;
      case "J":
        return Opcodes.LRETURN;
      default:
        return Opcodes.ARETURN;
    }
  }

  public static String getTypeName(String typeS) {
    return TYPES_MAP.get(typeS);
  }
}
