## JDBC 的各种由繁入简的插入和获取数据的底层测试以及最后的封装

### 一、数据库驱动的连接配置

https://blog.csdn.net/weixin_45943729/article/details/103839253

使用之前的两种方式，可以直接调用JdbcUtil实现连接。

下述使用的是基于德鲁伊连接池的使用测试。

然后补一个关闭资源方法,忘掉了.......

```java
//之后会进一步补充JdbcUtil内的关闭方法	
public static void close(Connection connection, Statement preparedStatement) {
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
```



### 二、向数据库中建表已经插入数据的测试

#### 2.1   测试一

```java
//测试一 本测试使用Statement,基本不会用了,有注入的风险
package com.jdbc.test;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.Test;
import com.jdbc.util.JdbcUtil;
/**
 * 测试类
 * 在本类中进行添加删除操作
 * 最后将测试所得代码封装成通用方法
 * @author Xiangxiang
 *	@create 2020
 */
public class ConnectionTest {
	@Test
	/**
	 * JDBC API是一系列的接口,使得应用程序能够进行数据库连接,执行SQL语句,得到返回结果
	 */
	public void test() { //本测试使用Statement,基本不会用了,有注入的风险
		//进行建表操作
		Connection connection = null;//初始化
		Statement statement = null; //执行体对象   注意导包
		try {
			 connection = JdbcUtil.getConnection(); //获取连接
			 statement = connection.createStatement(); //通过连接来获取执行体对象
//			 System.out.println(statement); // 检查是否连接成功
			 String sql = "create table if not exists students("
					 		+"id int auto_increment,"
					 		+"name varchar(20),"
					 		+"grade varchar(20),"
					 		+"gender enum('男','女') default '男',"
					 		+"score double,"
					 		+"primary key(id)"
					 		+")"; // 编写sql短语
			 int rows = statement.executeUpdate(sql); //执行sql短语返回行数
			 System.out.println(rows + "rows affected"); //打印检测作用行数
			 
			 //进行插入数据
			 sql = "insert into students("
						 +"name,"
						 +"grade,"
						 +"gender,"
						 +"score"
				 +") values ("
						 +"'相相',"
						 +"'七年级',"
						 +"'男',"
						 +"'98'"
					+")";
			 rows = statement.executeUpdate(sql);		
			System.out.println(rows + "rows affected");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JdbcUtil.close(connection, statement);			
		}
	}
}

```

---



#### 2.2  测试二

```java
package com.jdbc.test;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.junit.Test;
import com.jdbc.util.JdbcUtil;
/**
 *	测试二,使数据库没有注入的风险,通常使用预编译PreparedStatement 进行处理,以下只进行一条数据的插入
 * @author Xiangxiang
 *	@create 2020
 */
public class ConnectionTest {
	@Test
	/**
	 * 因为Statement有注入的风险,所以通常使用预编译PreparedStatement 进行处理
	 */
	public void test2() { //下述只进行插入数据操作,之后的ORMapping也只使用Students类
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = JdbcUtil.getConnection();
			String sql = "insert into students("
							+"name,"
							+"grade,"
							+"gender,"
							+"score"
						+") values ("
							+"?,"	
							+"?,"	
							+"?,"	
							+"?"
						+")"; //?预编译
			 preparedStatement = connection.prepareStatement(sql);
			 preparedStatement.setString(1, "吱吱"); //sql中从1开始
			 preparedStatement.setString(2, "八年级");
			 preparedStatement.setString(3, "女");
			 preparedStatement.setDouble(4, 93.5);
			 int rows = preparedStatement.executeUpdate();//在执行前插入数据
			 System.out.println(rows + "rows affected");
		
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			JdbcUtil.close(connection, preparedStatement);
		}
		

	}
	
}	
	
	
	
```

----

#### 2.3  测试三

