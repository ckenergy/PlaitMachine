package com.ckenergy.trace

/**
 * Created by chengkai on 2022/4/24.
 */
class MethodHashMap {

    private val root = MethodNode(hashMapOf(), null)

    private data class MethodNode(var nexts: HashMap<String, MethodNode?>?, var methodList: HashMap<String, List<String>?>?)

    fun put(pkg: String, methods:List<String>, list: List<String>?) {
        if (list.isNullOrEmpty()) return
        val pkgs = pkg.split("/")
        var cur = root
        pkgs.forEach {
            val nodes = cur.nexts ?: hashMapOf()
            val node = nodes[it] ?: MethodNode(null, null)
            nodes[it] = node
            cur.nexts = nodes
            cur = node
        }
//        cur.methodList = list
    }

}