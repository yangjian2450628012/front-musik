package tech.yobbo.engine.support.http;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import tech.yobbo.engine.support.util.JdbcUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Created by xiaoJ on 6/1/2017.
 *
 */
public class EngineDataService extends EngineDataServiceHelp {
    private static  EngineDataService instance      = null;
    private static DataSource dataSource            = null;

    private EngineDataService(){}
    public static EngineDataService getInstance(){
        if (instance == null) {
            instance = new EngineDataService();
        }
        return instance;
    }

    /**
     * 调用服务
     * @param url
     * @param request
     * @return
     */
    public static String process(String url,HttpServletRequest request){
        Map<String, String> parameters = getParameters(url);
        // 获取spring中的连接池
        ServletContext context = request.getServletContext();
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
        try {
            dataSource = (DataSource) ctx.getBean(Class.forName(EngineViewServlet.getDataSource()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (url.startsWith(INDEX_URL)) {
            return returnJSONResult(RESULT_CODE_SUCCESS, getIndexList(parameters));
        }
        return returnJSONResult(RESULT_CODE_ERROR, "Do not support this request, please contact with administrator.");
    }

    // 获取首页列表
    private static List getIndexList(Map<String, String> parameters) {
        JdbcUtils jdbcUtils = JdbcUtils.getInstance();
        try {
            jdbcUtils.getConnection(dataSource);
            List data = jdbcUtils.getDataBySql(INDEX_SQL,new Object[]{0,100000});
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