```java
/**
插入sql语句可以简写,表中的列可以看做成一个数组,通过遍历数组进行插入操作
*/
package com.jdbc.test;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.junit.Test;
import com.jdbc.util.JdbcUtil;
/**
 * 测试类
 * 在本类中进行添加删除操作
 * 最后将测试所得代码封装成通用方法
 * @author Xiangxiang
 *	@create 2020
 */
public class ConnectionTest {
	@Test
	public void test3() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		Object[] args = {"王伟","六年级","男",83};
		try {
			connection = JdbcUtil.getConnection();
			String sql = "insert into students(name, grade, gender, score) values (?, ?, ?, ?)";
			preparedStatement = connection.prepareStatement(sql);
			//处理?
			for(int i = 0 ; i < args.length;i++) {
				preparedStatement.setObject(i+1, args[i]); //从第一个位置插入
			}
			int rows = preparedStatement.executeUpdate();
			 System.out.println(rows + "rows affected");
		
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			JdbcUtil.close(connection, preparedStatement);
		}
		
	}
}


```

----

#### 2.4最后的封装以及测试四

##### 对上述跟新操作进行封装

```java
package com.jdbc.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
/**
 * 
 * @author Xiangxiang
 *	@create 2020
 */
public class CommonUtil {
	/**
	 * 通用的跟新操作
	 * 
	 * @param connection 连接
	 * @param sql 可以执行的sql短语 除select外的DML和DDL
	 * @param args 对象数组
	 * @return rows 执行操作所作用的行数
	 * @throws Exception 抛整个异常
	 */
	public static int update(Connection connection, String sql, Object... args) throws Exception {
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = connection.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				preparedStatement.setObject(i + 1, args[i]);// 将数组元素依次插入
			}
			int rows = preparedStatement.executeUpdate(); // 真正的执行sql
			return rows;

		} finally {
			JdbcUtil.close(connection, preparedStatement);
		}

	}
}
```



##### 测试封装

```java
	@Test
	/**
	 * 测试封装好的CommonUtil内的通用跟新操作
	 */
	public void test4() {
		String sql = "insert into students(name,grade,gender,score) values (?, ?, ?, ?)";
		Connection connection = null;
		try {
			int rows;
			connection = JdbcUtil.getConnection();
			//已经在方法中实现PreparedStatement
			rows = CommonUtil.update(connection, sql, "小刚","四年级","男",95.5);
			System.out.println(rows + "rows affected");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
```

***综上，插入操作封装及测试完成...***

----

### 三、从数据库中提取已有的数据测试

#### 3.1 测试一

```java
package com.jdbc.test;
/**
 * 从数据中获取数据信息的依次测试
 * @author Xiangxiang
 *	@create 2020
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import com.jdbc.util.JdbcUtil;

public class ResultSetTest {
/**
 * +----+------+--------+--------+-------+
| id | name | grade  | gender |	   score |
+----+------+--------+--------+-------+
|  1 | 相相  | 七年级 |     男      |    98 |
|  2 | 吱吱  | 八年级 |     女      |  93.5 |
|  3 | 王伟  | 六年级 |     男      |    83 |
|  4 | 小刚  | 四年级 |     男      |  95.5 |
|  5 | 小刚  | 四年级 |     男      |  95.5 |
+----+------+--------+--------+-------+
 */
	
	
	@Test
	public void test() {
		String sql = "select id , name ,grade , gender , score from students where id > ?";
		Connection connection = null;
		PreparedStatement preparedStatement = null;//预编译处理
		ResultSet resultSet = null; //结果集是一种资源最后需要关闭
		try {
			 connection = JdbcUtil.getConnection();
			 preparedStatement = connection.prepareStatement(sql);
			 preparedStatement.setObject(1, 4); //这里其实是设置从第几行开始,取多少个数据   可以去掉 
			 resultSet = preparedStatement.executeQuery();//执行Query 结果集的内部游标指向第一行之前  ..有点像集合中学习过的迭代器
			 while(resultSet.next()) {//移动游标到第一行
				 int id = resultSet.getInt(1); //参数是列
				 String name = resultSet.getString("name");  //这里也可以写成标签
				 String grade = resultSet.getString("grade");
				 String gender = resultSet.getString(4);
				 double score = resultSet.getDouble(5);
				 System.out.println("学生的编码是"+id+",学生的姓名是"+name+",学生的年级是"+grade+",学生的性别是"+gender+",学生的分数是"+score);
			 }
		
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			JdbcUtil.close(connection, preparedStatement);
			try {
				if(resultSet != null) {
					resultSet.close();//还没有重写JdbcUtil的关闭方法,之后会加上去
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}

```

