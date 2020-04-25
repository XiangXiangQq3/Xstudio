## HDFS客户端操作以及测试结果

### 一、HDFS客户端环境准备

**需要在Windows配置Hadoop环境**

![](C:\Users\Dell\Desktop\环境配置.png)

**启动hadoop102的hdfs**

![](C:\Users\Dell\Desktop\启动NN.png)

### 二、源码实现

#### 1.配置pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.atguigu</groupId>
    <artifactId>hdfs191122</artifactId>
    <version>1.0-SNAPSHOT</version>
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.8.2</version>
        </dependency>
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-client</artifactId>
            <version>2.7.2</version>
        </dependency>
    </dependencies>
```

#### 2.porperties

```properties
log4j.rootLogger=INFO, stdout
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d %p [%c] - %m%n
log4j.appender.logfile=org.apache.log4j.FileAppender
log4j.appender.logfile.File=target/spring.log
log4j.appender.logfile.layout=org.apache.log4j.PatternLayout
log4j.appender.logfile.layout.ConversionPattern=%d %p [%c] - %m%n
```

#### 3.客户端实现

```java
package com.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.io.IOException;
import java.net.URI;

/**
 * @author Xiangxiang
 * @date 2020/2/13 17:44
 * HDFS客户端的增删改查
 */
public class HDFSClient {
    private FileSystem fileSystem;

    /**
     * 创建集群的抽象对象
     */
    @Before
    public void before() throws IOException, InterruptedException {
        //获取URI,使用默认配置,用户名
        fileSystem = FileSystem.get(URI.create("hdfs://hadoop102:9000"),
                        new Configuration(),"xiangxiang");
    }

    //使用对象操作集群本身
    @Test
    public void mkdir() throws IOException { //创建目录
        fileSystem.mkdirs(new Path("/newDirectory"));
    }
    @Test
    /**
     * 修改默认配置,在本地上传文件给HDFS,配置两个DN存储
     */
    public void testConfiguration() throws IOException, InterruptedException {
        Configuration entries = new Configuration();
        entries.set("dfs.replication","2");
        fileSystem = FileSystem.get(URI.create("hdfs://hadoop102:9000"),
                entries,"xiangxiang");
        fileSystem.copyFromLocalFile(new Path("d:/input/Famous.txt"),new Path("/Famous.txt"));
    }
    @Test //下载文件到本地  会产生crc校验文件
    public void get() throws IOException {
        fileSystem.copyToLocalFile(new Path("/Famous.txt") , new Path("d:/"));
    }

    @Test
    public void rename() throws IOException {
        fileSystem.rename(new Path("/Famous.txt"),new Path("/FamousName"));
    }

    @Test
    /**
     * 相当于执行hadoop fs -ls /
     */
    public void ls() throws IOException {
        //返回一个数组
        FileStatus[] fileStatuses = fileSystem.listStatus(new Path("/"));
        for (FileStatus fileStatus : fileStatuses) {
            if(fileStatus.isDirectory()){
                System.out.println("是文件夹");
                System.out.println(fileStatus.getPath());
            }else {
                System.out.println("不是文件夹");
            }
        }
    }
    @Test
    public void lf() throws IOException {
        RemoteIterator<LocatedFileStatus> Iterator = fileSystem.listFiles(new Path("/"), true);
            while(Iterator.hasNext()){
                LocatedFileStatus file = Iterator.next();
                System.out.println(file.getPath());
                System.out.println("---------------------------------");
                //可以通过file获取块信息
                BlockLocation[] blockLocations = file.getBlockLocations();
                for (BlockLocation blockLocation : blockLocations) {
                    System.out.println(blockLocation.toString());
                }
            }
    }

    @Test
    /**
     * cat
     */
    public void cat() throws IOException {
        //开流
        FSDataInputStream inputStream = fileSystem.open(new Path("/FamousName"));
        //将流拷贝到屏幕上
        IOUtils.copyBytes(inputStream,System.out,1024);
        //关流
        IOUtils.closeStream(inputStream);
    }

    @After
    public void after() throws IOException {
        fileSystem.close();
    }
}

```



### 三、运行结果

#### 1.mkdir

![](C:\Users\Dell\Desktop\运行结果.png)

#### 2.testConfiguration

![](C:\Users\Dell\Desktop\2个集群上传.png)

#### 3.get()

![](C:\Users\Dell\Desktop\D盘输出.png)

#### 4.rename

![](C:\Users\Dell\Desktop\修改名称.png)

#### 5.ls

![](C:\Users\Dell\Desktop\ls方法.png)

#### 6.lf

![](C:\Users\Dell\Desktop\获取块信息.png)

#### 7.cat

![](C:\Users\Dell\Desktop\cat.png)