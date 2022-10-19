package com.ckenergy.trace.extension

import java.util.HashMap

/**
 * Created by chengkai on 2021/11/11.
 */
data class PlaintConfig(
    var classMap: HashMap<String, ArrayList<PlaitMethodList>?>? = null,
    var packages: HashMap<String, ArrayList<PlaitMethodList>?>? = null,
    var blackPackages: HashMap<String, ArrayList<PlaitMethodList>?>? = null
) {

}