/**
 * Copyright 2004 - 2017 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import fxlauncher.HeadlessMainLauncher;

public class Launcher {
  public static final String CONFIG_FILE_PROPERTY = "launcher.config.file";

  public static final String DEFAULT_CONFIG_FILE_NAME = "./conf/client.properties";

  public static final String SERVER_URL = "server.url";

  public static final String APP_PATH = "app.path";

  public static final String APP_FILE = "app.file";

  public static final String LAUNCH_TYPE = "launch.type";

  public final static Logger LOGGER = Logger.getLogger(Launcher.class.getSimpleName());

  public static void main( String[] pArgs ) throws Exception {
    // Check for configuration via system properties
    Properties lSystemProperties = System.getProperties();
    String lServerURL = lSystemProperties.getProperty(SERVER_URL);
    String lAppPath = lSystemProperties.getProperty(APP_PATH);
    String lAppFile = lSystemProperties.getProperty(APP_FILE);
    String lLaunchTypeString = lSystemProperties.getProperty(LAUNCH_TYPE);

    // Load configuration from property file
    String lConfigFileName = lSystemProperties.getProperty(CONFIG_FILE_PROPERTY, DEFAULT_CONFIG_FILE_NAME);
    File lFile = new File(lConfigFileName);
    String lConfigFilePath = lFile.getCanonicalPath();
    LaunchType lLaunchType;
    if (lServerURL == null || lAppPath == null || lAppFile == null || lLaunchTypeString == null) {

      LOGGER.info("Loading configuration from: " + lConfigFileName);
      Properties lProperties = new Properties();
      lProperties.load(new FileInputStream(lConfigFileName));

      if (lServerURL == null) {
        lServerURL = lProperties.getProperty(SERVER_URL);
      }
      if (lAppPath == null) {
        lAppPath = lProperties.getProperty(APP_PATH);
      }
      if (lAppFile == null) {
        lAppFile = lProperties.getProperty(APP_FILE);

      }
      if (lLaunchTypeString == null) {
        lLaunchTypeString = lProperties.getProperty(LAUNCH_TYPE);
      }
    }
    // Check configuration
    if (lServerURL == null) {
      String lMessage =
          "Property " + SERVER_URL + " not defined as system property nor via configuration file " + lConfigFilePath;
      throw new IllegalArgumentException(lMessage);
    }
    if (lAppPath == null) {
      String lMessage =
          "Property " + APP_PATH + " not defined as system property nor via configuration file " + lConfigFilePath;
      throw new IllegalArgumentException(lMessage);
    }
    if (lAppFile == null) {
      String lMessage =
          "Property " + APP_FILE + " not defined as system property nor via configuration file " + lConfigFilePath;
      throw new IllegalArgumentException(lMessage);
    }
    if (lLaunchTypeString == null) {
      String lMessage =
          "Property " + LAUNCH_TYPE + " not defined as system property nor via configuration file " + lConfigFilePath;
      throw new IllegalArgumentException(lMessage);
    }

    lLaunchType = LaunchType.valueOf(lLaunchTypeString);

    // Set server url also as system property.
    lSystemProperties.setProperty(SERVER_URL, lServerURL);

    // Build URI und app.xml path.
    String lURI = lServerURL + "/" + lAppPath;
    String lURIParam = "--uri=" + lURI;
    String lAppURI = lURI + "/" + lAppFile;
    String lAppParam = "--app=" + lAppURI;
    LOGGER.info("Using URI param: " + lURIParam);
    LOGGER.info("Using app param: " + lAppParam);
    String[] lParams = new String[] { lAppParam, lURIParam };

    // Check if server is available
    String lErrorMessage = checkAvailability(lServerURL, lAppURI);

    // Launch app with the required launcher.
    switch (lLaunchType) {
      case JavaFX:
        launchJavaFXApp(lParams, lErrorMessage);
        break;

      case Swing:
        launchSwingApp(lParams, lErrorMessage);
        break;

      case Daemon:
        launchDaemon(lParams, lErrorMessage, lServerURL, lAppURI);
        break;

      default:
        throw new RuntimeException("Unexpected launch type " + lLaunchTypeString);
    }
  }

  private static String checkAvailability( String pServerURL, String pAppURI ) throws MalformedURLException {
    URL lURL = new URL(pAppURI);

    String lErrorMessage;
    try {
      LOGGER.info("Trying to open connection to " + pAppURI);
      HttpURLConnection lConnection = (HttpURLConnection) lURL.openConnection();
      lConnection.setRequestMethod("GET");
      lConnection.connect();
      int lAppURIResponseCode = lConnection.getResponseCode();
      if (lAppURIResponseCode == HttpURLConnection.HTTP_OK) {
        lErrorMessage = null;
      }
      else {
        lErrorMessage = "Unable to connect to server '" + pServerURL
            + "'. Please ensure that you have a network connection.\n Received http status code '" + lAppURIResponseCode
            + "' when trying to access app uri '" + pAppURI + "'.";
      }
    }
    catch (IOException e) {
      lErrorMessage = "Unable to connect to server '" + pServerURL + "'. " + e.getMessage();
    }

    return lErrorMessage;

  }

  private static void launchJavaFXApp( String[] pParams, String pErrorMessage ) {
    if (pErrorMessage == null) {
      fxlauncher.Launcher.main(pParams);
    }
    else {
      LOGGER.severe(pErrorMessage);
      JOptionPane.showMessageDialog(null, pErrorMessage, "Error: Unable to connect to server",
          JOptionPane.WARNING_MESSAGE);
      System.exit(-1);
    }
  }

  private static void launchSwingApp( String[] pParams, String pErrorMessage ) {
    try {
      if (pErrorMessage == null) {
        HeadlessMainLauncher.main(pParams);
      }
      else {
        LOGGER.severe(pErrorMessage);
        JOptionPane.showMessageDialog(null, pErrorMessage, "Error: Unable to connect to server",
            JOptionPane.WARNING_MESSAGE);
      }
    }
    catch (Exception e) {
      Launcher.LOGGER.log(Level.SEVERE, "Stopping daemon due to exception.");
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static void launchDaemon( String[] pParams, String pErrorMessage, String pServerURL, String pAppURI )
    throws Exception {

    String lErrorMessage = pErrorMessage;
    while (lErrorMessage != null) {
      LOGGER.severe(pErrorMessage);
      // Wait for another 30 seconds. Until the server is may be available.
      LOGGER.info("Wating for 30 seconds before trying to connect again.");
      try {
        Thread.sleep(30 * 1000);
      }
      catch (InterruptedException e) {
        // Nothing to do.
      }
      lErrorMessage = checkAvailability(pServerURL, pAppURI);
    }
    // Starting client software.
    HeadlessMainLauncher.main(pParams);
  }
}
