## Hadoop概要

### 一、Hadoop简介

1.Hadoop是Apache基金会所维护的分布式系统的基础框架

2.主要解决海量的数据存储和海量的数据分析计算问题

3.广义上来说,Hadoop通常是指一个更加广泛的概念--Hadoop生态圈

### 二、Hadoop优势

1.高可靠性:其底层维护多个数据副本,所以即使其中某个计算元素或存储出现故障,也不会导致数据的损失

2.高扩展性：在各个集群间分配任务数据,可以方便扩展

3.高效性：在MapReduce的思想下,Hadoop是并行工作的，可以加快任务的处理速度

4.高容错性：能够自动将失败的任务重新分配

### 三、Hadoop组成

这里所说的是Hadoop2.x

**MapReduce(计算)**

**Yarn（资源调度）**

**HDFS(数据存储)**

**Common(辅助工具)**

#### 1.HDFS架构

**1>NameNode(NN)：**存储文件的元数据，如文件名，文件目录，文件属性，以及每个文件的块列表和所在的DataNode等.

**2>DataNode(DN):**在本地文件系统存储文件块数据,以及块数据的校验和.

**3>Secondary NameNode(2NN):**用来监视HDFS状态的辅助后台程序.

#### 2.YARN架构

调度内存和CPU  资源池  多个机器资源做统一调度

**1>RecouceManger (RM):**	

​	(1)处理客户端请求的作业提交

​	(2)监控NodeManger

​	(3)启动或监控ApplicationMaster

​	(4)资源的分配和调度

**2>NodeManger(NM):**

​	(1)管理单个节点上的资源(**管理自己机器的资源，并把信息报给RM)**

​	(2)处理来自RM的命令

​	(3)处理来自AM的命令

**3>ApplicationMaster(AM):**

​	(1)负责数据的切分

​	(2)为应用程序申请资源并分配给内部任务(**<u>跑任务时的推进者，由AM估算客户端任务需要多少资源，然后向RM申请资源，RM告诉每个NM执行启动Container，RM以Container形式划分给AM，AM再进行资源的处理</u>**)

​	(3)任务的监控与容错

**4>Container:**

​	是Yarn上的资源抽象，封装了某个节点上的维度资源，如内存，cpu，网络等.(**单位,一种容器**)

#### 3.MapReduce架构

MapReduce将计算分为两个阶段,Map和Reduce.

​	(1)Map阶段并行处理输入的数据.

​	(2)Reduce阶段对Map结果进行汇总.