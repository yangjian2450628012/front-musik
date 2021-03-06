package tech.yobbo.engine.support.http;

import tech.yobbo.engine.support.data.EngineDataManagerFacade;
import tech.yobbo.engine.support.util.JdbcUtils;
import tech.yobbo.engine.support.util.Utils;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/** 
 * engine业务逻辑处理类，主要包括数据库连接池的获取和转发前端发送请求到业务实现类
 * Created by xiaoJ on 6/1/2017.
 *
 */
public class EngineDataService extends EngineDataServiceHelp {
    private static  EngineDataService instance                      = null;
    private DataSource dataSource                                   = null;
    private String dataSource_className                             = null;
    private Date startTime											= null;
    private String db_type                                          = null;
    private String db_version                                       = null;
    private String jar_path                                         = null;
    private static EngineDataManagerFacade engineDataManagerFacade  = EngineDataManagerFacade.getInstance(); //获取具体数据操作类

    private EngineDataService(){}
    public static EngineDataService getInstance(){
        if (instance == null) {
            instance = new EngineDataService();
        }
        return instance;
    }

    /**
     * 初始化自动化引擎，包括创建相应依赖表
     */
    protected void init(){
        if(EngineViewServlet.webAppPath != null){
//            jar_path = EngineViewServlet.webappPath + "WEB-INF/lib/engine-1.0.0.jar"; // 最终版
            jar_path = "/D:/engineJar/engine-1.0.0.jar";
        }
        if(this.dataSource == null) return;
        // 获取连接池信息，判断数据库类型
        JdbcUtils jdbcUtils = JdbcUtils.getInstance();
        try {
            jdbcUtils.getConnection(this.dataSource);
            String mysql_version = "select CONCAT('mysql_',version()) as version from dual";
            String oracle_version = "select * from v$version";
            Object d_oracle = jdbcUtils.getOneData(oracle_version);
            Object d_mysql = jdbcUtils.getOneData(mysql_version);
            if(d_oracle != null){
                db_type = "oracle";
                db_version = d_oracle.toString();
            }else if(d_mysql != null){
                db_type = "mysql";
                db_version = d_mysql.toString();
            }
            if (db_type != null) {
                JarFile jarFile = new JarFile(jar_path);
                String path = EngineViewServlet.getResourcePath()+"/dbbase/"+db_type+"_base.sql";
                ZipEntry db_entry = jarFile.getEntry(path);
                InputStream in = jarFile.getInputStream(db_entry);
                List<String> sqlS = loadSql(Utils.read(in));
                jdbcUtils.execute(sqlS);
                in.close();
                jarFile.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                jdbcUtils.closeDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param sqls sql脚本内容
     * @return sql列表
     * @throws Exception
     */
    private List<String> loadSql(String sqls) throws Exception {
        List<String> sqlList = new ArrayList<String>();
        try {
            // Windows 下换行是 /r/n, Linux 下是 /n
            String[] sqlArr = sqls.toString().split("(;//s*//r//n)|(;//s*//n)");
            for (int i = 0; i < sqlArr.length; i++) {
                String sql = sqlArr[i].replaceAll("--.*", "").trim();
                if (!sql.equals("")) {
                    sqlList.add(sql);
                }
            }
            return sqlList;
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    /**
	 * 获取模板中数据,返回给模板
	 * @param url 前端发送的请求
	 * @param parameters URL中的参数列表
	 * @param context ServletContext上下文
	 * @return 返回模板中需要的数据
	 */
	public Object processTemplate(String url,Map<String, String> parameters,ServletContext context){
        if(dataSource == null){
            setDataSource(context);
        }
        if (url.startsWith("/index.html")) { //首页信息
            return  engineDataManagerFacade.getBasicInfo(parameters);
        }else if(url.startsWith("/code.html")){
        	return engineDataManagerFacade.getCodeInfo(parameters);
        }
        return null;
	}

	/**
     * 调用服务，ajax异步相关的，只要师ajax请求都会走process这个方法去处理
     * @param url
     * @param context 上下文
     * @return
     */
    public String process(String url,ServletContext context){
        Map<String, String> parameters = getParameters(url);
        if (dataSource == null) {
            setDataSource(context);
        }
        if (url.startsWith("/index.json")) {
            return returnJSONResult(RESULT_CODE_SUCCESS, engineDataManagerFacade.getIndexList());
        } else if(url.startsWith("/tree.json")){
            return returnJSONResult(RESULT_CODE_SUCCESS,engineDataManagerFacade.getTemplateTree(parameters));
        } else if(url.startsWith("/treeJavaBase.json")){
            return  returnJSONResult(RESULT_CODE_SUCCESS,engineDataManagerFacade.getJavaBaseTree(parameters));
        }
        return returnJSONResult(RESULT_CODE_ERROR, "Do not support this request, please contact with administrator.");
    }


    /**
     * 通过反射获取spring中dataSource连接池bean
     * 避免没导包，导致运行报错
     * @param context servletContext上下文
     */
    protected void setDataSource(ServletContext context){
    	if(EngineViewServlet.getDataSource() == null) return;
        System.out.println("初始化数据库连接池！");
        try {
        	// 通过反射获取spring中的连接池
			Class<?> _class = Class.forName("org.springframework.web.context.support.WebApplicationContextUtils");
			Method method  = _class.getMethod("getRequiredWebApplicationContext", ServletContext.class);
            Object ctx = method.invoke(method.getReturnType(), context);
           
            if(EngineViewServlet.getDataSource().contains(".")){
            	Method m = ctx.getClass().getMethod("getBean", Class.class);
                dataSource = (DataSource) m.invoke(ctx
                        , Class.forName(EngineViewServlet.getDataSource()));
                dataSource_className = EngineViewServlet.getDataSource();
            }else{
            	Method m = ctx.getClass().getMethod("getBean",String.class);
                dataSource = (DataSource) m.invoke(ctx, EngineViewServlet.getDataSource());
                dataSource_className = dataSource.getClass().getName();
            }
            Method m = ctx.getClass().getMethod("getStartupDate");
            Long startUpDate = (Long)m.invoke(ctx);
            startTime = new Date(startUpDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }
    public String getDataSource_className() {
        return dataSource_className;
    }
    public Date getStartTime(){
    	return startTime;
    }

    public String getJar_path() {
        return jar_path;
    }
    public String getDb_version() {
        return db_version;
    }
}