#### 3.2 测试二

```java
package com.jdbc.test;
/**
 * 从数据中获取数据信息的依次测试
 * @author Xiangxiang
 *	@create 2020
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.Test;

import com.jdbc.util.JdbcUtil;

public class ResultSetTest {
/**
 * +----+------+--------+--------+-------+
| id | name | grade  | gender | score |
+----+------+--------+--------+-------+
|  1 | 相相      | 七年级 |     男           |    98 |
|  2 | 吱吱      | 八年级 |     女           |  93.5 |
|  3 | 王伟      | 六年级 |     男           |    83 |
|  4 | 小刚      | 四年级 |     男           |  95.5 |
|  5 | 小刚      | 四年级 |     男           |  95.5 |
+----+------+--------+--------+-------+
 */
	
	
	@Test
	/**
	 * 通过ResultSetMetaData接口,获取数据的列数,通过循环遍历得到数据
	 */
	public void test2() {
		String sql = "select id , name , grade ,gender ,score from students ";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			connection = JdbcUtil.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();//获取虚表得结构
			int columnCount = resultSetMetaData.getColumnCount();//获得列数
			System.out.println("得到的行数是"+columnCount);//打印得到得列数
			for (int i = 0; i < columnCount; i++) {// 遍历每一列,并且赋值给标签 打印出title
				String label = resultSetMetaData.getColumnLabel(i + 1);
				System.out.print(label+"\t");
			}
			System.out.println();
			
			while(resultSet.next()) {//	//遍历每一行,并且返回boolean类型 true ----继续执行
				for(int i = 0 ; i < columnCount ;i++) {
					String label = resultSetMetaData.getColumnLabel(i+1);
					Object object = resultSet.getObject(label);
					System.out.print(object+"\t");
				}
				System.out.println();
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(resultSet != null) {
					resultSet.close();
				}
			} catch (Exception e2) {
				
			}
		}
	}
```

***综上，对数据库获取数据元素完成....下面就是一个综合大练习..***

*****

### 四、ORMapping以及反射

#### 4.1 测试二

```java
// Object DB Maping
package com.jdbc.test;
/**
 * 从数据中获取数据信息的依次测试
 * @author Xiangxiang
 *	@create 2020
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.Test;

import com.jdbc.javabean.Student;
import com.jdbc.util.JdbcUtil;

public class ResultSetTest {
/**
 * +----+------+--------+--------+-------+
| id | name | grade  | gender | score |
+----+------+--------+--------+-------+
|  1 | 相相      | 七年级 |     男           |    98 |
|  2 | 吱吱      | 八年级 |     女           |  93.5 |
|  3 | 王伟      | 六年级 |     男           |    83 |
|  4 | 小刚      | 四年级 |     男           |  95.5 |
|  5 | 小刚      | 四年级 |     男           |  95.5 |
+----+------+--------+--------+-------+
 */
	@Test
	/**
	 * ORMapping  Object DB Maping 
	 * 泛型 - Student集合的使用
	 */
	public void test3() {
		ArrayList<Student> list = new ArrayList<>();
		String sql = "select id , name ,grade , gender , score from students";
		Connection connection = null;
		PreparedStatement preparedStatement =null;
		ResultSet resultSet = null;
		try {
			connection = JdbcUtil.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			while(resultSet.next()) {
				int id = resultSet.getInt("id");
				String name = resultSet.getString("name");
				String grade = resultSet.getString("grade");
				String gender = resultSet.getString("gender");
				double score = resultSet.getDouble("score");
				Student student = new Student(id,name,grade,gender,score);
				list.add(student);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(resultSet != null) {
					resultSet.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		for (Student student : list) {
			System.out.println(student);
		}
		
	}
	
```

