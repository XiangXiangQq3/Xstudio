## MapRecuce-自定义InputForMat操作（本地模式）

**没有提交到yarn**

### 一、需求

**要求，将三个文件合并成为一个完整的新二进制内容文件（自定义合并小文件）**

**Key为路径，value为文件内容**

**步骤如下**

1.自定义一个类继承FileInputFormat

2.改下打碎数据RecordReader,封装成KV值

3.在输出时使用SecondFileOutPutFormat输出合并文件

### 二、准备内容

#### 输入内容：准备三个Famous文件写入D:/Input，里面分别写的是古代名人姓名**.

![](C:\Users\Dell\Desktop\新建input文件.png)

![](C:\Users\Dell\Desktop\准备材料.png)

#### 希望输出文件格式：以二进制为内容的part-r-00000合并文件

### 三、源码

#### 1.配置文件

##### 1>.pom.xml , 配置hadoop jar

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.atguigu</groupId>
    <artifactId>mapreduce</artifactId>
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

</project>
```

##### 2>properties配置

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

#### 2.自定义InPutForMat

##### 1>MyInputFormat

```java
package com.hadoop.mapreduce.myinputformat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import java.io.IOException;

/**
 * @author Xiangxiang
 * @date 2020/2/12 18:54
 * 输入是Text类型
 * 输出的是一个二进制文件
 */
public class MyInputFormat extends FileInputFormat<Text, BytesWritable> {
    //自定义InputFormat调用RecordReader对数据进行打碎

    /**
     *
     * @param split 要处理的切片
     * @param context 环境对象
     * @return 一个自定义的RecordReader
     * @throws IOException
     * @throws InterruptedException
     */
    public RecordReader createRecordReader(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
        return new MyRecordReader();
    }

    @Override
    protected boolean isSplitable(JobContext context, Path filename) {
        return false;
    }
}

```



##### 2>MyRecordReader

```java
package com.hadoop.mapreduce.myinputformat;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.IOException;

/**
 * @author Xiangxiang
 * @date 2020/2/12 18:56
 * 自定义RecordReader 将一整个文件读成一组KV值
 */
public class MyRecordReader extends RecordReader <Text , BytesWritable> {
    //插旗,默认文件是不可读的
    private boolean isRead = false;
    //key对象
    private Text key = new Text();
    //value对象
    private BytesWritable value = new BytesWritable();
    //声明文件切片
    FileSplit fs;
    //声明开关流对象
    FSDataInputStream stream;


    /**
     * 初始化
     * @param split
     * @param context
     * @throws IOException
     * @throws InterruptedException
     */
    public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
        fs = (FileSplit) split;
        //声明文件流 , 传入的是context的配置 这里是本地模式
        FileSystem fileSystem = FileSystem.get(context.getConfiguration());
        //开流
       stream = fileSystem.open(fs.getPath()); //传入文件路径
    }

    /**
     * 实际读文件的过程
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean nextKeyValue() throws IOException, InterruptedException {
        if(!isRead){ //如果第一次没有读
            //进行读取进程
            //读key值
            key.set(fs.getPath().toString());
            //读value值
            //读取文件的内容 , buffer里就是文件内容
            byte[] buffer = new byte[(int) fs.getLength()];
            //读value
            stream.read(buffer);
            //封装
            value.set(buffer, 0 ,buffer.length);
            //置为true
            isRead = true;
            return true ; // 返回true,表示已经读过了
        }else {
            return false;
        }
    }

    public Text getCurrentKey() throws IOException, InterruptedException {
        return key;
    }

    public BytesWritable getCurrentValue() throws IOException, InterruptedException {
        return value;
    }

    public float getProgress() throws IOException, InterruptedException {
       return isRead ? 1 : 0 ;  //如果已经读完 , 则返回 1 ，否则返回 0
    }

    public void close() throws IOException {
        //关流
        IOUtils.closeStream(stream);
    }
}

```



##### 3>MyInputFormatDriver

```java
package com.hadoop.mapreduce.myinputformat;
        import org.apache.hadoop.conf.Configuration;
        import org.apache.hadoop.fs.Path;
        import org.apache.hadoop.io.BytesWritable;
        import org.apache.hadoop.io.Text;
        import org.apache.hadoop.mapreduce.Job;
        import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
        import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
        import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

        import java.io.IOException;

/**
 * Driver客户端配置
 * 以及提交任务
 * Driver 1.调用 inputfomat的方法将数据进行切片(发送在切片客户端) ，每一个分开好的切片会被一个mastk处理
 * 然后
 * 2.maptask 处理切片 在inputformat中 ，调用第二个方法（用createrecordreader） 将数据打碎
 * maptask将获取的kv值导入mapper mapper....
 * @author Xiangxiang
 * @date 2020/2/12 18:57
 */
public class MyInputFormatDriver  {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Job job = Job.getInstance(new Configuration());//获取任务对象,使用默认配置
        job.setJarByClass(MyInputFormatDriver.class); //设置导jar

        job.setMapOutputKeyClass(Text.class); //Map的Key值输出类型
        job.setOutputValueClass(BytesWritable.class); //Map的Value值输出类型
        job.setOutputKeyClass(Text.class); //reduce结果输出key
        job.setOutputValueClass(BytesWritable.class); //reduce结果输出value

        //配置使用自定义的MyInputFormat
        job.setInputFormatClass(MyInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        //设置盘 输入输出路径
        FileInputFormat.setInputPaths(job,new Path("d:/input"));
        FileOutputFormat.setOutputPath(job,new Path("d:/output/reslut"));

        boolean b = job.waitForCompletion(true);//真正提交任务
        System.out.println(b ? "成功" : "失败");  //执行成功失败判断


    }

}

```

### 四、输出结果

##### 1>控制打印结果

![](C:\Users\Dell\Desktop\运行结果.png)

##### 2>D盘输出output

![](C:\Users\Dell\Desktop\输出output文件.png)

![](C:\Users\Dell\Desktop\result.png)

##### 3>二进制内容

![](C:\Users\Dell\Desktop\三个txt格式归并到一个二进制文件.png)