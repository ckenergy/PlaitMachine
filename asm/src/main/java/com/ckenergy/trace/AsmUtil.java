package com.ckenergy.trace;

import groovy.transform.CompileStatic;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;

class AsmUtil {
    static boolean isPublic(int access) {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    static boolean isProtected(int access) {
        return (access & Opcodes.ACC_PROTECTED) != 0;
    }

    /**
     * 比较两个字符串版本信息大小，例如 2.01.10 > 2.1.9.1.2
     *
     * @param version1 版本信息字符串
     * @param version2 版本信息字符串
     * @return 如果返回值为 0，表示版本相等；如果返回值为 1 表示 version1 大于 version2；如果返回值为 -1，表示 version1 小于 version2。
     */
    static int compareVersion(String version1, String version2) {
        String[] v1Array = version1.replace("-pre", "").split("\\.");
        String[] v2Array = version2.replace("-pre", "").split("\\.");
        int maxLength = Math.max(v1Array.length, v2Array.length);
        String str1, str2;
        for (int index = 0; index < maxLength; index++) {
            if (v1Array.length > index) {
                str1 = v1Array[index];
            } else {
                return -1;
            }
            if (v2Array.length > index) {
                str2 = v2Array[index];
            } else {
                return 1;
            }
            if (str1 != null && str2 != null) {
                try {
                    int num1 = Integer.valueOf(str1);
                    int num2 = Integer.valueOf(str2);
                    if (num1 != num2) {
                        return num1 - num2 > 0 ? 1 : -1;
                    }
                } catch (Exception ignored) {
                    return str1.compareTo(str2);
                }
            }
        }
        return 0;
    }


    static byte[] toByteArrayAndAutoCloseStream(InputStream input) throws Exception {
        ByteArrayOutputStream output = null;
        try {
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 4];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            output.flush();
            return output.toByteArray();
        } catch (Exception e) {
            throw e;
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * 获取 LOAD 或 STORE 的相反指令，例如 ILOAD => ISTORE，ASTORE => ALOAD
     *
     * @return 返回相对应的指令
     */
    static int convertOpcodes(int code) {
        int result = code;
        switch (code) {
            case Opcodes.ILOAD:
                result = Opcodes.ISTORE;
                break;
            case Opcodes.ALOAD:
                result = Opcodes.ASTORE;
                break;
            case Opcodes.LLOAD:
                result = Opcodes.LSTORE;
                break;
            case Opcodes.FLOAD:
                result = Opcodes.FSTORE;
                break;
            case Opcodes.DLOAD:
                result = Opcodes.DSTORE;
                break;
            case Opcodes.ISTORE:
                result = Opcodes.ILOAD;
                break;
            case Opcodes.ASTORE:
                result = Opcodes.ALOAD;
                break;
            case Opcodes.LSTORE:
                result = Opcodes.LLOAD;
                break;
            case Opcodes.FSTORE:
                result = Opcodes.FLOAD;
                break;
            case Opcodes.DSTORE:
                result = Opcodes.DLOAD;
                break;
        }
        return result;
    }
}