#### 4.2 测试二

```java
package com.jdbc.test;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.jdbc.javabean.Student;
import com.jdbc.util.JdbcUtil;
/**
 * 结果集测试
 * @author Xiangxiang
 *	@create 2020
 */
public class ResultSetTest {
/**
 * +----+------+--------+--------+-------+
| id | name | grade  | gender | score |
+----+------+--------+--------+-------+
|  1 | 相相      | 七年级 |     男           |    98 |
|  2 | 吱吱      | 八年级 |     女           |  93.5 |
|  3 | 王伟      | 六年级 |     男           |    83 |
|  4 | 小刚      | 四年级 |     男           |  95.5 |
|  5 | 小刚      | 四年级 |     男           |  95.5 |
+----+------+--------+--------+-------+
 */
	
	@Test
	/**
	 * 反射,通过属性反过来找对象
	 */
	public void test4() {
		List<Student> list = new ArrayList<>();
		String sql = "select id , name , grade ,gender , score from students";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			connection = JdbcUtil.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();//获得虚表结构
			while(resultSet.next()) { //遍历每一行
				Student student = new Student(); //创建实体对象,每一条对象有对应数据
			//获取标签名
			for(int i = 0 ; i < metaData.getColumnCount();i++) {
				String label = metaData.getColumnName(i+1);
				Object value = resultSet.getObject(label); //通过标签可以找到属性值
				//通过属性名获取属性定义对象
				Field field = Student.class.getDeclaredField(label);
				field.setAccessible(true);//暴力反射
				field.set(student, value);//通过反射来对对象赋值
			}
			list.add(student);
		}
			
			for (Student student : list) {
				System.out.println(student);
			}
			
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(resultSet != null) {
					resultSet.close();
				}
				JdbcUtil.close(connection, preparedStatement);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		
	}
	
```

#### 4.3 测试三

另一种获取Class对像的方式,为后面泛型引入以及封装做好准备 Class<Student> clazz = Student.class

```java
package com.jdbc.test;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.jdbc.javabean.Student;
import com.jdbc.util.JdbcUtil;
/**
 * 结果集测试
 * @author Xiangxiang
 *	@create 2020
 */
public class ResultSetTest {
/**
 * +----+------+--------+--------+-------+
| id | name | grade  | gender | score |
+----+------+--------+--------+-------+
|  1 | 相相      | 七年级 |     男           |    98 |
|  2 | 吱吱      | 八年级 |     女           |  93.5 |
|  3 | 王伟      | 六年级 |     男           |    83 |
|  4 | 小刚      | 四年级 |     男           |  95.5 |
|  5 | 小刚      | 四年级 |     男           |  95.5 |
+----+------+--------+--------+-------+
 */
	@Test
	/**
	 * 另一种获取Class对像的方式,为后面泛型引入以及封装做好准备 Class<Student> clazz = Student.class
	 */
	public void test5() {
		List<Student> list = new ArrayList<>();
		String sql = "select id , name , grade ,gender , score from students";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Class<Student> clazz = Student.class;
		try {
			connection = JdbcUtil.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();// 获得虚表结构
			while (resultSet.next()) { // 遍历每一行
				Student instance = clazz.newInstance(); // 获取对象
				for (int i = 0; i < metaData.getColumnCount(); i++) {
					String label = metaData.getColumnName(i + 1);
					Object value = resultSet.getObject(label); // 通过标签可以找到属性值
					// 通过属性名获取属性定义对象
					Field field = clazz.getDeclaredField(label);
					field.setAccessible(true);// 暴力反射
					field.set(instance, value);// 通过反射来对对象赋值
				}
				list.add(instance);
			}

			for (Student student : list) {
				System.out.println(student);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				JdbcUtil.close(connection, preparedStatement);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}

	}
```

#### 4.4 最后的封装以及测试四

##### 对上述操作进行封装

