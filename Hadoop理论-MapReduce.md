## Hadoop理论-MapReduce

### 一、MapReduce概述

#### 1.简述

MapReduce是**分布式运算程序的编程框架**,

其核心功能是将**用户编写的业务逻辑**和**自带默认组件**整合成一个完整的**分布式运算程序**,并发运算在一个Hadoop集群上.

#### 2.MapReduce核心思想



![](C:\Users\Dell\Desktop\微信图片_20200216122252.jpg)

**解析:**

1. 全程序分为两个阶段,**Map阶段和Reduce阶段**,
2. Map阶段MapTask是并发的,并行运行,不相干预,
3. Reduce阶段ReduceTask是并发的,互不相干,输入的结果依赖于MapTask所输出.



**后面会对各个重要的阶段进行更为详细的讲述.!**



### 二、MapReduce框架原理

![](C:\Users\Dell\Desktop\微信图片_20200216122252.jpg)

#### 1.InputFormat数据的输入

##### 1>MapTask的并行度决定Map阶段的任务处理并发度,会影响整个Job的处理速度.

**数据切片与MapTask并行度决定机制**

1. 一个Job的Map阶段并行度由客户端在提交Job时的**切片数**决定,
2. 每一个Split切片会分配一个MapTask进行处理,
3. 一般而言,**分块大小 = 切片大小**,
4. 切片不考虑数据集整体,而是针对每一个文件进行单独切片.

##### **2>Job提交流程概述**

通过看源码,可知Job端干了什么,下面是一些重要的总结.

1. 建立连接过程

   ​	1.创建提交Job的代理,

   ​	2.判断是本地Yarn还是远程.

2. 提交Job

   1. 创建给集群提交数据的路径,会在盘的根目录下生成一个tmp文件
   2. 获取Job的Id,并且创建Job的路径,
   3. 将jar包拷贝到集群,**会产生jar包**,
   4. 计算切片,调用InputFormat.getsplit()方法获取切片规划,**并将切片规则信息生成文件**,
   5. 向tmp中写入**xml配置文件的信息,**
   6. 提交Job,返回提交的状态.

**最终会在tmp中得到三个东西,**

Job.split 

Job.xml

Xxx.Jar

##### 3>InputFormat任务流程

**客户端会调用InputFormat得第一个方法,将数据切成很多分,每一份叫做.split，**

**后续工作中,maptask会调用InputFormat的第二个方法RecordReader,通过RecordReader将切片打碎变成KV值,**

**将KV值传入Mapper.**

##### 4>总结

简单的理解就是InputFormat会干两件事情,**第一个**就是将文件进行切片,客户端Job的提交时,会调用切片规划信息,

**第二个**是InputFormat里有RecordReader方法,可以将切片打碎为KV值得形式,传入Mapper,Mapper后面会有新的操作.

#### 2.Shuffle机制

##### 1.Shuffle简述

Map方法之后,Reduce方法之前的数据处理过程称为Shuffle.

##### 2.Shuffle原理图示

![](C:\Users\Dell\Desktop\微信图片_20200216122243.jpg)

##### 3.Shuffle流程详解

1. MapTask收集会调用Mapper中的Map方法输出里面的KV值,放入到环形缓冲区,
2. 环形缓冲区中左侧有内部索引,分区号,和一些配置信息;右侧是一组一组输入的KV值,当缓冲区到达80%时就会发生溢写,溢写的过程会进行**第一次溢出性的根据索引信息分区排序**,之后发生第二次溢写....
3. 当溢出多个文件时,会在**进行一次分区性质的归并排序**,将多个溢出的文件(第一次,第二次,第三次)进行**所谓的第二次归并排序**，
4. **第三次归并排序是发生在**,ReduceTask会将多个MapTask中已经归并好的文件在进行一次各个MapTask分区性质的归并排序,合并成一个大文件.
5. **到此,Shuffle过程就结束了**,后面ReduceTask会将KV值进行分组,然后调用Reducer的Reduce()方法对数据进行汇总,进行Reduce()业务逻辑的处理.

**注解：**shuffle阶段为了提高数据处理的效率,可以在MapTask阶段进行数据Combiner处理.

Combinner的父类是Reducer,所以**要求传入的数据必须有序**,所以这要求Combiner应用于Map阶段的排序完成后,也就前两次排序.注意的是Combinner是可选的,在不影响业务逻辑时,才能使用.

#### 3.OutputFormat数据输出

OutputFormat是MapReduce输出的基类,所有实现MapReduce输出都实现了OutFormat接口.

ReduceTask通过调用Reduce()对已经汇总好的数据,通过OutFormat输出写成文件,将结果传写到HDFS上.

-------

**总结：** 综上整个MapReduce较为详细的讲解就结束了...**需要好好理解**,**Shuffle阶段很重要！**