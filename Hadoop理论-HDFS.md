## **Hadoop**理论-HDFS

### 一、HDFS概述

**Hadoop Distributed File System** 

数据的海量需要一种新的系统来管理多台机器上的文件，这就是分布式文件管理系统.

HDFS就是其中的一种.

**HDFS使用场景:**适合一次写入,多次读出的场景,且不支持文件的修改，很适合做数据的分析,不适合做网盘.

### 二、HDFS组成架构

**1>NameNode(NM):Master,是一个管理者**

1. 管理HDFS的名称，
2. 配置副本策略，
3. 管理数据块的映射信息，
4. 处理客户端读写请求.

**2>DataNode(DN):Slave,NM下达命令,DN执行实际操作**

1. 存储实际的数据块，
2. 执行数据块的读写操作.

**3>Client:客户端**

1. 文件切分。文件上传HDFS时,客户端会对将文件进行切分,然后进行上传,
2. 与NN交互,获取文件的位置信息，
3. 与DN交互,读取或者写入数据，
4. 客户端会提供一些命令**管理HDFS,例如格式化**，
5. 客户端会提供一些命令来**访问HDFS,例如对HDFS增删改操作**.

**4>Second NameNode:***

1. 辅助NM,定期合并Fsimage和Edits,并推送给NN
2. 紧急状态下,可以恢复NN

---



**HDFS块大小**

HDFS中的文件在物理上是分块存储,默认大小为128M.

块大小可以通过配置改变，寻址时间和磁盘传输数据时间影响着块的大小

------



### 三、HDFS数据流

#### 1.HDFS的写数据流程

![](C:\Users\Dell\Desktop\u=2684548248,192409765&fm=15&gp=0.jpg)

1. 客户端通过Distributed FileSystem向NN请求上传文件,NN检查目标文件是否已经存在,
2. NN返回是否可以上传,
3. 客户端请求将切分好的第一个block上传，向NN请求返回DN,
4. NN向客户端返回DN,图示为DN1,DN2,DN3,
5. 客户端通过FSDataOutputStream请求DN1上传数据,DN2收到请求会继续调用DN2,DN2收到请求会继续调用DN3，然后通信管道建立完成.   (**这里的管道是一个串行的,一个不通,客户端会找下一个DN,只要一个存活任务就会进行)**,
6. DN1,DN2,DN3会逐级响应客户端,
7. 客户端开始传输第一个Block(从磁盘读取数据放到一个本地内存缓存),以packet为单位,DN1收到就会传给DN2,DN2再会传给DN3
8. 当第一个Block传输完成后,客户端会再次请求NN上传第二个Block服务器.

----------------------------------------------------------------------------------------------------------------------------------------------------

##### HDFS在写入数据过程时的节点选择：

**1>第一个DN时**

在HDFS写数据的过程中,NN会选择距离上传距离最近的DN接受数据.

计算距离时使用的是**拓扑-节点距离**

**2>副本节点的选择**

**机架感知:**

For the common case, when the replication factor is three, HDFS’s placement policy is to put one replica on the local machine if the writer is on a datanode, otherwise on a random datanode, another replica on a node in a different (remote) rack, and the last on a different node in the same remote rack. This policy cuts the inter-rack write traffic which generally improves write performance. The chance of rack failure is far less than that of node failure; this policy does not impact data reliability and availability guarantees. However, it does reduce the aggregate network bandwidth used when reading data since a block is placed in only two unique racks rather than three. With this policy, the replicas of a file do not evenly distribute across the racks. One third of replicas are on one node, two thirds of replicas are on one rack, and the other third are evenly distributed across the remaining racks. This policy improves write performance without compromising data reliability or read performance.

**这里是截取自官网 机架感知描述**

------



#### 2.HDFS的读数据流程

1. 客户端通过Distributed FileSystem向NN发送请求下载文件,NN通过查询元数据,找到文件块的DN所在地址,
2. 选择一台DN(就近),请求读取数据,
3. DN开始传输文件给客户端(在磁盘中读取数据输入流,以packet为单位传输),
4. 客户端接受packet,先在本地缓存,然后写入目标文件.

### 四、NN与2NN的关系

#### 背景：

​		元数据是存放再内存中的.如果只存放再内存中,元数据一但丢失,整个集群就无法工作了.

因此会在磁盘中备份元数据的FsImage.

​		**但是**,如果内存元数据更新时,会同时跟新FsImage,这样会导致效率过低.但如果不跟新就会产生一致性出错.

因此,引入Edits(只保存操作),每当元数据有更新或添加元数据时,修改内存中的元数据并且将操作保留再Edits里.

通过FsImage和Edits的合并,可以形成元数据.

​		**但是**,如果长时间的添加数据到Edits中,会导致该文件数据过大,效率降低.所以要定期的对FsImage和Edits进行合并,如果只有DN来完成的话,效率也会很低.所以就需要一个新的节点专门处理FsImage和Edits的合并.

#### 图示：

![](C:\Users\Dell\Desktop\u=406611112,1151152258&fm=26&gp=0.jpg)

#### 解释：

##### 第一阶段：NN启动

1. 第一次启动NN格式化后,创建Fsimage和Edits文件.(如果不是第一次启动,直接加载编辑日志和镜像文件到内存),
2. 客户端对元数据进行增删改的请求，
3. NN记录先操作日志,跟新滚动日志,
4. NN再内存中对元数据进行增删改(**安全性考虑**).

##### 第二阶段:2NN工作

1. 2NN询问NN是否需要CheckPoint.直接待会NN的检查结果.(**一分钟一次**),
2. 2NN请求执行CheckPoint,
3. (执行2NN)NN继续滚动正在写的日志,
4. NN将滚动前的Edits(**改名后**)以及Fsimage拷贝到2NN,
5. 2NN加载编辑日志和镜像文件到内存,
6. 2NN生成新的镜像文件Fsimage.checkpoint,
7. 将Fsimage.checkpoint拷贝到NN,
8. NN将Fsimage.checkpoint重命名Fsimage.

**CheckPoint触发条件：**1.定时时间到了(一小时一次),2.Edits中的数据满了 

### 五、DN的工作机制

1. 一个数据块在DN上以文件形式存储在磁盘上,包含两个文件,一个是数据本身,一个是元数据(包括数据块的长度,块数据,块数据的校验和,和时间戳)，

2. DN启动后向NN注册,通过后,周期性(1小时)的向NN报备所有的块信息,

3. 心跳每3s一次,每一次心跳都带有NN给DN的命令,

4. 如果超过10分30秒没有收到DN的心跳,则NN认为改节点不可用.

   