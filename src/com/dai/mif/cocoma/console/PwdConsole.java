/**
 * $Id: PwdConsole.java 107 2010-03-16 07:31:38Z rroeber $
 */
package com.dai.mif.cocoma.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author riedchr (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 4, 2010
 * @version $Revision: 107 $ ($Date:: 2010-03-16 08:31:38 +0100#$)
 */
public class PwdConsole {

    /**
     *
     * @param intro
     * @param prompt
     * @param repeatPrompt
     *
     * @return
     */
    public static String maskedPasswordPrompt(String intro, String prompt,
            String repeatPrompt) {

        return showPasswordPrompt(intro, prompt, repeatPrompt, true);
    }

    /**
     *
     * @param intro
     * @param prompt
     * @param repeatPrompt
     *
     * @return
     */
    public static String unmaskedPasswordPrompt(String intro, String prompt,
            String repeatPrompt) {

        return showPasswordPrompt(intro, prompt, repeatPrompt, false);
    }

    /**
     *
     * @param intro
     * @param prompt
     * @param repeatPrompt
     * @param masked
     *
     * @return
     */
    private static String showPasswordPrompt(String intro, String prompt,
            String repeatPrompt, boolean masked) {

        String pwd = "";
        String pwdRepeat = "";

        System.out.println("\n" + intro);
        System.out.println(prompt);

        ConsoleEraser consoleEraser = new ConsoleEraser();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(
                System.in));
        if (masked) {
            consoleEraser.start();
        }
        try {
            pwd = stdin.readLine();
        } catch (IOException e) {
            // TODO
        }
        if (masked) {
            consoleEraser.halt();
            System.out.print("\b");
        }

        System.out.println(repeatPrompt);
        consoleEraser = new ConsoleEraser();
        stdin = new BufferedReader(new InputStreamReader(System.in));
        if (masked) {
            consoleEraser.start();
        }
        try {
            pwdRepeat = stdin.readLine();
        } catch (IOException e) {
            // TODO
        }
        if (masked) {
            consoleEraser.halt();
            System.out.println("\b");
        }

        if (!pwd.equals(pwdRepeat)) {
            pwd = null;
        }

        return pwd;
    }

    // public static void main(String[] args) throws Exception {
    // ConsoleEraser consoleEraser = new ConsoleEraser();
    // System.out.print("Password?  ");
    // BufferedReader stdin = new BufferedReader(new InputStreamReader(
    // System.in));
    // consoleEraser.start();
    // String pass = stdin.readLine();
    // consoleEraser.halt();
    // System.out.print("\b");
    // System.out.println("Password: '" + pass + "'");
    // }
}

/**
 *
 *
 * @author Christian (NOW! Consulting GmbH) for Daimler AG, Project MIF
 * @author Last change by $Author: rroeber $
 *
 * @since Feb 4, 2010
 * @version $Revision: 107 $ ($Date:: $)
 */
class ConsoleEraser extends Thread {

    private boolean running = true;

    /**
     *
     */
    @Override
    public void run() {
        while (running) {
            System.out.print("\b ");
        }
    }

    /**
     *
     */
    public synchronized void halt() {
        running = false;
    }
}
