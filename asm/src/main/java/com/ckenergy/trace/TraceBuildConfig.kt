package com.ckenergy.trace

import java.io.File

/**
 * Created by chengkai on 2021/7/14.
 */
class TraceBuildConfig(val blackListFile: String?) {


    private val mBlackPackageMap = HashSet<String>()
    private val mBlackClassMap = HashSet<String>()
    private val mPackageMap = HashSet<String>()
    private val mClassMap = HashSet<String>()

    /**
     * parse the BlackFile in order to pass some class/method
     * @param processor
     */
    fun parseBlackFile(/*processor: MappingCollector*/) {
        val blackConfigFile = File(blackListFile)
        if (!blackConfigFile.exists()) {
            Log.d(TraceLogTransform.TAG,
                "black config file not exist ${blackConfigFile.absoluteFile}"
            )
            return
        }
        val blackStr: String = Contants.DEFAULT_BLACK_TRACE + FileUtil.readFileAsString(blackConfigFile.absolutePath)
        val blackArray = blackStr.split("\n").toTypedArray()
        if (blackArray != null) {
            for (black1 in blackArray) {
                var black = black1.trim { it <= ' ' }
                if (black.isEmpty()) {
                    continue
                }
                if (black.startsWith("#")) {
                    Log.d(TraceLogTransform.TAG,"[parseBlackFile] comment:$black")
                    continue
                }
                if (black.startsWith("[")) {
                    continue
                }
                val keepClass = "-keepclass "
                val keepPackage = "-keep "
                val traceClass = "-traceclass "
                val tracePackage = "-trace "
                if (black.startsWith(keepClass)) {
                    black = black.replace(keepClass, "")
                    Log.d(TraceLogTransform.TAG,"[parseBlackFile] $keepClass:$black")
                    mBlackClassMap.add(black)
                } else if (black.startsWith(keepPackage)) {
                    black = black.replace(keepPackage, "")
                    Log.d(TraceLogTransform.TAG,"[parseBlackFile] $keepPackage:$black")
                    mBlackPackageMap.add(black)
                }else if (black.startsWith(traceClass)) {
                    black = black.replace(traceClass, "")
                    Log.d(TraceLogTransform.TAG,"[parseBlackFile] $traceClass:$black")
                    mClassMap.add(black)
                }else if (black.startsWith(tracePackage)) {
                    black = black.replace(tracePackage, "")
                    Log.d(TraceLogTransform.TAG,"[parseBlackFile] $tracePackage:$black")
                    mPackageMap.add(black)
                }
            }
        }
        Log.d(TraceLogTransform.TAG, "[parseBlackFile] BlackClassMap size:${mBlackClassMap.size} BlackPrefixMap size:${mBlackPackageMap.size}")
    }

    /**
     * whether it need to trace by class filename
     * @param fileName
     * @return
     */
    fun isNeedTraceClass(fileName: String): Boolean {
        var isNeed = true
        if (fileName.endsWith(".class")) {
            for (unTraceCls in Contants.UN_TRACE_CLASS) {
                if (fileName.contains(unTraceCls)) {
                    isNeed = false
                    break
                }
            }
        } else {
            isNeed = false
        }
        return isNeed
    }

    /**
     * whether it need to trace.
     * if this class in collected set,it return true.
     * @param clsName
     * @param mappingCollector
     * @return
     */
    fun isNeedTrace(clsName: String/*, mappingCollector: MappingCollector?*/): Boolean {
        var isNeed = true
        if(mClassMap.isNotEmpty() || mPackageMap.isNotEmpty()) {
            isNeed = false
            if (mClassMap.contains(clsName)) {
                isNeed = true
            } else {
//            if (null != mappingCollector) {
//                clsName = mappingCollector.originalClassName(clsName, clsName)
//            }
                for (packageName in mPackageMap) {
                    if (clsName.startsWith(packageName)) {
                        isNeed = true
                        break
                    }
                }
            }
        }

        if (isNeed) {
            if (mBlackClassMap.contains(clsName)) {
                isNeed = false
            } else {
//            if (null != mappingCollector) {
//                clsName = mappingCollector.originalClassName(clsName, clsName)
//            }
                for (packageName in mBlackPackageMap) {
                    if (clsName.startsWith(packageName)) {
                        isNeed = false
                        break
                    }
                }
            }
        }
        return isNeed
    }


}