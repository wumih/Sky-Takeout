package com.sky.context;

public class BaseContext {
    // 创建一个 ThreadLocal，专门用来存 Long 类型的用户ID
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 1. 存数据 (工人把数据写进自己的本子)
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    // 2. 取数据 (工人从自己的本子上读数据)
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    // 3. 清理数据 (把本子擦干净，防止内存泄漏)
    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
