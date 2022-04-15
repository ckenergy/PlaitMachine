package com.ckenergy.trace.extension

import java.util.HashMap

/**
 * Created by chengkai on 2021/11/11.
 */
data class TraceConfig(var traceMap: HashMap<String, ArrayList<PlaitMethodList>?>? = null,
                       var packages: HashMap<String, ArrayList<PlaitMethodList>?>? = null,
                       var blackPackages: HashMap<String, ArrayList<PlaitMethodList>?>? = null) {

}