package com.unibeta.cloudtest.config.plugin.elements.impl;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;

import com.unibeta.cloudtest.config.plugin.CloudTestPluginFactory;
import com.unibeta.cloudtest.config.plugin.elements.UserTransactionPlugin;
import com.unibeta.vrules.utils.CommonUtils;

public class UserTransactionPluginImpl implements UserTransactionPlugin {

    // private static final String JNDI_JAVA_COMP_USER_TRANSACTION =
    // "java:comp/UserTransaction";
    private static Logger log = Logger
            .getLogger(UserTransactionPluginImpl.class);

    public UserTransaction getUserTransaction() throws Exception {

        UserTransaction trans = null;

        try {
            InitialContext context = new InitialContext();

            String userTransactionJNDI = CloudTestPluginFactory
                    .getParamConfigServicePlugin().getUserTransactionJNDI();
            if (CommonUtils.isNullOrEmpty(userTransactionJNDI)) {
                // userTransactionJNDI = JNDI_JAVA_COMP_USER_TRANSACTION;
                log.warn("JNDI user transaction was not found (or defined) in "
                        + InitialContext.class.getName()
                        + ". User transaction was disabled.");
                return null;
            }

            Object o = context.lookup(userTransactionJNDI);

            if (null != o && o instanceof UserTransaction) {
                trans = (UserTransaction) o;
            } else {
                throw new Exception(
                        "user transaction instance is not instance of "
                                + UserTransaction.class.getName()
                                + ((o == null) ? ",current instance is null "
                                        : ", current instance type is "
                                                + o.getClass().getName()));
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

        return trans;
    }

    public void after() throws Exception {

    }

    public void before() throws Exception {

    }
}
