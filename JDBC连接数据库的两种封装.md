### JDBC连接数据库



#### 1.没有通过常量池的简单连接

---



配置文件

```properties
driverClassName = com.mysql.jdbc.Driver
url = jdbc:mysql://127.0.0.1:3306/jdbc
user = root
password = 123456
```



---



主函数调用方法

```java
package com.atguigu.jdbc.test;

import java.io.IOException;
import java.sql.SQLException;

public class JdbcTest {
	public static void main(String[] args) throws ClassNotFoundException, IOException, SQLException {
		Method method = new Method();
		method.Jdbc();

	}
}

```

----

连接方法

```java
package com.atguigu.jdbc.test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Method {
	public void Jdbc() throws IOException, ClassNotFoundException, SQLException {
		InputStream inputStream = this.getClass().getClassLoader()
            				.getResourceAsStream("jdbc.properties");
		Properties properties = new Properties();
		properties.load(inputStream); //读流
		inputStream.close();
		
		String ClassName = properties.getProperty("driverClassName");
		String url = properties.getProperty("url");
		String user = properties.getProperty("user");
		String password = properties.getProperty("password");
		Class.forName(ClassName);
		Connection connection = DriverManager.getConnection(url, user, password);
		System.out.println(connection);
		connection.close();
		
	}
}

```

---



#### 2.通过连接池连接

在连接数据库时，为数据库建立一个"缓冲池"。在需要建立数据库连接时，只需要从”缓冲池“中取出一个，使用完毕之后再放回。

允许应用程序重复使用一个现有的数据库连接，而不是每次都重新建立一个。

其优点就是有：

1.资源重用，

2.更快的系统反应速度

3.新的资源分配手段

4.统一的连接管理，避免数据库连接泄露

---



以下是Druid数据库连接池,连接数据库代码

配置文件 druid.properties

```properties
driverClassName = com.mysql.jdbc.Driver
url = jdbc:mysql://127.0.0.1:3306/jdbc
username = root
password = 123456
initialSize = 5
maxActive = 50
minIdle = 10


```



JdbcUtil类构建连接池链接库方法

```java
package com.jdbc.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource; //注意导包,很容易出错

import com.alibaba.druid.pool.DruidDataSourceFactory;
/**
 *  进行封装 ,通过dataSource获取配置文件,连接数据库.
 * @author Xiangxiang
 *	@create 2020
 */
public class JdbcUtil {
	private static DataSource dataSource; // 将dataSource变成私有属性
	static {// 静态代码块 类在加载时,就把池子建好
		InputStream inputStream = JdbcUtil.class.getClassLoader().getResourceAsStream("druid.properties");
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
			inputStream.close();
			dataSource = DruidDataSourceFactory.createDataSource(properties);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection() throws SQLException {
		Connection connection = dataSource.getConnection(); // 连接
		return connection;
	}

}


```



测试类

```java
package com.jdbc.util;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;
/**
 *	 测试类  一定要配置mysql jar包
 * @author Xiangxiang
 *	@create 2020
 */
public class JdbcTest {
	@Test
	public void test() { 
		try {
			Connection connection = JdbcUtil.getConnection(); //获取连接
			System.out.println(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}


```





---

Note: Jdbc中使用连接池效率高效,之后的学习中会慢慢简化操作，sql语句也很重要。

