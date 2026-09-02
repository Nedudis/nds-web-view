package me.nedudis.nwv.client.browser;

import net.dimaskama.mcef.api.MCEFBrowser;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class BrowserAwtInput {
    private static final Map<Integer, Integer> GLFW_TO_AWT_VK = new HashMap<>();

    static {
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_ENTER, KeyEvent.VK_ENTER);
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_KP_ENTER, KeyEvent.VK_ENTER);
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_TAB, KeyEvent.VK_TAB);
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_BACKSPACE, KeyEvent.VK_BACK_SPACE);
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_LEFT, KeyEvent.VK_LEFT);
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_RIGHT, KeyEvent.VK_RIGHT);
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_UP, KeyEvent.VK_UP);
        GLFW_TO_AWT_VK.put(GLFW.GLFW_KEY_DOWN, KeyEvent.VK_DOWN);
    }

    public static int getAwtKeyCode(int glfwKey) {
        return GLFW_TO_AWT_VK.getOrDefault(glfwKey, glfwKey);
    }
    public static boolean mapIncludes(int glfwKey) { return GLFW_TO_AWT_VK.containsKey(glfwKey); }

    /** @return true, if key has been processed through AWT. */
    public static boolean trySendControlKey(MCEFBrowser browser, int glfwKey, boolean pressed) {

        Integer awtCode = GLFW_TO_AWT_VK.get(glfwKey);
        if (awtCode == null) return false;

        Component ui = browser.getCefBrowser().getUIComponent();
        if (ui == null) return false;

        long when = System.currentTimeMillis();
        int id = pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED;
        ui.dispatchEvent(new KeyEvent(ui, id, when, 0, awtCode, KeyEvent.CHAR_UNDEFINED));

        if (pressed) {
            char typedChar = switch (glfwKey) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> '\n';
                case GLFW.GLFW_KEY_TAB -> '\t';
                default -> KeyEvent.CHAR_UNDEFINED;
            };
            if (typedChar != KeyEvent.CHAR_UNDEFINED) {
                ui.dispatchEvent(new KeyEvent(ui, KeyEvent.KEY_TYPED, when, 0, KeyEvent.VK_UNDEFINED, typedChar));
            }
        }

        return true;
    }
}
