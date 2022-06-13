package com.ckenergy.trace

import com.ckenergy.trace.extension.PlaitMethodList
import com.ckenergy.trace.extension.PlaintConfig
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
    }

    private fun log(info: String) {
        if (className?.contains("MapCollections", true) == true)
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
        val pair = filterMethodListWithMethodName(name)
        val list = pair.first
        val annoList = pair.second

        val blackPair = filterBlackMethodListWithMethodName(name)
        val blackList = blackPair.first
        val blackAnonList = blackPair.second

        log("visitMethod name:$name traceMethod:$list,black:$blackList")
        val newList = filterMethodWithBlack(list, blackList)

        log("visitMethod name:$className.$name filterList:$newList")
        log("visitMethod name:$className.$name anonList:$annoList")
        log("visitMethod name:$className.$name blackAnonList:$blackAnonList")
        if (newList.isNullOrEmpty() && annoList.isNullOrEmpty()) {
                log( "visitMethod name:$className.$name list is empty")
            return result
        }
        return PlaitMethodVisitor(api, className!!, result, access, name, descriptor, newList, annoList, blackAnonList)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        log( "visitAnnotation descriptor:$descriptor, visible:$visible")
        if(!descriptor.isNullOrBlank() && !isABSClass)
            classAnoList.add(descriptor.replace(";", ""))
        return super.visitAnnotation(descriptor, visible)
    }

    /**
     * 初始化当前类关联的方法
     */
    private fun initFilterMethodListMap(name: String?) {
        if (name.isNullOrEmpty() || hasInitMethod) return
        hasInitMethod = true
        val list = filterMethodListWithClass(name, plaintConfig)
        val blackList = filterBlackMethodListWithClass(name, plaintConfig)

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
    }

    /**
     * 根据类名 获取过滤的方法
     */
    private fun filterMethodListWithClass(className: String?, plaintConfig: PlaintConfig?): MutableList<PlaitMethodList>? {
        if (className.isNullOrEmpty() || plaintConfig == null || (plaintConfig.classMap.isNullOrEmpty() && plaintConfig.packages.isNullOrEmpty())) return null

        val methodlist = arrayListOf<PlaitMethodList>()

        log("packages:${plaintConfig.packages}, anno:${classAnoList}")

        plaintConfig.packages?.forEach {
            if (it.value != null) {
                if (className.startsWith(it.key.replace("*", ""))) {
                    methodlist.addAll(it.value!!)
                }
                //把注解的类获取到
                if (it.key.contains("@") && classAnoList.contains(it.key.replace("@", "L"))) {
                    methodlist.addAll(it.value!!)
                }
            }
        }
        plaintConfig.classMap?.get(className)?.apply {
            methodlist.addAll(this)
        }
        return methodlist
    }

    /**
     * 根据类名 获取过滤的黑名单方法
     */
    private fun filterBlackMethodListWithClass(className: String?, plaintConfig: PlaintConfig?): MutableList<PlaitMethodList>? {
        if (className.isNullOrEmpty() || plaintConfig == null || plaintConfig.blackPackages.isNullOrEmpty()) return null

        log("blackPackages:${plaintConfig.blackPackages}, anno:${classAnoList}")

        val methodlist = arrayListOf<PlaitMethodList>()
        plaintConfig.blackPackages?.forEach {
            if (it.value != null) {
                if (className.startsWith(it.key.replace("*", ""))) {
                    methodlist.addAll(it.value!!)
                }
                if (it.key.contains("@") && classAnoList.contains(it.key.replace("@", "L"))) {
                    methodlist.addAll(it.value!!)
                }
            }
        }

        return methodlist
    }

    /**
     * 根据方法名过滤
     */
    private fun filterMethodListWithMethodName(methodName: String?): Pair<MutableList<PlaitMethodList>?, HashMap<String, List<PlaitMethodList>?>> {
        //如果为空再取一遍所有的
        var list = methodListMap?.get(methodName)
        methodListMap?.get(Constants.ALL)?.apply {
            if (list != null) {
                list!!.addAll(this)
            }else {
                list = this
            }
        }
        val annoList = hashMapOf<String, List<PlaitMethodList>?>()
        //在获取类注解内的方法
        methodListMap?.forEach {
            if (it.key.contains("@")) {
                annoList[it.key.replace("@","L")] = it.value
            }
        }
        return list to annoList
    }

    /**
     * 根据方法名过滤
     */
    private fun filterBlackMethodListWithMethodName(methodName: String?): Pair<MutableList<PlaitMethodList>?, HashMap<String, List<PlaitMethodList>?>> {
        var blackList = blackMethodMap?.get(methodName)
//        过滤黑名单的方法
        blackMethodMap?.get(Constants.ALL)?.apply {
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
        return blackList to blackAnonList
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