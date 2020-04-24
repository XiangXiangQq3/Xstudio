## SpringBoot-小小项目

- [需求说明]:通过SpringBoot可以在Web地址栏发送请求，获得一个Json字符串

  使用的知识有：Maven,Spring,SpringBoot,SpringMVC,Web(a little)..等相关注解

  --Idea

----

### 一、在Module中新建Maven工程

#### 1>其中pom.xml配置如下（POM =>Project Object Mode,项目对象模型）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.test.springweb</groupId>
    <artifactId>springweb</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <!--父类GAV-->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.2.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <!--导入Web模块-->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <!--将上面的parent改成web即可-->
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

</project>

<!-- 配置完成！！！！！-->
```

#### 2>在main-resources添加Tomcat配置文件--application.properties(一定要叫这个名字！)

在application.properties里,配置连接网页的端口,线程,字符编码等东西

```properties
server.server.context-pat=/
server.port=12510
server.server.session.timeout=60
server.tomcat.max-threads=800
server.tomcat.uri-encoding=utf-8
```

#### 3>在main中-java-com.springweb包下建一个SpringBoot类

```java
package com.springweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Xiangxiang
 * @date 2020/1/13 22:35
 */
@SpringBootApplication
public class SpringBoot {
    public static void main(String[] args) {
        SpringApplication.run(SpringBoot.class,args);
    }
}

```

![](F:\上课\服务启动成功.png)

### 二、SpringBoot代码

#### 1>com.springweb.entities

实体包下建Student类

```java
package com.springweb.entities;

/**
 * @author Xiangxiang
 * @date 2020/1/13 22:35
 */
public class Student {
    private Integer id;
    private String name;
    private String grade;
    private double score;

    public Student() {
    }

    public Student(Integer id, String name, String grade, double score) {
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.score = score;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", grade='" + grade + '\'' +
                ", score=" + score +
                '}';
    }
}

```

#### 2>com.springweb.controller

控制器代码：

```java
*package com.springweb.controller;

import com.springweb.entities.Student;
import com.springweb.service.StudentInter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Xiangxiang
 * @date 2020/1/13 22:34
 */
@Controller
public class StudentController {

    @Autowired //依赖注入注解  -- 有点类似于面向对象中的对象关联
    private StudentInter studentInter;
    //发送Web请求  使用到SpringMVC注解
    @ResponseBody
    @RequestMapping(value = "/getStudent") //请求映射 ,在Web中输入,将会给Web返回一个Json格式的字符串
    public Student getStudent(@RequestParam("id")/*映射请求参数,在Web中赋值*/ Integer id){
*        Student student = studentInter.getStudentId(id);//通过id 返回一个Student对象
        return  student;
    }
```

#### 3>com.springweb.service

Service层代码：

##### *StudentInter接口

```java
package com.springweb.service;

import com.springweb.entities.Student;

/**
 * @author Xiangxiang
 * @date 2020/1/13 22:35
 */
//接口实现类 从Service层获取对象属性
public class StudentService implements StudentInter{

    @Override
    public Student getStudentId(Integer id) {
        Student student = new Student(id,"相相","高三",500);
        return student;
    }
}

```

##### *StudentService类

```java
package com.springweb.service;

import com.springweb.entities.Student;

/**
 * @author Xiangxiang
 * @date 2020/1/13 22:35
 */
//接口实现类 从Service层获取对象属性
@Service
public class StudentService implements StudentInter{

    @Override
    public Student getStudentId(Integer id) {
        Student student = new Student(id,"相相","高三",500);
        return student;
    }
}

```

-----



### 三、运行结果

从SpringBoot的主函数Run进入



#### 1>报错白页分析

![](F:\上课\报错.png)









修改方式：clean下Maven,从新在Maven中测试

**很重要！！！！我改了一晚上才发现！**







#### 2>最终结果

![](F:\上课\结果.png)