```java
package com.jdbc.util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * 
 * @author Xiangxiang
 *	@create 2020
 */
public class CommonUtil {
	
	public static <T> List<T> getList(Connection connection ,Class<T> clazz , String sql) throws Exception{
		List<T> list = new ArrayList<>();
		connection = JdbcUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();
			
			while(resultSet.next()) {
				T instance = clazz.newInstance();
				for(int i = 0 ; i < metaData.getColumnCount();i++) {
					String label = metaData.getColumnLabel(i+1);
					Object value = resultSet.getObject(label);
					Field field = clazz.getDeclaredField(label);
					field.setAccessible(true);//暴力反射,要不然不会有效果
					field.set(instance, value);
				}
				list.add(instance);
			}
			
			
		} catch (Exception e) {
			// TODO: handle exception
		}finally {
			try {
				if(resultSet != null) {
					resultSet.close();
				}
				JdbcUtil.close(connection, preparedStatement);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return list; //返回一个集合
	}
	
```

##### 测试封装

```java
package com.jdbc.test;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.jdbc.javabean.Student;
import com.jdbc.util.CommonUtil;
import com.jdbc.util.JdbcUtil;
/**
 * 结果集测试
 * @author Xiangxiang
 *	@create 2020
 */
public class ResultSetTest {
/**
 * +----+------+--------+--------+-------+
| id | name | grade  | gender | score |
+----+------+--------+--------+-------+
|  1 | 相相      | 七年级 |     男           |    98 |
|  2 | 吱吱      | 八年级 |     女           |  93.5 |
|  3 | 王伟      | 六年级 |     男           |    83 |
|  4 | 小刚      | 四年级 |     男           |  95.5 |
|  5 | 小刚      | 四年级 |     男           |  95.5 |
+----+------+--------+--------+-------+
 */
	@Test
	/**
	 * 兄弟萌,终于完了,哈哈哈哈哈哈哈哈哈,最后一个测试
	 * 
	 * 封装测试
	 */
	public void test6() {
		try {
			List<Student> list = CommonUtil.getList(JdbcUtil.getConnection(), Student.class, "select id , name , grade , gender ,score from students");
			for (Student student : list) {
				System.out.println(list);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
```

---

### 五、JdbcUtil以及CommonUtil

#### 4.1 JdbcUtil源码

```java
package com.jdbc.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
	
	public static void close(Connection connection, Statement preparedStatement) {
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

```

#### 5.2 CommonUtil源码

```java
package com.jdbc.util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * 
 * @author Xiangxiang
 *	@create 2020
 */
public class CommonUtil {
	
	public static <T> List<T> getList(Connection connection ,Class<T> clazz , String sql) throws Exception{
		List<T> list = new ArrayList<>();
		connection = JdbcUtil.getConnection();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();
			
			while(resultSet.next()) {
				T instance = clazz.newInstance();
				for(int i = 0 ; i < metaData.getColumnCount();i++) {
					String label = metaData.getColumnLabel(i+1);
					Object value = resultSet.getObject(label);
					Field field = clazz.getDeclaredField(label);
					field.setAccessible(true);//暴力反射,要不然不会有效果
					field.set(instance, value);
				}
				list.add(instance);
			}
			
			
		} catch (Exception e) {
			// TODO: handle exception
		}finally {
			try {
				if(resultSet != null) {
					resultSet.close();
				}
				JdbcUtil.close(connection, preparedStatement);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return list; //返回一个集合
	}
	
	
	/**
	 * 通用的跟新操作
	 * 
	 * @param connection 连接
	 * @param sql 可以执行的sql短语 除select外的DML和DDL
	 * @param args 对象数组
	 * @return rows 执行操作所作用的行数
	 * @throws Exception 抛整个异常
	 */
	public static int update(Connection connection, String sql, Object... args) throws Exception {
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = connection.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				preparedStatement.setObject(i + 1, args[i]);// 将数组元素依次插入
			}
			int rows = preparedStatement.executeUpdate(); // 真正的执行sql
			return rows;

		} finally {
			JdbcUtil.close(connection, preparedStatement);
		}

	}
}

```









----

总算写完了,嘻嘻.....