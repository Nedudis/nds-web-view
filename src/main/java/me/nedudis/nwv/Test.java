package me.nedudis.nwv;

import net.minecraft.commands.CommandSourceStack;

public class Test {
    public static void main(String[] args) {
        for (java.lang.reflect.Method m : CommandSourceStack.class.getMethods()) {
            if (m.getName().toLowerCase().contains("perm")) {
                System.out.println(m.getName() + " " + m.getParameterTypes().length);
            }
        }
    }
}
