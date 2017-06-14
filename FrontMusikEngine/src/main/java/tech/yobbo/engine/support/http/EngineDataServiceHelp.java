package tech.yobbo.engine.support.http;

import tech.yobbo.engine.support.json.JSONUtils;
import tech.yobbo.engine.support.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by xiaoJ on 6/1/2017.
 */
public abstract class EngineDataServiceHelp {
    public static final int  RESULT_CODE_SUCCESS        = 1;
    public static final int  RESULT_CODE_ERROR          = -1;
    public static final String INDEX_SQL                = "select id,module_name,table_name,db_type,frame_type,remark from engine_bulid_history limit ?,?";

    /**
     * 获取url路径中的参数，放到map中
     * @param url
     * @return
     */
    public Map<String, String> getParameters(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            return Collections.<String, String> emptyMap();
        }

        String parametersStr = StringUtils.subString(url, "?", null);
        if (parametersStr == null || parametersStr.length() == 0) {
            return Collections.<String, String> emptyMap();
        }

        String[] parametersArray = parametersStr.split("&");
        Map<String, String> parameters = new LinkedHashMap<String, String>();

        for (String parameterStr : parametersArray) {
            int index = parameterStr.indexOf("=");
            if (index <= 0) {
                continue;
            }

            String name = parameterStr.substring(0, index);
            String value = parameterStr.substring(index + 1);
            parameters.put(name, value);
        }
        return parameters;
    }

    public static String returnJSONResult(int resultCode, Object content) {
        Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
        dataMap.put("ResultCode", resultCode);
        dataMap.put("Content", content);
        return JSONUtils.toJSONString(dataMap);
    }
	
	
}
