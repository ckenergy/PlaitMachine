package com.ckenergy.trace.utils

import java.io.File
import java.util.zip.ZipFile


/**
 * @date 2023/4/3
 * @desc
 */
object TraceUtil {

    /**
     * 过滤因为开启了jetified造成两个jar包的问题
     */
    fun filterJetifiedJar(list: List<File>): List<File> {
        val map = hashMapOf<String, ArrayList<File>>()
        list.map { file ->
            file.apply {
                val name = name.removePrefix("jetified-")
                val list = map[name] ?: arrayListOf<File>().also {
                    map[name] = it
                }
                list.add(this)
            }
        }

        map.filter { it.value.size > 1 }.forEach {
            val nameSet = hashSetOf<String>()
            val files = arrayListOf<File>().apply { addAll(it.value) }
            files.find { it.name.contains("jetified-") }?.let {
                files.remove(it)
                files.add(0, it)
            }
            files.forEach { file ->
                val nameSet1 = hashSetOf<String>()
                val zipFile = ZipFile(file)
                val enumeration = zipFile.entries()
                while (enumeration.hasMoreElements()) {
                    val entry = enumeration.nextElement()
                    if (!entry.name.contains("META-INF") && entry.name.endsWith(".class")) {
                        if (nameSet.contains(entry.name)) {
                            it.value.remove(file)
                            nameSet1.clear()
                            Log.d("TransformClassesTask", "remove file:${file.absoluteFile}")
                            break
                        }
                        nameSet1.add(entry.name)
                    }
                }
                if (nameSet1.isNotEmpty()) {
                    nameSet.addAll(nameSet1)
                }
                try {
                    zipFile.close()
                } catch (e: Exception) {
                }
            }
        }
        val filterAllJar = arrayListOf<File>()
        map.forEach { (t, u) ->
            filterAllJar.addAll(u)
        }
        return filterAllJar
    }

}