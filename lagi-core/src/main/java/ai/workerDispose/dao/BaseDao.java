package ai.workerDispose.dao;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BaseDao {
    // 定义连接数据库所需参数
    private static String driver;
    private static String url;
    private static String username;
    private static String password;

    // 将db.properties加载至内存输入流中
    static {
        Properties pro = new Properties();// 创建properties对象
        InputStream in = BaseDao.class.getClassLoader().getResourceAsStream("ai-landingbj.properties");
        try {
            pro.load(in);
            driver = pro.getProperty("driverClassName");
            url = pro.getProperty("jdbcUrl");
            username = pro.getProperty("username");
            password = pro.getProperty("password");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开连接
     */
    public Connection getCon() {
        Connection con = null;
        try {
            con = DriverManager.getConnection(url,username,password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    /**
     * 关闭连接
     */
    public void close(Connection con) {
        try {
            if (con != null)
                con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close(PreparedStatement pre) {
        try {
            if (pre != null)
                pre.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close(ResultSet res) {
        try {
            if (res != null)
                res.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close(Connection con, PreparedStatement pre, ResultSet res) {
        try {
            if (res != null)
                res.close();
            if (pre != null)
                pre.close();
            if (con != null)
                con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close(Connection con, PreparedStatement pre) {
        try {
            if (pre != null)
                pre.close();
            if (con != null)
                con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询
     *
     * @param <T>
     */
    public <T> List<T> select(Class<T> claz, String sql, Object... objs) {
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet res = null;
        List<T> list = new ArrayList<T>();
        try {
            con = getCon();// 打开连接
            pre = con.prepareStatement(sql);// 创建执行者,预编译
            // 为?占位符设置参数
            for (int i = 0; i < objs.length; i++) {
                pre.setObject(i + 1, objs[i]);// 从位置1开始设置
            }
            res = pre.executeQuery();// 执行sql
            while (res.next()) {
                T t = claz.newInstance();// 创建实例
                Field[] fields = claz.getDeclaredFields();// 获取所有字段
                // 遍历所有字段
                for (Field f : fields) {
                    f.setAccessible(true);// 设置私有字段可以访问
                    f.set(t, res.getObject(f.getName()));// 设置字段

                }
                list.add(t);// 添加至集合
            }
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            close(con, pre, res);// 关闭连接
        }
        return list;// 返回集合,没有数据则返回null
    }

    /**
     * 增删改
     *
     * @param sql
     * @param objs
     */
    public int executeUpdate(String sql, Object... objs) {
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet res = null;
        try {
            con = getCon();// 打开连接
            pre = con.prepareStatement(sql);// 创建执行者,预编译
            // 为?占位符设置参数
            for (int i = 0; i < objs.length; i++) {
                pre.setObject(i + 1, objs[i]);// 从位置1开始设置
            }
            return pre.executeUpdate();// 返回受影响的行数
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(con, pre, res);// 关闭连接
        }
        return 0;// 失败返回0
    }

    /**
     * 返id增
     *
     * @param sql
     * @param objs
     */
    public int executeUpdateGeneratedKeys(String sql, Object... objs) {
        Connection con = null;
        PreparedStatement pre = null;
        try {
            con = getCon();// 打开连接
            pre = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);// 创建执行者,预编译
            // 为?占位符设置参数
            for (int i = 0; i < objs.length; i++) {
                pre.setObject(i + 1, objs[i]);// 从位置1开始设置
            }
            int rowsAffected = pre.executeUpdate();// 返回受影响的行数
            int newCid = 0;
            ResultSet generatedKeys = pre.getGeneratedKeys();
            if (rowsAffected > 0) {
                if (generatedKeys.next()) {
                    newCid = generatedKeys.getInt(1); // 获取自增ID
                }
            }
            return newCid;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(con, pre);// 关闭连接
        }
        return 0;// 失败返回0
    }

    /**
     * 聚合查询
     *
     * @param sql
     * @param objs
     */
    public int selectCount(String sql, Object... objs) {
        Connection con = null;
        PreparedStatement pre = null;
        ResultSet res = null;
        try {
            con = getCon();// 打开连接
            pre = con.prepareStatement(sql);// 创建执行者,预编译
            // 为?占位符设置参数
            for (int i = 0; i < objs.length; i++) {
                pre.setObject(i + 1, objs[i]);// 从位置1开始设置
            }
            res = pre.executeQuery();// 执行查询,返回结果集
            if (res.next()) {
                return res.getInt(1);// 结果集的第一列
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(con, pre, res);// 关闭连接
        }
        return 0;// 失败返回0
    }

}
