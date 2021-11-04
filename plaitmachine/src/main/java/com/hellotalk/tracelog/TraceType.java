package com.hellotalk.tracelog;

/**
 * Created by chengkai on 2021/8/16.
 */

public enum TraceType {
     NORMAL(1);
     int type;
     TraceType(int type) {
          this.type = type;
     }
}
