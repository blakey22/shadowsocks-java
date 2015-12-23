package com.stfl.misc;

import java.lang.reflect.Constructor;

public class Reflection {
    public static Object get(String className, Object... args) {
        Object retValue = null;
        try {
            Class c = Class.forName(className);
            if (args.length == 0) {
                retValue = c.newInstance();
            }
            else if ((args.length & 1) == 0) {
                // args should come with pairs, for example
                // String.class, "arg1_value", String.class, "arg2_value"
                Class[] oParam = new Class[args.length / 2];
                for (int arg_i = 0, i = 0; arg_i < args.length; arg_i+=2, i++) {
                    oParam[i] = (Class)args[arg_i];
                }

                Constructor constructor = c.getConstructor(oParam);
                Object[] paramObjs = new Object[args.length / 2];
                for (int arg_i = 1, i = 0; arg_i < args.length; arg_i+=2, i++) {
                    paramObjs[i] = args[arg_i];
                }
                retValue = constructor.newInstance(paramObjs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retValue;
    }
}
