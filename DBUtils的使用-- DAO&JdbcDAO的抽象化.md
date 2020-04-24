## DBUtils的使用-- DAO&JdbcDAO的抽象化

### 一、DAO

### Data Access Object

### 二、StudentDAO类&QueryRunner

将QueryRunner作为StudentDAO的属性，其中StudentDAO中封装实现了QueryRunner的各种好用方法。

QueryRunner,提供数据库操作的一系列重载的update()和query()操作

其中：

​		BeanHandler: 把结果集转换为一个Bean,

​		BeanListHandler：把结果集转换为一个Bean集合,

​		ScalarHandler:  把结果集转为某个数据类型的值返回,该类型通常为String或其他8种数据类型,

下面将是代码实现:

---

#### 1>StudentDAO

```java
package com.jdbc.util;

import com.jdbc.javabean.Student;
import com.jdbc.util.JdbcUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Xiangxiang
 * @date 2020/1/10 18:34
 */
public class StudentDAO {
    private QueryRunner queryRunner = new QueryRunner();


    /**
     *获取整个集合 BeanListHandler
     * @param sql
     * @return list
     * @throws SQLException
     */
    public List<Student> getList(String sql) throws SQLException {
        Connection connection = null;
        try{
            connection = JdbcUtil.getConnection();
          return  queryRunner.query(connection,sql,new BeanListHandler<Student>(Student.class));
        } finally {
            JdbcUtil.close(connection);
        }
    }

    /**
     * BeanHandler
     * @param sql
     * @return 一个对象信息
     * @throws Exception
     */
    public Student getBean(String sql) throws Exception{
        Connection connection = null;
        try{
            connection = JdbcUtil.getConnection();
            return queryRunner.query(connection,sql,new BeanHandler<>(Student.class));
        }finally {
            JdbcUtil.close(connection);
        }

    }

    /**
     * ScalarHandler
     * @param sql
     * @return 返回第一列
     * @throws Exception
     */
    public Object getvalue (String sql) throws  Exception{
        Connection connection = null;
        try {
            connection = JdbcUtil.getConnection();
            return queryRunner.query(connection,sql,new ScalarHandler());
        }finally {
            JdbcUtil.close(connection);
        }
    }

    /**
     * 通用的跟新操作
     * @param sql
     * @return 返回一个作用的行数
     * @throws Exception
     */
    public int update(String sql) throws  Exception{
        Connection connection = null;
        try {
            connection = JdbcUtil.getConnection();
           return queryRunner.update(connection,sql);
        }finally {
            JdbcUtil.close(connection);
        }

    }



}
```

#### 2>测试

```java
import java.sql.SQLException;
import java.util.List;

/**
 * @author Xiangxiang
 * @date 2020/1/10 18:59
 * DAO
 * Data Access Object
 */
public class DaoTest {
    /**
     * 获取某个值 ScalarHandler
     * 这里打印的是第一行第一列的值
     */
    @Test
    public void test3() throws Exception {
        StudentDAO studentDAO = new StudentDAO();
        Object getvalue = studentDAO.getvalue("select * from students");
        System.out.println("getvalue = " + getvalue);
    }
    /**
     * 打印一条信息,BeanHandler
     * @throws Exception
     */
    @Test
    public void test2() throws Exception {
        StudentDAO studentDAO = new StudentDAO();
        Student bean = studentDAO.getBean("select * from students");
        System.out.println("bean = " + bean);

    }
    /**
     * 测试StudentDao中的获取集合方法
     * BeanListHandler
     * @throws SQLException
     */
    @Test
    public void test() throws SQLException {
        StudentDAO studentDAO = new StudentDAO();
        List<Student> list = studentDAO.getList("select * from students");
        System.out.println("list = " + list);
        for(Student student : list){
            System.out.println("student = " + student);
        }
    }
}
```

---

### 三、抽象化JdbcDAO

#### 1>JdbcDAO抽象&泛型的使用

```java
package com.jdbc.util;

        import org.apache.commons.dbutils.QueryRunner;
        import org.apache.commons.dbutils.handlers.BeanHandler;
        import org.apache.commons.dbutils.handlers.BeanListHandler;
        import org.apache.commons.dbutils.handlers.ScalarHandler;

        import java.sql.Connection;
        import java.util.List;

/**
 * 注意：在抽象化是要注意泛型，将之前的Student类转化为泛型
 *
 * @author Xiangxiang
 * @date 2020/1/10 20:56
 */
public abstract class JdbcDAO<T> {
    private QueryRunner queryRunner = new QueryRunner();
    private Class<T> clazz;

    public JdbcDAO(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * @param sql
     * @return list
     * @throws Exception
     */
    public List<T> getList(String sql) throws Exception {
        Connection connection = null;
        try {
            connection = JdbcUtil.getConnection();
            return queryRunner.query(connection, sql, new BeanListHandler<T>(clazz)); //类似于之前的Student.class
        } finally {
            JdbcUtil.close(connection);
        }
    }

    /**
     * @param sql
     * @return T 对象
     * @throws Exception
     */
    public T getBean(String sql) throws Exception {
        Connection connection = null;
        try {
            connection = JdbcUtil.getConnection();
            return queryRunner.query(connection, sql, new BeanHandler<T>(clazz));
        } finally {
            JdbcUtil.close(connection);
        }
    }

    /**
     * @param sql
     * @return Object类型的某一单值
     * @throws Exception
     */
    public Object getValue(String sql) throws Exception {
        Connection connection = null;
        try {
            connection = JdbcUtil.getConnection();
            return queryRunner.query(connection, sql, new ScalarHandler());
        } finally {
            JdbcUtil.close(connection);
        }
    }

    /**
     * @param sql
     * @return 作用的某一行个数
     * @throws Exception
     */
    public int update(String sql) throws Exception {
        Connection connection = null;
        try {
            connection = JdbcUtil.getConnection();
            return queryRunner.update(sql);
        } finally {
            JdbcUtil.close(connection);
        }
    }

}

```

#### 2>StudentDAO继承JdbcDAO

```java
package com.jdbc.util;

import com.jdbc.javabean.Student;
import com.jdbc.util.JdbcUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Xiangxiang
 * @date 2020/1/10 18:34
 */
public class StudentDAO extends JdbcDAO<Student>{ // 继承抽象类
    /**
     * 构造器调用到Student.class
     */
    public StudentDAO(){
        super(Student.class);
    }


}
```

#### 3>测试

```java
package com.jdbc.test;

import com.jdbc.util.StudentDAO;
import com.jdbc.javabean.Student;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Xiangxiang
 * @date 2020/1/10 18:59
 * DAO
 * Data Access Object
 */
public class DaoTest {
    /**
     * 测试JdbcDAO
     */
    @Test
    public void test4() throws Exception {
        StudentDAO studentDAO = new StudentDAO();
        System.out.println(studentDAO.getList("select * from students"));

    }
```

