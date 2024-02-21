package com.ckenergy.trace

import com.ckenergy.trace.extension.PlaitMethodList
import com.ckenergy.trace.extension.PlaintConfig
import com.ckenergy.trace.utils.Log
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.collections.HashMap

private const val TAG = "===PlaitClassVisitor==="
class PlaitClassVisitor(
    classVisitor: ClassVisitor, val plaintConfig: PlaintConfig?
) : ClassVisitor(Constants.ASM_VERSION, classVisitor) {

    private var className: String? = null
    private var superName: String? = null

    private var isABSClass = false
    
    private var isNeedTrace = false

    private var hasInitMethod = false

    private var methodListMap: HashMap<String, MutableList<PlaitMethodList>?>? = null
    private var blackMethodMap: HashMap<String, MutableList<PlaitMethodList>?>? = null
    private val classAnnotationList by lazy {
        ArrayList<String>()
    }

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
    }

    private fun log(info: String) {
        if (className?.contains("MainAct", true) == true)
            Log.d(TAG, "className:$className,$info")
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
        if (isABSClass || className.isNullOrEmpty() || Constants.UN_PLAINT_CLASS.find { className?.contains(it) == true } != null || Constants.DEFAULT_BLACK_METHOD.contains(name)) {
            return result
        }
        initFilterMethodListMap(className)
        if (!isNeedTrace) {
            return result
        }

        //如果为空再取一遍所有的
        val pair = filterMethodListWithMethodName(name, methodListMap)
        val list = pair.first
        val annotationList = pair.second

        val blackPair = filterMethodListWithMethodName(name, blackMethodMap)
        val blackList = blackPair.first
        val blackAnnotationList = blackPair.second

        log("visitMethod name:$name traceMethod:$list,black:$blackList")
        val newList = filterMethodWithBlack(list, blackList)

        log("visitMethod name:$name filterList:$newList")
        log("visitMethod name:$name anonList:$annotationList")
        log("visitMethod name:$name blackAnonList:$blackAnnotationList")
        if (newList.isNullOrEmpty() && annotationList.isEmpty()) {
                log( "visitMethod name:$className.$name list is empty")
            return result
        }
        return PlaitMethodVisitor(api, result, access, name, className!!, descriptor, newList, annotationList, blackAnnotationList)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        log("visitAnnotation descriptor:$descriptor, visible:$visible")
        if(!descriptor.isNullOrBlank() && !isABSClass)
            classAnnotationList.add(descriptor.replace(";", ""))
        return super.visitAnnotation(descriptor, visible)
    }

    /**
     * 初始化当前类关联的方法
     */
    private fun initFilterMethodListMap(name: String?) {
        if (name.isNullOrEmpty() || hasInitMethod) return
        hasInitMethod = true

        filterMethodListWithClass(name, plaintConfig)
        filterBlackMethodListWithClass(name, plaintConfig)

        log( "initFilterMethodListMap name:$name, map:$methodListMap, black:$blackMethodMap")
        isNeedTrace = !name.isNullOrEmpty() && !methodListMap.isNullOrEmpty()
    }

    /**
     * 根据类名 获取过滤的方法
     */
    private fun filterMethodListWithClass(className: String?, plaintConfig: PlaintConfig?) {
        if (className.isNullOrEmpty() || plaintConfig == null || (plaintConfig.classMap.isNullOrEmpty() && plaintConfig.packages.isNullOrEmpty())) return

        log("filterMethodListWithClass packages:${plaintConfig.packages}, anno:${classAnnotationList}")

        val newClassName = "$className/"

        plaintConfig.packages?.forEach {
            if (it.value != null) {
                if (newClassName.startsWith(it.key.replace("*", ""))) {
                    methodListMap = transformMethod(it.value!!, methodListMap)
                    //把注解的类获取到
                }else if (it.key.contains("@") && classAnnotationList.contains(it.key.replace("@", "L"))) {
                    methodListMap = transformMethod(it.value!!, methodListMap)
                }
            }
        }
        plaintConfig.classMap?.get(className)?.apply {
            methodListMap = transformMethod(this, methodListMap)
        }
    }

    /**
     * 根据类名 获取过滤的黑名单方法
     */
    private fun filterBlackMethodListWithClass(className: String?, plaintConfig: PlaintConfig?) {
        if (className.isNullOrEmpty() || plaintConfig == null || plaintConfig.blackPackages.isNullOrEmpty()) return

        log("filterBlackMethodListWithClass blackPackages:${plaintConfig.blackPackages}, annotation:${classAnnotationList}")

        val newClassName = "$className/"

        plaintConfig.blackPackages?.forEach {
            if (it.value != null) {
                if (newClassName.startsWith(it.key.replace("*", ""))) {
                    blackMethodMap = transformMethod(it.value!!, blackMethodMap)
                }else if (it.key.contains("@") && classAnnotationList.contains(it.key.replace("@", "L"))) {
                    blackMethodMap = transformMethod(it.value!!, blackMethodMap)
                }
            }
        }
    }

    private fun transformMethod(list: List<PlaitMethodList>, map: HashMap<String, MutableList<PlaitMethodList>?>?) : HashMap<String, MutableList<PlaitMethodList>?> {
        val newMap = map ?: hashMapOf()
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
        list.forEach { it1 ->
            it1.methodList.forEach {
                var list1 = newMap[it]
                if (list1 == null) {
                    list1 = ArrayList()
                }
                list1.add(it1)
                newMap[it] = list1
            }
        }
        return newMap
    }

    /**
     * 根据方法名过滤
     */
    private fun filterMethodListWithMethodName(methodName: String?, methodMap: HashMap<String, MutableList<PlaitMethodList>?>?)
        : Pair<MutableList<PlaitMethodList>?, HashMap<String, List<PlaitMethodList>?>> {
        val list = arrayListOf<PlaitMethodList>()
        methodMap?.get(methodName)?.let {
            list.addAll(it)
        }
        //再取一遍所有的
        methodMap?.get(Constants.ALL)?.let {
            list.addAll(it)
        }
        val annotationList = hashMapOf<String, List<PlaitMethodList>?>()
        //在获取类注解内的方法
        methodMap?.forEach {
            if (it.key.contains("@")) {
                annotationList[it.key.replace("@","L")] = it.value
            }
        }
        return list to annotationList
    }

    /**
     * 从过滤黑名单里的数据
     */
    private fun filterMethodWithBlack(list: MutableList<PlaitMethodList>?, blackList: MutableList<PlaitMethodList>?): List<PlaitMethodList>? {
        var newList: List<PlaitMethodList>? = list
        if (!list.isNullOrEmpty() && !blackList.isNullOrEmpty()) {
            newList = list.filter {//todo 优化算法
                blackList.find { it1 -> it.plaitClass == it1.plaitClass && it.plaitMethod == it1.plaitMethod } == null
            }
        }
        return newList
    }

}