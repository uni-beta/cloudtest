package com.unibeta.cloudtest.config.bind;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.unibeta.cloudtest.constant.CloudTestConstants;

/**
 * XML CDATA section wrap adapter.
 * 
 * @author jordan.xue
 */
public class XmlCDATAAdatper extends XmlAdapter<String, String> {

    @Override
    public String marshal(String v) throws Exception {

        if (v != null && !v.trim().startsWith(CloudTestConstants.CDATA_START)) {
            return CloudTestConstants.CDATA_START + v
                    + CloudTestConstants.CDATA_END;
        } else {
            return null;
        }
    }

    @Override
    public String unmarshal(String arg0) throws Exception {

        // TODO Auto-generated method stub
        return arg0;
    }

}
