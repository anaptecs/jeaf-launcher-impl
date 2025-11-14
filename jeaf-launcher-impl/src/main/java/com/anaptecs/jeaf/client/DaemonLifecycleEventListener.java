/**
 * Copyright 2004 - 2017 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.client;

/**
 * Interface defines notification mechanism to classes that are interested in the lifcycle events of applications run as
 * daemon.
 * 
 * @author JEAF Development Team
 * @version JEAF Release 1.3
 */
public interface DaemonLifecycleEventListener {

  void pauseRequested( );

  void shutdownRequested( );

  void stopRequested( );

  void continueRequested( );
}
