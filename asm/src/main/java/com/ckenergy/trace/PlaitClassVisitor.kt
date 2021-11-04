package com.ckenergy.trace

import com.ckenergy.trace.extension.PlaitMethodList
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.HashMap

class PlaitClassVisitor(
    classVisitor: ClassVisitor, val traceMap: Map<String, MutableList<PlaitMethodList>?>?
) : ClassVisitor(Contants.ASM_VERSION, classVisitor) {

    private var className: String? = null
    private var superName: String? = null

    private var isABSClass = false
    
    private var isNeedTrace = true

    var methodListMap: HashMap<String, MutableList<PlaitMethodList>?>? = null

    //类入口
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name
        this.superName = superName
        //如果是接口 isABSClass =true
        if (access and Opcodes.ACC_ABSTRACT > 0 || access and Opcodes.ACC_INTERFACE > 0) {
            this.isABSClass = true
        }

        val list = traceMap?.get(className)

        var map = methodListMap
        if (list != null && map == null) {
            map = HashMap()
            /*
            这里的多层嵌套循环，用map结构优化下，从
            a[1,2,3]
            b[1,3,4]
            c[2,3,4]
            转化成
            1 -> [a,b]
            2 -> [a,c]
            1,2,3为需要hook方法的名字，a，b，c为要织入的方法
             */
            list.forEach {
                it.methodList.forEach { it1 ->
                    val black = it.blackMethodList?.find { black ->
                        black == Contants.ALL || black == it1
                    }
                    if (black.isNullOrEmpty()) {
                        var list1 = map[it1]
                        if (list1 == null) {
                            list1 = ArrayList()
                        }
                        list1.add(it)
                        map[it1] = list1
                    }
                }
            }
            methodListMap = map
            println("==== visitMethod map:$map")
        }
        isNeedTrace = !name.isNullOrEmpty() && !map.isNullOrEmpty()

        if(isNeedTrace && !isABSClass) {
            Log.d(PlaintMachineTransform.TAG, "visit name:$name, superName:$superName，interfaces：${interfaces?.asList()}")
        }
    }

    override fun visitOuterClass(owner: String?, name: String?, descriptor: String?) {
        super.visitOuterClass(owner, name, descriptor)
    }

    //类中方法的入口
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val result = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (isABSClass || className.isNullOrEmpty() || !isNeedTrace) {
            return result
        }
        //如果为空再取一遍所有的
        val list = methodListMap?.get(name) ?: methodListMap?.get(Contants.ALL)
        println("visitMethod name:$name traceMethod:$list")
        if (name == "<clinit>" || "<init>" == name || "toString" == name || list.isNullOrEmpty()) {
            return result
        }
        return PlaitMethodVisitor(className!!, result, access, name, descriptor, list)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
//        println("visitAnnotation descriptor:$descriptor, visible:$visible")
        return super.visitAnnotation(descriptor, visible)
    }

}