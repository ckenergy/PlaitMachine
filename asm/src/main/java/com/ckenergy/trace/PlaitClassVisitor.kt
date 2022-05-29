package com.ckenergy.trace

import com.ckenergy.trace.extension.PlaitMethodList
import com.ckenergy.trace.extension.TraceConfig
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.collections.HashMap

private const val TAG = "===PlaitClassVisitor==="
class PlaitClassVisitor(
    classVisitor: ClassVisitor, val traceConfig: TraceConfig?
) : ClassVisitor(Contants.ASM_VERSION, classVisitor) {

    private var className: String? = null
    private var superName: String? = null

    private var isABSClass = false
    
    private var isNeedTrace = false

    var methodListMap: HashMap<String, MutableList<PlaitMethodList>?>? = null
    var blackMethodMap: HashMap<String, MutableList<PlaitMethodList>?>? = null
    private val classAnoList by lazy {
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

        if(isABSClass || name == null || Contants.UN_TRACE_CLASS.find { name.contains(it) } != null) return

        val list = getMethodList(name, traceConfig)
        val blackList = getBlackMethodList(name, traceConfig)

        log( "==== visit main:$name list:$list, blackList:$blackList")

        val map = HashMap<String, MutableList<PlaitMethodList>?>()
        val blackMap = HashMap<String, MutableList<PlaitMethodList>?>()
        if (list != null) {
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
                    var list1 = map[it1]
                    if (list1 == null) {
                        list1 = ArrayList()
                    }
                    list1.add(it)
                    map[it1] = list1
                }
            }
            methodListMap = map
        }

        if (blackList != null) {
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
            blackList.forEach {
                it.methodList?.onEach { it1 ->
                    var list2 = blackMap[it1]
                    if (list2 == null) {
                        list2 = ArrayList()
                    }
                    list2.add(it)
                    blackMap[it1] = list2
                }
            }
            blackMethodMap = blackMap
        }
        log( "==== visit name:$name, map:$map, black:$blackMap")
        isNeedTrace = !name.isNullOrEmpty() && !map.isNullOrEmpty()

        if(isNeedTrace && !isABSClass) {
            log( "visit NeedTrace name:$name")
        }
    }

    private fun log(info: String) {
//        if(className?.contains("DateStrings") == true)
//            Log.d(TAG, info)
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
        var list = methodListMap?.get(name)
        methodListMap?.get(Contants.ALL)?.apply {
            if (list != null) {
                list!!.addAll(this)
            }else {
                list = this
            }
        }
        val anonList = hashMapOf<String, List<PlaitMethodList>?>()
        //在获取类注解内的方法
        methodListMap?.forEach {
            if (it.key.contains("@")) {
                anonList[it.key.replace("@","L")] = it.value
            }
        }
        var blackList = blackMethodMap?.get(name)
//        过滤黑名单的方法
        blackMethodMap?.get(Contants.ALL)?.apply {
            if (blackList != null) {
                blackList!!.addAll(this)
            }else {
                blackList = this
            }
        }
        val blackAnonList = hashMapOf<String, List<PlaitMethodList>?>()
//        过滤黑名单注解的方法
        blackMethodMap?.forEach {
            if (it.key.contains("@")) {
                blackAnonList[it.key.replace("@","L")] = it.value
            }
        }
        log("visitMethod name:$name traceMethod:$list,black:$blackList")
        var newList: List<PlaitMethodList>? = list
        if (!list.isNullOrEmpty() && !blackList.isNullOrEmpty()) {
            newList = list!!.filter {//todo 优化算法
                blackList!!.find { it1 -> it.plaitClass == it1.plaitClass && it.plaitMethod == it1.plaitMethod } == null
            }
        }
        log("visitMethod name:$className.$name filterList:$newList")
        log("visitMethod name:$className.$name anonList:$anonList")
        log("visitMethod name:$className.$name blackAnonList:$blackAnonList")
        if (name == "<clinit>" || "<init>" == name || "toString" == name
            || (newList.isNullOrEmpty() && anonList.isNullOrEmpty())) {
                log( "visitMethod name:$className.$name list is empty")
            return result
        }
        return PlaitMethodVisitor(api, className!!, result, access, name, descriptor, newList, anonList, blackAnonList)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        log( "visitAnnotation descriptor:$descriptor, visible:$visible")
        if(!descriptor.isNullOrBlank() && !isABSClass)
            classAnoList.add(descriptor)
        return super.visitAnnotation(descriptor, visible)
    }

    private fun getMethodList(className: String?, traceConfig: TraceConfig?): MutableList<PlaitMethodList>? {
        if (className.isNullOrEmpty() || traceConfig == null || (traceConfig.traceMap.isNullOrEmpty() && traceConfig.packages.isNullOrEmpty())) return null

        val methodlist = arrayListOf<PlaitMethodList>()

        traceConfig.packages?.forEach {
            if (it.value != null) {
                if (className.startsWith(it.key.replace("*", ""))) {
                    methodlist.addAll(it.value!!)
                }
                if (classAnoList.contains(it.key.replace("@", "L"))) {
                    methodlist.addAll(it.value!!)
                }
            }
        }
        traceConfig.traceMap?.get(className)?.apply {
            methodlist.addAll(this)
        }
        return methodlist
    }

    private fun getBlackMethodList(className: String?, traceConfig: TraceConfig?): MutableList<PlaitMethodList>? {
        if (className.isNullOrEmpty() || traceConfig == null || traceConfig.blackPackages.isNullOrEmpty()) return null
        val methodlist = arrayListOf<PlaitMethodList>()
        traceConfig.blackPackages?.forEach {
            if (it.value != null) {
                if (className.startsWith(it.key.replace("*", ""))) {
                    methodlist.addAll(it.value!!)
                }
                if (classAnoList.contains(it.key.replace("@", "L"))) {
                    methodlist.addAll(it.value!!)
                }
            }
        }

        return methodlist
    }

}