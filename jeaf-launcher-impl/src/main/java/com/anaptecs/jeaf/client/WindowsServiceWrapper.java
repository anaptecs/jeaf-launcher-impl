/**
 * Copyright 2004 - 2016 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 * 
 * All rights reserved.
 */
package com.anaptecs.jeaf.client;

import org.boris.winrun4j.Service;
import org.boris.winrun4j.ServiceException;

/**
 * Class implements a generic wrapper for Windows Services. Java Application can be started as a service. As this
 * wrapper also uses a dynamic launcher, applications will be updated automatically.
 * 
 * @author JEAF Development Team
 * @version JEAF Release 1.3
 */
public class WindowsServiceWrapper implements Service {
  /**
   * Attribute is used to store if the service should be shutdown.
   */
  private boolean shutdown = false;

  private static final WindowsServiceWrapper INSTANCE = new WindowsServiceWrapper();

  private DaemonLifecycleEventListener listener;

  /**
   * Main method to start service wraper. However this method will only called when directly testing from your
   * development environment. In realy wolrd WinRun4J will call {@link #serviceMain(String[])}
   * 
   * @param pArgs Arguments to that the application-
   */
  public static void main( String[] pArgs ) {
    WindowsServiceWrapper lClient = WindowsServiceWrapper.getInstance();
    lClient.serviceMain(pArgs);
  }

  /**
   * Method returns only instance of this class.
   * 
   * @return Single instance of this class. The method never returns null.
   */
  public static WindowsServiceWrapper getInstance( ) {
    return INSTANCE;
  }

  /**
   * Constructor is private to enforce singelton pattern.
   */
  private WindowsServiceWrapper( ) {
  }

  /**
   * Method registers the passed listener for lifecyle events.
   * 
   * @param pListener Listener that should be notified about lifecycle events. The parameter may be null.
   */
  public void setListener( DaemonLifecycleEventListener pListener ) {
    listener = pListener;
  }

  /**
   * Method is defined by WinRun4J as main method for services.
   */
  @Override
  public int serviceMain( String[] pArguments ) {
    // Start application via launcher
    Launcher.LOGGER.info("Starting thread to launch daemon.");
    Thread lLaunchThread = new Thread(new LauncherRunnable(pArguments));
    lLaunchThread.start();

    // As we have a daemon / service we have to loop until a shutdown is requested
    Launcher.LOGGER.info("Entering wait loop for lifecycle requests to Windows Service.");
    while (!shutdown) {
      try {
        Thread.sleep(5000);
      }
      catch (InterruptedException e) {
        // Nothing to do.
      }
    }
    Launcher.LOGGER.info("Stopping daemon due to lifecyle request.");
    return 0;
  }

  /**
   * Method is defined by WinRun4J as entry point for service control requests.
   */
  @Override
  public int serviceRequest( int pControl ) throws ServiceException {
    switch (pControl) {
      case SERVICE_CONTROL_PAUSE:
        Launcher.LOGGER.info("Pause requested.");
        if (listener != null) {
          listener.pauseRequested();
        }
        Launcher.LOGGER.info("Pausing Windows Service.");
        break;

      // Shutdown
      case SERVICE_CONTROL_SHUTDOWN:
        Launcher.LOGGER.info("Shutdown requested.");
        if (listener != null) {
          listener.shutdownRequested();
        }
        Launcher.LOGGER.info("Shtutting down Windows Service.");
        shutdown = true;
        break;

      // Stop / Pause request
      case SERVICE_CONTROL_STOP:
        Launcher.LOGGER.info("Stop requested.");
        if (listener != null) {
          listener.stopRequested();
        }
        Launcher.LOGGER.info("Stopping Windows Service.");
        shutdown = true;
        break;

      // Resume
      case SERVICE_CONTROL_CONTINUE:
        Launcher.LOGGER.info("Continue requested.");
        if (listener != null) {
          listener.continueRequested();
        }
        Launcher.LOGGER.info("Continuing Windows Service.");
        break;

      default:
        Launcher.LOGGER.severe("Unsupported control: " + pControl);
        break;
    }
    return 0;
  }
}
