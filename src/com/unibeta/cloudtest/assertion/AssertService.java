package com.unibeta.cloudtest.assertion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.unibeta.vrules.engines.ValidationEngine;
import com.unibeta.vrules.engines.ValidationEngineFactory;

/**
 * A core assert validation service that is supported by vRules4j engine.
 * vRules4j v3 is needed.
 * 
 * @author jordan.xue
 */
public class AssertService {

    private static final int MAX_TRY_TIMES = 10;
    private static final CloudTestAssert CLOUD_TEST_ASSERT_OBJECT = new CloudTestAssert();
    private static Logger log = Logger.getLogger(AssertService.class);

    public List<AssertResult> doAssert(String fileName, String assertId,
            Object obj) {

        ValidationEngine engine = ValidationEngineFactory.getInstance();
        Map<String, Object> map = Collections
                .synchronizedMap(new LinkedHashMap<String, Object>());

        map.put("$$AssertObject$$", obj);
        map.put(assertId, CLOUD_TEST_ASSERT_OBJECT);

        String[] errs = null;
        int i = 0;
        boolean done = false;
        while (!done) {

            try {
                errs = engine.validate(map, fileName);
                done = true;
            } catch (ClassNotFoundException e) {
                if (i < MAX_TRY_TIMES) {
                    try {
                        Thread.sleep(1 * 1000);
                    } catch (InterruptedException e1) {
                        // Empty
                    }
                } else {
                    log.error("assertion failure " + MAX_TRY_TIMES
                            + " times caused by: " + e.getMessage(), e);
                    break;
                }
            } catch (Exception e) {
                log.error("assertion failure caused by: " + e.getMessage(), e);
                break;
            }

            i++;
        }

        if (errs == null) {
            return null;
        } else {
            List<AssertResult> list = new ArrayList<AssertResult>();

            for (String s : errs) {
                AssertResult a = new AssertResult();
                a.setErrorMessage(s);

                list.add(a);
            }

            return list;
        }

    }
}
