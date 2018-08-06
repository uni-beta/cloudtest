package com.unibeta.cloudtest.config.plugin.elements;

import javax.transaction.UserTransaction;

import com.unibeta.cloudtest.config.plugin.CloudTestPlugin;

/**
 * Transaction manager plugin interface.
 * 
 * @author jordan.xue
 * 
 */
public interface UserTransactionPlugin extends CloudTestPlugin {

  /**
   * Gets user transaction instance for common transaction control.
   * 
   * @return UserTransaction
   * @throws Exception
   */
  public UserTransaction getUserTransaction() throws Exception;

  /**
   * The actions before transaction.
   * 
   * @return
   * @throws Exception
   */
  public void before() throws Exception;

  /**
   * The actions after transaction.
   * 
   * @return
   * @throws Exception
   */
  public void after() throws Exception;
}
