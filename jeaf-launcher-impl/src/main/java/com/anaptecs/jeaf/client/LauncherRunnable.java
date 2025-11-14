/**
 * Copyright 2004 - 2017 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.client;

import java.util.logging.Level;

public class LauncherRunnable implements Runnable {
  String[] arguments;

  public LauncherRunnable( String[] pArguments ) {
    arguments = pArguments;
  }

  @Override
  public void run( ) {
    try {
      Launcher.main(arguments);
    }
    catch (Exception e) {
      Launcher.LOGGER.log(Level.SEVERE, "Stopping daemon due to exception.");
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
